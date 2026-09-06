from typing import List, Optional, Set
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import or_, and_, desc

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.profile_model import StudentProfile
from app.models.connection_model import Connection
from app.models.referral_model import ReferralRequest
from app.schemas.connection_schema import (
    ConnectionResponse,
    ConnectionUserSummary,
    ConnectionStatusResponse,
    ConnectionSuggestionResponse,
    NetworkSummaryResponse,
)
from app.auth.jwt_dependency import get_current_user
from app.services.notification_service import create_notification

router = APIRouter()


def build_user_summary(db: Session, user: User) -> ConnectionUserSummary:
    """Builds a normalized ConnectionUserSummary from User and corresponding Profile/Alumni tables."""
    summary = ConnectionUserSummary(
        user_id=user.id,
        name=user.name,
        email=user.email,
        role=user.role,
        is_verified=False
    )

    if user.role.lower() == "alumni":
        alumni_rec = db.query(Alumni).filter(
            or_(Alumni.user_id == user.id, Alumni.email == user.email)
        ).first()
        if alumni_rec:
            summary.company = alumni_rec.company
            summary.job_role = alumni_rec.job_role
            summary.department = alumni_rec.department
            summary.graduation_year = alumni_rec.graduation_year
            summary.location = alumni_rec.location
            summary.headline = f"{alumni_rec.job_role or 'Alumnus'} {f'at {alumni_rec.company}' if alumni_rec.company else ''}".strip()
            summary.is_verified = bool(getattr(alumni_rec, "is_verified", False))
    elif user.role.lower() == "student":
        student_rec = db.query(StudentProfile).filter(StudentProfile.user_id == user.id).first()
        if student_rec:
            summary.department = student_rec.branch
            summary.graduation_year = student_rec.year
            summary.avatar_url = student_rec.profile_image_url
            summary.headline = f"Student • {student_rec.branch or 'General'}{f' ({student_rec.year})' if student_rec.year else ''}"

    return summary


def serialize_connection(db: Session, conn: Connection) -> ConnectionResponse:
    sender_summary = build_user_summary(db, conn.sender) if conn.sender else None
    receiver_summary = build_user_summary(db, conn.receiver) if conn.receiver else None
    return ConnectionResponse(
        id=conn.id,
        sender_id=conn.sender_id,
        receiver_id=conn.receiver_id,
        status=conn.status,
        created_at=conn.created_at,
        updated_at=conn.updated_at,
        sender=sender_summary,
        receiver=receiver_summary,
    )


# 1. Send Connection Request
@router.post("/request/{user_id}", response_model=ConnectionResponse, status_code=status.HTTP_201_CREATED)
def send_connection_request(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if user_id == current_user.id:
        raise HTTPException(status_code=400, detail="You cannot connect with yourself.")

    target_user = db.query(User).filter(User.id == user_id, User.is_active == True).first()
    if not target_user:
        raise HTTPException(status_code=404, detail="Target user not found or is inactive.")

    # Check existing connection between the two users in either direction
    existing = db.query(Connection).filter(
        or_(
            and_(Connection.sender_id == current_user.id, Connection.receiver_id == user_id),
            and_(Connection.sender_id == user_id, Connection.receiver_id == current_user.id)
        )
    ).first()

    if existing:
        if existing.status == "ACCEPTED":
            raise HTTPException(status_code=409, detail="You are already connected with this user.")
        elif existing.status == "PENDING":
            if existing.sender_id == current_user.id:
                raise HTTPException(status_code=409, detail="Connection request already sent and pending.")
            else:
                raise HTTPException(status_code=409, detail="This user has already sent you a connection request. Please accept it.")
        else:
            # If REJECTED or CANCELLED, allow re-requesting by updating record
            existing.sender_id = current_user.id
            existing.receiver_id = user_id
            existing.status = "PENDING"
            existing.updated_at = datetime.utcnow()
            db.commit()
            db.refresh(existing)

            create_notification(
                db=db,
                user_id=user_id,
                title="New Connection Request",
                message=f"{current_user.name} sent you a connection request.",
                notification_type="CONNECTION"
            )
            return serialize_connection(db, existing)

    new_conn = Connection(
        sender_id=current_user.id,
        receiver_id=user_id,
        status="PENDING",
        created_at=datetime.utcnow(),
        updated_at=datetime.utcnow()
    )
    db.add(new_conn)
    db.commit()
    db.refresh(new_conn)

    create_notification(
        db=db,
        user_id=user_id,
        title="New Connection Request",
        message=f"{current_user.name} sent you a connection request.",
        notification_type="CONNECTION"
    )

    return serialize_connection(db, new_conn)


# 2. Get Received Requests (Pending)
@router.get("/received", response_model=List[ConnectionResponse])
def get_received_requests(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    requests = db.query(Connection).filter(
        Connection.receiver_id == current_user.id,
        Connection.status == "PENDING"
    ).order_by(desc(Connection.created_at)).all()

    return [serialize_connection(db, r) for r in requests]


# 3. Get Sent Requests (Pending)
@router.get("/sent", response_model=List[ConnectionResponse])
def get_sent_requests(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    requests = db.query(Connection).filter(
        Connection.sender_id == current_user.id,
        Connection.status == "PENDING"
    ).order_by(desc(Connection.created_at)).all()

    return [serialize_connection(db, r) for r in requests]


# 4. Get My Network (Accepted Connections)
@router.get("/my-network", response_model=List[ConnectionResponse])
def get_my_network(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    connections = db.query(Connection).filter(
        Connection.status == "ACCEPTED",
        or_(Connection.sender_id == current_user.id, Connection.receiver_id == current_user.id)
    ).order_by(desc(Connection.updated_at)).all()

    return [serialize_connection(db, c) for c in connections]


# 5. Accept Connection Request
@router.put("/{connection_id}/accept", response_model=ConnectionResponse)
def accept_connection(
    connection_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    conn = db.query(Connection).filter(Connection.id == connection_id).first()
    if not conn:
        raise HTTPException(status_code=404, detail="Connection request not found.")

    if conn.receiver_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only the recipient can accept this connection request.")

    if conn.status != "PENDING":
        raise HTTPException(status_code=400, detail=f"Cannot accept request with status '{conn.status}'.")

    conn.status = "ACCEPTED"
    conn.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(conn)

    create_notification(
        db=db,
        user_id=conn.sender_id,
        title="Connection Request Accepted",
        message=f"{current_user.name} accepted your connection request.",
        notification_type="CONNECTION"
    )

    return serialize_connection(db, conn)


# 6. Reject Connection Request
@router.put("/{connection_id}/reject", response_model=ConnectionResponse)
def reject_connection(
    connection_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    conn = db.query(Connection).filter(Connection.id == connection_id).first()
    if not conn:
        raise HTTPException(status_code=404, detail="Connection request not found.")

    if conn.receiver_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only the recipient can reject this connection request.")

    if conn.status != "PENDING":
        raise HTTPException(status_code=400, detail=f"Cannot reject request with status '{conn.status}'.")

    conn.status = "REJECTED"
    conn.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(conn)

    return serialize_connection(db, conn)


# 7. Remove or Cancel Connection
@router.delete("/{connection_id}")
def remove_connection(
    connection_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    conn = db.query(Connection).filter(Connection.id == connection_id).first()
    if not conn:
        raise HTTPException(status_code=404, detail="Connection not found.")

    if current_user.id not in (conn.sender_id, conn.receiver_id):
        raise HTTPException(status_code=403, detail="Not authorized to remove this connection.")

    db.delete(conn)
    db.commit()
    return {"message": "Connection removed successfully."}


# 8. Check Connection Status with Target User
@router.get("/status/{user_id}", response_model=ConnectionStatusResponse)
def get_connection_status(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if user_id == current_user.id:
        return ConnectionStatusResponse(user_id=user_id, status="SELF", connection_id=None)

    conn = db.query(Connection).filter(
        or_(
            and_(Connection.sender_id == current_user.id, Connection.receiver_id == user_id),
            and_(Connection.sender_id == user_id, Connection.receiver_id == current_user.id)
        ),
        Connection.status.in_(["PENDING", "ACCEPTED"])
    ).first()

    if not conn:
        return ConnectionStatusResponse(user_id=user_id, status="NOT_CONNECTED", connection_id=None)

    if conn.status == "ACCEPTED":
        return ConnectionStatusResponse(user_id=user_id, status="CONNECTED", connection_id=conn.id)
    elif conn.sender_id == current_user.id:
        return ConnectionStatusResponse(user_id=user_id, status="PENDING_SENT", connection_id=conn.id)
    else:
        return ConnectionStatusResponse(user_id=user_id, status="PENDING_RECEIVED", connection_id=conn.id)


# 9. Connection Suggestions (Rule-based, 0–100 score)
@router.get("/suggestions", response_model=List[ConnectionSuggestionResponse])
def get_connection_suggestions(
    limit: int = 15,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    # 1. Existing connection partner IDs to exclude
    existing_conns = db.query(Connection).filter(
        or_(Connection.sender_id == current_user.id, Connection.receiver_id == current_user.id),
        Connection.status.in_(["PENDING", "ACCEPTED"])
    ).all()

    excluded_user_ids: Set[int] = {current_user.id}
    for c in existing_conns:
        excluded_user_ids.add(c.sender_id)
        excluded_user_ids.add(c.receiver_id)

    # 2. Extract current user attributes
    my_dept = ""
    my_year = ""
    my_company = ""
    my_skills_set: Set[str] = set()
    my_interests_set: Set[str] = set()

    if current_user.role.lower() == "alumni":
        my_alumni = db.query(Alumni).filter(
            or_(Alumni.user_id == current_user.id, Alumni.email == current_user.email)
        ).first()
        if my_alumni:
            my_dept = (my_alumni.department or "").strip().lower()
            my_year = (my_alumni.graduation_year or "").strip()
            my_company = (my_alumni.company or "").strip().lower()
            if my_alumni.skills:
                my_skills_set = {s.strip().lower() for s in my_alumni.skills.split(",") if s.strip()}
    else:
        my_profile = db.query(StudentProfile).filter(StudentProfile.user_id == current_user.id).first()
        if my_profile:
            my_dept = (my_profile.branch or "").strip().lower()
            my_year = (my_profile.year or "").strip()
            if my_profile.skills:
                my_skills_set = {s.strip().lower() for s in my_profile.skills.split(",") if s.strip()}
            if my_profile.interests:
                my_interests_set = {i.strip().lower() for i in my_profile.interests.split(",") if i.strip()}

    # 3. Candidate users (active, not in excluded)
    candidates = db.query(User).filter(
        User.id.notin_(excluded_user_ids),
        User.is_active == True,
        User.role != "admin"
    ).all()

    suggestions: List[ConnectionSuggestionResponse] = []

    for cand in candidates:
        summary = build_user_summary(db, cand)
        score = 15  # Baseline score for active community member
        reasons: List[str] = []

        cand_dept = (summary.department or "").strip().lower()
        cand_year = (summary.graduation_year or "").strip()
        cand_company = (summary.company or "").strip().lower()

        # Query skills for candidate
        cand_skills_str = ""
        if cand.role.lower() == "alumni":
            alumni_row = db.query(Alumni).filter(or_(Alumni.user_id == cand.id, Alumni.email == cand.email)).first()
            if alumni_row and alumni_row.skills:
                cand_skills_str = alumni_row.skills
        else:
            student_row = db.query(StudentProfile).filter(StudentProfile.user_id == cand.id).first()
            if student_row and student_row.skills:
                cand_skills_str = student_row.skills

        cand_skills_set = {s.strip().lower() for s in cand_skills_str.split(",") if s.strip()}

        # Matching factors
        if my_dept and cand_dept and my_dept in cand_dept or cand_dept in my_dept:
            score += 30
            reasons.append(f"Same department: {summary.department}")

        if my_year and cand_year and my_year == cand_year:
            score += 20
            reasons.append(f"Class batch match: {summary.graduation_year}")

        if my_company and cand_company and my_company == cand_company:
            score += 25
            reasons.append(f"Same company: {summary.company}")

        shared_skills = my_skills_set.intersection(cand_skills_set)
        if shared_skills:
            skill_pts = min(25, len(shared_skills) * 10)
            score += skill_pts
            top_shared = list(shared_skills)[:2]
            reasons.append(f"Shared skills: {', '.join(s.title() for s in top_shared)}")

        if my_interests_set and cand_skills_set:
            interest_overlap = my_interests_set.intersection(cand_skills_set)
            if interest_overlap:
                score += 15
                reasons.append(f"Matches your interests: {list(interest_overlap)[0].title()}")

        if not reasons:
            if cand.role.lower() == "alumni":
                reasons.append("Active alumni mentor in network")
            else:
                reasons.append("Active student member in community")

        normalized_score = min(100, score)

        suggestions.append(ConnectionSuggestionResponse(
            user_id=cand.id,
            name=summary.name,
            email=summary.email,
            role=summary.role,
            headline=summary.headline,
            company=summary.company,
            job_role=summary.job_role,
            department=summary.department,
            graduation_year=summary.graduation_year,
            location=summary.location,
            skills=cand_skills_str or None,
            avatar_url=summary.avatar_url,
            is_verified=summary.is_verified,
            suggestion_score=normalized_score,
            suggestion_reasons=reasons
        ))

    suggestions.sort(key=lambda s: s.suggestion_score, reverse=True)
    return suggestions[:limit]


# 10. Personalized Network Summary
@router.get("/network-summary", response_model=NetworkSummaryResponse)
def get_network_summary(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    total_conns = db.query(Connection).filter(
        Connection.status == "ACCEPTED",
        or_(Connection.sender_id == current_user.id, Connection.receiver_id == current_user.id)
    ).count()

    pending_recv = db.query(Connection).filter(
        Connection.receiver_id == current_user.id,
        Connection.status == "PENDING"
    ).count()

    pending_sent = db.query(Connection).filter(
        Connection.sender_id == current_user.id,
        Connection.status == "PENDING"
    ).count()

    referrals_sent = db.query(ReferralRequest).filter(
        ReferralRequest.requester_id == current_user.id
    ).count()

    referrals_recv = db.query(ReferralRequest).filter(
        ReferralRequest.alumni_id == current_user.id
    ).count()

    return NetworkSummaryResponse(
        total_connections=total_conns,
        pending_requests_received=pending_recv,
        pending_requests_sent=pending_sent,
        total_referrals_sent=referrals_sent,
        total_referrals_received=referrals_recv,
    )

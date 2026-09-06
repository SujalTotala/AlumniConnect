from typing import List, Optional
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import or_, and_, desc

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.opportunity_model import Opportunity
from app.models.connection_model import Connection
from app.models.referral_model import ReferralRequest
from app.schemas.referral_schema import (
    ReferralCreateRequest,
    ReferralResponse,
    ReferralOpportunitySummary,
    EligibleAlumniResponse,
)
from app.routes.connection_routes import build_user_summary
from app.auth.jwt_dependency import get_current_user
from app.services.notification_service import create_notification

router = APIRouter()


def serialize_referral(db: Session, ref: ReferralRequest) -> ReferralResponse:
    opp_summary = None
    if ref.opportunity:
        opp_summary = ReferralOpportunitySummary(
            id=ref.opportunity.id,
            title=ref.opportunity.title,
            company=ref.opportunity.company,
            opportunity_type=ref.opportunity.opportunity_type,
            location=ref.opportunity.location,
            application_url=ref.opportunity.application_url
        )

    requester_summary = build_user_summary(db, ref.requester) if ref.requester else None
    alumni_summary = build_user_summary(db, ref.alumni) if ref.alumni else None

    return ReferralResponse(
        id=ref.id,
        opportunity_id=ref.opportunity_id,
        requester_id=ref.requester_id,
        alumni_id=ref.alumni_id,
        message=ref.message,
        status=ref.status,
        created_at=ref.created_at,
        updated_at=ref.updated_at,
        opportunity=opp_summary,
        requester=requester_summary,
        alumni=alumni_summary
    )


# 1. Submit Referral Request (Eligible connected Alumni only)
@router.post("/", response_model=ReferralResponse, status_code=status.HTTP_201_CREATED)
def create_referral_request(
    data: ReferralCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if current_user.id == data.alumni_id:
        raise HTTPException(status_code=400, detail="You cannot request a referral from yourself.")

    # 1. Verify Opportunity exists
    opp = db.query(Opportunity).filter(Opportunity.id == data.opportunity_id).first()
    if not opp:
        raise HTTPException(status_code=404, detail="Opportunity not found.")

    # 2. Verify target user exists and is an alumnus
    target_user = db.query(User).filter(User.id == data.alumni_id, User.is_active == True).first()
    if not target_user:
        raise HTTPException(status_code=404, detail="Alumni recipient not found or is inactive.")

    alumni_profile = db.query(Alumni).filter(
        or_(Alumni.user_id == target_user.id, Alumni.email == target_user.email)
    ).first()
    if target_user.role.lower() != "alumni" and not alumni_profile:
        raise HTTPException(status_code=400, detail="Target recipient is not registered as an alumnus.")

    # 3. Verify ACCEPTED connection relationship
    connection = db.query(Connection).filter(
        Connection.status == "ACCEPTED",
        or_(
            and_(Connection.sender_id == current_user.id, Connection.receiver_id == data.alumni_id),
            and_(Connection.sender_id == data.alumni_id, Connection.receiver_id == current_user.id)
        )
    ).first()

    if not connection:
        raise HTTPException(
            status_code=400,
            detail="Referral requests can only be sent to connected Alumni. Please send a connection request first."
        )

    # 4. Check for duplicate active referral request
    existing_referral = db.query(ReferralRequest).filter(
        ReferralRequest.opportunity_id == data.opportunity_id,
        ReferralRequest.requester_id == current_user.id,
        ReferralRequest.alumni_id == data.alumni_id,
        ReferralRequest.status.in_(["PENDING", "ACCEPTED"])
    ).first()

    if existing_referral:
        raise HTTPException(
            status_code=409,
            detail=f"An active referral request ({existing_referral.status}) already exists for this opportunity with this alumnus."
        )

    new_referral = ReferralRequest(
        opportunity_id=data.opportunity_id,
        requester_id=current_user.id,
        alumni_id=data.alumni_id,
        message=data.message.strip() if data.message else None,
        status="PENDING",
        created_at=datetime.utcnow(),
        updated_at=datetime.utcnow()
    )
    db.add(new_referral)
    db.commit()
    db.refresh(new_referral)

    create_notification(
        db=db,
        user_id=data.alumni_id,
        title="New Referral Request",
        message=f"{current_user.name} requested a referral for {opp.title} at {opp.company}.",
        notification_type="REFERRAL"
    )

    return serialize_referral(db, new_referral)


# 2. Get Sent Referral Requests (Requester view)
@router.get("/sent", response_model=List[ReferralResponse])
def get_sent_referrals(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    referrals = db.query(ReferralRequest).filter(
        ReferralRequest.requester_id == current_user.id
    ).order_by(desc(ReferralRequest.created_at)).all()

    return [serialize_referral(db, r) for r in referrals]


# 3. Get Received Referral Requests (Alumni view)
@router.get("/received", response_model=List[ReferralResponse])
def get_received_referrals(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    referrals = db.query(ReferralRequest).filter(
        ReferralRequest.alumni_id == current_user.id
    ).order_by(desc(ReferralRequest.created_at)).all()

    return [serialize_referral(db, r) for r in referrals]


# 4. Get Eligible Connected Alumni for an Opportunity
@router.get("/eligible-alumni/{opportunity_id}", response_model=List[EligibleAlumniResponse])
def get_eligible_alumni_for_opportunity(
    opportunity_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    opp = db.query(Opportunity).filter(Opportunity.id == opportunity_id).first()
    if not opp:
        raise HTTPException(status_code=404, detail="Opportunity not found.")

    opp_company = (opp.company or "").strip().lower()

    # Get all accepted connections for current user
    conns = db.query(Connection).filter(
        Connection.status == "ACCEPTED",
        or_(Connection.sender_id == current_user.id, Connection.receiver_id == current_user.id)
    ).all()

    connected_partner_ids = [
        c.receiver_id if c.sender_id == current_user.id else c.sender_id
        for c in conns
    ]

    if not connected_partner_ids:
        return []

    # Filter connected partners who are Alumni
    alumni_users = db.query(User).filter(
        User.id.in_(connected_partner_ids),
        User.is_active == True,
        User.role == "alumni"
    ).all()

    eligible: List[EligibleAlumniResponse] = []
    for u in alumni_users:
        alumni_rec = db.query(Alumni).filter(
            or_(Alumni.user_id == u.id, Alumni.email == u.email)
        ).first()

        alumni_company = (alumni_rec.company if alumni_rec else "") or ""
        is_match = bool(opp_company and alumni_company and (
            opp_company in alumni_company.lower() or alumni_company.lower() in opp_company
        ))

        eligible.append(EligibleAlumniResponse(
            user_id=u.id,
            name=u.name,
            company=alumni_company or None,
            job_role=alumni_rec.job_role if alumni_rec else None,
            department=alumni_rec.department if alumni_rec else None,
            graduation_year=alumni_rec.graduation_year if alumni_rec else None,
            is_company_match=is_match,
            avatar_url=None
        ))

    # Sort company matches first, then by name
    eligible.sort(key=lambda a: (not a.is_company_match, a.name.lower()))
    return eligible


# 5. Accept Referral Request (Alumni recipient only)
@router.put("/{referral_id}/accept", response_model=ReferralResponse)
def accept_referral(
    referral_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    ref = db.query(ReferralRequest).filter(ReferralRequest.id == referral_id).first()
    if not ref:
        raise HTTPException(status_code=404, detail="Referral request not found.")

    if ref.alumni_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only the alumni recipient can accept this referral request.")

    if ref.status != "PENDING":
        raise HTTPException(status_code=400, detail=f"Cannot accept referral request with status '{ref.status}'.")

    ref.status = "ACCEPTED"
    ref.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(ref)

    opp_title = ref.opportunity.title if ref.opportunity else "the opportunity"
    create_notification(
        db=db,
        user_id=ref.requester_id,
        title="Referral Request Accepted",
        message=f"{current_user.name} accepted your referral request for {opp_title}.",
        notification_type="REFERRAL"
    )

    return serialize_referral(db, ref)


# 6. Decline Referral Request (Alumni recipient only)
@router.put("/{referral_id}/decline", response_model=ReferralResponse)
def decline_referral(
    referral_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    ref = db.query(ReferralRequest).filter(ReferralRequest.id == referral_id).first()
    if not ref:
        raise HTTPException(status_code=404, detail="Referral request not found.")

    if ref.alumni_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only the alumni recipient can decline this referral request.")

    if ref.status != "PENDING":
        raise HTTPException(status_code=400, detail=f"Cannot decline referral request with status '{ref.status}'.")

    ref.status = "DECLINED"
    ref.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(ref)

    opp_title = ref.opportunity.title if ref.opportunity else "the opportunity"
    create_notification(
        db=db,
        user_id=ref.requester_id,
        title="Referral Request Declined",
        message=f"{current_user.name} declined your referral request for {opp_title}.",
        notification_type="REFERRAL"
    )

    return serialize_referral(db, ref)


# 7. Complete Referral Request (Alumni recipient marks referral completed)
@router.put("/{referral_id}/complete", response_model=ReferralResponse)
def complete_referral(
    referral_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    ref = db.query(ReferralRequest).filter(ReferralRequest.id == referral_id).first()
    if not ref:
        raise HTTPException(status_code=404, detail="Referral request not found.")

    if ref.alumni_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only the alumni recipient can complete this referral request.")

    if ref.status != "ACCEPTED":
        raise HTTPException(status_code=400, detail=f"Cannot mark completed for referral with status '{ref.status}'. Must be ACCEPTED first.")

    ref.status = "COMPLETED"
    ref.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(ref)

    opp_title = ref.opportunity.title if ref.opportunity else "the opportunity"
    create_notification(
        db=db,
        user_id=ref.requester_id,
        title="Referral Completed",
        message=f"{current_user.name} marked your referral for {opp_title} as completed! Best of luck!",
        notification_type="REFERRAL"
    )

    return serialize_referral(db, ref)


# 8. Cancel Referral Request (Requester cancels while PENDING)
@router.delete("/{referral_id}")
def cancel_referral(
    referral_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    ref = db.query(ReferralRequest).filter(ReferralRequest.id == referral_id).first()
    if not ref:
        raise HTTPException(status_code=404, detail="Referral request not found.")

    if ref.requester_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only the requester can cancel this referral request.")

    if ref.status != "PENDING":
        raise HTTPException(status_code=400, detail=f"Cannot cancel referral request with status '{ref.status}'.")

    db.delete(ref)
    db.commit()
    return {"message": "Referral request cancelled successfully."}

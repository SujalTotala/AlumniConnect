from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.mentorship_model import MentorshipRequest
from app.schemas.alumni_schema import AlumniResponse
from app.schemas.mentorship_schema import (
    MentorshipRequestCreate,
    MentorshipStatusUpdate,
    MentorshipRequestResponse
)
from app.auth.jwt_dependency import get_current_user, require_role
from app.services.notification_service import create_notification

router = APIRouter()

# Get Available Mentors
@router.get("/mentors", response_model=List[AlumniResponse])
def get_available_mentors(
    department: Optional[str] = Query(None),
    company: Optional[str] = Query(None),
    skills: Optional[str] = Query(None),
    db: Session = Depends(get_db)
):
    query = db.query(Alumni).filter(Alumni.mentorship_available == True)

    if department:
        query = query.filter(Alumni.department.ilike(f"%{department}%"))
    if company:
        query = query.filter(Alumni.company.ilike(f"%{company}%"))
    if skills:
        query = query.filter(Alumni.skills.ilike(f"%{skills}%"))

    mentors = query.all()
    # If no explicitly marked mentors, return alumni with valid companies/skills
    if not mentors:
        mentors = db.query(Alumni).filter(Alumni.company.isnot(None)).limit(20).all()

    return mentors

# Send Mentorship Request (Students only or any authenticated user)
@router.post("/requests", response_model=MentorshipRequestResponse, status_code=status.HTTP_201_CREATED)
def send_mentorship_request(
    request_data: MentorshipRequestCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    # Verify mentor exists
    mentor = db.query(User).filter(User.id == request_data.mentor_id).first()
    if not mentor:
        # Check if mentor_id matches an Alumni.id
        alumni = db.query(Alumni).filter(Alumni.id == request_data.mentor_id).first()
        if alumni and alumni.user_id:
            mentor = db.query(User).filter(User.id == alumni.user_id).first()
        elif alumni:
            # Alumni without user_id record yet: find or link by email
            mentor = db.query(User).filter(User.email == alumni.email).first()

    if not mentor:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Mentor user not found"
        )

    if mentor.id == current_user.id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You cannot send a mentorship request to yourself"
        )

    # Check for existing pending request
    existing_req = db.query(MentorshipRequest).filter(
        MentorshipRequest.student_id == current_user.id,
        MentorshipRequest.mentor_id == mentor.id,
        MentorshipRequest.status == "PENDING"
    ).first()

    if existing_req:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="A pending mentorship request already exists for this mentor"
        )

    new_request = MentorshipRequest(
        student_id=current_user.id,
        mentor_id=mentor.id,
        message=request_data.message,
        status="PENDING"
    )
    db.add(new_request)
    db.commit()
    db.refresh(new_request)

    # Send notification to mentor
    create_notification(
        db=db,
        user_id=mentor.id,
        title="New Mentorship Request",
        message=f"{current_user.name} sent you a mentorship request: '{request_data.message[:60]}...'",
        notification_type="MENTORSHIP"
    )

    return {
        "id": new_request.id,
        "student_id": new_request.student_id,
        "mentor_id": new_request.mentor_id,
        "message": new_request.message,
        "status": new_request.status,
        "response_note": new_request.response_note,
        "created_at": new_request.created_at,
        "student_name": current_user.name,
        "student_email": current_user.email,
        "mentor_name": mentor.name,
        "mentor_email": mentor.email
    }

# Get Sent Requests (Student's outgoing requests)
@router.get("/requests/sent", response_model=List[MentorshipRequestResponse])
def get_sent_requests(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    requests = db.query(MentorshipRequest).filter(
        MentorshipRequest.student_id == current_user.id
    ).order_by(MentorshipRequest.id.desc()).all()

    result = []
    for req in requests:
        mentor = db.query(User).filter(User.id == req.mentor_id).first()
        result.append({
            "id": req.id,
            "student_id": req.student_id,
            "mentor_id": req.mentor_id,
            "message": req.message,
            "status": req.status,
            "response_note": req.response_note,
            "created_at": req.created_at,
            "student_name": current_user.name,
            "student_email": current_user.email,
            "mentor_name": mentor.name if mentor else "Unknown",
            "mentor_email": mentor.email if mentor else "Unknown"
        })
    return result

# Get Received Requests (Mentor's incoming requests)
@router.get("/requests/received", response_model=List[MentorshipRequestResponse])
def get_received_requests(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    requests = db.query(MentorshipRequest).filter(
        MentorshipRequest.mentor_id == current_user.id
    ).order_by(MentorshipRequest.id.desc()).all()

    result = []
    for req in requests:
        student = db.query(User).filter(User.id == req.student_id).first()
        result.append({
            "id": req.id,
            "student_id": req.student_id,
            "mentor_id": req.mentor_id,
            "message": req.message,
            "status": req.status,
            "response_note": req.response_note,
            "created_at": req.created_at,
            "student_name": student.name if student else "Unknown",
            "student_email": student.email if student else "Unknown",
            "mentor_name": current_user.name,
            "mentor_email": current_user.email
        })
    return result

# Accept Mentorship Request (Mentor)
@router.put("/requests/{request_id}/accept", response_model=MentorshipRequestResponse)
def accept_mentorship_request(
    request_id: int,
    status_data: Optional[MentorshipStatusUpdate] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    req = db.query(MentorshipRequest).filter(MentorshipRequest.id == request_id).first()
    if not req:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Mentorship request not found")

    if req.mentor_id != current_user.id and current_user.role.lower() != "admin":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized to update this request")

    req.status = "ACCEPTED"
    if status_data and status_data.response_note:
        req.response_note = status_data.response_note

    db.commit()
    db.refresh(req)

    # Notify student
    create_notification(
        db=db,
        user_id=req.student_id,
        title="Mentorship Request Accepted! 🎉",
        message=f"{current_user.name} has accepted your mentorship request.",
        notification_type="MENTORSHIP"
    )

    student = db.query(User).filter(User.id == req.student_id).first()
    return {
        "id": req.id,
        "student_id": req.student_id,
        "mentor_id": req.mentor_id,
        "message": req.message,
        "status": req.status,
        "response_note": req.response_note,
        "created_at": req.created_at,
        "student_name": student.name if student else "Unknown",
        "student_email": student.email if student else "Unknown",
        "mentor_name": current_user.name,
        "mentor_email": current_user.email
    }

# Reject Mentorship Request (Mentor)
@router.put("/requests/{request_id}/reject", response_model=MentorshipRequestResponse)
def reject_mentorship_request(
    request_id: int,
    status_data: Optional[MentorshipStatusUpdate] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    req = db.query(MentorshipRequest).filter(MentorshipRequest.id == request_id).first()
    if not req:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Mentorship request not found")

    if req.mentor_id != current_user.id and current_user.role.lower() != "admin":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized to update this request")

    req.status = "REJECTED"
    if status_data and status_data.response_note:
        req.response_note = status_data.response_note

    db.commit()
    db.refresh(req)

    # Notify student
    create_notification(
        db=db,
        user_id=req.student_id,
        title="Mentorship Request Update",
        message=f"{current_user.name} is currently unavailable for mentorship.",
        notification_type="MENTORSHIP"
    )

    student = db.query(User).filter(User.id == req.student_id).first()
    return {
        "id": req.id,
        "student_id": req.student_id,
        "mentor_id": req.mentor_id,
        "message": req.message,
        "status": req.status,
        "response_note": req.response_note,
        "created_at": req.created_at,
        "student_name": student.name if student else "Unknown",
        "student_email": student.email if student else "Unknown",
        "mentor_name": current_user.name,
        "mentor_email": current_user.email
    }

# Complete Mentorship Request
@router.put("/requests/{request_id}/complete", response_model=MentorshipRequestResponse)
def complete_mentorship_request(
    request_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    req = db.query(MentorshipRequest).filter(MentorshipRequest.id == request_id).first()
    if not req:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Mentorship request not found")

    if req.mentor_id != current_user.id and req.student_id != current_user.id and current_user.role.lower() != "admin":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized to complete this request")

    req.status = "COMPLETED"
    db.commit()
    db.refresh(req)

    student = db.query(User).filter(User.id == req.student_id).first()
    mentor = db.query(User).filter(User.id == req.mentor_id).first()

    return {
        "id": req.id,
        "student_id": req.student_id,
        "mentor_id": req.mentor_id,
        "message": req.message,
        "status": req.status,
        "response_note": req.response_note,
        "created_at": req.created_at,
        "student_name": student.name if student else "Unknown",
        "student_email": student.email if student else "Unknown",
        "mentor_name": mentor.name if mentor else "Unknown",
        "mentor_email": mentor.email if mentor else "Unknown"
    }

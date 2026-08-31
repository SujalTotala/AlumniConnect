from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.event_model import Event, EventRegistration
from app.models.mentorship_model import MentorshipRequest
from app.models.opportunity_model import Opportunity
from app.schemas.admin_schema import (
    AdminStatisticsResponse,
    UserStatusUpdate,
    UserAdminResponse
)
from app.auth.jwt_dependency import require_role

router = APIRouter()

# Get Platform Statistics (Admin only)
@router.get("/statistics", response_model=AdminStatisticsResponse)
def get_admin_statistics(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    total_users = db.query(User).count()
    total_alumni = db.query(Alumni).count()
    total_students = db.query(User).filter(User.role == "student").count()
    active_mentors = db.query(Alumni).filter(Alumni.mentorship_available == True).count()
    total_events = db.query(Event).count()
    total_event_registrations = db.query(EventRegistration).count()
    total_opportunities = db.query(Opportunity).count()
    pending_mentorship_requests = db.query(MentorshipRequest).filter(
        MentorshipRequest.status == "PENDING"
    ).count()

    return {
        "total_users": total_users,
        "total_alumni": total_alumni,
        "total_students": total_students,
        "active_mentors": active_mentors,
        "total_events": total_events,
        "total_event_registrations": total_event_registrations,
        "total_opportunities": total_opportunities,
        "pending_mentorship_requests": pending_mentorship_requests
    }

# Get Users List (Admin only)
@router.get("/users", response_model=List[UserAdminResponse])
def get_admin_users(
    search: Optional[str] = Query(None),
    role: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    query = db.query(User)

    if role:
        query = query.filter(User.role == role.lower())
    if search:
        query = query.filter(User.name.ilike(f"%{search}%") | User.email.ilike(f"%{search}%"))

    users = query.order_by(User.id.desc()).all()
    return users

# Update User Status (Admin only)
@router.put("/users/{user_id}/status", response_model=UserAdminResponse)
def update_user_status(
    user_id: int,
    status_data: UserStatusUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")

    user.is_active = status_data.is_active
    db.commit()
    db.refresh(user)

    return user

# Delete User (Admin only)
@router.delete("/users/{user_id}")
def delete_user(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")

    if user.id == current_user.id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Admin cannot delete their own account")

    db.delete(user)
    db.commit()

    return {"message": "User deleted successfully"}

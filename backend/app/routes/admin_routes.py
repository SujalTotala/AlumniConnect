from typing import List, Optional
import csv
import io
from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from sqlalchemy.orm import Session
from sqlalchemy import func

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.event_model import Event, EventRegistration
from app.models.mentorship_model import MentorshipRequest
from app.models.opportunity_model import Opportunity
from app.models.announcement_model import Announcement
from app.schemas.admin_schema import (
    AdminStatisticsResponse,
    UserStatusUpdate,
    UserAdminResponse
)
from app.auth.jwt_dependency import require_role

router = APIRouter()

# Get Platform Statistics with breakdowns (Admin only)
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

    verified_alumni = db.query(Alumni).filter(Alumni.is_verified == True).count()
    total_announcements = db.query(Announcement).count()

    # Aggregated breakdown: alumni by department
    dept_rows = (
        db.query(Alumni.department, func.count(Alumni.id))
        .filter(Alumni.department.isnot(None), Alumni.department != "")
        .group_by(Alumni.department)
        .order_by(func.count(Alumni.id).desc())
        .limit(8)
        .all()
    )
    alumni_by_dept = {row[0]: row[1] for row in dept_rows if row[0]}

    # Aggregated breakdown: alumni by graduation year
    year_rows = (
        db.query(Alumni.graduation_year, func.count(Alumni.id))
        .filter(Alumni.graduation_year.isnot(None), Alumni.graduation_year != "")
        .group_by(Alumni.graduation_year)
        .order_by(Alumni.graduation_year.desc())
        .limit(8)
        .all()
    )
    alumni_by_year = {row[0]: row[1] for row in year_rows if row[0]}

    # Aggregated breakdown: alumni by company
    comp_rows = (
        db.query(Alumni.company, func.count(Alumni.id))
        .filter(Alumni.company.isnot(None), Alumni.company != "")
        .group_by(Alumni.company)
        .order_by(func.count(Alumni.id).desc())
        .limit(8)
        .all()
    )
    alumni_by_company = {row[0]: row[1] for row in comp_rows if row[0]}

    return {
        "total_users": total_users,
        "total_alumni": total_alumni,
        "total_students": total_students,
        "active_mentors": active_mentors,
        "total_events": total_events,
        "total_event_registrations": total_event_registrations,
        "total_opportunities": total_opportunities,
        "pending_mentorship_requests": pending_mentorship_requests,
        "verified_alumni": verified_alumni,
        "total_announcements": total_announcements,
        "alumni_by_department": alumni_by_dept,
        "alumni_by_graduation_year": alumni_by_year,
        "alumni_by_company": alumni_by_company,
    }

# Alumni Verification Toggle (Admin only)
@router.put("/alumni/{alumni_id}/verify")
def toggle_alumni_verification(
    alumni_id: int,
    is_verified: bool = Query(..., description="Target verification status"),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    alumni = db.query(Alumni).filter(Alumni.id == alumni_id).first()
    if not alumni:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Alumni profile not found")

    alumni.is_verified = is_verified
    db.commit()
    db.refresh(alumni)

    return {
        "id": alumni.id,
        "name": alumni.name,
        "is_verified": alumni.is_verified,
        "message": f"Alumni verification {'granted' if is_verified else 'revoked'} successfully",
    }

# Secure CSV Export of Alumni Directory (Admin only)
@router.get("/alumni/export-csv")
def export_alumni_csv(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    alumni_records = db.query(Alumni).order_by(Alumni.id.asc()).all()

    output = io.StringIO()
    writer = csv.writer(output, quoting=csv.QUOTE_MINIMAL)

    # Header - Non-sensitive columns only
    writer.writerow([
        "ID",
        "Name",
        "Email",
        "Graduation Year",
        "Department",
        "Company",
        "Job Role",
        "Location",
        "Skills",
        "Verified",
        "Mentorship Available",
        "Joined Date",
    ])

    for a in alumni_records:
        writer.writerow([
            a.id,
            a.name or "",
            a.email or "",
            a.graduation_year or "",
            a.department or "",
            a.company or "",
            a.job_role or "",
            a.location or "",
            a.skills or "",
            "Yes" if getattr(a, "is_verified", False) else "No",
            "Yes" if a.mentorship_available else "No",
            a.created_at.strftime("%Y-%m-%d") if a.created_at else "",
        ])

    csv_data = output.getvalue()
    output.close()

    return Response(
        content=csv_data,
        media_type="text/csv",
        headers={
            "Content-Disposition": "attachment; filename=alumni_directory.csv",
            "Cache-Control": "no-cache, no-store, must-revalidate",
        },
    )

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

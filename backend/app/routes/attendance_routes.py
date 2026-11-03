from datetime import datetime, timedelta
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.event_model import Event, EventRegistration
from app.models.attendance_model import EventAttendance
from app.schemas.attendance_schema import (
    QRCodeTokenResponse,
    AttendanceCheckInRequest,
    ManualAttendanceRequest,
    AttendanceRecordResponse,
    AttendanceStatsResponse
)
from app.auth.jwt_dependency import get_current_user
from app.auth.auth_handler import create_access_token, decode_access_token
from app.services.audit_service import log_admin_action

router = APIRouter()

def is_event_organizer(user: User, event: Event) -> bool:
    return user.role.lower() == "admin" or (event.created_by is not None and event.created_by == user.id)

# 1. Generate QR check-in token for event (Admin or Organizer)
@router.post("/{event_id}/attendance/qr-token", response_model=QRCodeTokenResponse)
def generate_event_qr_token(
    event_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event not found"
        )

    if not is_event_organizer(current_user, event):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to generate attendance QR for this event"
        )

    expires_delta = timedelta(hours=24)
    payload = {
        "event_id": event.id,
        "purpose": "event_attendance",
        "organizer_id": current_user.id
    }
    qr_token = create_access_token(data=payload, expires_delta=expires_delta)
    expires_at = (datetime.utcnow() + expires_delta).isoformat()

    return {
        "event_id": event.id,
        "event_title": event.title,
        "qr_token": qr_token,
        "expires_at": expires_at
    }

# 2. Check-in via QR token (Authenticated Attendee)
@router.post("/attendance/check-in", response_model=AttendanceRecordResponse, status_code=status.HTTP_201_CREATED)
def check_in_attendee(
    request: AttendanceCheckInRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    token_payload = decode_access_token(request.qr_token.strip())
    if not token_payload or token_payload.get("purpose") != "event_attendance":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid or expired event QR attendance token"
        )

    event_id = token_payload.get("event_id")
    if not event_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Malformed attendance token: missing event reference"
        )

    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event no longer exists"
        )

    # Validate user is registered
    registration = db.query(EventRegistration).filter(
        EventRegistration.event_id == event_id,
        EventRegistration.user_id == current_user.id
    ).first()
    if not registration:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You are not registered for this event. Please register before checking in."
        )

    # Validate duplicate check-in
    existing_attendance = db.query(EventAttendance).filter(
        EventAttendance.event_id == event_id,
        EventAttendance.user_id == current_user.id
    ).first()
    if existing_attendance:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You have already checked in to this event"
        )

    new_attendance = EventAttendance(
        event_id=event_id,
        user_id=current_user.id,
        registration_id=registration.id,
        checked_in_at=datetime.utcnow(),
        checked_in_by=current_user.id,
        checkin_method="QR",
        created_at=datetime.utcnow()
    )
    db.add(new_attendance)
    db.commit()
    db.refresh(new_attendance)

    return {
        "id": new_attendance.id,
        "event_id": new_attendance.event_id,
        "user_id": new_attendance.user_id,
        "registration_id": new_attendance.registration_id,
        "checked_in_at": new_attendance.checked_in_at,
        "checkin_method": new_attendance.checkin_method,
        "user_name": current_user.name,
        "user_email": current_user.email,
        "user_role": current_user.role
    }

# 3. Manual attendance check-in (Admin or Organizer)
@router.post("/{event_id}/attendance/manual", response_model=AttendanceRecordResponse, status_code=status.HTTP_201_CREATED)
def manual_check_in(
    event_id: int,
    request: ManualAttendanceRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event not found"
        )

    if not is_event_organizer(current_user, event):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to manage attendance for this event"
        )

    target_user = db.query(User).filter(User.id == request.user_id).first()
    if not target_user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )

    registration = db.query(EventRegistration).filter(
        EventRegistration.event_id == event_id,
        EventRegistration.user_id == target_user.id
    ).first()
    if not registration:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"{target_user.name} is not registered for this event"
        )

    existing = db.query(EventAttendance).filter(
        EventAttendance.event_id == event_id,
        EventAttendance.user_id == target_user.id
    ).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"{target_user.name} has already checked in to this event"
        )

    attendance = EventAttendance(
        event_id=event_id,
        user_id=target_user.id,
        registration_id=registration.id,
        checked_in_at=datetime.utcnow(),
        checked_in_by=current_user.id,
        checkin_method="MANUAL",
        created_at=datetime.utcnow()
    )
    db.add(attendance)
    db.commit()
    db.refresh(attendance)

    if current_user.role.lower() == "admin":
        log_admin_action(
            db=db,
            admin_user_id=current_user.id,
            action="MANUAL_CHECKIN",
            target_type="ATTENDANCE",
            target_id=str(attendance.id),
            details={"event_id": event_id, "event_title": event.title, "user_id": target_user.id, "user_name": target_user.name}
        )

    return {
        "id": attendance.id,
        "event_id": attendance.event_id,
        "user_id": attendance.user_id,
        "registration_id": attendance.registration_id,
        "checked_in_at": attendance.checked_in_at,
        "checkin_method": attendance.checkin_method,
        "user_name": target_user.name,
        "user_email": target_user.email,
        "user_role": target_user.role
    }

# 4. View Attendance Roster (Admin or Organizer)
@router.get("/{event_id}/attendance", response_model=List[AttendanceRecordResponse])
def get_event_attendance_list(
    event_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event not found"
        )

    if not is_event_organizer(current_user, event):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to view attendance for this event"
        )

    records = db.query(EventAttendance).filter(
        EventAttendance.event_id == event_id
    ).order_by(EventAttendance.checked_in_at.desc()).all()

    result = []
    for rec in records:
        u = db.query(User).filter(User.id == rec.user_id).first()
        result.append({
            "id": rec.id,
            "event_id": rec.event_id,
            "user_id": rec.user_id,
            "registration_id": rec.registration_id,
            "checked_in_at": rec.checked_in_at,
            "checkin_method": rec.checkin_method,
            "user_name": u.name if u else "Unknown User",
            "user_email": u.email if u else "",
            "user_role": u.role if u else "student"
        })

    return result

# 5. View Attendance Statistics (Admin, Organizer, or Authenticated Member)
@router.get("/{event_id}/attendance/stats", response_model=AttendanceStatsResponse)
def get_event_attendance_stats(
    event_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event not found"
        )

    total_registered = db.query(EventRegistration).filter(
        EventRegistration.event_id == event_id
    ).count()

    total_attended = db.query(EventAttendance).filter(
        EventAttendance.event_id == event_id
    ).count()

    rate = round((total_attended / total_registered * 100.0), 1) if total_registered > 0 else 0.0

    return {
        "event_id": event.id,
        "event_title": event.title,
        "total_registered": total_registered,
        "total_attended": total_attended,
        "attendance_percentage": rate
    }

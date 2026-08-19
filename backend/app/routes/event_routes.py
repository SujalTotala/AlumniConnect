from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.event_model import Event, EventRegistration
from app.schemas.event_schema import (
    EventCreate,
    EventUpdate,
    EventResponse,
    EventRegistrationResponse
)
from app.auth.jwt_dependency import get_current_user, get_optional_current_user, require_role
from app.services.notification_service import create_notification

router = APIRouter()

# Get all events
@router.get("/", response_model=List[EventResponse])
def get_events(
    event_type: Optional[str] = Query(None, description="Filter by event type"),
    search: Optional[str] = Query(None, description="Search by title or description"),
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    query = db.query(Event)

    if event_type:
        query = query.filter(Event.event_type.ilike(f"%{event_type}%"))
    if search:
        query = query.filter(
            Event.title.ilike(f"%{search}%") | Event.description.ilike(f"%{search}%")
        )

    events = query.order_by(Event.id.desc()).all()
    result = []

    user_reg_event_ids = set()
    if current_user:
        regs = db.query(EventRegistration.event_id).filter(
            EventRegistration.user_id == current_user.id
        ).all()
        user_reg_event_ids = {r[0] for r in regs}

    for ev in events:
        reg_count = db.query(EventRegistration).filter(
            EventRegistration.event_id == ev.id
        ).count()
        ev_dict = {
            "id": ev.id,
            "title": ev.title,
            "description": ev.description,
            "event_type": ev.event_type,
            "event_date": ev.event_date,
            "start_time": ev.start_time,
            "location": ev.location,
            "meeting_url": ev.meeting_url,
            "image_url": ev.image_url,
            "created_by": ev.created_by,
            "created_at": ev.created_at,
            "registrations_count": reg_count,
            "is_registered": ev.id in user_reg_event_ids
        }
        result.append(ev_dict)

    return result

# Get single event
@router.get("/{event_id}", response_model=EventResponse)
def get_event_by_id(
    event_id: int,
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event not found"
        )

    reg_count = db.query(EventRegistration).filter(
        EventRegistration.event_id == event.id
    ).count()
    is_registered = False
    if current_user:
        reg = db.query(EventRegistration).filter(
            EventRegistration.event_id == event.id,
            EventRegistration.user_id == current_user.id
        ).first()
        is_registered = reg is not None

    return {
        "id": event.id,
        "title": event.title,
        "description": event.description,
        "event_type": event.event_type,
        "event_date": event.event_date,
        "start_time": event.start_time,
        "location": event.location,
        "meeting_url": event.meeting_url,
        "image_url": event.image_url,
        "created_by": event.created_by,
        "created_at": event.created_at,
        "registrations_count": reg_count,
        "is_registered": is_registered
    }

# Create Event (Admin or Alumni)
@router.post("/", response_model=EventResponse, status_code=status.HTTP_201_CREATED)
def create_event(
    event_data: EventCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin", "alumni"]))
):
    new_event = Event(
        title=event_data.title,
        description=event_data.description,
        event_type=event_data.event_type or "Alumni Meet",
        event_date=event_data.event_date,
        start_time=event_data.start_time,
        location=event_data.location,
        meeting_url=event_data.meeting_url,
        image_url=event_data.image_url,
        created_by=current_user.id
    )
    db.add(new_event)
    db.commit()
    db.refresh(new_event)

    return {
        "id": new_event.id,
        "title": new_event.title,
        "description": new_event.description,
        "event_type": new_event.event_type,
        "event_date": new_event.event_date,
        "start_time": new_event.start_time,
        "location": new_event.location,
        "meeting_url": new_event.meeting_url,
        "image_url": new_event.image_url,
        "created_by": new_event.created_by,
        "created_at": new_event.created_at,
        "registrations_count": 0,
        "is_registered": False
    }

# Update Event (Admin or Creator)
@router.put("/{event_id}", response_model=EventResponse)
def update_event(
    event_id: int,
    event_update: EventUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event not found"
        )

    if current_user.role.lower() != "admin" and event.created_by != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to update this event"
        )

    update_dict = event_update.model_dump(exclude_unset=True)
    for field, value in update_dict.items():
        if value is not None:
            setattr(event, field, value)

    db.commit()
    db.refresh(event)

    reg_count = db.query(EventRegistration).filter(
        EventRegistration.event_id == event.id
    ).count()

    return {
        "id": event.id,
        "title": event.title,
        "description": event.description,
        "event_type": event.event_type,
        "event_date": event.event_date,
        "start_time": event.start_time,
        "location": event.location,
        "meeting_url": event.meeting_url,
        "image_url": event.image_url,
        "created_by": event.created_by,
        "created_at": event.created_at,
        "registrations_count": reg_count,
        "is_registered": False
    }

# Delete Event (Admin or Creator)
@router.delete("/{event_id}")
def delete_event(
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

    if current_user.role.lower() != "admin" and event.created_by != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to delete this event"
        )

    db.delete(event)
    db.commit()

    return {"message": "Event deleted successfully"}

# Register for Event
@router.post("/{event_id}/register", status_code=status.HTTP_201_CREATED)
def register_for_event(
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

    existing_reg = db.query(EventRegistration).filter(
        EventRegistration.event_id == event_id,
        EventRegistration.user_id == current_user.id
    ).first()

    if existing_reg:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You are already registered for this event"
        )

    new_reg = EventRegistration(
        event_id=event_id,
        user_id=current_user.id,
        registration_status="CONFIRMED"
    )
    db.add(new_reg)
    db.commit()

    # Create notification for user
    create_notification(
        db=db,
        user_id=current_user.id,
        title="Event Registration Confirmed",
        message=f"You have successfully registered for '{event.title}' scheduled for {event.event_date}.",
        notification_type="EVENT"
    )

    return {"message": "Successfully registered for event", "registration_id": new_reg.id}

# Cancel Event Registration
@router.delete("/{event_id}/register")
def cancel_event_registration(
    event_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    registration = db.query(EventRegistration).filter(
        EventRegistration.event_id == event_id,
        EventRegistration.user_id == current_user.id
    ).first()

    if not registration:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Registration not found"
        )

    db.delete(registration)
    db.commit()

    return {"message": "Event registration cancelled successfully"}

# View registrations for an event (Admin or Creator)
@router.get("/{event_id}/registrations", response_model=List[EventRegistrationResponse])
def get_event_registrations(
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

    if current_user.role.lower() != "admin" and event.created_by != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to view registrations for this event"
        )

    registrations = db.query(EventRegistration).filter(
        EventRegistration.event_id == event_id
    ).all()

    result = []
    for reg in registrations:
        u = db.query(User).filter(User.id == reg.user_id).first()
        result.append({
            "id": reg.id,
            "event_id": reg.event_id,
            "user_id": reg.user_id,
            "registration_status": reg.registration_status,
            "registered_at": reg.registered_at,
            "user_name": u.name if u else "Unknown",
            "user_email": u.email if u else "Unknown",
            "user_role": u.role if u else "Unknown"
        })

    return result

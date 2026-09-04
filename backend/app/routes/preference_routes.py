from datetime import datetime
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.preference_model import NotificationPreference
from app.schemas.preference_schema import NotificationPreferenceUpdate, NotificationPreferenceResponse
from app.auth.jwt_dependency import get_current_user

router = APIRouter()

@router.get("/", response_model=NotificationPreferenceResponse)
def get_notification_preferences(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    pref = db.query(NotificationPreference).filter(NotificationPreference.user_id == current_user.id).first()
    if not pref:
        return NotificationPreferenceResponse(
            user_id=current_user.id,
            events=True,
            mentorship=True,
            opportunities=True,
            announcements=True,
            updated_at=datetime.utcnow(),
        )

    return NotificationPreferenceResponse(
        user_id=pref.user_id,
        events=pref.events,
        mentorship=pref.mentorship,
        opportunities=pref.opportunities,
        announcements=pref.announcements,
        updated_at=pref.updated_at,
    )

@router.put("/", response_model=NotificationPreferenceResponse)
def update_notification_preferences(
    update_data: NotificationPreferenceUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    pref = db.query(NotificationPreference).filter(NotificationPreference.user_id == current_user.id).first()
    if not pref:
        pref = NotificationPreference(
            user_id=current_user.id,
            events=update_data.events if update_data.events is not None else True,
            mentorship=update_data.mentorship if update_data.mentorship is not None else True,
            opportunities=update_data.opportunities if update_data.opportunities is not None else True,
            announcements=update_data.announcements if update_data.announcements is not None else True,
            updated_at=datetime.utcnow(),
        )
        db.add(pref)
    else:
        if update_data.events is not None:
            pref.events = update_data.events
        if update_data.mentorship is not None:
            pref.mentorship = update_data.mentorship
        if update_data.opportunities is not None:
            pref.opportunities = update_data.opportunities
        if update_data.announcements is not None:
            pref.announcements = update_data.announcements
        pref.updated_at = datetime.utcnow()

    db.commit()
    db.refresh(pref)

    return NotificationPreferenceResponse(
        user_id=pref.user_id,
        events=pref.events,
        mentorship=pref.mentorship,
        opportunities=pref.opportunities,
        announcements=pref.announcements,
        updated_at=pref.updated_at,
    )

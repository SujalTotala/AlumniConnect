from sqlalchemy.orm import Session
from app.models.notification_model import Notification

def create_notification(
    db: Session,
    user_id: int,
    title: str,
    message: str,
    notification_type: str = "GENERAL"
) -> Notification:
    notification = Notification(
        user_id=user_id,
        title=title,
        message=message,
        notification_type=notification_type
    )
    db.add(notification)
    db.commit()
    db.refresh(notification)
    return notification

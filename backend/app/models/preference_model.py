from datetime import datetime
from sqlalchemy import Column, Integer, Boolean, DateTime, ForeignKey
from app.database.database import Base

class NotificationPreference(Base):
    __tablename__ = "notification_preferences"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False, index=True)
    
    events = Column(Boolean, default=True, nullable=False)
    mentorship = Column(Boolean, default=True, nullable=False)
    opportunities = Column(Boolean, default=True, nullable=False)
    announcements = Column(Boolean, default=True, nullable=False)
    
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

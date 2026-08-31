from datetime import datetime
from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship
from app.database.database import Base

class Event(Base):
    __tablename__ = "events"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=False)
    event_type = Column(String, nullable=False, default="Alumni Meet")
    event_date = Column(String, nullable=False)
    start_time = Column(String, nullable=True)
    location = Column(String, nullable=False)
    meeting_url = Column(String, nullable=True)
    image_url = Column(String, nullable=True)
    created_by = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationship to registrations
    registrations = relationship("EventRegistration", back_populates="event", cascade="all, delete-orphan")


class EventRegistration(Base):
    __tablename__ = "event_registrations"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, ForeignKey("events.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    registration_status = Column(String, nullable=False, default="CONFIRMED")
    registered_at = Column(DateTime, default=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint("event_id", "user_id", name="uq_event_user_registration"),
    )

    event = relationship("Event", back_populates="registrations")

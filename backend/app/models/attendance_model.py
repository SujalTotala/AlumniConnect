from datetime import datetime
from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship
from app.database.database import Base

class EventAttendance(Base):
    __tablename__ = "event_attendance"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, ForeignKey("events.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    registration_id = Column(Integer, ForeignKey("event_registrations.id", ondelete="SET NULL"), nullable=True)
    checked_in_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    checked_in_by = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    checkin_method = Column(String, nullable=False, default="QR")  # "QR" or "MANUAL"
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    __table_args__ = (
        UniqueConstraint("event_id", "user_id", name="uq_event_user_attendance"),
    )

    event = relationship("Event", foreign_keys=[event_id])
    user = relationship("User", foreign_keys=[user_id])
    checked_by_user = relationship("User", foreign_keys=[checked_in_by])

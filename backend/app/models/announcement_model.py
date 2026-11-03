from datetime import datetime
from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, Index
from app.database.database import Base

class Announcement(Base):
    __tablename__ = "announcements"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    content = Column(Text, nullable=False)
    category = Column(String, nullable=False, default="GENERAL")  # GENERAL, COLLEGE_NOTICE, ALUMNI_MEET, PLACEMENT, IMPORTANT
    priority = Column(String, nullable=False, default="NORMAL", index=True)  # NORMAL, HIGH, URGENT
    
    created_by = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    audience_type = Column(String, nullable=False, default="ALL_USERS", server_default="ALL_USERS", index=True)  # ALL_USERS, STUDENTS, ALUMNI, SEGMENT
    segment_id = Column(Integer, ForeignKey("alumni_segments.id", ondelete="SET NULL"), nullable=True, index=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False, index=True)
    expires_at = Column(DateTime, nullable=True, index=True)

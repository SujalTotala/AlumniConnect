from datetime import datetime
from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from app.database.database import Base

class Achievement(Base):
    __tablename__ = "achievements"

    id = Column(Integer, primary_key=True, index=True)
    alumni_id = Column(Integer, ForeignKey("alumni.id", ondelete="SET NULL"), nullable=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=False)
    category = Column(String, nullable=False)  # AWARD, CERTIFICATION, PROMOTION, PUBLICATION, ENTREPRENEURSHIP, SOCIAL_IMPACT, OTHER
    organization = Column(String, nullable=True)
    achievement_date = Column(String, nullable=True)
    proof_url = Column(String, nullable=True)
    status = Column(String, nullable=False, default="PENDING")  # PENDING, APPROVED, REJECTED
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    alumni = relationship("Alumni", foreign_keys=[alumni_id])
    user = relationship("User", foreign_keys=[user_id])

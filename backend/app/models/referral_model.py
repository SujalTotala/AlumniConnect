from datetime import datetime
from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, CheckConstraint
from sqlalchemy.orm import relationship
from app.database.database import Base

class ReferralRequest(Base):
    __tablename__ = "referral_requests"
    __table_args__ = (
        CheckConstraint("requester_id != alumni_id", name="check_requester_not_alumni"),
    )

    id = Column(Integer, primary_key=True, index=True)
    opportunity_id = Column(Integer, ForeignKey("opportunities.id", ondelete="CASCADE"), nullable=False, index=True)
    requester_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    alumni_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    
    message = Column(Text, nullable=True)
    status = Column(String, nullable=False, default="PENDING", index=True)  # PENDING, ACCEPTED, DECLINED, COMPLETED
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    opportunity = relationship("Opportunity", foreign_keys=[opportunity_id], lazy="joined")
    requester = relationship("User", foreign_keys=[requester_id], lazy="joined")
    alumni = relationship("User", foreign_keys=[alumni_id], lazy="joined")

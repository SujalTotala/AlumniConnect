from datetime import datetime
from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey
from app.database.database import Base

class Opportunity(Base):
    __tablename__ = "opportunities"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    company = Column(String, nullable=False)
    description = Column(Text, nullable=False)
    opportunity_type = Column(String, nullable=False, default="Full-Time Job")  # Internship, Full-Time Job, Referral, Hackathon, Scholarship, Workshop
    location = Column(String, nullable=True)
    deadline = Column(String, nullable=True)
    application_url = Column(String, nullable=True)
    
    posted_by = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

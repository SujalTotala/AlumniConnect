from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey
from app.database.database import Base

class Alumni(Base):
    __tablename__ = "alumni"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(
        Integer,
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
    )

    name = Column(String, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)

    graduation_year = Column(String, nullable=True)
    department = Column(String, nullable=True)
    company = Column(String, nullable=True)
    job_role = Column(String, nullable=True)
    location = Column(String, nullable=True)
    skills = Column(String, nullable=True)
    bio = Column(String, nullable=True)
    linkedin_url = Column(String, nullable=True)
    github_url = Column(String, nullable=True)
    mentorship_available = Column(Boolean, default=False)
    is_verified = Column(Boolean, default=False, nullable=False)
    
    created_at = Column(DateTime, default=datetime.utcnow)

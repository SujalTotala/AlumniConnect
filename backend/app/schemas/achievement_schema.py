from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict

class AchievementCreate(BaseModel):
    title: str
    description: str
    category: str  # AWARD, CERTIFICATION, PROMOTION, PUBLICATION, ENTREPRENEURSHIP, SOCIAL_IMPACT, OTHER
    organization: Optional[str] = None
    achievement_date: Optional[str] = None
    proof_url: Optional[str] = None

class AchievementUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    category: Optional[str] = None
    organization: Optional[str] = None
    achievement_date: Optional[str] = None
    proof_url: Optional[str] = None

class AchievementResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    alumni_id: Optional[int] = None
    user_id: int
    alumni_name: str
    alumni_company: Optional[str] = None
    alumni_job_role: Optional[str] = None
    is_verified: bool = False
    title: str
    description: str
    category: str
    organization: Optional[str] = None
    achievement_date: Optional[str] = None
    proof_url: Optional[str] = None
    status: str
    created_at: datetime
    updated_at: datetime

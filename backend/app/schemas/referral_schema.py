from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field, ConfigDict
from app.schemas.connection_schema import ConnectionUserSummary

class ReferralCreateRequest(BaseModel):
    opportunity_id: int
    alumni_id: int
    message: Optional[str] = Field(None, max_length=1000)

class ReferralOpportunitySummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    company: str
    opportunity_type: str
    location: Optional[str] = None
    application_url: Optional[str] = None

class ReferralResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    opportunity_id: int
    requester_id: int
    alumni_id: int
    message: Optional[str] = None
    status: str  # PENDING, ACCEPTED, DECLINED, COMPLETED
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
    opportunity: Optional[ReferralOpportunitySummary] = None
    requester: Optional[ConnectionUserSummary] = None
    alumni: Optional[ConnectionUserSummary] = None

class EligibleAlumniResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    user_id: int
    name: str
    company: Optional[str] = None
    job_role: Optional[str] = None
    department: Optional[str] = None
    graduation_year: Optional[str] = None
    is_company_match: bool = False
    avatar_url: Optional[str] = None

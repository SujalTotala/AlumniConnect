from typing import Optional
from datetime import datetime
from pydantic import BaseModel, ConfigDict

class OpportunityCreate(BaseModel):
    title: str
    company: str
    description: str
    opportunity_type: Optional[str] = "Full-Time Job"
    location: Optional[str] = None
    deadline: Optional[str] = None
    application_url: Optional[str] = None

class OpportunityUpdate(BaseModel):
    title: Optional[str] = None
    company: Optional[str] = None
    description: Optional[str] = None
    opportunity_type: Optional[str] = None
    location: Optional[str] = None
    deadline: Optional[str] = None
    application_url: Optional[str] = None

class OpportunityResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    company: str
    description: str
    opportunity_type: str
    location: Optional[str] = None
    deadline: Optional[str] = None
    application_url: Optional[str] = None
    posted_by: Optional[int] = None
    created_at: Optional[datetime] = None
    poster_name: Optional[str] = None

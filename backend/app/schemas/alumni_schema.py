from typing import Optional
from pydantic import BaseModel, ConfigDict

class AlumniCreate(BaseModel):
    name: str
    email: str
    graduation_year: Optional[str] = None
    department: Optional[str] = None
    company: Optional[str] = None
    job_role: Optional[str] = None
    location: Optional[str] = None
    skills: Optional[str] = None
    bio: Optional[str] = None
    linkedin_url: Optional[str] = None
    github_url: Optional[str] = None
    mentorship_available: Optional[bool] = False

class AlumniUpdate(BaseModel):
    name: Optional[str] = None
    email: Optional[str] = None
    graduation_year: Optional[str] = None
    department: Optional[str] = None
    company: Optional[str] = None
    job_role: Optional[str] = None
    location: Optional[str] = None
    skills: Optional[str] = None
    bio: Optional[str] = None
    linkedin_url: Optional[str] = None
    github_url: Optional[str] = None
    mentorship_available: Optional[bool] = None

class AlumniResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: Optional[int] = None
    name: str
    email: str
    graduation_year: Optional[str] = None
    department: Optional[str] = None
    company: Optional[str] = None
    job_role: Optional[str] = None
    location: Optional[str] = None
    skills: Optional[str] = None
    bio: Optional[str] = None
    linkedin_url: Optional[str] = None
    github_url: Optional[str] = None
    mentorship_available: Optional[bool] = False
    is_verified: bool = False
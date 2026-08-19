from typing import Optional, Any, Dict
from pydantic import BaseModel, ConfigDict

class StudentProfileCreate(BaseModel):
    branch: Optional[str] = None
    year: Optional[str] = None
    skills: Optional[str] = None
    interests: Optional[str] = None
    bio: Optional[str] = None
    profile_image_url: Optional[str] = None

class StudentProfileUpdate(BaseModel):
    branch: Optional[str] = None
    year: Optional[str] = None
    skills: Optional[str] = None
    interests: Optional[str] = None
    bio: Optional[str] = None
    profile_image_url: Optional[str] = None

class StudentProfileResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    branch: Optional[str] = None
    year: Optional[str] = None
    skills: Optional[str] = None
    interests: Optional[str] = None
    bio: Optional[str] = None
    profile_image_url: Optional[str] = None

class ProfileMeResponse(BaseModel):
    id: int
    name: str
    email: str
    role: str
    is_active: bool
    profile: Optional[Dict[str, Any]] = None

class ProfileMeUpdate(BaseModel):
    name: Optional[str] = None
    # Alumni-specific fields
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
    # Student-specific fields
    branch: Optional[str] = None
    year: Optional[str] = None
    interests: Optional[str] = None
    profile_image_url: Optional[str] = None

from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict

class SuccessStoryCreate(BaseModel):
    title: str
    summary: str
    content: str
    company: Optional[str] = None
    role: Optional[str] = None
    achievement_date: Optional[str] = None
    image_url: Optional[str] = None

class SuccessStoryUpdate(BaseModel):
    title: Optional[str] = None
    summary: Optional[str] = None
    content: Optional[str] = None
    company: Optional[str] = None
    role: Optional[str] = None
    achievement_date: Optional[str] = None
    image_url: Optional[str] = None

class SuccessStoryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    author_user_id: int
    alumni_id: Optional[int] = None
    author_name: str
    author_email: str
    is_verified: bool = False
    title: str
    summary: str
    content: str
    company: Optional[str] = None
    role: Optional[str] = None
    achievement_date: Optional[str] = None
    image_url: Optional[str] = None
    status: str
    created_at: datetime
    updated_at: datetime

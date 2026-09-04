from typing import Optional, Literal
from datetime import datetime
from pydantic import BaseModel, ConfigDict, Field

PriorityType = Literal["NORMAL", "HIGH", "URGENT"]
CategoryType = Literal["GENERAL", "COLLEGE_NOTICE", "ALUMNI_MEET", "PLACEMENT", "IMPORTANT"]

class AnnouncementCreate(BaseModel):
    title: str = Field(..., min_length=3, max_length=200)
    content: str = Field(..., min_length=5)
    category: Optional[str] = "GENERAL"
    priority: Optional[PriorityType] = "NORMAL"
    expires_at: Optional[datetime] = None

class AnnouncementUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=3, max_length=200)
    content: Optional[str] = Field(None, min_length=5)
    category: Optional[str] = None
    priority: Optional[PriorityType] = None
    expires_at: Optional[datetime] = None

class AnnouncementResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    content: str
    category: str
    priority: str
    created_by: Optional[int] = None
    author_name: Optional[str] = None
    created_at: datetime
    expires_at: Optional[datetime] = None

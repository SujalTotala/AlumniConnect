from typing import Optional
from datetime import datetime
from pydantic import BaseModel, ConfigDict

class MentorshipRequestCreate(BaseModel):
    mentor_id: int
    message: str

class MentorshipStatusUpdate(BaseModel):
    response_note: Optional[str] = None

class MentorshipRequestResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    student_id: int
    mentor_id: int
    message: str
    status: str
    response_note: Optional[str] = None
    created_at: Optional[datetime] = None
    student_name: Optional[str] = None
    student_email: Optional[str] = None
    mentor_name: Optional[str] = None
    mentor_email: Optional[str] = None

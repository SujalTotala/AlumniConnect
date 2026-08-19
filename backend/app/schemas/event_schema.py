from typing import Optional
from datetime import datetime
from pydantic import BaseModel, ConfigDict

class EventCreate(BaseModel):
    title: str
    description: str
    event_type: Optional[str] = "Alumni Meet"
    event_date: str
    start_time: Optional[str] = None
    location: str
    meeting_url: Optional[str] = None
    image_url: Optional[str] = None

class EventUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    event_type: Optional[str] = None
    event_date: Optional[str] = None
    start_time: Optional[str] = None
    location: Optional[str] = None
    meeting_url: Optional[str] = None
    image_url: Optional[str] = None

class EventResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    description: str
    event_type: str
    event_date: str
    start_time: Optional[str] = None
    location: str
    meeting_url: Optional[str] = None
    image_url: Optional[str] = None
    created_by: Optional[int] = None
    created_at: Optional[datetime] = None
    registrations_count: Optional[int] = 0
    is_registered: Optional[bool] = False

class EventRegistrationResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    event_id: int
    user_id: int
    registration_status: str
    registered_at: Optional[datetime] = None
    user_name: Optional[str] = None
    user_email: Optional[str] = None
    user_role: Optional[str] = None

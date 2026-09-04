from typing import Optional
from datetime import datetime
from pydantic import BaseModel, ConfigDict

class AdminStatisticsResponse(BaseModel):
    total_users: int
    total_alumni: int
    total_students: int
    active_mentors: int
    total_events: int
    total_event_registrations: int
    total_opportunities: int
    pending_mentorship_requests: int
    verified_alumni: Optional[int] = 0
    total_announcements: Optional[int] = 0
    alumni_by_department: Optional[dict] = None
    alumni_by_graduation_year: Optional[dict] = None
    alumni_by_company: Optional[dict] = None

class UserStatusUpdate(BaseModel):
    is_active: bool

class UserAdminResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    email: str
    role: str
    is_active: bool
    created_at: Optional[datetime] = None

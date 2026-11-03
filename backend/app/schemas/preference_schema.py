from typing import Optional
from datetime import datetime
from pydantic import BaseModel, ConfigDict

class NotificationPreferenceUpdate(BaseModel):
    events: Optional[bool] = None
    mentorship: Optional[bool] = None
    opportunities: Optional[bool] = None
    announcements: Optional[bool] = None

class NotificationPreferenceResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    user_id: int
    events: bool = True
    mentorship: bool = True
    opportunities: bool = True
    announcements: bool = True
    updated_at: Optional[datetime] = None

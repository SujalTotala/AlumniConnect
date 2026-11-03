from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict

class QRCodeTokenResponse(BaseModel):
    event_id: int
    event_title: str
    qr_token: str
    expires_at: str

class AttendanceCheckInRequest(BaseModel):
    qr_token: str

class ManualAttendanceRequest(BaseModel):
    user_id: int

class AttendanceRecordResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    event_id: int
    user_id: int
    registration_id: Optional[int] = None
    checked_in_at: datetime
    checkin_method: str
    user_name: str
    user_email: str
    user_role: str

class AttendanceStatsResponse(BaseModel):
    event_id: int
    event_title: str
    total_registered: int
    total_attended: int
    attendance_percentage: float

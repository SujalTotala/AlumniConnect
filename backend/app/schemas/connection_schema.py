from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel, ConfigDict

class ConnectionUserSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    user_id: int
    name: str
    email: str
    role: str
    headline: Optional[str] = None
    company: Optional[str] = None
    job_role: Optional[str] = None
    department: Optional[str] = None
    graduation_year: Optional[str] = None
    location: Optional[str] = None
    avatar_url: Optional[str] = None
    is_verified: Optional[bool] = False

class ConnectionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    sender_id: int
    receiver_id: int
    status: str
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
    sender: Optional[ConnectionUserSummary] = None
    receiver: Optional[ConnectionUserSummary] = None

class ConnectionStatusResponse(BaseModel):
    user_id: int
    status: str  # "CONNECTED", "PENDING_SENT", "PENDING_RECEIVED", "NOT_CONNECTED"
    connection_id: Optional[int] = None

class ConnectionSuggestionResponse(BaseModel):
    user_id: int
    name: str
    email: str
    role: str
    headline: Optional[str] = None
    company: Optional[str] = None
    job_role: Optional[str] = None
    department: Optional[str] = None
    graduation_year: Optional[str] = None
    location: Optional[str] = None
    skills: Optional[str] = None
    avatar_url: Optional[str] = None
    is_verified: Optional[bool] = False
    suggestion_score: int
    suggestion_reasons: List[str]

class NetworkSummaryResponse(BaseModel):
    total_connections: int
    pending_requests_received: int
    pending_requests_sent: int
    total_referrals_sent: Optional[int] = 0
    total_referrals_received: Optional[int] = 0

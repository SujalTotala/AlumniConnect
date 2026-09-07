from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict

class CommunityCreate(BaseModel):
    name: str
    description: str
    community_type: Optional[str] = "GENERAL"

class CommunityResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    description: str
    community_type: str
    created_by: Optional[int] = None
    creator_name: Optional[str] = None
    members_count: int = 0
    is_member: bool = False
    my_role: Optional[str] = None
    created_at: datetime
    is_active: bool = True

class CommunityMemberResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    community_id: int
    user_id: int
    user_name: str
    user_email: str
    user_role: str
    member_role: str
    joined_at: datetime

class CommunityPostCreate(BaseModel):
    content: str

class CommunityPostResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    community_id: int
    author_id: int
    author_name: str
    author_role: str
    content: str
    created_at: datetime

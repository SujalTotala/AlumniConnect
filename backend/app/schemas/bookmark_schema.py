from typing import Optional, Literal, Dict, Any
from datetime import datetime
from pydantic import BaseModel, ConfigDict

AllowedEntityType = Literal["alumni", "opportunity", "event"]

class BookmarkCreate(BaseModel):
    entity_type: AllowedEntityType
    entity_id: int

class BookmarkResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    entity_type: str
    entity_id: int
    created_at: datetime
    entity_preview: Optional[Dict[str, Any]] = None

class BookmarkCheckResponse(BaseModel):
    is_bookmarked: bool
    bookmark_id: Optional[int] = None

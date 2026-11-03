from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import BaseModel, ConfigDict, Field

# --- Segment Schemas ---
class AlumniSegmentCreate(BaseModel):
    name: str = Field(..., min_length=2, max_length=100)
    description: Optional[str] = None
    filters: Dict[str, Any] = Field(..., description="Dynamic filter criteria dictionary")

class AlumniSegmentUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=2, max_length=100)
    description: Optional[str] = None
    filters: Optional[Dict[str, Any]] = None

class AlumniSegmentResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    description: Optional[str] = None
    filters: Dict[str, Any]
    estimated_count: int = 0
    created_by: Optional[int] = None
    created_at: datetime
    updated_at: datetime

# --- Bulk Import Schemas ---
class ImportPreviewRow(BaseModel):
    row_number: int
    name: str
    email: str
    graduation_year: Optional[str] = None
    department: Optional[str] = None
    company: Optional[str] = None
    job_role: Optional[str] = None
    location: Optional[str] = None
    skills: Optional[str] = None
    bio: Optional[str] = None
    linkedin_url: Optional[str] = None
    status: str  # VALID, INVALID, EXACT_DUPLICATE, POSSIBLE_DUPLICATE
    duplicate_reason: Optional[str] = None
    errors: List[str] = []
    matched_alumni_id: Optional[int] = None

class ImportPreviewResponse(BaseModel):
    import_session_id: str
    file_name: str
    total_rows: int
    valid_rows: int
    invalid_rows: int
    exact_duplicates: int
    possible_duplicates: int
    preview_rows: List[ImportPreviewRow]

class ImportCommitRequest(BaseModel):
    import_session_id: str
    mode: str = Field("SKIP_EXISTING", description="SKIP_EXISTING, UPDATE_EXISTING, CREATE_NEW_ONLY")

class ImportJobResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    import_session_id: str
    file_name: str
    total_rows: int
    created_count: int
    updated_count: int
    skipped_count: int
    duplicate_count: int
    status: str
    mode: Optional[str] = None
    created_at: datetime
    completed_at: Optional[datetime] = None

# --- Data Quality Schemas ---
class DataQualityResponse(BaseModel):
    total_alumni: int
    verified_count: int
    unverified_count: int
    average_completion_percentage: float
    missing_fields_count: Dict[str, int]
    completion_distribution: Dict[str, int]
    duplicate_candidates_count: int

# --- Audit Log Schemas ---
class AuditLogResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    admin_user_id: Optional[int] = None
    admin_name: Optional[str] = None
    action: str
    target_type: str
    target_id: Optional[str] = None
    details: Optional[str] = None
    ip_address: Optional[str] = None
    created_at: datetime

from datetime import datetime
from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey
from app.database.database import Base

class AlumniImportJob(Base):
    __tablename__ = "alumni_import_jobs"

    id = Column(Integer, primary_key=True, index=True)
    import_session_id = Column(String, unique=True, index=True, nullable=False)
    file_name = Column(String, nullable=False)
    total_rows = Column(Integer, default=0, nullable=False)
    created_count = Column(Integer, default=0, nullable=False)
    updated_count = Column(Integer, default=0, nullable=False)
    skipped_count = Column(Integer, default=0, nullable=False)
    duplicate_count = Column(Integer, default=0, nullable=False)
    status = Column(String, default="PREVIEWED", nullable=False, index=True)  # PREVIEWED, COMMITTED, FAILED
    mode = Column(String, nullable=True)  # SKIP_EXISTING, UPDATE_EXISTING, CREATE_NEW_ONLY
    raw_preview_json = Column(Text, nullable=True)
    performed_by = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False, index=True)
    completed_at = Column(DateTime, nullable=True)

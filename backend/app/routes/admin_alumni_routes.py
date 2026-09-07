import csv
import io
import json
import re
import uuid
from datetime import datetime
from typing import List, Optional, Dict, Any

from fastapi import APIRouter, Depends, HTTPException, Query, UploadFile, File, status
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from sqlalchemy import func, or_

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.import_job_model import AlumniImportJob
from app.models.segment_model import AlumniSegment
from app.models.audit_model import AdminAuditLog
from app.auth.jwt_dependency import require_role
from app.services.audit_service import log_admin_action
from app.services.segment_service import (
    build_segment_query,
    count_segment_members,
    get_segment_members,
    calculate_alumni_completion,
    ALUMNI_PROFILE_FIELDS
)
from app.schemas.admin_alumni_schema import (
    AlumniSegmentCreate,
    AlumniSegmentUpdate,
    AlumniSegmentResponse,
    ImportPreviewRow,
    ImportPreviewResponse,
    ImportCommitRequest,
    ImportJobResponse,
    DataQualityResponse,
    AuditLogResponse
)

router = APIRouter()

# Formula injection prevention helper
def sanitize_csv_cell(val: Any) -> str:
    if val is None:
        return ""
    text = str(val).strip()
    if text and text[0] in ("=", "+", "-", "@", "\t", "\r"):
        return f"'{text}"
    return text

# Email validation regex
EMAIL_REGEX = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")

# --- 1. CSV Template Download ---
@router.get("/alumni/template")
def download_alumni_csv_template(
    current_user: User = Depends(require_role(["admin"]))
):
    """Download standard CSV template for bulk alumni import."""
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow([
        "name",
        "email",
        "graduation_year",
        "department",
        "company",
        "job_role",
        "location",
        "skills",
        "bio",
        "linkedin_url"
    ])
    # Sample row
    writer.writerow([
        "Jane Doe",
        "jane.doe@example.com",
        "2022",
        "Computer Science",
        "Acme Technologies",
        "Software Engineer",
        "New York, NY",
        "Python, React, SQL",
        "Passionate full-stack developer",
        "https://linkedin.com/in/janedoe"
    ])
    output.seek(0)
    return StreamingResponse(
        io.BytesIO(output.getvalue().encode("utf-8")),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=alumni_import_template.csv"}
    )

# --- 2. Bulk Alumni CSV Import Preview ---
@router.post("/alumni/import/preview", response_model=ImportPreviewResponse)
async def preview_alumni_import(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Upload alumni CSV, validate structure, check duplicates, and store server preview."""
    if not file.filename.lower().endswith(".csv"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Uploaded file must be a CSV (.csv)"
        )

    content = await file.read()
    try:
        decoded = content.decode("utf-8-sig")
    except UnicodeDecodeError:
        try:
            decoded = content.decode("latin-1")
        except Exception:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Unable to decode file as UTF-8 or Latin-1."
            )

    reader = csv.DictReader(io.StringIO(decoded))
    if not reader.fieldnames:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="CSV file appears to be empty or has no header row."
        )

    header_map = {name.strip().lower(): name for name in reader.fieldnames if name}
    if "name" not in header_map or "email" not in header_map:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="CSV must contain at least 'name' and 'email' header columns."
        )

    preview_rows: List[ImportPreviewRow] = []
    seen_emails_in_file = set()
    exact_duplicates = 0
    possible_duplicates = 0
    invalid_rows = 0
    valid_rows = 0

    existing_alumni = db.query(Alumni).all()
    alumni_by_email = {a.email.strip().lower(): a for a in existing_alumni if a.email}
    alumni_by_name_dept = {}
    for a in existing_alumni:
        key = (a.name.strip().lower(), (a.department or "").strip().lower(), (a.graduation_year or "").strip())
        alumni_by_name_dept[key] = a

    row_number = 1
    for raw_row in reader:
        row_number += 1
        row_data = {k.strip().lower(): (v.strip() if v else "") for k, v in raw_row.items() if k}
        
        name = row_data.get("name", "")
        email = row_data.get("email", "")
        grad_year = row_data.get("graduation_year", "")
        dept = row_data.get("department", "")
        company = row_data.get("company", "")
        job_role = row_data.get("job_role", "")
        location = row_data.get("location", "")
        skills = row_data.get("skills", "")
        bio = row_data.get("bio", "")
        linkedin = row_data.get("linkedin_url", "")

        errors = []
        status_flag = "VALID"
        duplicate_reason = None
        matched_alumni_id = None

        if not name:
            errors.append("Name is required")
        if not email:
            errors.append("Email is required")
        elif not EMAIL_REGEX.match(email):
            errors.append("Invalid email format")

        email_lower = email.lower()
        if email_lower in seen_emails_in_file:
            errors.append("Duplicate email within this CSV file")
        seen_emails_in_file.add(email_lower)

        if errors:
            status_flag = "INVALID"
            invalid_rows += 1
        elif email_lower in alumni_by_email:
            status_flag = "EXACT_DUPLICATE"
            duplicate_reason = f"Existing alumni with email {email} already found (ID #{alumni_by_email[email_lower].id})"
            matched_alumni_id = alumni_by_email[email_lower].id
            exact_duplicates += 1
        else:
            name_key = (name.lower(), dept.lower(), grad_year)
            if name_key in alumni_by_name_dept:
                status_flag = "POSSIBLE_DUPLICATE"
                duplicate_reason = f"Possible duplicate: same name & department/year matches #{alumni_by_name_dept[name_key].id}"
                matched_alumni_id = alumni_by_name_dept[name_key].id
                possible_duplicates += 1
            else:
                valid_rows += 1

        preview_rows.append(
            ImportPreviewRow(
                row_number=row_number,
                name=name,
                email=email,
                graduation_year=grad_year or None,
                department=dept or None,
                company=company or None,
                job_role=job_role or None,
                location=location or None,
                skills=skills or None,
                bio=bio or None,
                linkedin_url=linkedin or None,
                status=status_flag,
                duplicate_reason=duplicate_reason,
                errors=errors,
                matched_alumni_id=matched_alumni_id
            )
        )

    session_id = str(uuid.uuid4())
    job = AlumniImportJob(
        import_session_id=session_id,
        file_name=file.filename,
        total_rows=len(preview_rows),
        created_count=0,
        updated_count=0,
        skipped_count=0,
        duplicate_count=exact_duplicates + possible_duplicates,
        status="PREVIEWED",
        raw_preview_json=json.dumps([r.model_dump() for r in preview_rows]),
        performed_by=current_user.id,
        created_at=datetime.utcnow()
    )
    db.add(job)
    db.commit()

    return ImportPreviewResponse(
        import_session_id=session_id,
        file_name=file.filename,
        total_rows=len(preview_rows),
        valid_rows=valid_rows,
        invalid_rows=invalid_rows,
        exact_duplicates=exact_duplicates,
        possible_duplicates=possible_duplicates,
        preview_rows=preview_rows
    )

# --- 3. Bulk Alumni CSV Import Commit ---
@router.post("/alumni/import/commit", response_model=ImportJobResponse)
def commit_alumni_import(
    request: ImportCommitRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Commit an authenticated import session with the selected resolution mode."""
    job = db.query(AlumniImportJob).filter(
        AlumniImportJob.import_session_id == request.import_session_id
    ).first()

    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Import session not found"
        )

    if job.status == "COMMITTED":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="This import session has already been committed."
        )

    mode = request.mode.upper().strip()
    if mode not in ("SKIP_EXISTING", "UPDATE_EXISTING", "CREATE_NEW_ONLY"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid mode. Allowed: SKIP_EXISTING, UPDATE_EXISTING, CREATE_NEW_ONLY"
        )

    if mode == "CREATE_NEW_ONLY" and job.duplicate_count > 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Cannot commit in CREATE_NEW_ONLY mode: {job.duplicate_count} duplicate record(s) were found."
        )

    preview_rows_data = json.loads(job.raw_preview_json or "[]")
    created_count = 0
    updated_count = 0
    skipped_count = 0

    try:
        for row in preview_rows_data:
            st = row.get("status")
            if st == "INVALID":
                skipped_count += 1
                continue

            email = row.get("email", "").strip().lower()

            if st == "EXACT_DUPLICATE":
                if mode in ("SKIP_EXISTING", "CREATE_NEW_ONLY"):
                    skipped_count += 1
                    continue
                elif mode == "UPDATE_EXISTING":
                    existing = db.query(Alumni).filter(func.lower(Alumni.email) == email).first()
                    if existing:
                        # SAFE UPDATE: Update only provided non-empty fields.
                        # Never overwrite is_verified or credentials!
                        if row.get("name") and str(row["name"]).strip():
                            existing.name = str(row["name"]).strip()
                        if row.get("graduation_year") and str(row["graduation_year"]).strip():
                            existing.graduation_year = str(row["graduation_year"]).strip()
                        if row.get("department") and str(row["department"]).strip():
                            existing.department = str(row["department"]).strip()
                        if row.get("company") and str(row["company"]).strip():
                            existing.company = str(row["company"]).strip()
                        if row.get("job_role") and str(row["job_role"]).strip():
                            existing.job_role = str(row["job_role"]).strip()
                        if row.get("location") and str(row["location"]).strip():
                            existing.location = str(row["location"]).strip()
                        if row.get("skills") and str(row["skills"]).strip():
                            existing.skills = str(row["skills"]).strip()
                        if row.get("bio") and str(row["bio"]).strip():
                            existing.bio = str(row["bio"]).strip()
                        if row.get("linkedin_url") and str(row["linkedin_url"]).strip():
                            existing.linkedin_url = str(row["linkedin_url"]).strip()
                        updated_count += 1
                    else:
                        skipped_count += 1

            elif st == "POSSIBLE_DUPLICATE":
                # Safe update rule: Never automatically merge possible duplicate records.
                if mode == "UPDATE_EXISTING":
                    skipped_count += 1
                else:
                    new_alumni = Alumni(
                        name=row.get("name", "").strip(),
                        email=row.get("email", "").strip(),
                        graduation_year=row.get("graduation_year") or None,
                        department=row.get("department") or None,
                        company=row.get("company") or None,
                        job_role=row.get("job_role") or None,
                        location=row.get("location") or None,
                        skills=row.get("skills") or None,
                        bio=row.get("bio") or None,
                        linkedin_url=row.get("linkedin_url") or None,
                        is_verified=False
                    )
                    db.add(new_alumni)
                    created_count += 1

            elif st == "VALID":
                new_alumni = Alumni(
                    name=row.get("name", "").strip(),
                    email=row.get("email", "").strip(),
                    graduation_year=row.get("graduation_year") or None,
                    department=row.get("department") or None,
                    company=row.get("company") or None,
                    job_role=row.get("job_role") or None,
                    location=row.get("location") or None,
                    skills=row.get("skills") or None,
                    bio=row.get("bio") or None,
                    linkedin_url=row.get("linkedin_url") or None,
                    is_verified=False
                )
                db.add(new_alumni)
                created_count += 1

        job.status = "COMMITTED"
        job.mode = mode
        job.created_count = created_count
        job.updated_count = updated_count
        job.skipped_count = skipped_count
        job.completed_at = datetime.utcnow()

        db.commit()
        db.refresh(job)

        log_admin_action(
            db=db,
            admin_user_id=current_user.id,
            action="IMPORT_COMMIT",
            target_type="IMPORT_JOB",
            target_id=str(job.id),
            details={
                "session_id": job.import_session_id,
                "file_name": job.file_name,
                "mode": mode,
                "created": created_count,
                "updated": updated_count,
                "skipped": skipped_count
            }
        )

        return job

    except Exception as exc:
        db.rollback()
        job.status = "FAILED"
        db.commit()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Import failed during commit transaction: {str(exc)}"
        )

# --- 4. Import History ---
@router.get("/alumni/import/history", response_model=List[ImportJobResponse])
def get_import_history(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """List previous bulk import jobs ordered by creation date."""
    jobs = db.query(AlumniImportJob).order_by(AlumniImportJob.created_at.desc()).limit(50).all()
    return jobs

# --- 5. Data Quality Dashboard ---
@router.get("/alumni/data-quality", response_model=DataQualityResponse)
def get_data_quality_metrics(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Institutional data quality dashboard with real DB metrics."""
    alumni_list = db.query(Alumni).all()
    total_alumni = len(alumni_list)

    if total_alumni == 0:
        return DataQualityResponse(
            total_alumni=0,
            verified_count=0,
            unverified_count=0,
            average_completion_percentage=0.0,
            missing_fields_count={f: 0 for f in ALUMNI_PROFILE_FIELDS},
            completion_distribution={"0-25%": 0, "26-50%": 0, "51-75%": 0, "76-100%": 0},
            duplicate_candidates_count=0
        )

    verified_count = sum(1 for a in alumni_list if a.is_verified)
    unverified_count = total_alumni - verified_count

    missing_fields = {f: 0 for f in ALUMNI_PROFILE_FIELDS}
    distribution = {"0-25%": 0, "26-50%": 0, "51-75%": 0, "76-100%": 0}
    total_completion_pct = 0

    for a in alumni_list:
        for f in ALUMNI_PROFILE_FIELDS:
            val = getattr(a, f, None)
            if not val or not str(val).strip():
                missing_fields[f] += 1

        pct = calculate_alumni_completion(a)
        total_completion_pct += pct

        if pct <= 25:
            distribution["0-25%"] += 1
        elif pct <= 50:
            distribution["26-50%"] += 1
        elif pct <= 75:
            distribution["51-75%"] += 1
        else:
            distribution["76-100%"] += 1

    avg_pct = round(total_completion_pct / total_alumni, 1)

    # Calculate duplicate candidates (same name and graduation_year, or same email)
    name_groups = (
        db.query(Alumni.name, Alumni.graduation_year, func.count(Alumni.id))
        .filter(Alumni.name.isnot(None), Alumni.graduation_year.isnot(None))
        .group_by(Alumni.name, Alumni.graduation_year)
        .having(func.count(Alumni.id) > 1)
        .all()
    )
    duplicate_candidates = sum(row[2] for row in name_groups)

    return DataQualityResponse(
        total_alumni=total_alumni,
        verified_count=verified_count,
        unverified_count=unverified_count,
        average_completion_percentage=avg_pct,
        missing_fields_count=missing_fields,
        completion_distribution=distribution,
        duplicate_candidates_count=duplicate_candidates
    )

# --- 6. Alumni Segments CRUD & Export ---

@router.post("/alumni/segments", response_model=AlumniSegmentResponse, status_code=status.HTTP_201_CREATED)
def create_alumni_segment(
    segment_in: AlumniSegmentCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Create a new dynamic alumni segment."""
    existing = db.query(AlumniSegment).filter(AlumniSegment.name == segment_in.name.strip()).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Segment with name '{segment_in.name}' already exists"
        )

    segment = AlumniSegment(
        name=segment_in.name.strip(),
        description=segment_in.description.strip() if segment_in.description else None,
        filters_json=json.dumps(segment_in.filters),
        created_by=current_user.id,
        created_at=datetime.utcnow(),
        updated_at=datetime.utcnow()
    )
    db.add(segment)
    db.commit()
    db.refresh(segment)

    log_admin_action(
        db=db,
        admin_user_id=current_user.id,
        action="CREATE_SEGMENT",
        target_type="SEGMENT",
        target_id=str(segment.id),
        details={"name": segment.name, "filters": segment_in.filters}
    )

    est_count = count_segment_members(db, segment_in.filters)
    return AlumniSegmentResponse(
        id=segment.id,
        name=segment.name,
        description=segment.description,
        filters=json.loads(segment.filters_json),
        estimated_count=est_count,
        created_by=segment.created_by,
        created_at=segment.created_at,
        updated_at=segment.updated_at
    )

@router.get("/alumni/segments", response_model=List[AlumniSegmentResponse])
def list_alumni_segments(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """List all saved segments with live estimated member count."""
    segments = db.query(AlumniSegment).order_by(AlumniSegment.created_at.desc()).all()
    results = []
    for s in segments:
        filters = json.loads(s.filters_json or "{}")
        est_count = count_segment_members(db, filters)
        results.append(
            AlumniSegmentResponse(
                id=s.id,
                name=s.name,
                description=s.description,
                filters=filters,
                estimated_count=est_count,
                created_by=s.created_by,
                created_at=s.created_at,
                updated_at=s.updated_at
            )
        )
    return results

@router.post("/alumni/segments/preview")
def preview_segment_criteria(
    filters: Dict[str, Any],
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Preview criteria match count and first 10 matching members without saving."""
    est_count = count_segment_members(db, filters)
    sample_members = get_segment_members(db, filters, limit=10, offset=0)
    return {
        "estimated_count": est_count,
        "sample": [
            {
                "id": m.id,
                "name": m.name,
                "email": m.email,
                "department": m.department,
                "graduation_year": m.graduation_year,
                "company": m.company,
                "job_role": m.job_role,
                "is_verified": m.is_verified
            }
            for m in sample_members
        ]
    }

@router.get("/alumni/segments/{segment_id}", response_model=AlumniSegmentResponse)
def get_segment_details(
    segment_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Get segment details by ID."""
    segment = db.query(AlumniSegment).filter(AlumniSegment.id == segment_id).first()
    if not segment:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Segment not found")

    filters = json.loads(segment.filters_json or "{}")
    est_count = count_segment_members(db, filters)

    return AlumniSegmentResponse(
        id=segment.id,
        name=segment.name,
        description=segment.description,
        filters=filters,
        estimated_count=est_count,
        created_by=segment.created_by,
        created_at=segment.created_at,
        updated_at=segment.updated_at
    )

@router.put("/alumni/segments/{segment_id}", response_model=AlumniSegmentResponse)
def update_alumni_segment(
    segment_id: int,
    segment_in: AlumniSegmentUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Update saved segment criteria or description."""
    segment = db.query(AlumniSegment).filter(AlumniSegment.id == segment_id).first()
    if not segment:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Segment not found")

    if segment_in.name is not None:
        trimmed = segment_in.name.strip()
        existing = db.query(AlumniSegment).filter(AlumniSegment.name == trimmed, AlumniSegment.id != segment_id).first()
        if existing:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Another segment already uses this name")
        segment.name = trimmed

    if segment_in.description is not None:
        segment.description = segment_in.description.strip()

    if segment_in.filters is not None:
        segment.filters_json = json.dumps(segment_in.filters)

    segment.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(segment)

    log_admin_action(
        db=db,
        admin_user_id=current_user.id,
        action="UPDATE_SEGMENT",
        target_type="SEGMENT",
        target_id=str(segment.id),
        details={"name": segment.name, "filters": json.loads(segment.filters_json)}
    )

    filters = json.loads(segment.filters_json)
    est_count = count_segment_members(db, filters)

    return AlumniSegmentResponse(
        id=segment.id,
        name=segment.name,
        description=segment.description,
        filters=filters,
        estimated_count=est_count,
        created_by=segment.created_by,
        created_at=segment.created_at,
        updated_at=segment.updated_at
    )

@router.delete("/alumni/segments/{segment_id}")
def delete_alumni_segment(
    segment_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Delete a saved segment."""
    segment = db.query(AlumniSegment).filter(AlumniSegment.id == segment_id).first()
    if not segment:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Segment not found")

    seg_name = segment.name
    db.delete(segment)
    db.commit()

    log_admin_action(
        db=db,
        admin_user_id=current_user.id,
        action="DELETE_SEGMENT",
        target_type="SEGMENT",
        target_id=str(segment_id),
        details={"name": seg_name}
    )

    return {"message": f"Segment '{seg_name}' deleted successfully"}

@router.get("/alumni/segments/{segment_id}/members")
def get_segment_members_list(
    segment_id: int,
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Paginated list of alumni members matching segment criteria."""
    segment = db.query(AlumniSegment).filter(AlumniSegment.id == segment_id).first()
    if not segment:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Segment not found")

    filters = json.loads(segment.filters_json or "{}")
    total = count_segment_members(db, filters)
    members = get_segment_members(db, filters, limit=limit, offset=offset)

    return {
        "segment_id": segment.id,
        "segment_name": segment.name,
        "total_count": total,
        "limit": limit,
        "offset": offset,
        "members": [
            {
                "id": m.id,
                "name": m.name,
                "email": m.email,
                "graduation_year": m.graduation_year,
                "department": m.department,
                "company": m.company,
                "job_role": m.job_role,
                "location": m.location,
                "skills": m.skills,
                "is_verified": m.is_verified,
                "completion_percentage": calculate_alumni_completion(m)
            }
            for m in members
        ]
    }

@router.get("/alumni/segments/{segment_id}/export")
def export_segment_members_csv(
    segment_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Export segment members as CSV with formula injection prevention."""
    segment = db.query(AlumniSegment).filter(AlumniSegment.id == segment_id).first()
    if not segment:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Segment not found")

    filters = json.loads(segment.filters_json or "{}")
    members = get_segment_members(db, filters, limit=5000, offset=0)

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow([
        "ID",
        "Name",
        "Email",
        "Graduation Year",
        "Department",
        "Company",
        "Job Role",
        "Location",
        "Skills",
        "Verified",
        "Completion %"
    ])

    for m in members:
        writer.writerow([
            sanitize_csv_cell(m.id),
            sanitize_csv_cell(m.name),
            sanitize_csv_cell(m.email),
            sanitize_csv_cell(m.graduation_year),
            sanitize_csv_cell(m.department),
            sanitize_csv_cell(m.company),
            sanitize_csv_cell(m.job_role),
            sanitize_csv_cell(m.location),
            sanitize_csv_cell(m.skills),
            "Yes" if m.is_verified else "No",
            sanitize_csv_cell(calculate_alumni_completion(m))
        ])

    clean_name = re.sub(r'[^a-zA-Z0-9_\-]', '_', segment.name.lower())
    output.seek(0)
    return StreamingResponse(
        io.BytesIO(output.getvalue().encode("utf-8")),
        media_type="text/csv",
        headers={"Content-Disposition": f"attachment; filename=segment_{clean_name}.csv"}
    )

# --- 7. Admin Audit Log ---
@router.get("/audit-logs", response_model=List[AuditLogResponse])
@router.get("/alumni/audit-logs", response_model=List[AuditLogResponse])
def get_audit_logs(
    action: Optional[str] = Query(None, description="Filter by action code"),
    target_type: Optional[str] = Query(None, description="Filter by target type"),
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    """Retrieve audit logs of administrative actions."""
    query = db.query(AdminAuditLog)
    if action:
        query = query.filter(AdminAuditLog.action == action.upper().strip())
    if target_type:
        query = query.filter(AdminAuditLog.target_type == target_type.upper().strip())

    logs = query.order_by(AdminAuditLog.created_at.desc()).offset(offset).limit(limit).all()

    admin_ids = {l.admin_user_id for l in logs if l.admin_user_id}
    admins = {}
    if admin_ids:
        for u in db.query(User).filter(User.id.in_(admin_ids)).all():
            admins[u.id] = u.name

    return [
        AuditLogResponse(
            id=l.id,
            admin_user_id=l.admin_user_id,
            admin_name=admins.get(l.admin_user_id, "System / Administrator"),
            action=l.action,
            target_type=l.target_type,
            target_id=l.target_id,
            details=l.details,
            ip_address=l.ip_address,
            created_at=l.created_at
        )
        for l in logs
    ]

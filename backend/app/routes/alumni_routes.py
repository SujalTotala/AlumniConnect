from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from sqlalchemy import or_

from app.database.db_dependency import get_db
from app.models.alumni_model import Alumni
from app.models.user_model import User
from app.schemas.alumni_schema import AlumniCreate, AlumniUpdate, AlumniResponse
from app.auth.jwt_dependency import get_current_user, get_optional_current_user, require_role

router = APIRouter()

# Get All Alumni with optional search and filters
@router.get("/", response_model=List[AlumniResponse])
def get_alumni(
    search: Optional[str] = Query(None, description="Search by name, department, company, skills, or location"),
    department: Optional[str] = Query(None, description="Filter by department/branch"),
    graduation_year: Optional[str] = Query(None, description="Filter by graduation year"),
    company: Optional[str] = Query(None, description="Filter by company"),
    location: Optional[str] = Query(None, description="Filter by location"),
    mentorship_available: Optional[bool] = Query(None, description="Filter by mentorship availability"),
    db: Session = Depends(get_db)
):
    query = db.query(Alumni)

    if search:
        search_pattern = f"%{search}%"
        query = query.filter(
            or_(
                Alumni.name.ilike(search_pattern),
                Alumni.email.ilike(search_pattern),
                Alumni.department.ilike(search_pattern),
                Alumni.company.ilike(search_pattern),
                Alumni.job_role.ilike(search_pattern),
                Alumni.skills.ilike(search_pattern),
                Alumni.location.ilike(search_pattern),
                Alumni.graduation_year.ilike(search_pattern),
            )
        )

    if department:
        query = query.filter(Alumni.department.ilike(f"%{department}%"))
    if graduation_year:
        query = query.filter(Alumni.graduation_year == graduation_year)
    if company:
        query = query.filter(Alumni.company.ilike(f"%{company}%"))
    if location:
        query = query.filter(Alumni.location.ilike(f"%{location}%"))
    if mentorship_available is not None:
        query = query.filter(Alumni.mentorship_available == mentorship_available)

    return query.order_by(Alumni.id.desc()).all()

# Get Single Alumni by ID
@router.get("/{alumni_id}", response_model=AlumniResponse)
def get_alumni_by_id(alumni_id: int, db: Session = Depends(get_db)):
    alumni = db.query(Alumni).filter(Alumni.id == alumni_id).first()
    if not alumni:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Alumni not found"
        )
    return alumni

# Helper function to add or update an alumni profile
def _create_or_update_alumni(alumni: AlumniCreate, db: Session, current_user: Optional[User] = None):
    existing = db.query(Alumni).filter(Alumni.email == alumni.email).first()
    if existing:
        # Update existing record
        for field, value in alumni.model_dump(exclude_unset=True).items():
            if value is not None:
                setattr(existing, field, value)
        if current_user and not existing.user_id:
            existing.user_id = current_user.id
        db.commit()
        db.refresh(existing)
        return {"message": "Alumni profile updated successfully", "id": existing.id}

    new_alumni = Alumni(
        user_id=current_user.id if current_user else None,
        name=alumni.name,
        email=alumni.email,
        graduation_year=alumni.graduation_year,
        department=alumni.department,
        company=alumni.company,
        job_role=alumni.job_role,
        location=alumni.location,
        skills=alumni.skills,
        bio=alumni.bio,
        linkedin_url=alumni.linkedin_url,
        github_url=alumni.github_url,
        mentorship_available=alumni.mentorship_available if alumni.mentorship_available is not None else False
    )

    db.add(new_alumni)
    db.commit()
    db.refresh(new_alumni)

    return {"message": "Alumni added successfully", "id": new_alumni.id}

# Add Alumni - Standard POST /alumni/
@router.post("/", status_code=status.HTTP_201_CREATED)
def create_alumni_standard(
    alumni: AlumniCreate,
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    return _create_or_update_alumni(alumni, db, current_user)

# Add Alumni - Legacy POST /alumni/add (Preserves backward compatibility)
@router.post("/add", status_code=status.HTTP_201_CREATED)
def create_alumni_legacy(
    alumni: AlumniCreate,
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    return _create_or_update_alumni(alumni, db, current_user)

# Update Alumni
@router.put("/{alumni_id}", response_model=AlumniResponse)
def update_alumni(
    alumni_id: int,
    alumni_update: AlumniUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    alumni = db.query(Alumni).filter(Alumni.id == alumni_id).first()
    if not alumni:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Alumni not found"
        )

    # Permission check: user can only edit their own profile unless admin
    if current_user.role.lower() != "admin" and alumni.email != current_user.email and alumni.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to update this alumni record"
        )

    update_data = alumni_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(alumni, field, value)

    db.commit()
    db.refresh(alumni)
    return alumni

# Delete Alumni (RBAC Protected: Admin or Alumni owner)
@router.delete("/{alumni_id}")
def delete_alumni(
    alumni_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    alumni = db.query(Alumni).filter(Alumni.id == alumni_id).first()

    if not alumni:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Alumni not found"
        )

    # Only admin or the alumni who owns the profile can delete it
    if current_user.role.lower() != "admin" and alumni.email != current_user.email and alumni.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to delete this alumni record"
        )

    db.delete(alumni)
    db.commit()

    return {"message": "Alumni deleted successfully"}
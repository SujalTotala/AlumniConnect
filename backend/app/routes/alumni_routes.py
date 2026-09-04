from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from sqlalchemy import or_

from app.database.db_dependency import get_db
from app.models.alumni_model import Alumni
from app.models.user_model import User
from app.models.profile_model import StudentProfile
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
    job_role: Optional[str] = Query(None, description="Filter by job role"),
    location: Optional[str] = Query(None, description="Filter by location"),
    skills: Optional[str] = Query(None, description="Filter by skills"),
    is_verified: Optional[bool] = Query(None, description="Filter by verified status"),
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
    if job_role:
        query = query.filter(Alumni.job_role.ilike(f"%{job_role}%"))
    if location:
        query = query.filter(Alumni.location.ilike(f"%{location}%"))
    if skills:
        query = query.filter(Alumni.skills.ilike(f"%{skills}%"))
    if is_verified is not None:
        query = query.filter(Alumni.is_verified == is_verified)
    if mentorship_available is not None:
        query = query.filter(Alumni.mentorship_available == mentorship_available)

    return query.order_by(Alumni.id.desc()).all()

# Get Rule-Based Recommended Mentors (Must be before /{alumni_id})
@router.get("/recommendations/mentors")
def get_recommended_mentors(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    # Fetch current user profile characteristics
    user_branch = ""
    user_skills = ""
    user_interests = ""

    if current_user.role == "student":
        sp = db.query(StudentProfile).filter(StudentProfile.user_id == current_user.id).first()
        if sp:
            user_branch = sp.branch or ""
            user_skills = sp.skills or ""
            user_interests = sp.interests or ""
    elif current_user.role == "alumni":
        ap = db.query(Alumni).filter(Alumni.user_id == current_user.id).first()
        if ap:
            user_branch = ap.department or ""
            user_skills = ap.skills or ""
            user_interests = ap.job_role or ap.company or ""

    # Query available mentors
    mentors_query = db.query(Alumni).filter(Alumni.mentorship_available == True)
    if current_user.role == "alumni":
        mentors_query = mentors_query.filter(Alumni.user_id != current_user.id)

    available_mentors = mentors_query.all()
    recommendations = []

    for mentor in available_mentors:
        raw_score = 0
        max_available = 0
        match_reasons = []

        # Factor 1: Department / Branch overlap (Weight: 40)
        if user_branch and mentor.department:
            max_available += 40
            ub = user_branch.strip().lower()
            md = mentor.department.strip().lower()
            if ub in md or md in ub:
                raw_score += 40
                match_reasons.append(f"Same department: {mentor.department}")

        # Factor 2: Skill Overlap (Weight: 35)
        if user_skills and mentor.skills:
            max_available += 35
            u_tokens = {s.strip().lower() for s in user_skills.replace(",", " ").split() if len(s.strip()) > 1}
            m_tokens = {s.strip().lower() for s in mentor.skills.replace(",", " ").split() if len(s.strip()) > 1}
            common = u_tokens.intersection(m_tokens)
            if common:
                ratio = len(common) / max(len(u_tokens), 1)
                earned = int(35 * min(ratio, 1.0))
                raw_score += max(earned, 15)  # At least 15 if there is overlap
                match_reasons.append(f"Matching skills: {', '.join(sorted(common)[:3]).title()}")

        # Factor 3: Interest & Desired Role / Company Alignment (Weight: 25)
        if user_interests and (mentor.job_role or mentor.company):
            max_available += 25
            u_interests = {t.strip().lower() for t in user_interests.replace(",", " ").split() if len(t.strip()) > 2}
            target_tokens = set()
            if mentor.job_role:
                target_tokens.update(t.strip().lower() for t in mentor.job_role.replace(",", " ").split() if len(t.strip()) > 2)
            if mentor.company:
                target_tokens.update(t.strip().lower() for t in mentor.company.replace(",", " ").split() if len(t.strip()) > 2)

            overlap = u_interests.intersection(target_tokens)
            if overlap:
                raw_score += 25
                target = mentor.job_role or mentor.company
                match_reasons.append(f"Career alignment: {target}")

        # Compute normalized score (0–100)
        if max_available > 0:
            match_score = round((raw_score / max_available) * 100)
        else:
            # Baseline compatibility when profile is still empty
            match_score = 50
            if mentor.department:
                match_reasons.append(f"Available mentor in {mentor.department}")
            else:
                match_reasons.append("Active mentor available for guidance")

        if not match_reasons:
            match_reasons.append("Verified available mentor in Alumni network")

        recommendations.append({
            "alumni": AlumniResponse.model_validate(mentor),
            "match_score": match_score,
            "match_reasons": match_reasons,
        })

    # Sort descending by compatibility score
    recommendations.sort(key=lambda r: r["match_score"], reverse=True)
    return recommendations[:10]

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
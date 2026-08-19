from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.profile_model import StudentProfile
from app.schemas.profile_schema import ProfileMeResponse, ProfileMeUpdate
from app.auth.jwt_dependency import get_current_user

router = APIRouter()

@router.get("/me", response_model=ProfileMeResponse)
def get_my_profile(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    profile_data = {}
    if current_user.role.lower() == "alumni":
        alumni = db.query(Alumni).filter(
            (Alumni.user_id == current_user.id) | (Alumni.email == current_user.email)
        ).first()
        if alumni:
            profile_data = {
                "graduation_year": alumni.graduation_year,
                "department": alumni.department,
                "company": alumni.company,
                "job_role": alumni.job_role,
                "location": alumni.location,
                "skills": alumni.skills,
                "bio": alumni.bio,
                "linkedin_url": alumni.linkedin_url,
                "github_url": alumni.github_url,
                "mentorship_available": alumni.mentorship_available,
            }
    elif current_user.role.lower() == "student":
        student = db.query(StudentProfile).filter(StudentProfile.user_id == current_user.id).first()
        if student:
            profile_data = {
                "branch": student.branch,
                "year": student.year,
                "skills": student.skills,
                "interests": student.interests,
                "bio": student.bio,
                "profile_image_url": student.profile_image_url,
            }

    return {
        "id": current_user.id,
        "name": current_user.name,
        "email": current_user.email,
        "role": current_user.role,
        "is_active": current_user.is_active,
        "profile": profile_data
    }

@router.put("/me", response_model=ProfileMeResponse)
def update_my_profile(
    update_data: ProfileMeUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    if update_data.name:
        current_user.name = update_data.name

    if current_user.role.lower() == "alumni":
        alumni = db.query(Alumni).filter(
            (Alumni.user_id == current_user.id) | (Alumni.email == current_user.email)
        ).first()
        if not alumni:
            alumni = Alumni(
                user_id=current_user.id,
                name=current_user.name,
                email=current_user.email
            )
            db.add(alumni)

        if update_data.name:
            alumni.name = update_data.name
        if update_data.graduation_year is not None:
            alumni.graduation_year = update_data.graduation_year
        if update_data.department is not None:
            alumni.department = update_data.department
        if update_data.company is not None:
            alumni.company = update_data.company
        if update_data.job_role is not None:
            alumni.job_role = update_data.job_role
        if update_data.location is not None:
            alumni.location = update_data.location
        if update_data.skills is not None:
            alumni.skills = update_data.skills
        if update_data.bio is not None:
            alumni.bio = update_data.bio
        if update_data.linkedin_url is not None:
            alumni.linkedin_url = update_data.linkedin_url
        if update_data.github_url is not None:
            alumni.github_url = update_data.github_url
        if update_data.mentorship_available is not None:
            alumni.mentorship_available = update_data.mentorship_available

    elif current_user.role.lower() == "student":
        student = db.query(StudentProfile).filter(StudentProfile.user_id == current_user.id).first()
        if not student:
            student = StudentProfile(user_id=current_user.id)
            db.add(student)

        if update_data.branch is not None:
            student.branch = update_data.branch
        if update_data.year is not None:
            student.year = update_data.year
        if update_data.skills is not None:
            student.skills = update_data.skills
        if update_data.interests is not None:
            student.interests = update_data.interests
        if update_data.bio is not None:
            student.bio = update_data.bio
        if update_data.profile_image_url is not None:
            student.profile_image_url = update_data.profile_image_url

    db.commit()
    db.refresh(current_user)

    return get_my_profile(current_user=current_user, db=db)

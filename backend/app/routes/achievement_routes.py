from datetime import datetime
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.achievement_model import Achievement
from app.schemas.achievement_schema import (
    AchievementCreate,
    AchievementUpdate,
    AchievementResponse
)
from app.auth.jwt_dependency import get_current_user, get_optional_current_user, require_role
from app.services.notification_service import create_notification
from app.services.audit_service import log_admin_action

router = APIRouter()

def format_achievement_response(ach: Achievement, db: Session) -> dict:
    alumni = None
    if ach.alumni_id:
        alumni = db.query(Alumni).filter(Alumni.id == ach.alumni_id).first()
    elif ach.user_id:
        alumni = db.query(Alumni).filter(Alumni.user_id == ach.user_id).first()

    u = db.query(User).filter(User.id == ach.user_id).first()
    is_verified = alumni.is_verified if alumni and alumni.is_verified else False

    return {
        "id": ach.id,
        "alumni_id": ach.alumni_id,
        "user_id": ach.user_id,
        "alumni_name": alumni.name if alumni else (u.name if u else "Alumni Member"),
        "alumni_company": alumni.company if alumni else None,
        "alumni_job_role": alumni.job_role if alumni else None,
        "is_verified": is_verified,
        "title": ach.title,
        "description": ach.description,
        "category": ach.category,
        "organization": ach.organization,
        "achievement_date": ach.achievement_date,
        "proof_url": ach.proof_url,
        "status": ach.status,
        "created_at": ach.created_at,
        "updated_at": ach.updated_at
    }

# 1. Submit Achievement (Verified Alumni or Admin)
@router.post("/", response_model=AchievementResponse, status_code=status.HTTP_201_CREATED)
def submit_achievement(
    ach_in: AchievementCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    alumni = db.query(Alumni).filter(Alumni.user_id == current_user.id).first()
    is_verified = (alumni is not None and alumni.is_verified) or (current_user.role.lower() == "admin")

    if not is_verified:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only verified alumni can submit achievements for recognition."
        )

    valid_categories = {
        "AWARD", "CERTIFICATION", "PROMOTION", "PUBLICATION",
        "ENTREPRENEURSHIP", "SOCIAL_IMPACT", "OTHER"
    }
    cat = ach_in.category.strip().upper()
    if cat not in valid_categories:
        cat = "OTHER"

    achievement = Achievement(
        alumni_id=alumni.id if alumni else None,
        user_id=current_user.id,
        title=ach_in.title.strip(),
        description=ach_in.description.strip(),
        category=cat,
        organization=ach_in.organization.strip() if ach_in.organization else None,
        achievement_date=ach_in.achievement_date,
        proof_url=ach_in.proof_url.strip() if ach_in.proof_url else None,
        status="PENDING",
        created_at=datetime.utcnow(),
        updated_at=datetime.utcnow()
    )
    db.add(achievement)
    db.commit()
    db.refresh(achievement)

    return format_achievement_response(achievement, db)

# 2. Get Public Approved Achievements
@router.get("/", response_model=List[AchievementResponse])
def get_approved_achievements(
    category: Optional[str] = Query(None, description="Filter by category"),
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    query = db.query(Achievement).filter(Achievement.status == "APPROVED")
    if category:
        query = query.filter(Achievement.category == category.strip().upper())

    achievements = query.order_by(Achievement.created_at.desc()).all()
    return [format_achievement_response(a, db) for a in achievements]

# 3. Get Current User's Achievements
@router.get("/mine", response_model=List[AchievementResponse])
def get_my_achievements(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    achievements = db.query(Achievement).filter(
        Achievement.user_id == current_user.id
    ).order_by(Achievement.created_at.desc()).all()

    return [format_achievement_response(a, db) for a in achievements]

# 4. Get Achievements for an Alumni Profile
@router.get("/alumni/{alumni_id}", response_model=List[AchievementResponse])
def get_achievements_for_alumni(
    alumni_id: int,
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    alumni = db.query(Alumni).filter(Alumni.id == alumni_id).first()
    if not alumni:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Alumni not found"
        )

    # Match by alumni_id or user_id
    query = db.query(Achievement).filter(
        Achievement.status == "APPROVED"
    )
    if alumni.user_id:
        query = query.filter(
            (Achievement.alumni_id == alumni.id) | (Achievement.user_id == alumni.user_id)
        )
    else:
        query = query.filter(Achievement.alumni_id == alumni.id)

    achievements = query.order_by(Achievement.created_at.desc()).all()
    return [format_achievement_response(a, db) for a in achievements]

# 5. Get Single Achievement Details
@router.get("/{achievement_id}", response_model=AchievementResponse)
def get_achievement_by_id(
    achievement_id: int,
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )

    is_author = current_user and achievement.user_id == current_user.id
    is_admin = current_user and current_user.role.lower() == "admin"
    if achievement.status != "APPROVED" and not is_author and not is_admin:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="This achievement is awaiting moderation"
        )

    return format_achievement_response(achievement, db)

# 6. Delete Achievement (Author or Admin)
@router.delete("/{achievement_id}")
def delete_achievement(
    achievement_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )

    if achievement.user_id != current_user.id and current_user.role.lower() != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to delete this achievement"
        )

    db.delete(achievement)
    db.commit()
    return {"message": "Achievement deleted successfully"}

# --- ADMIN MODERATION ROUTES ---

@router.get("/admin/pending", response_model=List[AchievementResponse])
def get_pending_achievements(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    achievements = db.query(Achievement).filter(
        Achievement.status == "PENDING"
    ).order_by(Achievement.created_at.desc()).all()

    return [format_achievement_response(a, db) for a in achievements]

@router.put("/admin/{achievement_id}/approve", response_model=AchievementResponse)
def approve_achievement(
    achievement_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )

    achievement.status = "APPROVED"
    achievement.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(achievement)

    log_admin_action(
        db=db,
        admin_user_id=current_user.id,
        action="MODERATE_ACHIEVEMENT",
        target_type="ACHIEVEMENT",
        target_id=str(achievement.id),
        details={"title": achievement.title, "status": "APPROVED"}
    )

    create_notification(
        db=db,
        user_id=achievement.user_id,
        title="Achievement Approved! 🏆",
        message=f"Your achievement '{achievement.title}' has been approved and added to your profile recognition badges.",
        notification_type="GENERAL"
    )

    return format_achievement_response(achievement, db)

@router.put("/admin/{achievement_id}/reject", response_model=AchievementResponse)
def reject_achievement(
    achievement_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )

    achievement.status = "REJECTED"
    achievement.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(achievement)

    log_admin_action(
        db=db,
        admin_user_id=current_user.id,
        action="MODERATE_ACHIEVEMENT",
        target_type="ACHIEVEMENT",
        target_id=str(achievement.id),
        details={"title": achievement.title, "status": "REJECTED"}
    )

    create_notification(
        db=db,
        user_id=achievement.user_id,
        title="Achievement Review Update",
        message=f"Your achievement submission '{achievement.title}' was reviewed and not approved at this time.",
        notification_type="GENERAL"
    )

    return format_achievement_response(achievement, db)

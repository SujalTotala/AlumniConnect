from datetime import datetime
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.story_model import SuccessStory
from app.schemas.story_schema import (
    SuccessStoryCreate,
    SuccessStoryUpdate,
    SuccessStoryResponse
)
from app.auth.jwt_dependency import get_current_user, get_optional_current_user, require_role
from app.services.notification_service import create_notification
from app.services.audit_service import log_admin_action

router = APIRouter()

def format_story_response(story: SuccessStory, db: Session) -> dict:
    author = db.query(User).filter(User.id == story.author_user_id).first()
    alumni = None
    if story.alumni_id:
        alumni = db.query(Alumni).filter(Alumni.id == story.alumni_id).first()
    elif author:
        alumni = db.query(Alumni).filter(Alumni.user_id == author.id).first()

    is_verified = alumni.is_verified if alumni and alumni.is_verified else False

    return {
        "id": story.id,
        "author_user_id": story.author_user_id,
        "alumni_id": story.alumni_id,
        "author_name": author.name if author else "Alumni Member",
        "author_email": author.email if author else "",
        "is_verified": is_verified,
        "title": story.title,
        "summary": story.summary,
        "content": story.content,
        "company": story.company or (alumni.company if alumni else None),
        "role": story.role or (alumni.job_role if alumni else None),
        "achievement_date": story.achievement_date,
        "image_url": story.image_url,
        "status": story.status,
        "created_at": story.created_at,
        "updated_at": story.updated_at
    }

# 1. Submit Success Story (Verified Alumni or Admin)
@router.post("/", response_model=SuccessStoryResponse, status_code=status.HTTP_201_CREATED)
def submit_success_story(
    story_in: SuccessStoryCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    alumni = db.query(Alumni).filter(Alumni.user_id == current_user.id).first()
    is_verified = (alumni is not None and alumni.is_verified) or (current_user.role.lower() == "admin")

    if not is_verified:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only verified alumni can submit success stories."
        )

    story = SuccessStory(
        author_user_id=current_user.id,
        alumni_id=alumni.id if alumni else None,
        title=story_in.title.strip(),
        summary=story_in.summary.strip(),
        content=story_in.content.strip(),
        company=story_in.company.strip() if story_in.company else (alumni.company if alumni else None),
        role=story_in.role.strip() if story_in.role else (alumni.job_role if alumni else None),
        achievement_date=story_in.achievement_date,
        image_url=story_in.image_url,
        status="PENDING",
        created_at=datetime.utcnow(),
        updated_at=datetime.utcnow()
    )
    db.add(story)
    db.commit()
    db.refresh(story)

    return format_story_response(story, db)

# 2. Get Public Approved Success Stories
@router.get("/", response_model=List[SuccessStoryResponse])
def get_approved_stories(
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    stories = db.query(SuccessStory).filter(
        SuccessStory.status == "APPROVED"
    ).order_by(SuccessStory.created_at.desc()).all()

    return [format_story_response(s, db) for s in stories]

# 3. Get Current User's Stories
@router.get("/mine", response_model=List[SuccessStoryResponse])
def get_my_stories(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    stories = db.query(SuccessStory).filter(
        SuccessStory.author_user_id == current_user.id
    ).order_by(SuccessStory.created_at.desc()).all()

    return [format_story_response(s, db) for s in stories]

# 4. Get Story Details
@router.get("/{story_id}", response_model=SuccessStoryResponse)
def get_story_by_id(
    story_id: int,
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    story = db.query(SuccessStory).filter(SuccessStory.id == story_id).first()
    if not story:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Success story not found"
        )

    # Allow if approved, or if author, or if admin
    is_author = current_user and story.author_user_id == current_user.id
    is_admin = current_user and current_user.role.lower() == "admin"
    if story.status != "APPROVED" and not is_author and not is_admin:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="This success story is awaiting moderation"
        )

    return format_story_response(story, db)

# 5. Update Story (Author only while pending, or Admin)
@router.put("/{story_id}", response_model=SuccessStoryResponse)
def update_story(
    story_id: int,
    story_in: SuccessStoryUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    story = db.query(SuccessStory).filter(SuccessStory.id == story_id).first()
    if not story:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Success story not found"
        )

    if story.author_user_id != current_user.id and current_user.role.lower() != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to update this story"
        )

    if story_in.title is not None:
        story.title = story_in.title.strip()
    if story_in.summary is not None:
        story.summary = story_in.summary.strip()
    if story_in.content is not None:
        story.content = story_in.content.strip()
    if story_in.company is not None:
        story.company = story_in.company.strip()
    if story_in.role is not None:
        story.role = story_in.role.strip()
    if story_in.achievement_date is not None:
        story.achievement_date = story_in.achievement_date
    if story_in.image_url is not None:
        story.image_url = story_in.image_url

    story.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(story)

    return format_story_response(story, db)

# 6. Delete Story (Author or Admin)
@router.delete("/{story_id}")
def delete_story(
    story_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    story = db.query(SuccessStory).filter(SuccessStory.id == story_id).first()
    if not story:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Success story not found"
        )

    if story.author_user_id != current_user.id and current_user.role.lower() != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to delete this story"
        )

    db.delete(story)
    db.commit()
    return {"message": "Success story deleted successfully"}

# --- ADMIN MODERATION ROUTES ---

@router.get("/admin/pending", response_model=List[SuccessStoryResponse])
def get_pending_stories(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    stories = db.query(SuccessStory).filter(
        SuccessStory.status == "PENDING"
    ).order_by(SuccessStory.created_at.desc()).all()

    return [format_story_response(s, db) for s in stories]

@router.put("/admin/{story_id}/approve", response_model=SuccessStoryResponse)
def approve_story(
    story_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    story = db.query(SuccessStory).filter(SuccessStory.id == story_id).first()
    if not story:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Success story not found"
        )

    story.status = "APPROVED"
    story.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(story)

    log_admin_action(
        db=db,
        admin_user_id=current_user.id,
        action="MODERATE_STORY",
        target_type="STORY",
        target_id=str(story.id),
        details={"title": story.title, "status": "APPROVED"}
    )

    create_notification(
        db=db,
        user_id=story.author_user_id,
        title="Success Story Approved! 🎉",
        message=f"Your success story '{story.title}' has been approved and published to the alumni community.",
        notification_type="GENERAL"
    )

    return format_story_response(story, db)

@router.put("/admin/{story_id}/reject", response_model=SuccessStoryResponse)
def reject_story(
    story_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"]))
):
    story = db.query(SuccessStory).filter(SuccessStory.id == story_id).first()
    if not story:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Success story not found"
        )

    story.status = "REJECTED"
    story.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(story)

    log_admin_action(
        db=db,
        admin_user_id=current_user.id,
        action="MODERATE_STORY",
        target_type="STORY",
        target_id=str(story.id),
        details={"title": story.title, "status": "REJECTED"}
    )

    create_notification(
        db=db,
        user_id=story.author_user_id,
        title="Success Story Update",
        message=f"Your success story submission '{story.title}' was reviewed and not approved at this time.",
        notification_type="GENERAL"
    )

    return format_story_response(story, db)

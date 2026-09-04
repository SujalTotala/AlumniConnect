from typing import List, Optional
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from sqlalchemy import or_, desc, case

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.announcement_model import Announcement
from app.schemas.announcement_schema import AnnouncementCreate, AnnouncementUpdate, AnnouncementResponse
from app.auth.jwt_dependency import get_current_user, require_role

router = APIRouter()

@router.get("/", response_model=List[AnnouncementResponse])
def get_announcements(
    include_expired: bool = Query(False, description="Include expired announcements (Admin only)"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    query = db.query(Announcement)

    # Only admins can request expired announcements
    if not include_expired or current_user.role != "admin":
        now = datetime.utcnow()
        query = query.filter(or_(Announcement.expires_at.is_(None), Announcement.expires_at >= now))

    # Priority ordering: URGENT > HIGH > NORMAL, then newest created_at
    priority_order = case(
        (Announcement.priority == "URGENT", 1),
        (Announcement.priority == "HIGH", 2),
        else_=3,
    )

    announcements = query.order_by(priority_order, desc(Announcement.created_at)).all()

    results = []
    author_ids = {a.created_by for a in announcements if a.created_by}
    authors = {}
    if author_ids:
        for u in db.query(User).filter(User.id.in_(author_ids)).all():
            authors[u.id] = u.name

    for a in announcements:
        results.append(
            AnnouncementResponse(
                id=a.id,
                title=a.title,
                content=a.content,
                category=a.category,
                priority=a.priority,
                created_by=a.created_by,
                author_name=authors.get(a.created_by, "Administrator"),
                created_at=a.created_at,
                expires_at=a.expires_at,
            )
        )

    return results

@router.post("/", response_model=AnnouncementResponse, status_code=status.HTTP_201_CREATED)
def create_announcement(
    announcement_data: AnnouncementCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"])),
):
    announcement = Announcement(
        title=announcement_data.title.strip(),
        content=announcement_data.content.strip(),
        category=(announcement_data.category or "GENERAL").upper().strip(),
        priority=(announcement_data.priority or "NORMAL").upper().strip(),
        created_by=current_user.id,
        expires_at=announcement_data.expires_at,
    )
    db.add(announcement)
    db.commit()
    db.refresh(announcement)

    return AnnouncementResponse(
        id=announcement.id,
        title=announcement.title,
        content=announcement.content,
        category=announcement.category,
        priority=announcement.priority,
        created_by=announcement.created_by,
        author_name=current_user.name,
        created_at=announcement.created_at,
        expires_at=announcement.expires_at,
    )

@router.put("/{announcement_id}", response_model=AnnouncementResponse)
def update_announcement(
    announcement_id: int,
    data: AnnouncementUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"])),
):
    announcement = db.query(Announcement).filter(Announcement.id == announcement_id).first()
    if not announcement:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Announcement not found")

    if data.title is not None:
        announcement.title = data.title.strip()
    if data.content is not None:
        announcement.content = data.content.strip()
    if data.category is not None:
        announcement.category = data.category.upper().strip()
    if data.priority is not None:
        announcement.priority = data.priority.upper().strip()
    if data.expires_at is not None:
        announcement.expires_at = data.expires_at

    db.commit()
    db.refresh(announcement)

    author_name = current_user.name
    if announcement.created_by and announcement.created_by != current_user.id:
        creator = db.query(User).filter(User.id == announcement.created_by).first()
        if creator:
            author_name = creator.name

    return AnnouncementResponse(
        id=announcement.id,
        title=announcement.title,
        content=announcement.content,
        category=announcement.category,
        priority=announcement.priority,
        created_by=announcement.created_by,
        author_name=author_name,
        created_at=announcement.created_at,
        expires_at=announcement.expires_at,
    )

@router.delete("/{announcement_id}")
def delete_announcement(
    announcement_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin"])),
):
    announcement = db.query(Announcement).filter(Announcement.id == announcement_id).first()
    if not announcement:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Announcement not found")

    db.delete(announcement)
    db.commit()
    return {"message": "Announcement deleted successfully"}

from typing import List, Optional, Set
import json
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from sqlalchemy import or_, desc, case, func

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.segment_model import AlumniSegment
from app.models.announcement_model import Announcement
from app.models.preference_model import NotificationPreference
from app.schemas.announcement_schema import AnnouncementCreate, AnnouncementUpdate, AnnouncementResponse
from app.auth.jwt_dependency import get_current_user, require_role
from app.services.audit_service import log_admin_action
from app.services.segment_service import alumni_matches_criteria, get_segment_members
from app.services.notification_service import create_notification

router = APIRouter()

@router.get("/", response_model=List[AnnouncementResponse])
def get_announcements(
    include_expired: bool = Query(False, description="Include expired announcements (Admin only)"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    query = db.query(Announcement)

    # Only admins can request expired announcements
    if not include_expired or current_user.role.lower() != "admin":
        now = datetime.utcnow()
        query = query.filter(or_(Announcement.expires_at.is_(None), Announcement.expires_at >= now))

    role = current_user.role.lower()
    if role == "student":
        query = query.filter(Announcement.audience_type.in_(["ALL_USERS", "STUDENTS"]))
    elif role == "alumni":
        # Find current alumni record
        alumni = db.query(Alumni).filter(
            or_(Alumni.user_id == current_user.id, Alumni.email == current_user.email)
        ).first()

        # Find matching segments for this alumni
        all_segments = db.query(AlumniSegment).all()
        matching_seg_ids = []
        if alumni:
            for seg in all_segments:
                filters = json.loads(seg.filters_json or "{}")
                if alumni_matches_criteria(alumni, filters):
                    matching_seg_ids.append(seg.id)

        audience_conditions = [Announcement.audience_type.in_(["ALL_USERS", "ALUMNI"])]
        if matching_seg_ids:
            audience_conditions.append(
                (Announcement.audience_type == "SEGMENT") & (Announcement.segment_id.in_(matching_seg_ids))
            )
        query = query.filter(or_(*audience_conditions))

    # Priority ordering: URGENT > HIGH > NORMAL, then newest created_at
    priority_order = case(
        (Announcement.priority == "URGENT", 1),
        (Announcement.priority == "HIGH", 2),
        else_=3,
    )

    announcements = query.order_by(priority_order, desc(Announcement.created_at)).all()

    author_ids = {a.created_by for a in announcements if a.created_by}
    authors = {}
    if author_ids:
        for u in db.query(User).filter(User.id.in_(author_ids)).all():
            authors[u.id] = u.name

    results = []
    for a in announcements:
        results.append(
            AnnouncementResponse(
                id=a.id,
                title=a.title,
                content=a.content,
                category=a.category,
                priority=a.priority,
                audience_type=a.audience_type,
                segment_id=a.segment_id,
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
    audience = (announcement_data.audience_type or "ALL_USERS").upper().strip()
    if audience not in ("ALL_USERS", "STUDENTS", "ALUMNI", "SEGMENT"):
        audience = "ALL_USERS"

    seg_id = announcement_data.segment_id
    if audience == "SEGMENT":
        if not seg_id:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="A valid segment_id must be provided when audience_type is 'SEGMENT'."
            )
        segment = db.query(AlumniSegment).filter(AlumniSegment.id == seg_id).first()
        if not segment:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Segment #{seg_id} does not exist."
            )

    announcement = Announcement(
        title=announcement_data.title.strip(),
        content=announcement_data.content.strip(),
        category=(announcement_data.category or "GENERAL").upper().strip(),
        priority=(announcement_data.priority or "NORMAL").upper().strip(),
        audience_type=audience,
        segment_id=seg_id if audience == "SEGMENT" else None,
        created_by=current_user.id,
        expires_at=announcement_data.expires_at,
    )
    db.add(announcement)
    db.commit()
    db.refresh(announcement)

    # Log admin audit log
    log_admin_action(
        db=db,
        admin_user_id=current_user.id,
        action="CREATE_ANNOUNCEMENT",
        target_type="ANNOUNCEMENT",
        target_id=str(announcement.id),
        details={
            "title": announcement.title,
            "category": announcement.category,
            "priority": announcement.priority,
            "audience_type": announcement.audience_type,
            "segment_id": announcement.segment_id
        }
    )

    # In-app notification dispatch respecting NotificationPreference.announcements
    target_user_ids: Set[int] = set()
    if audience == "ALL_USERS":
        users = db.query(User.id).filter(User.id != current_user.id).all()
        target_user_ids = {u[0] for u in users}
    elif audience == "STUDENTS":
        users = db.query(User.id).filter(User.role == "student", User.id != current_user.id).all()
        target_user_ids = {u[0] for u in users}
    elif audience == "ALUMNI":
        users = db.query(User.id).filter(User.role == "alumni", User.id != current_user.id).all()
        target_user_ids = {u[0] for u in users}
    elif audience == "SEGMENT" and seg_id:
        segment = db.query(AlumniSegment).filter(AlumniSegment.id == seg_id).first()
        if segment:
            filters = json.loads(segment.filters_json or "{}")
            members = get_segment_members(db, filters, limit=5000, offset=0)
            member_emails = [m.email.lower() for m in members if m.email]
            if member_emails:
                users = db.query(User.id).filter(func.lower(User.email).in_(member_emails), User.id != current_user.id).all()
                target_user_ids = {u[0] for u in users}

    if target_user_ids:
        # Exclude users with disabled announcement notifications
        opted_out = db.query(NotificationPreference.user_id).filter(
            NotificationPreference.user_id.in_(target_user_ids),
            NotificationPreference.announcements == False
        ).all()
        opted_out_ids = {o[0] for o in opted_out}
        active_recipients = target_user_ids - opted_out_ids

        for uid in active_recipients:
            create_notification(
                db=db,
                user_id=uid,
                title=f"New Announcement: {announcement.title}",
                message=announcement.content[:160] + ("..." if len(announcement.content) > 160 else ""),
                notification_type="GENERAL"
            )

    return AnnouncementResponse(
        id=announcement.id,
        title=announcement.title,
        content=announcement.content,
        category=announcement.category,
        priority=announcement.priority,
        audience_type=announcement.audience_type,
        segment_id=announcement.segment_id,
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
    if data.audience_type is not None:
        aud = data.audience_type.upper().strip()
        if aud in ("ALL_USERS", "STUDENTS", "ALUMNI", "SEGMENT"):
            announcement.audience_type = aud
    if data.segment_id is not None:
        announcement.segment_id = data.segment_id
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
        audience_type=announcement.audience_type,
        segment_id=announcement.segment_id,
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

    title = announcement.title
    db.delete(announcement)
    db.commit()

    log_admin_action(
        db=db,
        admin_user_id=current_user.id,
        action="DELETE_ANNOUNCEMENT",
        target_type="ANNOUNCEMENT",
        target_id=str(announcement_id),
        details={"title": title}
    )

    return {"message": "Announcement deleted successfully"}

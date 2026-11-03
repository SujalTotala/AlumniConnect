from typing import List, Dict, Any
from datetime import datetime
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import desc

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.opportunity_model import Opportunity
from app.models.event_model import Event
from app.models.announcement_model import Announcement
from app.models.alumni_model import Alumni
from app.auth.jwt_dependency import get_current_user

router = APIRouter()

@router.get("/", response_model=List[Dict[str, Any]])
def get_activity_feed(
    limit: int = 15,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    feed = []

    # 1. Recent Announcements (non-expired)
    now = datetime.utcnow()
    announcements = (
        db.query(Announcement)
        .filter((Announcement.expires_at.is_(None)) | (Announcement.expires_at >= now))
        .order_by(desc(Announcement.created_at))
        .limit(5)
        .all()
    )
    for a in announcements:
        feed.append({
            "id": f"announcement-{a.id}",
            "entity_id": a.id,
            "type": "ANNOUNCEMENT",
            "title": a.title,
            "description": a.content[:140] + ("..." if len(a.content) > 140 else ""),
            "badge": a.priority,
            "category": a.category,
            "created_at": a.created_at.isoformat() if a.created_at else None,
        })

    # 2. Recent Opportunities
    opportunities = (
        db.query(Opportunity)
        .order_by(desc(Opportunity.created_at))
        .limit(5)
        .all()
    )
    for o in opportunities:
        feed.append({
            "id": f"opportunity-{o.id}",
            "entity_id": o.id,
            "type": "OPPORTUNITY",
            "title": o.title,
            "description": f"{o.opportunity_type} at {o.company} • {o.location or 'Remote'}",
            "badge": o.opportunity_type,
            "category": o.company,
            "created_at": o.created_at.isoformat() if o.created_at else None,
        })

    # 3. Recent Events
    events = (
        db.query(Event)
        .order_by(desc(Event.created_at))
        .limit(5)
        .all()
    )
    for e in events:
        feed.append({
            "id": f"event-{e.id}",
            "entity_id": e.id,
            "type": "EVENT",
            "title": e.title,
            "description": f"{e.event_type} on {e.event_date} • {e.location}",
            "badge": e.event_type,
            "category": e.event_date,
            "created_at": e.created_at.isoformat() if e.created_at else None,
        })

    # 4. Recent Verified Alumni additions
    alumni_list = (
        db.query(Alumni)
        .order_by(desc(Alumni.created_at))
        .limit(5)
        .all()
    )
    for a in alumni_list:
        role_comp = f"{a.job_role or ''} {f'at {a.company}' if a.company else ''}".strip()
        feed.append({
            "id": f"alumni-{a.id}",
            "entity_id": a.id,
            "type": "ALUMNI_JOINED",
            "title": f"Alumni Member: {a.name}",
            "description": f"{role_comp or (a.department or 'Alumni Network')} • Class of {a.graduation_year or 'N/A'}",
            "badge": "Verified" if getattr(a, "is_verified", False) else "Member",
            "category": a.department or "Alumni",
            "created_at": a.created_at.isoformat() if a.created_at else None,
        })

    # Sort all aggregated items by created_at descending
    feed.sort(key=lambda item: item["created_at"] or "", reverse=True)
    return feed[:limit]

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.opportunity_model import Opportunity
from app.models.event_model import Event
from app.models.bookmark_model import UserBookmark
from app.schemas.bookmark_schema import BookmarkCreate, BookmarkResponse, BookmarkCheckResponse
from app.auth.jwt_dependency import get_current_user

router = APIRouter()

def get_entity_preview(db: Session, entity_type: str, entity_id: int):
    if entity_type == "alumni":
        item = db.query(Alumni).filter(Alumni.id == entity_id).first()
        if item:
            return {
                "title": item.name,
                "subtitle": f"{item.job_role or ''} {f'at {item.company}' if item.company else ''}".strip(),
                "department": item.department,
                "company": item.company,
                "is_verified": getattr(item, "is_verified", False),
            }
    elif entity_type == "opportunity":
        item = db.query(Opportunity).filter(Opportunity.id == entity_id).first()
        if item:
            return {
                "title": item.title,
                "subtitle": item.company,
                "opportunity_type": item.opportunity_type,
                "deadline": item.deadline,
                "location": item.location,
            }
    elif entity_type == "event":
        item = db.query(Event).filter(Event.id == entity_id).first()
        if item:
            return {
                "title": item.title,
                "subtitle": item.event_type,
                "event_date": item.event_date,
                "location": item.location,
            }
    return None

@router.post("/", response_model=BookmarkResponse, status_code=status.HTTP_201_CREATED)
def create_bookmark(
    bookmark_data: BookmarkCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    entity_type = bookmark_data.entity_type.lower()
    entity_id = bookmark_data.entity_id

    # Verify referenced entity actually exists
    if entity_type == "alumni":
        exists = db.query(Alumni).filter(Alumni.id == entity_id).first()
    elif entity_type == "opportunity":
        exists = db.query(Opportunity).filter(Opportunity.id == entity_id).first()
    elif entity_type == "event":
        exists = db.query(Event).filter(Event.id == entity_id).first()
    else:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Unsupported entity type '{entity_type}'")

    if not exists:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"{entity_type.capitalize()} with ID {entity_id} does not exist",
        )

    # Check for existing bookmark
    existing = db.query(UserBookmark).filter(
        UserBookmark.user_id == current_user.id,
        UserBookmark.entity_type == entity_type,
        UserBookmark.entity_id == entity_id,
    ).first()

    if existing:
        preview = get_entity_preview(db, existing.entity_type, existing.entity_id)
        return BookmarkResponse(
            id=existing.id,
            user_id=existing.user_id,
            entity_type=existing.entity_type,
            entity_id=existing.entity_id,
            created_at=existing.created_at,
            entity_preview=preview,
        )

    new_bm = UserBookmark(
        user_id=current_user.id,
        entity_type=entity_type,
        entity_id=entity_id,
    )
    db.add(new_bm)
    db.commit()
    db.refresh(new_bm)

    preview = get_entity_preview(db, new_bm.entity_type, new_bm.entity_id)
    return BookmarkResponse(
        id=new_bm.id,
        user_id=new_bm.user_id,
        entity_type=new_bm.entity_type,
        entity_id=new_bm.entity_id,
        created_at=new_bm.created_at,
        entity_preview=preview,
    )

@router.get("/", response_model=List[BookmarkResponse])
def get_user_bookmarks(
    entity_type: Optional[str] = Query(None, description="Optional entity_type filter"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    query = db.query(UserBookmark).filter(UserBookmark.user_id == current_user.id)
    if entity_type:
        query = query.filter(UserBookmark.entity_type == entity_type.lower())

    bookmarks = query.order_by(UserBookmark.created_at.desc()).all()
    results = []
    for bm in bookmarks:
        preview = get_entity_preview(db, bm.entity_type, bm.entity_id)
        results.append(
            BookmarkResponse(
                id=bm.id,
                user_id=bm.user_id,
                entity_type=bm.entity_type,
                entity_id=bm.entity_id,
                created_at=bm.created_at,
                entity_preview=preview,
            )
        )
    return results

@router.get("/check/{entity_type}/{entity_id}", response_model=BookmarkCheckResponse)
def check_bookmark(
    entity_type: str,
    entity_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    existing = db.query(UserBookmark).filter(
        UserBookmark.user_id == current_user.id,
        UserBookmark.entity_type == entity_type.lower(),
        UserBookmark.entity_id == entity_id,
    ).first()

    return BookmarkCheckResponse(
        is_bookmarked=existing is not None,
        bookmark_id=existing.id if existing else None,
    )

@router.delete("/{bookmark_id}")
def delete_bookmark_by_id(
    bookmark_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    bm = db.query(UserBookmark).filter(UserBookmark.id == bookmark_id).first()
    if not bm:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Bookmark not found")

    if bm.user_id != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Cannot delete another user's bookmark")

    db.delete(bm)
    db.commit()
    return {"message": "Bookmark removed successfully"}

@router.delete("/{entity_type}/{entity_id}")
def delete_bookmark_by_entity(
    entity_type: str,
    entity_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    bm = db.query(UserBookmark).filter(
        UserBookmark.user_id == current_user.id,
        UserBookmark.entity_type == entity_type.lower(),
        UserBookmark.entity_id == entity_id,
    ).first()
    if not bm:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Bookmark not found")

    db.delete(bm)
    db.commit()
    return {"message": "Bookmark removed successfully"}

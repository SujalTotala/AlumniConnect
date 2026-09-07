from datetime import datetime
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.community_model import Community, CommunityMember, CommunityPost
from app.schemas.community_schema import (
    CommunityCreate,
    CommunityResponse,
    CommunityMemberResponse,
    CommunityPostCreate,
    CommunityPostResponse
)
from app.auth.jwt_dependency import get_current_user, get_optional_current_user, require_role

router = APIRouter()

def format_community_response(comm: Community, current_user: Optional[User], db: Session) -> dict:
    creator = db.query(User).filter(User.id == comm.created_by).first() if comm.created_by else None
    members_count = db.query(CommunityMember).filter(CommunityMember.community_id == comm.id).count()

    is_member = False
    my_role = None
    if current_user:
        membership = db.query(CommunityMember).filter(
            CommunityMember.community_id == comm.id,
            CommunityMember.user_id == current_user.id
        ).first()
        if membership:
            is_member = True
            my_role = membership.role

    return {
        "id": comm.id,
        "name": comm.name,
        "description": comm.description,
        "community_type": comm.community_type,
        "created_by": comm.created_by,
        "creator_name": creator.name if creator else "Admin",
        "members_count": members_count,
        "is_member": is_member,
        "my_role": my_role,
        "created_at": comm.created_at,
        "is_active": comm.is_active
    }

# 1. List all active communities
@router.get("/", response_model=List[CommunityResponse])
def get_communities(
    community_type: Optional[str] = Query(None, description="Filter by type: CITY, BATCH, DEPARTMENT, INDUSTRY, GENERAL"),
    search: Optional[str] = Query(None, description="Search by name or description"),
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    query = db.query(Community).filter(Community.is_active == True)

    if community_type:
        query = query.filter(Community.community_type.ilike(f"%{community_type}%"))
    if search:
        query = query.filter(
            Community.name.ilike(f"%{search}%") | Community.description.ilike(f"%{search}%")
        )

    communities = query.order_by(Community.name.asc()).all()
    return [format_community_response(c, current_user, db) for c in communities]

# 2. List user's joined communities
@router.get("/my", response_model=List[CommunityResponse])
def get_my_communities(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    memberships = db.query(CommunityMember).filter(
        CommunityMember.user_id == current_user.id
    ).all()
    comm_ids = [m.community_id for m in memberships]

    communities = db.query(Community).filter(
        Community.id.in_(comm_ids),
        Community.is_active == True
    ).order_by(Community.name.asc()).all()

    return [format_community_response(c, current_user, db) for c in communities]

# 3. Create a community (Admin or Alumni)
@router.post("/", response_model=CommunityResponse, status_code=status.HTTP_201_CREATED)
def create_community(
    comm_in: CommunityCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin", "alumni"]))
):
    existing = db.query(Community).filter(Community.name.ilike(comm_in.name.strip())).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="A community with this name already exists"
        )

    community = Community(
        name=comm_in.name.strip(),
        description=comm_in.description.strip(),
        community_type=(comm_in.community_type or "GENERAL").strip().upper(),
        created_by=current_user.id,
        created_at=datetime.utcnow(),
        is_active=True
    )
    db.add(community)
    db.commit()
    db.refresh(community)

    # Automatically add creator as moderator
    member = CommunityMember(
        community_id=community.id,
        user_id=current_user.id,
        role="MODERATOR",
        joined_at=datetime.utcnow()
    )
    db.add(member)
    db.commit()

    return format_community_response(community, current_user, db)

# 4. Get community details
@router.get("/{community_id}", response_model=CommunityResponse)
def get_community_by_id(
    community_id: int,
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_current_user)
):
    community = db.query(Community).filter(Community.id == community_id).first()
    if not community:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Community not found"
        )

    return format_community_response(community, current_user, db)

# 5. Join community
@router.post("/{community_id}/join", response_model=CommunityResponse, status_code=status.HTTP_201_CREATED)
def join_community(
    community_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    community = db.query(Community).filter(Community.id == community_id).first()
    if not community:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Community not found"
        )

    existing_membership = db.query(CommunityMember).filter(
        CommunityMember.community_id == community_id,
        CommunityMember.user_id == current_user.id
    ).first()
    if existing_membership:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You are already a member of this community"
        )

    membership = CommunityMember(
        community_id=community_id,
        user_id=current_user.id,
        role="MEMBER",
        joined_at=datetime.utcnow()
    )
    db.add(membership)
    db.commit()

    return format_community_response(community, current_user, db)

# 6. Leave community
@router.delete("/{community_id}/leave")
def leave_community(
    community_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    membership = db.query(CommunityMember).filter(
        CommunityMember.community_id == community_id,
        CommunityMember.user_id == current_user.id
    ).first()
    if not membership:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="You are not a member of this community"
        )

    db.delete(membership)
    db.commit()

    return {"message": "Left community successfully"}

# 7. List community members
@router.get("/{community_id}/members", response_model=List[CommunityMemberResponse])
def get_community_members(
    community_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    community = db.query(Community).filter(Community.id == community_id).first()
    if not community:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Community not found"
        )

    memberships = db.query(CommunityMember).filter(
        CommunityMember.community_id == community_id
    ).order_by(CommunityMember.joined_at.asc()).all()

    result = []
    for m in memberships:
        u = db.query(User).filter(User.id == m.user_id).first()
        result.append({
            "id": m.id,
            "community_id": m.community_id,
            "user_id": m.user_id,
            "user_name": u.name if u else "Member",
            "user_email": u.email if u else "",
            "user_role": u.role if u else "student",
            "member_role": m.role,
            "joined_at": m.joined_at
        })

    return result

# 8. List community discussion posts
@router.get("/{community_id}/posts", response_model=List[CommunityPostResponse])
def get_community_posts(
    community_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    community = db.query(Community).filter(Community.id == community_id).first()
    if not community:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Community not found"
        )

    posts = db.query(CommunityPost).filter(
        CommunityPost.community_id == community_id
    ).order_by(CommunityPost.created_at.desc()).all()

    result = []
    for p in posts:
        author = db.query(User).filter(User.id == p.author_id).first()
        result.append({
            "id": p.id,
            "community_id": p.community_id,
            "author_id": p.author_id,
            "author_name": author.name if author else "Member",
            "author_role": author.role if author else "student",
            "content": p.content,
            "created_at": p.created_at
        })

    return result

# 9. Create a post in community (Members only)
@router.post("/{community_id}/posts", response_model=CommunityPostResponse, status_code=status.HTTP_201_CREATED)
def create_community_post(
    community_id: int,
    post_in: CommunityPostCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    community = db.query(Community).filter(Community.id == community_id).first()
    if not community:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Community not found"
        )

    # Check membership (or admin)
    is_member = db.query(CommunityMember).filter(
        CommunityMember.community_id == community_id,
        CommunityMember.user_id == current_user.id
    ).first() is not None or current_user.role.lower() == "admin"

    if not is_member:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You must join this community before posting messages"
        )

    content_str = post_in.content.strip()
    if not content_str:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Post content cannot be empty"
        )

    post = CommunityPost(
        community_id=community_id,
        author_id=current_user.id,
        content=content_str,
        created_at=datetime.utcnow()
    )
    db.add(post)
    db.commit()
    db.refresh(post)

    return {
        "id": post.id,
        "community_id": post.community_id,
        "author_id": post.author_id,
        "author_name": current_user.name,
        "author_role": current_user.role,
        "content": post.content,
        "created_at": post.created_at
    }

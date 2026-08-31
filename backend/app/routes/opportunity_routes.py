from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.opportunity_model import Opportunity
from app.schemas.opportunity_schema import (
    OpportunityCreate,
    OpportunityUpdate,
    OpportunityResponse
)
from app.auth.jwt_dependency import get_current_user, require_role

router = APIRouter()

# Get all opportunities
@router.get("/", response_model=List[OpportunityResponse])
def get_opportunities(
    opportunity_type: Optional[str] = Query(None, description="Filter by type (Internship, Full-Time Job, Referral, etc.)"),
    search: Optional[str] = Query(None, description="Search in title, company, description"),
    location: Optional[str] = Query(None, description="Filter by location"),
    db: Session = Depends(get_db)
):
    query = db.query(Opportunity)

    if opportunity_type:
        query = query.filter(Opportunity.opportunity_type.ilike(f"%{opportunity_type}%"))
    if location:
        query = query.filter(Opportunity.location.ilike(f"%{location}%"))
    if search:
        query = query.filter(
            Opportunity.title.ilike(f"%{search}%") |
            Opportunity.company.ilike(f"%{search}%") |
            Opportunity.description.ilike(f"%{search}%")
        )

    opportunities = query.order_by(Opportunity.id.desc()).all()
    result = []
    for opp in opportunities:
        poster = db.query(User).filter(User.id == opp.posted_by).first() if opp.posted_by else None
        result.append({
            "id": opp.id,
            "title": opp.title,
            "company": opp.company,
            "description": opp.description,
            "opportunity_type": opp.opportunity_type,
            "location": opp.location,
            "deadline": opp.deadline,
            "application_url": opp.application_url,
            "posted_by": opp.posted_by,
            "created_at": opp.created_at,
            "poster_name": poster.name if poster else "Alumni Community"
        })
    return result

# Get single opportunity
@router.get("/{opportunity_id}", response_model=OpportunityResponse)
def get_opportunity_by_id(opportunity_id: int, db: Session = Depends(get_db)):
    opp = db.query(Opportunity).filter(Opportunity.id == opportunity_id).first()
    if not opp:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Opportunity not found")

    poster = db.query(User).filter(User.id == opp.posted_by).first() if opp.posted_by else None
    return {
        "id": opp.id,
        "title": opp.title,
        "company": opp.company,
        "description": opp.description,
        "opportunity_type": opp.opportunity_type,
        "location": opp.location,
        "deadline": opp.deadline,
        "application_url": opp.application_url,
        "posted_by": opp.posted_by,
        "created_at": opp.created_at,
        "poster_name": poster.name if poster else "Alumni Community"
    }

# Create Opportunity (Admin or Alumni)
@router.post("/", response_model=OpportunityResponse, status_code=status.HTTP_201_CREATED)
def create_opportunity(
    opp_data: OpportunityCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role(["admin", "alumni"]))
):
    new_opp = Opportunity(
        title=opp_data.title,
        company=opp_data.company,
        description=opp_data.description,
        opportunity_type=opp_data.opportunity_type or "Full-Time Job",
        location=opp_data.location,
        deadline=opp_data.deadline,
        application_url=opp_data.application_url,
        posted_by=current_user.id
    )
    db.add(new_opp)
    db.commit()
    db.refresh(new_opp)

    return {
        "id": new_opp.id,
        "title": new_opp.title,
        "company": new_opp.company,
        "description": new_opp.description,
        "opportunity_type": new_opp.opportunity_type,
        "location": new_opp.location,
        "deadline": new_opp.deadline,
        "application_url": new_opp.application_url,
        "posted_by": new_opp.posted_by,
        "created_at": new_opp.created_at,
        "poster_name": current_user.name
    }

# Update Opportunity (Admin or Creator)
@router.put("/{opportunity_id}", response_model=OpportunityResponse)
def update_opportunity(
    opportunity_id: int,
    opp_update: OpportunityUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    opp = db.query(Opportunity).filter(Opportunity.id == opportunity_id).first()
    if not opp:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Opportunity not found")

    if current_user.role.lower() != "admin" and opp.posted_by != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized to edit this opportunity")

    update_dict = opp_update.model_dump(exclude_unset=True)
    for field, value in update_dict.items():
        if value is not None:
            setattr(opp, field, value)

    db.commit()
    db.refresh(opp)

    poster = db.query(User).filter(User.id == opp.posted_by).first() if opp.posted_by else None
    return {
        "id": opp.id,
        "title": opp.title,
        "company": opp.company,
        "description": opp.description,
        "opportunity_type": opp.opportunity_type,
        "location": opp.location,
        "deadline": opp.deadline,
        "application_url": opp.application_url,
        "posted_by": opp.posted_by,
        "created_at": opp.created_at,
        "poster_name": poster.name if poster else "Alumni Community"
    }

# Delete Opportunity (Admin or Creator)
@router.delete("/{opportunity_id}")
def delete_opportunity(
    opportunity_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    opp = db.query(Opportunity).filter(Opportunity.id == opportunity_id).first()
    if not opp:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Opportunity not found")

    if current_user.role.lower() != "admin" and opp.posted_by != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized to delete this opportunity")

    db.delete(opp)
    db.commit()

    return {"message": "Opportunity deleted successfully"}

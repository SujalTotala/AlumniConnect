from typing import Dict, Any, List
from sqlalchemy import or_, and_, func
from sqlalchemy.orm import Session
from app.models.alumni_model import Alumni

ALUMNI_PROFILE_FIELDS = [
    "graduation_year",
    "department",
    "company",
    "job_role",
    "location",
    "skills",
    "bio",
    "linkedin_url"
]

def calculate_alumni_completion(alumni: Alumni) -> int:
    filled = 0
    for field in ALUMNI_PROFILE_FIELDS:
        val = getattr(alumni, field, None)
        if val and str(val).strip():
            filled += 1
    return round((filled / len(ALUMNI_PROFILE_FIELDS)) * 100)

def build_segment_query(db: Session, criteria: Dict[str, Any]):
    """Builds a SQLAlchemy query for Alumni matching structured criteria."""
    query = db.query(Alumni)
    if not criteria:
        return query

    # 1. Departments filter (list of strings or comma-separated or single string)
    departments = criteria.get("departments")
    if not departments and criteria.get("department"):
        departments = [criteria.get("department")]
    if departments:
        if isinstance(departments, str):
            departments = [d.strip() for d in departments.split(",") if d.strip()]
        dept_conditions = [func.lower(Alumni.department) == d.lower() for d in departments]
        query = query.filter(or_(*dept_conditions))

    # 2. Graduation year range or specific graduation years
    grad_years = criteria.get("graduation_years")
    if grad_years:
        if isinstance(grad_years, str):
            grad_years = [y.strip() for y in grad_years.split(",") if y.strip()]
        query = query.filter(Alumni.graduation_year.in_([str(y) for y in grad_years]))

    grad_min = criteria.get("graduation_year_min")
    if grad_min:
        query = query.filter(Alumni.graduation_year >= str(grad_min))

    grad_max = criteria.get("graduation_year_max")
    if grad_max:
        query = query.filter(Alumni.graduation_year <= str(grad_max))

    # 3. Companies (list or substring)
    companies = criteria.get("companies")
    if not companies and criteria.get("company"):
        companies = [criteria.get("company")]
    if companies:
        if isinstance(companies, str):
            companies = [c.strip() for c in companies.split(",") if c.strip()]
        comp_conditions = [Alumni.company.ilike(f"%{c}%") for c in companies]
        query = query.filter(or_(*comp_conditions))

    # 4. Locations
    locations = criteria.get("locations")
    if not locations and criteria.get("location"):
        locations = [criteria.get("location")]
    if locations:
        if isinstance(locations, str):
            locations = [l.strip() for l in locations.split(",") if l.strip()]
        loc_conditions = [Alumni.location.ilike(f"%{l}%") for l in locations]
        query = query.filter(or_(*loc_conditions))

    # 5. Skills
    skills = criteria.get("skills")
    if skills:
        if isinstance(skills, str):
            skills = [s.strip() for s in skills.split(",") if s.strip()]
        skill_conditions = [Alumni.skills.ilike(f"%{s}%") for s in skills]
        query = query.filter(or_(*skill_conditions))

    # 6. Mentorship availability
    if "mentorship_available" in criteria and criteria["mentorship_available"] is not None:
        val = bool(criteria["mentorship_available"])
        query = query.filter(Alumni.mentorship_available == val)

    # 7. Verification status
    if "is_verified" in criteria and criteria["is_verified"] is not None:
        val = bool(criteria["is_verified"])
        query = query.filter(Alumni.is_verified == val)

    # 8. Free-text search query
    search = criteria.get("search")
    if search and search.strip():
        term = f"%{search.strip()}%"
        query = query.filter(
            or_(
                Alumni.name.ilike(term),
                Alumni.email.ilike(term),
                Alumni.company.ilike(term),
                Alumni.job_role.ilike(term)
            )
        )

    return query

def count_segment_members(db: Session, criteria: Dict[str, Any]) -> int:
    query = build_segment_query(db, criteria)
    return query.count()

def get_segment_members(db: Session, criteria: Dict[str, Any], limit: int = 100, offset: int = 0) -> List[Alumni]:
    query = build_segment_query(db, criteria)
    return query.order_by(Alumni.id.asc()).offset(offset).limit(limit).all()

def alumni_matches_criteria(alumni: Alumni, criteria: Dict[str, Any]) -> bool:
    """Checks whether a given Alumni model instance matches criteria in-memory."""
    if not criteria:
        return True

    # Department
    departments = criteria.get("departments")
    if not departments and criteria.get("department"):
        departments = [criteria.get("department")]
    if departments:
        if isinstance(departments, str):
            departments = [d.strip() for d in departments.split(",") if d.strip()]
        if not alumni.department or alumni.department.lower() not in [d.lower() for d in departments]:
            return False

    # Graduation years
    grad_years = criteria.get("graduation_years")
    if grad_years:
        if isinstance(grad_years, str):
            grad_years = [y.strip() for y in grad_years.split(",") if y.strip()]
        if not alumni.graduation_year or str(alumni.graduation_year) not in [str(y) for y in grad_years]:
            return False

    grad_min = criteria.get("graduation_year_min")
    if grad_min and (not alumni.graduation_year or str(alumni.graduation_year) < str(grad_min)):
        return False

    grad_max = criteria.get("graduation_year_max")
    if grad_max and (not alumni.graduation_year or str(alumni.graduation_year) > str(grad_max)):
        return False

    # Companies
    companies = criteria.get("companies")
    if not companies and criteria.get("company"):
        companies = [criteria.get("company")]
    if companies:
        if isinstance(companies, str):
            companies = [c.strip() for c in companies.split(",") if c.strip()]
        if not alumni.company or not any(c.lower() in alumni.company.lower() for c in companies):
            return False

    # Locations
    locations = criteria.get("locations")
    if not locations and criteria.get("location"):
        locations = [criteria.get("location")]
    if locations:
        if isinstance(locations, str):
            locations = [l.strip() for l in locations.split(",") if l.strip()]
        if not alumni.location or not any(l.lower() in alumni.location.lower() for l in locations):
            return False

    # Skills
    skills = criteria.get("skills")
    if skills:
        if isinstance(skills, str):
            skills = [s.strip() for s in skills.split(",") if s.strip()]
        if not alumni.skills or not any(s.lower() in alumni.skills.lower() for s in skills):
            return False

    # Mentorship available
    if "mentorship_available" in criteria and criteria["mentorship_available"] is not None:
        if bool(alumni.mentorship_available) != bool(criteria["mentorship_available"]):
            return False

    # Verification status
    if "is_verified" in criteria and criteria["is_verified"] is not None:
        if bool(alumni.is_verified) != bool(criteria["is_verified"]):
            return False

    return True

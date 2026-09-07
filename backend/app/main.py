from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import ALLOWED_ORIGINS, ENVIRONMENT
from app.database.database import Base, engine

# Import all models to ensure metadata registration
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.profile_model import StudentProfile
from app.models.event_model import Event, EventRegistration
from app.models.mentorship_model import MentorshipRequest
from app.models.opportunity_model import Opportunity
from app.models.notification_model import Notification
from app.models.connection_model import Connection
from app.models.referral_model import ReferralRequest
from app.models.attendance_model import EventAttendance
from app.models.story_model import SuccessStory
from app.models.community_model import Community, CommunityMember, CommunityPost
from app.models.achievement_model import Achievement
from app.models.import_job_model import AlumniImportJob
from app.models.segment_model import AlumniSegment
from app.models.audit_model import AdminAuditLog

# Import all routers
from app.routes.auth_routes import router as auth_router
from app.routes.profile_routes import router as profile_router
from app.routes.alumni_routes import router as alumni_router
from app.routes.event_routes import router as event_router
from app.routes.mentorship_routes import router as mentorship_router
from app.routes.opportunity_routes import router as opportunity_router
from app.routes.notification_routes import router as notification_router
from app.routes.admin_routes import router as admin_router
from app.routes.bookmark_routes import router as bookmark_router
from app.routes.announcement_routes import router as announcement_router
from app.routes.activity_routes import router as activity_router
from app.routes.preference_routes import router as preference_router
from app.routes.connection_routes import router as connection_router
from app.routes.referral_routes import router as referral_router
from app.routes.attendance_routes import router as attendance_router
from app.routes.story_routes import router as story_router
from app.routes.community_routes import router as community_router
from app.routes.achievement_routes import router as achievement_router
from app.routes.admin_alumni_routes import router as admin_alumni_router

# Keep local development convenient, but production schema changes must be
# applied explicitly through Alembic before the application starts.
if ENVIRONMENT != "production":
    Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="AlumniConnect API",
    description="Unified REST API for AlumniConnect Cross-Platform Engagement System",
    version="1.0.0"
)

# CORS Configuration — environment-driven
# In production ALLOWED_ORIGINS must be explicitly set (comma-separated).
allowed_origins_env = ALLOWED_ORIGINS
env = ENVIRONMENT

# Parse explicit origins from environment
default_origins = [
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "https://alumniconnect.vercel.app",
    "https://alumni-connect.vercel.app"
]
if allowed_origins_env and allowed_origins_env.strip():
    parsed_origins = [o.strip() for o in allowed_origins_env.split(",") if o.strip() and o.strip() != "*"]
    origins = list(dict.fromkeys(default_origins + parsed_origins))
else:
    origins = default_origins

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_origin_regex=r"^https://.*\.vercel\.app$",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Health / Test Route
@app.get("/")
def home():
    return {
        "status": "online",
        "message": "AlumniConnect Backend Running",
        "version": "1.0.1-authfix"
    }


# Health endpoint suitable for uptime checks
@app.get("/health")
def health():
    return {"status": "ok", "service": "AlumniConnect API"}

# Mount Modular Routers
app.include_router(auth_router, prefix="/auth", tags=["Authentication"])
app.include_router(profile_router, prefix="/profile", tags=["Profiles"])
app.include_router(alumni_router, prefix="/alumni", tags=["Alumni Directory"])
app.include_router(event_router, prefix="/events", tags=["Events"])
app.include_router(mentorship_router, prefix="/mentorship", tags=["Mentorship"])
app.include_router(mentorship_router, prefix="/mentors", tags=["Mentors Alias"])
app.include_router(opportunity_router, prefix="/opportunities", tags=["Opportunities"])
app.include_router(notification_router, prefix="/notifications", tags=["Notifications"])
app.include_router(admin_router, prefix="/admin", tags=["Administration"])
app.include_router(bookmark_router, prefix="/bookmarks", tags=["Bookmarks"])
app.include_router(announcement_router, prefix="/announcements", tags=["Announcements"])
app.include_router(activity_router, prefix="/activity-feed", tags=["Activity Feed"])
app.include_router(preference_router, prefix="/notification-preferences", tags=["Notification Preferences"])
app.include_router(connection_router, prefix="/connections", tags=["Connections & Networking"])
app.include_router(referral_router, prefix="/referrals", tags=["Referrals"])
app.include_router(attendance_router, prefix="/events", tags=["Event Attendance"])
app.include_router(story_router, prefix="/success-stories", tags=["Success Stories"])
app.include_router(community_router, prefix="/communities", tags=["Communities"])
app.include_router(achievement_router, prefix="/achievements", tags=["Achievements"])
app.include_router(admin_alumni_router, prefix="/admin", tags=["Alumni Data Management"])

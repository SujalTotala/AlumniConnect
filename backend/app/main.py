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

if env == "production":
    if not allowed_origins_env:
        raise RuntimeError("ALLOWED_ORIGINS environment variable must be set in production (comma-separated origins)")
    origins = [o.strip() for o in allowed_origins_env.split(",") if o.strip()]
    # Disallow wildcard origins in production when credentials are used
    if any(o == "*" for o in origins):
        raise RuntimeError("Wildcard origin '*' is not allowed in production. Provide explicit ALLOWED_ORIGINS.")
    allow_credentials = True
else:
    # Development/testing defaults — allow common local dev origins if not provided
    if allowed_origins_env:
        origins = [o.strip() for o in allowed_origins_env.split(",") if o.strip()]
    else:
        origins = ["http://localhost:5173", "http://127.0.0.1:5173"]
    # Allow credentials for local flows
    allow_credentials = True

if "*" in origins and allow_credentials:
    # Defensive: do not combine wildcard origins with credentials
    allow_credentials = False

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=allow_credentials,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Health / Test Route
@app.get("/")
def home():
    return {
        "status": "online",
        "message": "AlumniConnect Backend Running",
        "version": "1.0.0"
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

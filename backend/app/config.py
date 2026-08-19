import os
from pathlib import Path

# Load .env file manually when present (developer convenience only)
# This preserves local development behavior while allowing environment
# variables to override values in production/deployment.
env_path = Path(__file__).resolve().parent.parent / ".env"
if env_path.exists():
    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, val = line.split("=", 1)
                os.environ.setdefault(key.strip(), val.strip())

# Environment selection: development | testing | production
ENVIRONMENT = os.getenv("ENVIRONMENT", "development").lower()
SUPPORTED_ENVIRONMENTS = {"development", "testing", "production"}
if ENVIRONMENT not in SUPPORTED_ENVIRONMENTS:
    raise RuntimeError(
        "ENVIRONMENT must be one of: development, testing, production"
    )

# Core configuration values (must come from env in production)
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./alumni.db")
ALGORITHM = os.getenv("ALGORITHM", "HS256")
try:
    ACCESS_TOKEN_EXPIRE_MINUTES = int(
        os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "60")
    )
except ValueError as exc:
    raise RuntimeError("ACCESS_TOKEN_EXPIRE_MINUTES must be an integer") from exc

if ACCESS_TOKEN_EXPIRE_MINUTES <= 0:
    raise RuntimeError("ACCESS_TOKEN_EXPIRE_MINUTES must be greater than zero")

# SECRET_KEY handling: require a real secret in non-development environments
_secret = os.getenv("SECRET_KEY")
if ENVIRONMENT == "production":
    normalized_secret = (_secret or "").strip()
    insecure_secrets = {
        "change_me_with_strong_secret",
        "alumniconnect_development_fallback_change_me",
    }
    if len(normalized_secret) < 32 or normalized_secret.lower() in insecure_secrets:
        raise RuntimeError(
            "SECRET_KEY must be set to a strong value of at least 32 characters "
            "in production"
        )
    SECRET_KEY = normalized_secret
else:
    # Development & testing: allow a fallback but prefer explicit env var
    SECRET_KEY = _secret or "alumniconnect_development_fallback_change_me"

# ALLOWED_ORIGINS is parsed by the application middleware (comma separated)
ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS", "")

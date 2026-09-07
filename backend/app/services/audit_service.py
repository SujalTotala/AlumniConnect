import json
from typing import Optional, Any
from sqlalchemy.orm import Session
from app.models.audit_model import AdminAuditLog

SENSITIVE_KEYS = {"password", "token", "access_token", "refresh_token", "secret", "authorization"}

def sanitize_details(details: Any) -> Optional[str]:
    if details is None:
        return None
    if isinstance(details, str):
        return details
    if isinstance(details, dict):
        cleaned = {}
        for k, v in details.items():
            if any(s in k.lower() for s in SENSITIVE_KEYS):
                cleaned[k] = "[REDACTED]"
            else:
                cleaned[k] = v
        return json.dumps(cleaned, default=str)
    return str(details)

def log_admin_action(
    db: Session,
    admin_user_id: Optional[int],
    action: str,
    target_type: str,
    target_id: Optional[str] = None,
    details: Any = None,
    ip_address: Optional[str] = None
) -> AdminAuditLog:
    """Safely records an institutional admin audit log entry."""
    entry = AdminAuditLog(
        admin_user_id=admin_user_id,
        action=action,
        target_type=target_type,
        target_id=str(target_id) if target_id is not None else None,
        details=sanitize_details(details),
        ip_address=ip_address
    )
    db.add(entry)
    db.commit()
    db.refresh(entry)
    return entry

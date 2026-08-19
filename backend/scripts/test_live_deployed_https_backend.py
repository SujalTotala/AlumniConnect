import os
import sys
import time
from pathlib import Path
import httpx

BACKEND_DIR = Path(__file__).resolve().parent.parent

# Load .env
env_path = BACKEND_DIR / ".env"
if env_path.exists():
    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                os.environ.setdefault(k.strip(), v.strip())

base_url = os.getenv("BACKEND_HTTPS_URL")
if not base_url:
    print("ERROR: BACKEND_HTTPS_URL is not configured in backend/.env", file=sys.stderr)
    sys.exit(1)

base_url = base_url.rstrip("/")

def run_suite():
    print("=" * 70)
    print("RUNNING LIVE DEPLOYED HTTPS BACKEND VERIFICATION SUITE")
    print("=" * 70)
    print("Target: Verified Remote HTTPS Backend (URL masked for security)")

    client = httpx.Client(base_url=base_url, timeout=120.0, follow_redirects=True)

    # 1. Verify GET /
    print("\n--- 1. ROOT & HEALTH ENDPOINTS ---")
    r_root = client.get("/")
    assert r_root.status_code == 200, f"GET / failed with status {r_root.status_code}: {r_root.text}"
    print(f"[PASS] GET / returned 200 OK: {r_root.json()}")

    # 2. Check /health if available
    r_health = client.get("/health")
    if r_health.status_code == 200:
        print(f"[PASS] GET /health returned 200 OK: {r_health.json()}")
    else:
        print(f"[INFO] Root endpoint serves as primary health check (status: 200)")

    # 3. Test Authentication (POST /auth/login and GET /auth/me)
    print("\n--- 2. AUTHENTICATION & JWT VERIFICATION ---")
    admin_creds = {"email": "admin_p4@alumni.edu", "password": "AdminPass123!"}
    r_login = client.post("/auth/login", json=admin_creds)
    assert r_login.status_code == 200, f"Admin login failed with status {r_login.status_code}: {r_login.text}"
    admin_token = r_login.json()["access_token"]
    admin_headers = {"Authorization": f"Bearer {admin_token}"}
    print("[PASS] POST /auth/login successfully authenticated admin credentials")

    r_me = client.get("/auth/me", headers=admin_headers)
    assert r_me.status_code == 200, f"GET /auth/me failed: {r_me.text}"
    me_data = r_me.json()
    assert me_data["role"] == "admin"
    print(f"[PASS] GET /auth/me returned authenticated session (role: {me_data['role']})")

    # 4. PostgreSQL-backed Endpoints
    print("\n--- 3. POSTGRESQL-BACKED ENDPOINT VERIFICATION ---")
    # Alumni
    r_alumni = client.get("/alumni/")
    assert r_alumni.status_code == 200, f"GET /alumni/ failed: {r_alumni.text}"
    alumni_list = r_alumni.json()
    print(f"[PASS] GET /alumni/ returned {len(alumni_list)} records from PostgreSQL")
    assert len(alumni_list) >= 20

    # Events
    r_events = client.get("/events/")
    assert r_events.status_code == 200, f"GET /events/ failed: {r_events.text}"
    events_list = r_events.json()
    print(f"[PASS] GET /events/ returned {len(events_list)} records from PostgreSQL")
    assert len(events_list) >= 13

    # Mentorship Available Mentors
    r_mentors = client.get("/mentorship/mentors")
    assert r_mentors.status_code == 200, f"GET /mentorship/mentors failed: {r_mentors.text}"
    mentors_list = r_mentors.json()
    print(f"[PASS] GET /mentorship/mentors returned {len(mentors_list)} active mentors from PostgreSQL")
    assert len(mentors_list) >= 9

    # Opportunities
    r_opp = client.get("/opportunities/")
    assert r_opp.status_code == 200, f"GET /opportunities/ failed: {r_opp.text}"
    opp_list = r_opp.json()
    print(f"[PASS] GET /opportunities/ returned {len(opp_list)} opportunities from PostgreSQL")
    assert len(opp_list) >= 6

    # Notifications (using admin token)
    r_notif = client.get("/notifications/", headers=admin_headers)
    assert r_notif.status_code == 200, f"GET /notifications/ failed: {r_notif.text}"
    notif_list = r_notif.json()
    print(f"[PASS] GET /notifications/ returned {len(notif_list)} notifications for admin from PostgreSQL")

    # Admin Statistics
    r_stats = client.get("/admin/statistics", headers=admin_headers)
    assert r_stats.status_code == 200, f"GET /admin/statistics failed: {r_stats.text}"
    stats = r_stats.json()
    print(f"[PASS] GET /admin/statistics live metrics: {stats}")
    assert stats["total_users"] >= 43
    assert stats["total_alumni"] >= 20

    # 5. RBAC Security Verification over HTTPS
    print("\n--- 4. RBAC & SECURITY VERIFICATION ---")
    ts = int(time.time())
    student_creds = {
        "name": "Live HTTPS Tester",
        "email": f"https_tester_{ts}@alumni.edu",
        "password": "SecurePass123!",
        "role": "student"
    }
    r_reg = client.post("/auth/register", json=student_creds)
    assert r_reg.status_code == 201, f"Student registration failed: {r_reg.text}"
    student_token = r_reg.json()["access_token"]
    student_headers = {"Authorization": f"Bearer {student_token}"}
    print(f"[PASS] Created and authenticated test student (ID: {r_reg.json()['user']['id']})")

    # Non-admin attempting to access /admin/statistics must be rejected with 403
    r_forbidden = client.get("/admin/statistics", headers=student_headers)
    assert r_forbidden.status_code == 403, f"Expected 403 Forbidden, got {r_forbidden.status_code}"
    print("[PASS] RBAC Enforced: Non-admin student blocked from admin endpoint (403 Forbidden)")

    print("\n" + "=" * 70)
    print("ALL LIVE DEPLOYED HTTPS BACKEND TESTS PASSED WITH 100% SUCCESS!")
    print("=" * 70)

if __name__ == "__main__":
    run_suite()

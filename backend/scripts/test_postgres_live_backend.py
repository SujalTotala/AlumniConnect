import os
import sys
import time
from pathlib import Path

BACKEND_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BACKEND_DIR))

# Load .env
env_path = BACKEND_DIR / ".env"
if env_path.exists():
    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                os.environ.setdefault(k.strip(), v.strip())

target_url = os.getenv("TARGET_DATABASE_URL")
assert target_url, "TARGET_DATABASE_URL is required"

# Temporarily point DATABASE_URL to PostgreSQL target
os.environ["DATABASE_URL"] = target_url

# Now import FastAPI app and TestClient
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def run_tests():
    print("=" * 70)
    print("RUNNING SAFE LIVE BACKEND INTEGRATION TESTS AGAINST POSTGRESQL TARGET")
    print("=" * 70)

    # 1. Root / Health endpoint check
    r = client.get("/")
    assert r.status_code == 200, f"Root endpoint failed: {r.text}"
    print(f"[PASS] GET / returned 200 OK: {r.json()}")

    # 2. Authenticate existing admin
    admin_login = client.post("/auth/login", json={"email": "admin_p4@alumni.edu", "password": "AdminPass123!"})
    assert admin_login.status_code == 200, f"Admin login failed: {admin_login.text}"
    admin_token = admin_login.json()["access_token"]
    admin_h = {"Authorization": f"Bearer {admin_token}"}
    print("[PASS] Successfully authenticated migrated Admin on PostgreSQL")

    # 3. Verify Admin Live Statistics from PostgreSQL
    stats_r = client.get("/admin/statistics", headers=admin_h)
    assert stats_r.status_code == 200, f"Admin statistics failed: {stats_r.text}"
    stats = stats_r.json()
    print(f"[PASS] Admin statistics on PostgreSQL: {stats}")
    assert stats["total_users"] >= 43
    assert stats["total_alumni"] >= 20

    # 4. Verify Alumni Directory fetch
    alumni_r = client.get("/alumni/")
    assert alumni_r.status_code == 200, f"Alumni fetch failed: {alumni_r.text}"
    print(f"[PASS] Retrieved {len(alumni_r.json())} alumni profiles from PostgreSQL")

    # 5. Verify Events Directory fetch
    events_r = client.get("/events/")
    assert events_r.status_code == 200, f"Events fetch failed: {events_r.text}"
    print(f"[PASS] Retrieved {len(events_r.json())} events from PostgreSQL")

    # 6. Verify Opportunities Directory fetch
    opp_r = client.get("/opportunities/")
    assert opp_r.status_code == 200, f"Opportunities fetch failed: {opp_r.text}"
    print(f"[PASS] Retrieved {len(opp_r.json())} opportunities from PostgreSQL")

    # 7. Create a new test user and verify PostgreSQL write and sequence auto-increment
    ts = int(time.time())
    new_student = {
        "name": "Live PG Tester",
        "email": f"pg_tester_{ts}@alumni.edu",
        "password": "SecurePass123!",
        "role": "student"
    }
    reg_r = client.post("/auth/register", json=new_student)
    assert reg_r.status_code == 201, f"Registration failed: {reg_r.text}"
    student_token = reg_r.json()["access_token"]
    student_h = {"Authorization": f"Bearer {student_token}"}
    print(f"[PASS] Successfully registered new user on PostgreSQL with auto-assigned ID: {reg_r.json()['user']['id']}")

    # 8. User profile retrieval on PostgreSQL
    me_r = client.get("/auth/me", headers=student_h)
    assert me_r.status_code == 200
    assert me_r.json()["email"] == new_student["email"]
    print("[PASS] GET /auth/me verified for new session on PostgreSQL")

    # 9. RBAC Security verification on PostgreSQL
    forbidden_r = client.get("/admin/statistics", headers=student_h)
    assert forbidden_r.status_code == 403
    print("[PASS] RBAC verification passed: student blocked from admin endpoint (403)")

    print("=" * 70)
    print("ALL LIVE POSTGRESQL BACKEND INTEGRATION TESTS COMPLETED SUCCESSFULLY!")
    print("=" * 70)

if __name__ == "__main__":
    run_tests()

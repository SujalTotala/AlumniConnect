from fastapi.testclient import TestClient
from app.main import app
from app.database.database import get_db, Base, engine
import uuid

client = TestClient(app)

def test_phase6a_android_auth_flow():
    # 1. Register a student user matching Android RegisterRequest
    test_id = uuid.uuid4().hex[:6]
    student_email = f"android_student_{test_id}@test.com"
    register_payload = {
        "name": f"Android Student {test_id}",
        "email": student_email,
        "password": "Password123!",
        "role": "student"
    }

    reg_resp = client.post("/auth/register", json=register_payload)
    assert reg_resp.status_code == 201, f"Register failed: {reg_resp.text}"
    reg_data = reg_resp.json()
    assert "access_token" in reg_data
    assert reg_data["user"]["email"] == student_email
    assert reg_data["user"]["role"] == "student"

    # 2. Login user matching Android LoginRequest
    login_payload = {
        "email": student_email,
        "password": "Password123!"
    }
    login_resp = client.post("/auth/login", json=login_payload)
    assert login_resp.status_code == 200, f"Login failed: {login_resp.text}"
    login_data = login_resp.json()
    assert "access_token" in login_data
    token = login_data["access_token"]
    assert token is not None

    # 3. Call /auth/me with Bearer token matching Android AuthInterceptor
    headers = {"Authorization": f"Bearer {token}"}
    me_resp = client.get("/auth/me", headers=headers)
    assert me_resp.status_code == 200, f"Auth Me failed: {me_resp.text}"
    me_data = me_resp.json()
    assert me_data["email"] == student_email
    assert me_data["name"] == register_payload["name"]
    assert me_data["role"] == "student"
    assert me_data["is_active"] is True

    # 4. Register an alumni user matching Android RegisterRequest
    alumni_email = f"android_alumni_{test_id}@test.com"
    alumni_payload = {
        "name": f"Android Alumni {test_id}",
        "email": alumni_email,
        "password": "Password123!",
        "role": "alumni"
    }
    alumni_reg_resp = client.post("/auth/register", json=alumni_payload)
    assert alumni_reg_resp.status_code == 201
    alumni_data = alumni_reg_resp.json()
    assert alumni_data["user"]["role"] == "alumni"

    print("\n[SUCCESS] Phase 6A Android Authentication & JWT Validation Tests Passed 100%!")

if __name__ == "__main__":
    test_phase6a_android_auth_flow()

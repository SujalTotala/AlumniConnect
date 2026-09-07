import time
import unittest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

class TestAuthSmoke(unittest.TestCase):
    def setUp(self):
        self.ts = int(time.time() * 1000)

    def test_01_register_student(self):
        email = f"student_smoke_{self.ts}@example.com"
        res = client.post("/auth/register", json={
            "name": "Smoke Student",
            "email": email,
            "password": "Password123!",
            "role": "student"
        })
        self.assertEqual(res.status_code, 201)
        data = res.json()
        self.assertIn("access_token", data)
        self.assertEqual(data["user"]["role"], "student")
        self.assertEqual(data["user"]["email"], email)

    def test_02_login_student_case_insensitive(self):
        email = f"case_student_{self.ts}@example.com"
        # Register in lowercase
        r_reg = client.post("/auth/register", json={
            "name": "Case Student",
            "email": email,
            "password": "Password123!",
            "role": "student"
        })
        self.assertEqual(r_reg.status_code, 201)

        # Login with mixed case email
        mixed_email = f"CASE_Student_{self.ts}@EXAMPLE.COM"
        res = client.post("/auth/login", json={
            "email": mixed_email,
            "password": "Password123!"
        })
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertIn("access_token", data)
        self.assertEqual(data["user"]["email"], email)

    def test_03_auth_me(self):
        email = f"me_test_{self.ts}@example.com"
        r_reg = client.post("/auth/register", json={
            "name": "Me User",
            "email": email,
            "password": "Password123!",
            "role": "student"
        })
        self.assertEqual(r_reg.status_code, 201)
        token = r_reg.json()["access_token"]

        res = client.get("/auth/me", headers={"Authorization": f"Bearer {token}"})
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["email"], email)

    def test_04_register_alumni(self):
        email = f"alumni_smoke_{self.ts}@example.com"
        res = client.post("/auth/register", json={
            "name": "Smoke Alumni",
            "email": email,
            "password": "Password123!",
            "role": "alumni"
        })
        self.assertEqual(res.status_code, 201)
        data = res.json()
        self.assertEqual(data["user"]["role"], "alumni")

    def test_05_login_alumni(self):
        email = f"alumni_log_{self.ts}@example.com"
        client.post("/auth/register", json={
            "name": "Log Alumni",
            "email": email,
            "password": "Password123!",
            "role": "alumni"
        })
        res = client.post("/auth/login", json={
            "email": email,
            "password": "Password123!"
        })
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["user"]["role"], "alumni")

    def test_06_wrong_password_rejected(self):
        email = f"wrong_pwd_{self.ts}@example.com"
        client.post("/auth/register", json={
            "name": "Wrong Pwd",
            "email": email,
            "password": "Password123!",
            "role": "student"
        })
        res = client.post("/auth/login", json={
            "email": email,
            "password": "IncorrectPassword!"
        })
        self.assertEqual(res.status_code, 400)
        self.assertIn("Invalid email or password", res.json()["detail"])

    def test_07_duplicate_email_rejected(self):
        email = f"dup_email_{self.ts}@example.com"
        r1 = client.post("/auth/register", json={
            "name": "First User",
            "email": email,
            "password": "Password123!",
            "role": "student"
        })
        self.assertEqual(r1.status_code, 201)

        r2 = client.post("/auth/register", json={
            "name": "Second User",
            "email": email.upper(),
            "password": "Password123!",
            "role": "student"
        })
        self.assertEqual(r2.status_code, 400)
        self.assertIn("already registered", r2.json()["detail"])

    def test_08_invalid_role_defaults_or_handled(self):
        email = f"inv_role_{self.ts}@example.com"
        res = client.post("/auth/register", json={
            "name": "Role Test",
            "email": email,
            "password": "Password123!",
            "role": "Student"
        })
        self.assertEqual(res.status_code, 201)
        self.assertEqual(res.json()["user"]["role"], "student")

    def test_09_inactive_user_rejected(self):
        from app.database.database import SessionLocal
        from app.models.user_model import User

        email = f"inactive_{self.ts}@example.com"
        client.post("/auth/register", json={
            "name": "Inactive User",
            "email": email,
            "password": "Password123!",
            "role": "student"
        })

        db = SessionLocal()
        u = db.query(User).filter(User.email == email).first()
        u.is_active = False
        db.commit()
        db.close()

        res = client.post("/auth/login", json={
            "email": email,
            "password": "Password123!"
        })
        self.assertEqual(res.status_code, 403)
        self.assertIn("deactivated", res.json()["detail"].lower())

if __name__ == "__main__":
    unittest.main()

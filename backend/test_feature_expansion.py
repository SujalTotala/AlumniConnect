import sys
import unittest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

class TestFeatureExpansionPass1(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # 1. Authenticate Admin using seeded credentials
        res_admin = client.post("/auth/login", json={"email": "admin_p4@alumni.edu", "password": "AdminPass123!"})
        if res_admin.status_code != 200:
            res_admin = client.post("/auth/register", json={
                "name": "Admin Officer",
                "email": "admin_p4@alumni.edu",
                "password": "AdminPass123!",
                "role": "admin"
            })
        cls.admin_token = res_admin.json().get("access_token")
        cls.admin_headers = {"Authorization": f"Bearer {cls.admin_token}"}

        # 2. Register Student with unique timestamp
        import time
        ts = int(time.time())
        student_email = f"student_feat_{ts}@example.com"
        res_student = client.post("/auth/register", json={
            "name": "Student Feat Tester",
            "email": student_email,
            "password": "Password123!",
            "role": "student"
        })
        if res_student.status_code != 200:
            res_student = client.post("/auth/login", json={"email": student_email, "password": "Password123!"})
        cls.student_token = res_student.json().get("access_token")
        cls.student_headers = {"Authorization": f"Bearer {cls.student_token}"}

        # 3. Register Alumni with unique timestamp
        alumni_email = f"alumni_feat_{ts}@example.com"
        res_alumni = client.post("/auth/register", json={
            "name": "Alumni Mentor Feat",
            "email": alumni_email,
            "password": "Password123!",
            "role": "alumni"
        })
        if res_alumni.status_code != 200:
            res_alumni = client.post("/auth/login", json={"email": alumni_email, "password": "Password123!"})
        cls.alumni_token = res_alumni.json().get("access_token")
        cls.alumni_headers = {"Authorization": f"Bearer {cls.alumni_token}"}

        # Setup profiles
        client.put("/profile/me", headers=cls.student_headers, json={
            "branch": "Computer Science",
            "year": "2025",
            "skills": "Python, React, FastApi",
            "interests": "Machine Learning, Backend Development"
        })

        client.put("/profile/me", headers=cls.alumni_headers, json={
            "graduation_year": "2020",
            "department": "Computer Science",
            "company": "TechCorp Global",
            "job_role": "Senior Backend Engineer",
            "skills": "Python, Django, PostgreSQL, FastAPI",
            "mentorship_available": True
        })

        # Fetch alumni ID for testing
        alumni_list = client.get("/alumni/").json()
        cls.alumni_id = alumni_list[0]["id"]

        # Create an opportunity for testing
        opp_res = client.post("/opportunities/", headers=cls.alumni_headers, json={
            "title": "Backend Intern 2026",
            "company": "TechCorp Global",
            "description": "Looking for enthusiastic Python/FastAPI interns.",
            "opportunity_type": "INTERNSHIP",
            "location": "Remote",
            "deadline": "2026-12-31",
            "application_url": "https://techcorp.example.com/apply"
        })
        cls.opportunity_id = opp_res.json()["id"]

    def test_01_alumni_verification(self):
        # 1. Non-admin cannot verify
        res = client.put(f"/admin/alumni/{self.alumni_id}/verify?is_verified=true", headers=self.student_headers)
        self.assertEqual(res.status_code, 403)

        # 2. Admin can verify
        res = client.put(f"/admin/alumni/{self.alumni_id}/verify?is_verified=true", headers=self.admin_headers)
        self.assertEqual(res.status_code, 200)
        self.assertTrue(res.json()["is_verified"])

        # 3. Alumni directory filters by verified
        res_dir = client.get(f"/alumni/?is_verified=true")
        self.assertEqual(res_dir.status_code, 200)
        found = any(a["id"] == self.alumni_id and a["is_verified"] is True for a in res_dir.json())
        self.assertTrue(found)

    def test_02_bookmarks_lifecycle(self):
        # 1. Invalid entity type rejected
        res = client.post("/bookmarks/", headers=self.student_headers, json={
            "entity_type": "invalid_type",
            "entity_id": 1
        })
        self.assertEqual(res.status_code, 422)

        # 2. Non-existent entity returns 404
        res = client.post("/bookmarks/", headers=self.student_headers, json={
            "entity_type": "alumni",
            "entity_id": 999999
        })
        self.assertEqual(res.status_code, 404)

        # 3. Valid bookmark creation
        res = client.post("/bookmarks/", headers=self.student_headers, json={
            "entity_type": "alumni",
            "entity_id": self.alumni_id
        })
        self.assertEqual(res.status_code, 201)
        bm_id = res.json()["id"]
        self.assertIsNotNone(res.json()["entity_preview"])

        # 4. Check bookmark status
        res_check = client.get(f"/bookmarks/check/alumni/{self.alumni_id}", headers=self.student_headers)
        self.assertEqual(res_check.status_code, 200)
        self.assertTrue(res_check.json()["is_bookmarked"])

        # 5. List bookmarks
        res_list = client.get("/bookmarks/", headers=self.student_headers)
        self.assertEqual(res_list.status_code, 200)
        self.assertTrue(any(b["id"] == bm_id for b in res_list.json()))

        # 6. Delete bookmark by entity
        res_del = client.delete(f"/bookmarks/alumni/{self.alumni_id}", headers=self.student_headers)
        self.assertEqual(res_del.status_code, 200)

        # 7. Check bookmark status is now false
        res_check2 = client.get(f"/bookmarks/check/alumni/{self.alumni_id}", headers=self.student_headers)
        self.assertEqual(res_check2.status_code, 200)
        self.assertFalse(res_check2.json()["is_bookmarked"])

    def test_03_announcements_lifecycle(self):
        # 1. Non-admin cannot create announcement
        res = client.post("/announcements/", headers=self.student_headers, json={
            "title": "Unauthorized Notice",
            "content": "This should be blocked."
        })
        self.assertEqual(res.status_code, 403)

        # 2. Admin creates announcement
        res = client.post("/announcements/", headers=self.admin_headers, json={
            "title": "Annual Alumni Meet 2026",
            "content": "Registration is now open for the grand reunion in campus auditorium.",
            "category": "ALUMNI_MEET",
            "priority": "HIGH"
        })
        self.assertEqual(res.status_code, 201)
        announcement_id = res.json()["id"]
        self.assertEqual(res.json()["priority"], "HIGH")

        # 3. Authenticated user can read announcements
        res_get = client.get("/announcements/", headers=self.student_headers)
        self.assertEqual(res_get.status_code, 200)
        self.assertTrue(any(a["id"] == announcement_id for a in res_get.json()))

        # 4. Admin deletes announcement
        res_del = client.delete(f"/announcements/{announcement_id}", headers=self.admin_headers)
        self.assertEqual(res_del.status_code, 200)

    def test_04_mentor_recommendations(self):
        res = client.get("/alumni/recommendations/mentors", headers=self.student_headers)
        self.assertEqual(res.status_code, 200)
        recs = res.json()
        self.assertIsInstance(recs, list)
        if recs:
            top_rec = recs[0]
            self.assertIn("match_score", top_rec)
            self.assertIn("match_reasons", top_rec)
            self.assertGreaterEqual(top_rec["match_score"], 0)
            self.assertLessEqual(top_rec["match_score"], 100)
            self.assertIsInstance(top_rec["match_reasons"], list)

    def test_05_profile_completion_suggestions(self):
        res = client.get("/profile/completion-suggestions", headers=self.student_headers)
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertIn("completion_percentage", data)
        self.assertIn("missing_fields", data)
        self.assertGreaterEqual(data["completion_percentage"], 0)
        self.assertLessEqual(data["completion_percentage"], 100)

    def test_06_notification_preferences(self):
        # 1. Default preferences
        res = client.get("/notification-preferences/", headers=self.student_headers)
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertTrue(data["events"])
        self.assertTrue(data["mentorship"])

        # 2. Update preferences
        res_up = client.put("/notification-preferences/", headers=self.student_headers, json={
            "events": False,
            "announcements": True
        })
        self.assertEqual(res_up.status_code, 200)
        self.assertFalse(res_up.json()["events"])
        self.assertTrue(res_up.json()["announcements"])

    def test_07_activity_feed(self):
        res = client.get("/activity-feed/", headers=self.student_headers)
        self.assertEqual(res.status_code, 200)
        feed = res.json()
        self.assertIsInstance(feed, list)

    def test_08_admin_analytics_and_csv_export(self):
        # 1. Enhanced analytics
        res_stats = client.get("/admin/statistics", headers=self.admin_headers)
        self.assertEqual(res_stats.status_code, 200)
        stats = res_stats.json()
        self.assertIn("verified_alumni", stats)
        self.assertIn("total_announcements", stats)

        # 2. CSV Export blocked for non-admin
        res_csv_blocked = client.get("/admin/alumni/export-csv", headers=self.student_headers)
        self.assertEqual(res_csv_blocked.status_code, 403)

        # 3. CSV Export succeeds for admin
        res_csv = client.get("/admin/alumni/export-csv", headers=self.admin_headers)
        self.assertEqual(res_csv.status_code, 200)
        self.assertEqual(res_csv.headers.get("content-type"), "text/csv; charset=utf-8")
        self.assertIn("attachment; filename=alumni_directory.csv", res_csv.headers.get("content-disposition", ""))
        content = res_csv.text
        self.assertIn("Name,Email,Graduation Year", content)

if __name__ == "__main__":
    unittest.main()

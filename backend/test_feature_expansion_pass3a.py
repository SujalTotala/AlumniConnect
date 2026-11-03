import io
import time
import unittest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

class TestFeatureExpansionPass3A(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        ts = int(time.time() * 1000)

        from app.database.database import SessionLocal
        from app.models.user_model import User
        from app.models.alumni_model import Alumni
        from app.models.preference_model import NotificationPreference
        from app.auth.auth_handler import hash_password, create_access_token

        db = SessionLocal()

        # 1. Admin setup
        admin_user = db.query(User).filter(User.role == "admin").first()
        if not admin_user:
            admin_user = User(
                name="Pass3A Admin",
                email=f"admin_pass3a_{ts}@alumni.edu",
                password=hash_password("AdminPass123!"),
                role="admin",
                is_active=True
            )
            db.add(admin_user)
            db.commit()
            db.refresh(admin_user)

        cls.admin_user_id = admin_user.id
        cls.admin_token = create_access_token(data={"sub": admin_user.email, "role": "admin", "id": admin_user.id})
        cls.admin_headers = {"Authorization": f"Bearer {cls.admin_token}"}

        # 2. Student setup
        cls.student_email = f"student_pass3a_{ts}@example.com"
        res_s = client.post("/auth/register", json={
            "name": "Pass3A Student",
            "email": cls.student_email,
            "password": "Password123!",
            "role": "student"
        })
        cls.student_token = res_s.json().get("access_token")
        cls.student_headers = {"Authorization": f"Bearer {cls.student_token}"}
        me_s = client.get("/auth/me", headers=cls.student_headers).json()
        cls.student_id = me_s["id"]

        # 3. Target Alumni 1 (CS, 2022, Silicon Valley, Opted-in to announcements)
        cls.alumni1_email = f"alumni1_cs_{ts}@example.com"
        res_a1 = client.post("/auth/register", json={
            "name": "Alex Mercer",
            "email": cls.alumni1_email,
            "password": "Password123!",
            "role": "alumni"
        })
        cls.alumni1_token = res_a1.json().get("access_token")
        cls.alumni1_headers = {"Authorization": f"Bearer {cls.alumni1_token}"}
        me_a1 = client.get("/auth/me", headers=cls.alumni1_headers).json()
        cls.alumni1_user_id = me_a1["id"]

        a1_rec = db.query(Alumni).filter(Alumni.user_id == cls.alumni1_user_id).first()
        if not a1_rec:
            a1_rec = Alumni(
                user_id=cls.alumni1_user_id,
                name="Alex Mercer",
                email=cls.alumni1_email,
                department="Computer Science",
                graduation_year="2022",
                company="TechGiant",
                job_role="Senior Architect",
                location="San Francisco, CA",
                skills="Python, Distributed Systems",
                is_verified=True,
                mentorship_available=True
            )
            db.add(a1_rec)
        else:
            a1_rec.department = "Computer Science"
            a1_rec.graduation_year = "2022"
            a1_rec.company = "TechGiant"
            a1_rec.job_role = "Senior Architect"
            a1_rec.location = "San Francisco, CA"
            a1_rec.skills = "Python, Distributed Systems"
            a1_rec.is_verified = True
            a1_rec.mentorship_available = True

        # 4. Target Alumni 2 (CS, 2022, Opted-OUT of announcements)
        cls.alumni2_email = f"alumni2_cs_{ts}@example.com"
        res_a2 = client.post("/auth/register", json={
            "name": "Sarah Connor",
            "email": cls.alumni2_email,
            "password": "Password123!",
            "role": "alumni"
        })
        cls.alumni2_token = res_a2.json().get("access_token")
        cls.alumni2_headers = {"Authorization": f"Bearer {cls.alumni2_token}"}
        me_a2 = client.get("/auth/me", headers=cls.alumni2_headers).json()
        cls.alumni2_user_id = me_a2["id"]

        a2_rec = db.query(Alumni).filter(Alumni.user_id == cls.alumni2_user_id).first()
        if not a2_rec:
            a2_rec = Alumni(
                user_id=cls.alumni2_user_id,
                name="Sarah Connor",
                email=cls.alumni2_email,
                department="Computer Science",
                graduation_year="2022",
                company="Cyberdyne",
                job_role="Security Engineer",
                location="Austin, TX",
                is_verified=True
            )
            db.add(a2_rec)
        else:
            a2_rec.department = "Computer Science"
            a2_rec.graduation_year = "2022"
            a2_rec.company = "Cyberdyne"
            a2_rec.job_role = "Security Engineer"
            a2_rec.location = "Austin, TX"
            a2_rec.is_verified = True

        # Set Alumni 2 notification preference for announcements to False
        pref2 = db.query(NotificationPreference).filter(NotificationPreference.user_id == cls.alumni2_user_id).first()
        if not pref2:
            pref2 = NotificationPreference(user_id=cls.alumni2_user_id, announcements=False)
            db.add(pref2)
        else:
            pref2.announcements = False

        db.commit()
        db.close()

    def test_01_bulk_import_template_and_preview(self):
        # 1. Download CSV template
        res_tmpl = client.get("/admin/alumni/template", headers=self.admin_headers)
        self.assertEqual(res_tmpl.status_code, 200)
        self.assertIn("text/csv", res_tmpl.headers["content-type"])
        self.assertIn("name,email,graduation_year", res_tmpl.text)

        # 2. Upload sample CSV with valid new, exact duplicate, and soft duplicate
        ts = int(time.time() * 1000)
        csv_content = (
            "name,email,graduation_year,department,company,job_role,location,skills,bio,linkedin_url\n"
            f"New Alumni {ts},new_alumni_{ts}@test.edu,2023,Mechanical,AutoCorp,Design Lead,Detroit,CAD,Bio,https://linkedin.com/in/new{ts}\n"
            f"Alex Mercer Updated,{self.alumni1_email},2022,Computer Science,TechGiant Updated,Lead Architect,San Francisco,Python; Go,Updated Bio,https://linkedin.com/in/alex\n"
            f"Alex Mercer,different_email_{ts}@test.edu,2022,Computer Science,SomeCo,Dev,SF,JS,Bio,https://linkedin.com/in/other\n"
            "Invalid Row,,2020,Civil,,,,,\n"
        )
        file_obj = io.BytesIO(csv_content.encode("utf-8"))

        res_preview = client.post(
            "/admin/alumni/import/preview",
            headers=self.admin_headers,
            files={"file": ("alumni_batch.csv", file_obj, "text/csv")}
        )
        self.assertEqual(res_preview.status_code, 200)
        data = res_preview.json()
        self.assertEqual(data["total_rows"], 4)
        self.assertEqual(data["valid_rows"], 1)
        self.assertEqual(data["exact_duplicates"], 1)
        self.assertEqual(data["possible_duplicates"], 1)
        self.assertEqual(data["invalid_rows"], 1)
        self.assertTrue(bool(data["import_session_id"]))
        TestFeatureExpansionPass3A.import_session_id = data["import_session_id"]
        TestFeatureExpansionPass3A.new_alumni_email = f"new_alumni_{ts}@test.edu"

    def test_02_safe_update_existing_import_commit(self):
        session_id = getattr(self, "import_session_id", None)
        self.assertIsNotNone(session_id)

        # Commit with UPDATE_EXISTING
        res_commit = client.post(
            "/admin/alumni/import/commit",
            headers=self.admin_headers,
            json={"import_session_id": session_id, "mode": "UPDATE_EXISTING"}
        )
        self.assertEqual(res_commit.status_code, 200)
        commit_data = res_commit.json()
        self.assertEqual(commit_data["status"], "COMMITTED")
        self.assertEqual(commit_data["created_count"], 1)  # the valid new row
        self.assertEqual(commit_data["updated_count"], 1)  # exact duplicate updated safely
        self.assertEqual(commit_data["skipped_count"], 2)  # 1 invalid + 1 possible duplicate safely not auto-merged

        # Verify existing record was safely updated without losing verification
        from app.database.database import SessionLocal
        from app.models.alumni_model import Alumni
        db = SessionLocal()
        alumni1 = db.query(Alumni).filter(Alumni.email == self.alumni1_email).first()
        self.assertIsNotNone(alumni1)
        self.assertEqual(alumni1.company, "TechGiant Updated")
        self.assertEqual(alumni1.job_role, "Lead Architect")
        self.assertTrue(alumni1.is_verified)  # verification was preserved!

        # Verify new record was created
        new_alumni = db.query(Alumni).filter(Alumni.email == self.new_alumni_email).first()
        self.assertIsNotNone(new_alumni)
        self.assertEqual(new_alumni.department, "Mechanical")
        db.close()

    def test_03_create_new_only_rejection_on_duplicates(self):
        # Upload another CSV containing an exact duplicate
        csv_content = (
            "name,email,graduation_year,department\n"
            f"Duplicate Attempt,{self.alumni1_email},2022,Computer Science\n"
        )
        file_obj = io.BytesIO(csv_content.encode("utf-8"))

        res_preview = client.post(
            "/admin/alumni/import/preview",
            headers=self.admin_headers,
            files={"file": ("dup_check.csv", file_obj, "text/csv")}
        )
        self.assertEqual(res_preview.status_code, 200)
        session_id = res_preview.json()["import_session_id"]

        # Attempt commit with CREATE_NEW_ONLY mode -> MUST fail with 400
        res_commit = client.post(
            "/admin/alumni/import/commit",
            headers=self.admin_headers,
            json={"import_session_id": session_id, "mode": "CREATE_NEW_ONLY"}
        )
        self.assertEqual(res_commit.status_code, 400)
        self.assertIn("duplicate record(s) were found", res_commit.json()["detail"])

    def test_04_data_quality_dashboard(self):
        res = client.get("/admin/alumni/data-quality", headers=self.admin_headers)
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertGreaterEqual(data["total_alumni"], 2)
        self.assertGreaterEqual(data["verified_count"], 1)
        self.assertIn("average_completion_percentage", data)
        self.assertIn("missing_fields_count", data)
        self.assertIn("completion_distribution", data)
        self.assertIn("0-25%", data["completion_distribution"])
        self.assertIn("76-100%", data["completion_distribution"])

    def test_05_dynamic_segments_crud_and_preview(self):
        ts = int(time.time() * 1000)
        criteria = {
            "departments": ["Computer Science"],
            "graduation_year_min": "2020",
            "graduation_year_max": "2025"
        }

        # 1. Preview criteria without saving
        res_prev = client.post(
            "/admin/alumni/segments/preview",
            headers=self.admin_headers,
            json=criteria
        )
        self.assertEqual(res_prev.status_code, 200)
        prev_data = res_prev.json()
        self.assertGreaterEqual(prev_data["estimated_count"], 2)

        # 2. Create saved segment
        seg_name = f"CS Recent Grads {ts}"
        res_create = client.post(
            "/admin/alumni/segments",
            headers=self.admin_headers,
            json={
                "name": seg_name,
                "description": "Computer Science graduates between 2020 and 2025",
                "filters": criteria
            }
        )
        self.assertEqual(res_create.status_code, 201)
        seg_data = res_create.json()
        seg_id = seg_data["id"]
        TestFeatureExpansionPass3A.segment_id = seg_id
        TestFeatureExpansionPass3A.segment_name = seg_name

        # 3. List segments
        res_list = client.get("/admin/alumni/segments", headers=self.admin_headers)
        self.assertEqual(res_list.status_code, 200)
        self.assertTrue(any(s["id"] == seg_id for s in res_list.json()))

        # 4. View segment members
        res_members = client.get(f"/admin/alumni/segments/{seg_id}/members", headers=self.admin_headers)
        self.assertEqual(res_members.status_code, 200)
        members_data = res_members.json()
        self.assertGreaterEqual(members_data["total_count"], 2)
        member_emails = [m["email"] for m in members_data["members"]]
        self.assertIn(self.alumni1_email, member_emails)
        self.assertIn(self.alumni2_email, member_emails)

        # 5. Export segment CSV with formula injection mitigation
        res_export = client.get(f"/admin/alumni/segments/{seg_id}/export", headers=self.admin_headers)
        self.assertEqual(res_export.status_code, 200)
        self.assertIn("text/csv", res_export.headers["content-type"])
        self.assertIn("Alex Mercer", res_export.text)

    def test_06_targeted_announcements_and_notifications(self):
        seg_id = getattr(self, "segment_id", None)
        self.assertIsNotNone(seg_id)

        # 1. Admin posts announcement targeted to this segment
        ts = int(time.time() * 1000)
        ann_title = f"Exclusive CS Masterclass {ts}"
        res_ann = client.post(
            "/announcements/",
            headers=self.admin_headers,
            json={
                "title": ann_title,
                "content": "Special AI engineering session for CS alumni.",
                "category": "COLLEGE_NOTICE",
                "priority": "HIGH",
                "audience_type": "SEGMENT",
                "segment_id": seg_id
            }
        )
        self.assertEqual(res_ann.status_code, 201)
        ann_data = res_ann.json()
        self.assertEqual(ann_data["audience_type"], "SEGMENT")
        self.assertEqual(ann_data["segment_id"], seg_id)

        # 2. Student requests announcements -> MUST NOT see the segment announcement
        res_s_ann = client.get("/announcements/", headers=self.student_headers)
        self.assertEqual(res_s_ann.status_code, 200)
        titles_student = [a["title"] for a in res_s_ann.json()]
        self.assertNotIn(ann_title, titles_student)

        # 3. Alumni 1 (matching segment criteria) requests announcements -> MUST see it
        res_a1_ann = client.get("/announcements/", headers=self.alumni1_headers)
        self.assertEqual(res_a1_ann.status_code, 200)
        titles_a1 = [a["title"] for a in res_a1_ann.json()]
        self.assertIn(ann_title, titles_a1)

        # 4. Check in-app notification delivery
        # Alumni 1 (announcements enabled) -> received notification
        res_a1_notif = client.get("/notifications/", headers=self.alumni1_headers)
        self.assertEqual(res_a1_notif.status_code, 200)
        notif_titles_a1 = [n["title"] for n in res_a1_notif.json()]
        self.assertTrue(any(ann_title in t for t in notif_titles_a1))

        # Alumni 2 (announcements opted out) -> did NOT receive notification
        res_a2_notif = client.get("/notifications/", headers=self.alumni2_headers)
        self.assertEqual(res_a2_notif.status_code, 200)
        notif_titles_a2 = [n["title"] for n in res_a2_notif.json()]
        self.assertFalse(any(ann_title in t for t in notif_titles_a2))

    def test_07_admin_audit_logs(self):
        # Query audit logs
        res_logs = client.get("/admin/audit-logs", headers=self.admin_headers)
        self.assertEqual(res_logs.status_code, 200)
        logs = res_logs.json()
        self.assertGreaterEqual(len(logs), 1)

        actions = [l["action"] for l in logs]
        self.assertTrue(any(a in actions for a in ["IMPORT_COMMIT", "CREATE_SEGMENT", "CREATE_ANNOUNCEMENT"]))

        # Verify no sensitive credentials appear in details
        for l in logs:
            if l.get("details"):
                self.assertNotIn("password", l["details"].lower())
                self.assertNotIn("token", l["details"].lower())

    def test_08_cohort_analytics(self):
        res_stats = client.get("/admin/statistics", headers=self.admin_headers)
        self.assertEqual(res_stats.status_code, 200)
        data = res_stats.json()
        self.assertIn("cohort_analytics", data)
        cohorts = data["cohort_analytics"]
        self.assertIsInstance(cohorts, dict)
        if "2022" in cohorts:
            self.assertIn("total_alumni", cohorts["2022"])
            self.assertIn("verified_alumni", cohorts["2022"])
            self.assertIn("active_mentors", cohorts["2022"])

if __name__ == "__main__":
    unittest.main()

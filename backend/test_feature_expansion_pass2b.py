import time
import unittest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


class TestFeatureExpansionPass2B(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        ts = int(time.time() * 1000)

        # 1. Admin setup
        from app.database.database import SessionLocal
        from app.models.user_model import User
        from app.models.alumni_model import Alumni
        from app.auth.auth_handler import hash_password, create_access_token

        db = SessionLocal()
        admin_user = db.query(User).filter(User.role == "admin").first()
        if not admin_user:
            admin_user = User(
                name="Pass2B Admin",
                email=f"admin_pass2b_{ts}@alumni.edu",
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
        cls.student_email = f"student_pass2b_{ts}@example.com"
        res_s = client.post("/auth/register", json={
            "name": "Pass2B Student",
            "email": cls.student_email,
            "password": "Password123!",
            "role": "student"
        })
        cls.student_token = res_s.json().get("access_token")
        cls.student_headers = {"Authorization": f"Bearer {cls.student_token}"}
        me_s = client.get("/auth/me", headers=cls.student_headers).json()
        cls.student_id = me_s["id"]

        # 3. Verified Alumni setup
        cls.alumni_email = f"alumni_pass2b_{ts}@example.com"
        res_a = client.post("/auth/register", json={
            "name": "Pass2B Verified Alumni",
            "email": cls.alumni_email,
            "password": "Password123!",
            "role": "alumni"
        })
        cls.alumni_token = res_a.json().get("access_token")
        cls.alumni_headers = {"Authorization": f"Bearer {cls.alumni_token}"}
        me_a = client.get("/auth/me", headers=cls.alumni_headers).json()
        cls.alumni_user_id = me_a["id"]

        # Mark alumni as verified in database
        alumni_rec = db.query(Alumni).filter(Alumni.user_id == cls.alumni_user_id).first()
        if not alumni_rec:
            alumni_rec = Alumni(
                user_id=cls.alumni_user_id,
                name="Pass2B Verified Alumni",
                email=cls.alumni_email,
                is_verified=True
            )
            db.add(alumni_rec)
        else:
            alumni_rec.is_verified = True
        db.commit()
        db.refresh(alumni_rec)
        cls.alumni_id = alumni_rec.id

        # 4. Unverified Alumni setup
        cls.unverified_email = f"unverified_pass2b_{ts}@example.com"
        res_u = client.post("/auth/register", json={
            "name": "Pass2B Unverified",
            "email": cls.unverified_email,
            "password": "Password123!",
            "role": "alumni"
        })
        cls.unverified_token = res_u.json().get("access_token")
        cls.unverified_headers = {"Authorization": f"Bearer {cls.unverified_token}"}
        me_u = client.get("/auth/me", headers=cls.unverified_headers).json()
        cls.unverified_user_id = me_u["id"]
        
        unv_rec = db.query(Alumni).filter(Alumni.user_id == cls.unverified_user_id).first()
        if unv_rec:
            unv_rec.is_verified = False
            db.commit()

        # 5. Create an event by Alumni (Organizer)
        res_evt = client.post("/events/", headers=cls.alumni_headers, json={
            "title": f"Pass2B Tech Summit {ts}",
            "description": "Annual technical conference and networking meetup",
            "event_date": "2026-10-15T10:00:00",
            "location": "Convention Center Main Hall"
        })
        cls.event_id = res_evt.json().get("id")
        cls.qr_token = None

        db.close()

    # ----------------------------------------------------
    # 1. EVENT QR ATTENDANCE TESTS
    # ----------------------------------------------------
    def test_01_qr_token_authorization(self):
        # Non-organizer / non-admin cannot generate QR token
        res_forbidden = client.post(
            f"/events/{self.event_id}/attendance/qr-token",
            headers=self.student_headers
        )
        self.assertEqual(res_forbidden.status_code, 403)

        # Event organizer can generate QR token
        res_success = client.post(
            f"/events/{self.event_id}/attendance/qr-token",
            headers=self.alumni_headers
        )
        self.assertEqual(res_success.status_code, 200)
        data = res_success.json()
        self.assertIn("qr_token", data)
        self.assertIn("expires_at", data)
        self.assertEqual(data["event_id"], self.event_id)
        TestFeatureExpansionPass2B.qr_token = data["qr_token"]

    def test_02_qr_check_in_lifecycle(self):
        # 1. Attempt check-in without event registration -> should fail with 400
        res_unreg = client.post(
            "/events/attendance/check-in",
            headers=self.student_headers,
            json={"qr_token": self.qr_token}
        )
        self.assertEqual(res_unreg.status_code, 400)
        self.assertIn("not registered", res_unreg.json()["detail"].lower())

        # 2. Register for the event
        res_reg = client.post(
            f"/events/{self.event_id}/register",
            headers=self.student_headers
        )
        self.assertEqual(res_reg.status_code, 201)

        # 3. Check in with valid QR token -> 201 Created
        res_checkin = client.post(
            "/events/attendance/check-in",
            headers=self.student_headers,
            json={"qr_token": self.qr_token}
        )
        self.assertEqual(res_checkin.status_code, 201)
        data = res_checkin.json()
        self.assertEqual(data["event_id"], self.event_id)
        self.assertEqual(data["user_id"], self.student_id)
        self.assertEqual(data["checkin_method"], "QR")

        # 4. Duplicate check-in -> should reject with 400
        res_dup = client.post(
            "/events/attendance/check-in",
            headers=self.student_headers,
            json={"qr_token": self.qr_token}
        )
        self.assertEqual(res_dup.status_code, 400)
        self.assertIn("already checked in", res_dup.json()["detail"].lower())

    def test_03_manual_check_in_and_stats(self):
        # Register unverified user for the event first
        client.post(f"/events/{self.event_id}/register", headers=self.unverified_headers)

        # Manual check-in by organizer
        res_manual = client.post(
            f"/events/{self.event_id}/attendance/manual",
            headers=self.alumni_headers,
            json={"user_id": self.unverified_user_id, "notes": "Checked in at reception desk"}
        )
        self.assertEqual(res_manual.status_code, 201)
        self.assertEqual(res_manual.json()["checkin_method"], "MANUAL")

        # Fetch attendance roster
        res_list = client.get(
            f"/events/{self.event_id}/attendance",
            headers=self.alumni_headers
        )
        self.assertEqual(res_list.status_code, 200)
        attendees = res_list.json()
        self.assertGreaterEqual(len(attendees), 2)

        # Fetch attendance stats
        res_stats = client.get(
            f"/events/{self.event_id}/attendance/stats",
            headers=self.alumni_headers
        )
        self.assertEqual(res_stats.status_code, 200)
        stats = res_stats.json()
        self.assertGreaterEqual(stats["total_registered"], 2)
        self.assertGreaterEqual(stats["total_attended"], 2)
        self.assertGreaterEqual(stats["attendance_percentage"], 50.0)

    # ----------------------------------------------------
    # 2. SUCCESS STORIES TESTS
    # ----------------------------------------------------
    def test_04_success_story_submission_and_moderation(self):
        # 1. Unverified alumni cannot submit story -> 403
        res_unv = client.post("/success-stories/", headers=self.unverified_headers, json={
            "title": "Unverified Story",
            "summary": "Should be rejected",
            "content": "This story should not be allowed until verified."
        })
        self.assertEqual(res_unv.status_code, 403)

        # 2. Student cannot submit story -> 403
        res_stu = client.post("/success-stories/", headers=self.student_headers, json={
            "title": "Student Story",
            "summary": "Should be rejected",
            "content": "Students cannot submit alumni stories."
        })
        self.assertEqual(res_stu.status_code, 403)

        # 3. Verified alumni submits story -> 201 PENDING
        res_sub = client.post("/success-stories/", headers=self.alumni_headers, json={
            "title": "From Campus to Cloud Architect",
            "summary": "How our engineering curriculum led to a successful cloud career",
            "content": "Detailed journey through internships, open-source contributions, and landing a staff role.",
            "company": "Hyperscale Cloud Corp",
            "role_title": "Lead Cloud Architect",
            "graduation_year": 2020
        })
        self.assertEqual(res_sub.status_code, 201)
        story_id = res_sub.json()["id"]
        self.assertEqual(res_sub.json()["status"], "PENDING")

        # 4. Public feed should NOT list pending story
        res_pub = client.get("/success-stories/")
        self.assertEqual(res_pub.status_code, 200)
        public_ids = [s["id"] for s in res_pub.json()]
        self.assertNotIn(story_id, public_ids)

        # 5. Author can see their own story in /mine
        res_mine = client.get("/success-stories/mine", headers=self.alumni_headers)
        self.assertEqual(res_mine.status_code, 200)
        my_ids = [s["id"] for s in res_mine.json()]
        self.assertIn(story_id, my_ids)

        # 6. Admin sees story in pending queue
        res_pending = client.get("/success-stories/admin/pending", headers=self.admin_headers)
        self.assertEqual(res_pending.status_code, 200)
        pending_ids = [s["id"] for s in res_pending.json()]
        self.assertIn(story_id, pending_ids)

        # 7. Admin approves story
        res_appr = client.put(f"/success-stories/admin/{story_id}/approve", headers=self.admin_headers)
        self.assertEqual(res_appr.status_code, 200)
        self.assertEqual(res_appr.json()["status"], "APPROVED")

        # 8. Story is now in public feed
        res_pub2 = client.get("/success-stories/")
        self.assertEqual(res_pub2.status_code, 200)
        pub_ids2 = [s["id"] for s in res_pub2.json()]
        self.assertIn(story_id, pub_ids2)

    # ----------------------------------------------------
    # 3. COMMUNITIES / CHAPTERS TESTS
    # ----------------------------------------------------
    def test_05_community_lifecycle_and_discussions(self):
        ts = int(time.time() * 1000)
        # 1. Create a City Chapter community
        res_comm = client.post("/communities/", headers=self.alumni_headers, json={
            "name": f"Bengaluru Tech Alumni {ts}",
            "description": "Regional chapter for alumni in the Bengaluru IT corridor",
            "community_type": "CITY",
            "city": "Bengaluru",
            "country": "India"
        })
        self.assertEqual(res_comm.status_code, 201)
        comm_data = res_comm.json()
        comm_id = comm_data["id"]
        # Creator automatically joined
        self.assertEqual(comm_data["members_count"], 1)

        # 2. List communities
        res_list = client.get("/communities/", headers=self.student_headers)
        self.assertEqual(res_list.status_code, 200)
        comm_ids = [c["id"] for c in res_list.json()]
        self.assertIn(comm_id, comm_ids)

        # 3. Student joins community
        res_join = client.post(f"/communities/{comm_id}/join", headers=self.student_headers)
        self.assertEqual(res_join.status_code, 201)

        # 4. Duplicate join rejection
        res_dup_join = client.post(f"/communities/{comm_id}/join", headers=self.student_headers)
        self.assertEqual(res_dup_join.status_code, 400)

        # 5. Post in community
        res_post = client.post(f"/communities/{comm_id}/posts", headers=self.student_headers, json={
            "content": "Excited to connect with alumni in Karnataka region!"
        })
        self.assertEqual(res_post.status_code, 201)
        self.assertEqual(res_post.json()["content"], "Excited to connect with alumni in Karnataka region!")

        # 6. List posts
        res_posts = client.get(f"/communities/{comm_id}/posts", headers=self.student_headers)
        self.assertEqual(res_posts.status_code, 200)
        self.assertGreaterEqual(len(res_posts.json()), 1)

        # 7. Student leaves community
        res_leave = client.delete(f"/communities/{comm_id}/leave", headers=self.student_headers)
        self.assertEqual(res_leave.status_code, 200)

    # ----------------------------------------------------
    # 4. ACHIEVEMENTS & RECOGNITION TESTS
    # ----------------------------------------------------
    def test_06_achievement_submission_and_moderation(self):
        # 1. Student cannot submit achievement -> 403
        res_stu = client.post("/achievements/", headers=self.student_headers, json={
            "title": "Student Award",
            "description": "Student achievement submission",
            "category": "HONOR"
        })
        self.assertEqual(res_stu.status_code, 403)

        # 2. Verified alumni submits achievement -> 201 PENDING
        res_ach = client.post("/achievements/", headers=self.alumni_headers, json={
            "title": "Forbes 30 Under 30 Enterprise Tech",
            "description": "Recognized for contributions to open-source developer tooling.",
            "category": "HONOR",
            "issuer": "Forbes Magazine",
            "issue_date": "2026-01-15",
            "evidence_url": "https://forbes.com/lists/30-under-30"
        })
        self.assertEqual(res_ach.status_code, 201)
        ach_id = res_ach.json()["id"]
        self.assertEqual(res_ach.json()["status"], "PENDING")

        # 3. Admin views pending achievements
        res_pend = client.get("/achievements/admin/pending", headers=self.admin_headers)
        self.assertEqual(res_pend.status_code, 200)
        p_ids = [a["id"] for a in res_pend.json()]
        self.assertIn(ach_id, p_ids)

        # 4. Admin approves achievement
        res_appr = client.put(f"/achievements/admin/{ach_id}/approve", headers=self.admin_headers)
        self.assertEqual(res_appr.status_code, 200)
        self.assertEqual(res_appr.json()["status"], "APPROVED")

        # 5. Fetch alumni approved achievements
        res_alm = client.get(f"/achievements/alumni/{self.alumni_id}")
        self.assertEqual(res_alm.status_code, 200)
        alm_ids = [a["id"] for a in res_alm.json()]
        self.assertIn(ach_id, alm_ids)

    # ----------------------------------------------------
    # 5. ADMIN ANALYTICS EXTENSION TEST
    # ----------------------------------------------------
    def test_07_admin_analytics_metrics(self):
        res_stats = client.get("/admin/statistics", headers=self.admin_headers)
        self.assertEqual(res_stats.status_code, 200)
        stats = res_stats.json()

        # Verify all 9 Pass 2B metrics exist
        self.assertIn("events_registered", stats)
        self.assertIn("event_attendance_total", stats)
        self.assertIn("event_attendance_rate", stats)
        self.assertIn("success_stories_total", stats)
        self.assertIn("success_stories_pending", stats)
        self.assertIn("communities_total", stats)
        self.assertIn("community_memberships", stats)
        self.assertIn("achievements_total", stats)
        self.assertIn("achievements_pending", stats)

        # Validate numeric types
        self.assertIsInstance(stats["events_registered"], int)
        self.assertIsInstance(stats["event_attendance_total"], int)
        self.assertIsInstance(stats["event_attendance_rate"], float)
        self.assertGreaterEqual(stats["event_attendance_total"], 1)
        self.assertGreaterEqual(stats["success_stories_total"], 1)
        self.assertGreaterEqual(stats["communities_total"], 1)
        self.assertGreaterEqual(stats["achievements_total"], 1)


if __name__ == "__main__":
    unittest.main()

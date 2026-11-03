import time
import unittest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


class TestFeatureExpansionPass2A(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        ts = int(time.time() * 1000)

        # 1. Admin setup - find existing admin or create directly via DB session
        from app.database.database import SessionLocal
        from app.models.user_model import User
        from app.auth.auth_handler import hash_password, create_access_token

        db = SessionLocal()
        admin_user = db.query(User).filter(User.role == "admin").first()
        if not admin_user:
            admin_user = User(
                name="System Administrator",
                email="admin_system@alumni.edu",
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
        db.close()

        # 2. Student A setup
        cls.student_a_email = f"student_a_{ts}@example.com"
        res_sa = client.post("/auth/register", json={
            "name": "Student Alice",
            "email": cls.student_a_email,
            "password": "Password123!",
            "role": "student"
        })
        cls.student_a_token = res_sa.json().get("access_token")
        cls.student_a_headers = {"Authorization": f"Bearer {cls.student_a_token}"}
        me_sa = client.get("/auth/me", headers=cls.student_a_headers).json()
        cls.student_a_id = me_sa["id"]

        # Student A profile (Computer Science, React, Python)
        client.put("/profile/me", headers=cls.student_a_headers, json={
            "branch": "Computer Science",
            "year": "2025",
            "skills": "React, Python, TypeScript",
            "interests": "Cloud Computing, Full Stack",
            "bio": "Passionate software engineering student"
        })

        # 3. Student B setup
        cls.student_b_email = f"student_b_{ts}@example.com"
        res_sb = client.post("/auth/register", json={
            "name": "Student Bob",
            "email": cls.student_b_email,
            "password": "Password123!",
            "role": "student"
        })
        cls.student_b_token = res_sb.json().get("access_token")
        cls.student_b_headers = {"Authorization": f"Bearer {cls.student_b_token}"}
        me_sb = client.get("/auth/me", headers=cls.student_b_headers).json()
        cls.student_b_id = me_sb["id"]

        # 4. Alumni X setup (Google Alumnus)
        cls.alumni_x_email = f"alumni_x_{ts}@example.com"
        res_ax = client.post("/auth/register", json={
            "name": "Alumni Xavier",
            "email": cls.alumni_x_email,
            "password": "Password123!",
            "role": "alumni"
        })
        cls.alumni_x_token = res_ax.json().get("access_token")
        cls.alumni_x_headers = {"Authorization": f"Bearer {cls.alumni_x_token}"}
        me_ax = client.get("/auth/me", headers=cls.alumni_x_headers).json()
        cls.alumni_x_id = me_ax["id"]

        # Register/create Alumni record for Xavier
        res_alumni_rec = client.post("/alumni/", headers=cls.admin_headers, json={
            "name": "Alumni Xavier",
            "email": cls.alumni_x_email,
            "department": "Computer Science",
            "graduation_year": "2020",
            "company": "Google",
            "job_role": "Staff Software Engineer",
            "skills": "Python, Go, Distributed Systems, Cloud Computing",
            "mentorship_available": True
        })

        # 5. Alumni Y setup (Amazon Alumnus, not initially connected)
        cls.alumni_y_email = f"alumni_y_{ts}@example.com"
        res_ay = client.post("/auth/register", json={
            "name": "Alumni Yolanda",
            "email": cls.alumni_y_email,
            "password": "Password123!",
            "role": "alumni"
        })
        cls.alumni_y_token = res_ay.json().get("access_token")
        cls.alumni_y_headers = {"Authorization": f"Bearer {cls.alumni_y_token}"}
        me_ay = client.get("/auth/me", headers=cls.alumni_y_headers).json()
        cls.alumni_y_id = me_ay["id"]

        client.post("/alumni/", headers=cls.admin_headers, json={
            "name": "Alumni Yolanda",
            "email": cls.alumni_y_email,
            "department": "Electrical Engineering",
            "graduation_year": "2018",
            "company": "Amazon",
            "job_role": "Solutions Architect",
            "skills": "AWS, Architecture",
            "mentorship_available": True
        })

        # 6. Create Opportunity posted by Admin (Google Software Engineer)
        res_opp = client.post("/opportunities/", headers=cls.admin_headers, json={
            "title": "Software Engineer II - Cloud",
            "company": "Google",
            "description": "Join Google Cloud engineering team.",
            "opportunity_type": "Full-Time Job",
            "location": "Mountain View, CA",
            "deadline": "2026-12-31"
        })
        cls.opportunity_id = res_opp.json()["id"]

    # ─────────────────────────────────────────────────────────────
    # CONNECTIONS TESTS
    # ─────────────────────────────────────────────────────────────

    def test_01_self_connection_rejected(self):
        res = client.post(
            f"/connections/request/{self.student_a_id}",
            headers=self.student_a_headers
        )
        self.assertEqual(res.status_code, 400)
        self.assertIn("cannot connect with yourself", res.json()["detail"].lower())

    def test_02_create_connection_request(self):
        res = client.post(
            f"/connections/request/{self.alumni_x_id}",
            headers=self.student_a_headers
        )
        self.assertEqual(res.status_code, 201)
        data = res.json()
        self.assertEqual(data["sender_id"], self.student_a_id)
        self.assertEqual(data["receiver_id"], self.alumni_x_id)
        self.assertEqual(data["status"], "PENDING")
        self.__class__.conn_a_x_id = data["id"]

    def test_03_duplicate_connection_rejected(self):
        # Same direction duplicate
        res1 = client.post(
            f"/connections/request/{self.alumni_x_id}",
            headers=self.student_a_headers
        )
        self.assertEqual(res1.status_code, 409)

        # Reverse direction duplicate
        res2 = client.post(
            f"/connections/request/{self.student_a_id}",
            headers=self.alumni_x_headers
        )
        self.assertEqual(res2.status_code, 409)

    def test_04_connection_status_endpoint(self):
        res = client.get(f"/connections/status/{self.alumni_x_id}", headers=self.student_a_headers)
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["status"], "PENDING_SENT")

        res_recv = client.get(f"/connections/status/{self.student_a_id}", headers=self.alumni_x_headers)
        self.assertEqual(res_recv.status_code, 200)
        self.assertEqual(res_recv.json()["status"], "PENDING_RECEIVED")

    def test_05_unauthorized_accept_rejected(self):
        # Student B tries to accept request sent to Alumni X
        res = client.put(
            f"/connections/{self.conn_a_x_id}/accept",
            headers=self.student_b_headers
        )
        self.assertEqual(res.status_code, 403)

    def test_06_receiver_accepts_connection(self):
        res = client.put(
            f"/connections/{self.conn_a_x_id}/accept",
            headers=self.alumni_x_headers
        )
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.json()["status"], "ACCEPTED")

        # Now status should be CONNECTED
        st_res = client.get(f"/connections/status/{self.alumni_x_id}", headers=self.student_a_headers)
        self.assertEqual(st_res.json()["status"], "CONNECTED")

    def test_07_network_listing(self):
        # Student A should see Alumni X in my-network
        res_a = client.get("/connections/my-network", headers=self.student_a_headers)
        self.assertEqual(res_a.status_code, 200)
        conns_a = res_a.json()
        self.assertTrue(any(c["id"] == self.conn_a_x_id for c in conns_a))

        # Alumni X should see Student A in my-network
        res_x = client.get("/connections/my-network", headers=self.alumni_x_headers)
        self.assertEqual(res_x.status_code, 200)
        conns_x = res_x.json()
        self.assertTrue(any(c["id"] == self.conn_a_x_id for c in conns_x))

    def test_08_connection_suggestions(self):
        res = client.get("/connections/suggestions", headers=self.student_a_headers)
        self.assertEqual(res.status_code, 200)
        suggestions = res.json()
        self.assertIsInstance(suggestions, list)

        # Should exclude self (student_a_id) and already connected (alumni_x_id)
        suggested_ids = [s["user_id"] for s in suggestions]
        self.assertNotIn(self.student_a_id, suggested_ids)
        self.assertNotIn(self.alumni_x_id, suggested_ids)

        # Each suggestion must have valid score and reasons
        for s in suggestions:
            self.assertGreaterEqual(s["suggestion_score"], 0)
            self.assertLessEqual(s["suggestion_score"], 100)
            self.assertIsInstance(s["suggestion_reasons"], list)

    def test_09_remove_and_reconnect(self):
        # Student B connects with Alumni Y
        res_req = client.post(f"/connections/request/{self.alumni_y_id}", headers=self.student_b_headers)
        self.assertEqual(res_req.status_code, 201)
        temp_conn_id = res_req.json()["id"]

        # Student B cancels/removes the request
        res_del = client.delete(f"/connections/{temp_conn_id}", headers=self.student_b_headers)
        self.assertEqual(res_del.status_code, 200)

        # Status is now NOT_CONNECTED
        status_res = client.get(f"/connections/status/{self.alumni_y_id}", headers=self.student_b_headers)
        self.assertEqual(status_res.json()["status"], "NOT_CONNECTED")

    # ─────────────────────────────────────────────────────────────
    # REFERRAL TESTS
    # ─────────────────────────────────────────────────────────────

    def test_10_referral_invalid_opportunity(self):
        res = client.post("/referrals/", headers=self.student_a_headers, json={
            "opportunity_id": 999999,
            "alumni_id": self.alumni_x_id,
            "message": "Please refer me"
        })
        self.assertEqual(res.status_code, 404)

    def test_11_referral_non_connected_alumni_rejected(self):
        # Student A is NOT connected with Alumni Y
        res = client.post("/referrals/", headers=self.student_a_headers, json={
            "opportunity_id": self.opportunity_id,
            "alumni_id": self.alumni_y_id,
            "message": "Please refer me"
        })
        self.assertEqual(res.status_code, 400)
        self.assertIn("connected alumni", res.json()["detail"].lower())

    def test_12_eligible_alumni_for_opportunity(self):
        # Student A queries eligible alumni for Google Opportunity
        res = client.get(f"/referrals/eligible-alumni/{self.opportunity_id}", headers=self.student_a_headers)
        self.assertEqual(res.status_code, 200)
        eligible = res.json()
        self.assertTrue(len(eligible) >= 1)

        # Alumni Xavier works at Google, so is_company_match should be True
        x_match = next((a for a in eligible if a["user_id"] == self.alumni_x_id), None)
        self.assertIsNotNone(x_match)
        self.assertTrue(x_match["is_company_match"])

    def test_13_create_valid_referral_request(self):
        res = client.post("/referrals/", headers=self.student_a_headers, json={
            "opportunity_id": self.opportunity_id,
            "alumni_id": self.alumni_x_id,
            "message": "Hi Xavier, I would love a referral for the Cloud SWE role at Google!"
        })
        self.assertEqual(res.status_code, 201)
        data = res.json()
        self.assertEqual(data["requester_id"], self.student_a_id)
        self.assertEqual(data["alumni_id"], self.alumni_x_id)
        self.assertEqual(data["opportunity_id"], self.opportunity_id)
        self.assertEqual(data["status"], "PENDING")
        self.__class__.referral_id = data["id"]

    def test_14_duplicate_referral_rejected(self):
        res = client.post("/referrals/", headers=self.student_a_headers, json={
            "opportunity_id": self.opportunity_id,
            "alumni_id": self.alumni_x_id,
            "message": "Duplicate attempt"
        })
        self.assertEqual(res.status_code, 409)

    def test_15_unauthorized_referral_update_rejected(self):
        # Student B tries to accept referral meant for Alumni X
        res = client.put(
            f"/referrals/{self.referral_id}/accept",
            headers=self.student_b_headers
        )
        self.assertEqual(res.status_code, 403)

    def test_16_referral_lifecycle_accept_and_complete(self):
        # 1. Alumni X views received referrals
        res_rec = client.get("/referrals/received", headers=self.alumni_x_headers)
        self.assertEqual(res_rec.status_code, 200)
        self.assertTrue(any(r["id"] == self.referral_id for r in res_rec.json()))

        # 2. Alumni X accepts referral
        res_acc = client.put(f"/referrals/{self.referral_id}/accept", headers=self.alumni_x_headers)
        self.assertEqual(res_acc.status_code, 200)
        self.assertEqual(res_acc.json()["status"], "ACCEPTED")

        # 3. Alumni X completes referral
        res_comp = client.put(f"/referrals/{self.referral_id}/complete", headers=self.alumni_x_headers)
        self.assertEqual(res_comp.status_code, 200)
        self.assertEqual(res_comp.json()["status"], "COMPLETED")

        # 4. Requester views sent referrals
        res_sent = client.get("/referrals/sent", headers=self.student_a_headers)
        self.assertEqual(res_sent.status_code, 200)
        sent_ref = next(r for r in res_sent.json() if r["id"] == self.referral_id)
        self.assertEqual(sent_ref["status"], "COMPLETED")

    def test_17_notifications_created(self):
        # Check that notifications exist for Student A and Alumni X
        res_notif_x = client.get("/notifications/", headers=self.alumni_x_headers)
        self.assertEqual(res_notif_x.status_code, 200)
        notifs_x = res_notif_x.json()
        # Should have CONNECTION and REFERRAL notifications
        types_x = [n.get("notification_type") for n in notifs_x]
        self.assertTrue("CONNECTION" in types_x or "REFERRAL" in types_x)

    def test_18_admin_statistics_aggregate_metrics(self):
        res = client.get("/admin/statistics", headers=self.admin_headers)
        self.assertEqual(res.status_code, 200)
        data = res.json()
        self.assertIn("total_connections", data)
        self.assertIn("accepted_connections", data)
        self.assertIn("pending_connection_requests", data)
        self.assertIn("total_referral_requests", data)
        self.assertIn("accepted_referral_requests", data)
        self.assertGreaterEqual(data["total_connections"], 1)
        self.assertGreaterEqual(data["total_referral_requests"], 1)


if __name__ == "__main__":
    unittest.main()

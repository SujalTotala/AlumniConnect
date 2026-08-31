import sys
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def run_tests():
    print("=" * 60)
    print("STARTING PHASE 4 COMPREHENSIVE BACKEND TEST SUITE")
    print("=" * 60)

    # 1. Health Endpoint
    res = client.get("/")
    assert res.status_code == 200, f"Health check failed: {res.text}"
    print("[PASS] Health endpoint active")

    # 2. Authentication: Register Admin, Alumni, Student
    users = {
        "admin": {"name": "Admin Officer", "email": "admin_p4@alumni.edu", "password": "AdminPass123!", "role": "admin"},
        "alumni": {"name": "Alumni Mentor Alex", "email": "mentor_alex@tech.com", "password": "MentorPass123!", "role": "alumni"},
        "student": {"name": "Student Maya", "email": "maya@student.edu", "password": "StudentPass123!", "role": "student"}
    }

    tokens = {}
    for role, u in users.items():
        # Try register
        r = client.post("/auth/register", json=u)
        if r.status_code == 201:
            tokens[role] = r.json()["access_token"]
        elif r.status_code == 400:
            # Login if already exists
            login_r = client.post("/auth/login", json={"email": u["email"], "password": u["password"]})
            assert login_r.status_code == 200, f"Login failed for {role}: {login_r.text}"
            tokens[role] = login_r.json()["access_token"]
        print(f"[PASS] Authenticated {role.upper()} session successfully")

    admin_h = {"Authorization": f"Bearer {tokens['admin']}"}
    alumni_h = {"Authorization": f"Bearer {tokens['alumni']}"}
    student_h = {"Authorization": f"Bearer {tokens['student']}"}

    # 3. Profile Module Tests
    # Update Alumni profile
    r = client.put("/profile/me", json={
        "name": "Alex Mercer",
        "company": "NextGen AI Corp",
        "job_role": "Lead Architect",
        "department": "Computer Science",
        "graduation_year": "2021",
        "location": "San Francisco, CA",
        "skills": "Python, Distributed Systems, ML",
        "mentorship_available": True
    }, headers=alumni_h)
    assert r.status_code == 200, f"Alumni profile update failed: {r.text}"
    assert r.json()["profile"]["company"] == "NextGen AI Corp"
    assert r.json()["profile"]["mentorship_available"] is True
    print("[PASS] Alumni profile updated with mentorship availability")

    # Update Student profile
    r = client.put("/profile/me", json={
        "name": "Maya Lin",
        "branch": "Information Technology",
        "year": "3rd Year",
        "skills": "Python, React, SQL",
        "interests": "Cloud Computing & AI"
    }, headers=student_h)
    assert r.status_code == 200, f"Student profile update failed: {r.text}"
    assert r.json()["profile"]["branch"] == "Information Technology"
    print("[PASS] Student profile updated successfully")

    # 4. Alumni Directory & Mentors Listing
    r = client.get("/alumni/")
    assert r.status_code == 200
    assert len(r.json()) >= 1
    print(f"[PASS] Alumni directory listing returned {len(r.json())} alumni")

    r = client.get("/mentorship/mentors")
    assert r.status_code == 200
    mentors = r.json()
    assert len(mentors) >= 1
    # Find mentor user id
    me_mentor = client.get("/auth/me", headers=alumni_h).json()
    mentor_user_id = me_mentor["id"]
    print(f"[PASS] Mentors listing returned {len(mentors)} available mentors (Target ID: {mentor_user_id})")

    # 5. Events Module Tests
    event_payload = {
        "title": "Global Alumni Summit 2026",
        "description": "Connecting alumni and students worldwide for tech discussions and career growth.",
        "event_type": "Alumni Meet",
        "event_date": "2026-10-15",
        "start_time": "10:00 AM",
        "location": "Grand Hall & Live Stream",
        "meeting_url": "https://meet.alumniconnect.edu/summit2026"
    }
    r = client.post("/events/", json=event_payload, headers=admin_h)
    assert r.status_code == 201, f"Event creation failed: {r.text}"
    event_id = r.json()["id"]
    print(f"[PASS] Admin created event: '{event_payload['title']}' (ID: {event_id})")

    # Student registers for event
    r = client.post(f"/events/{event_id}/register", headers=student_h)
    assert r.status_code == 201, f"Event registration failed: {r.text}"
    print("[PASS] Student registered for event successfully")

    # Test duplicate registration rejection
    r = client.post(f"/events/{event_id}/register", headers=student_h)
    assert r.status_code == 400, "Duplicate registration was not blocked"
    print("[PASS] Duplicate event registration blocked with 400 Bad Request")

    # Admin checks event registrations
    r = client.get(f"/events/{event_id}/registrations", headers=admin_h)
    assert r.status_code == 200
    assert len(r.json()) >= 1
    assert any(reg["user_email"] == users["student"]["email"] for reg in r.json())
    print(f"[PASS] Event attendee list verified by Admin ({len(r.json())} attendees)")

    # 6. Mentorship Request Flow
    req_payload = {
        "mentor_id": mentor_user_id,
        "message": "Hi Alex, I would appreciate your guidance on entering distributed systems engineering."
    }
    r = client.post("/mentorship/requests", json=req_payload, headers=student_h)
    assert r.status_code == 201, f"Mentorship request creation failed: {r.text}"
    req_id = r.json()["id"]
    print(f"[PASS] Student sent mentorship request (ID: {req_id})")

    # Duplicate request blocked
    r = client.post("/mentorship/requests", json=req_payload, headers=student_h)
    assert r.status_code == 400, "Duplicate mentorship request was not rejected"
    print("[PASS] Duplicate mentorship request rejected with 400 Bad Request")

    # Student checks sent requests
    r = client.get("/mentorship/requests/sent", headers=student_h)
    assert r.status_code == 200
    assert any(req["id"] == req_id for req in r.json())
    print("[PASS] Student verified request in Sent inbox")

    # Mentor checks received requests
    r = client.get("/mentorship/requests/received", headers=alumni_h)
    assert r.status_code == 200
    assert any(req["id"] == req_id for req in r.json())
    print("[PASS] Mentor received request in Incoming inbox")

    # Mentor accepts request
    r = client.put(f"/mentorship/requests/{req_id}/accept", json={
        "response_note": "Glad to connect Maya! Let's schedule a session next Tuesday."
    }, headers=alumni_h)
    assert r.status_code == 200
    assert r.json()["status"] == "ACCEPTED"
    print("[PASS] Mentor accepted mentorship request with response note")

    # 7. Opportunities Module
    opp_payload = {
        "title": "Cloud Infrastructure Intern",
        "company": "NextGen AI Corp",
        "description": "3-month summer internship working with Kubernetes and Python backend services.",
        "opportunity_type": "Internship",
        "location": "Remote / SF",
        "deadline": "2026-09-30",
        "application_url": "https://nextgen.ai/careers/intern"
    }
    r = client.post("/opportunities/", json=opp_payload, headers=alumni_h)
    assert r.status_code == 201, f"Opportunity creation failed: {r.text}"
    opp_id = r.json()["id"]
    print(f"[PASS] Alumni posted opportunity: '{opp_payload['title']}' (ID: {opp_id})")

    # Student lists opportunities
    r = client.get("/opportunities/?opportunity_type=Internship", headers=student_h)
    assert r.status_code == 200
    assert any(opp["id"] == opp_id for opp in r.json())
    print(f"[PASS] Student filtered and viewed opportunity (Found {len(r.json())} internships)")

    # 8. Notifications Module
    # Student should have received notifications from event registration and mentorship acceptance
    r = client.get("/notifications/", headers=student_h)
    assert r.status_code == 200
    notifs = r.json()
    assert len(notifs) >= 1
    notif_id = notifs[0]["id"]
    print(f"[PASS] Student notifications inbox verified ({len(notifs)} notifications found)")

    # Mark single notification as read
    r = client.put(f"/notifications/{notif_id}/read", headers=student_h)
    assert r.status_code == 200
    assert r.json()["is_read"] is True
    print("[PASS] Single notification marked as read")

    # Mark all read
    r = client.put("/notifications/read-all", headers=student_h)
    assert r.status_code == 200
    count_r = client.get("/notifications/unread-count", headers=student_h)
    assert count_r.json()["unread_count"] == 0
    print("[PASS] All notifications marked read (Unread count: 0)")

    # 9. Admin Statistics & RBAC Verification
    # Student cannot access admin statistics
    r = client.get("/admin/statistics", headers=student_h)
    assert r.status_code == 403, "RBAC failed: Student was able to access Admin statistics"
    print("[PASS] RBAC: Student access to /admin/statistics correctly blocked with 403 Forbidden")

    # Admin accesses real platform statistics
    r = client.get("/admin/statistics", headers=admin_h)
    assert r.status_code == 200
    stats = r.json()
    assert stats["total_users"] >= 3
    assert stats["total_events"] >= 1
    assert stats["total_event_registrations"] >= 1
    assert stats["total_opportunities"] >= 1
    print("[PASS] Real Admin platform metrics verified:")
    for k, v in stats.items():
        print(f"   - {k}: {v}")

    # Admin user list
    r = client.get("/admin/users", headers=admin_h)
    assert r.status_code == 200
    assert len(r.json()) >= 3
    print(f"[PASS] Admin user management verified ({len(r.json())} registered users)")

    print("=" * 60)
    print("ALL PHASE 4 BACKEND MODULE TESTS COMPLETED WITH 100% SUCCESS!")
    print("=" * 60)

if __name__ == "__main__":
    run_tests()

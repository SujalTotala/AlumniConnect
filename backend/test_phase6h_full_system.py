import sys
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def run_tests():
    print("=" * 60)
    print("STARTING PHASE 6H COMPREHENSIVE SYSTEM INTEGRATION TEST SUITE")
    print("=" * 60)

    # 1. Health Endpoint
    res = client.get("/")
    assert res.status_code == 200, f"Health check failed: {res.text}"
    print("[PASS] Health endpoint active")

    import time
    ts = int(time.time())
    test_users = {
        "admin": {"name": "Admin Officer", "email": "admin_p4@alumni.edu", "password": "AdminPass123!", "role": "admin"},
        "alumni": {"name": "Phase6H Alumni", "email": f"alumni_6h_{ts}@alumni.edu", "password": "AlumniPass123!", "role": "alumni"},
        "student": {"name": "Phase6H Student", "email": f"student_6h_{ts}@alumni.edu", "password": "StudentPass123!", "role": "student"},
        "student_b": {"name": "Phase6H Student B", "email": f"student_b_6h_{ts}@alumni.edu", "password": "StudentBPass123!", "role": "student"}
    }

    tokens = {}
    for role, u in test_users.items():
        # Try register
        r = client.post("/auth/register", json=u)
        if r.status_code == 201:
            tokens[role] = r.json()["access_token"]
        elif r.status_code == 400:
            # Login if already exists (e.g. repeated test runs)
            login_r = client.post("/auth/login", json={"email": u["email"], "password": u["password"]})
            assert login_r.status_code == 200, f"Login failed for {role}: {login_r.text}"
            tokens[role] = login_r.json()["access_token"]
        print(f"[PASS] Registered/Authenticated {role.upper()} session successfully")

    # Verify admin registration restriction
    # Since at least one admin now exists (admin_6h_ts), any new admin registrations must be blocked
    malicious_admin = {"name": "Hacker Admin", "email": f"hacker_admin_{ts}@alumni.edu", "password": "HackerPass123!", "role": "admin"}
    r = client.post("/auth/register", json=malicious_admin)
    assert r.status_code == 400, "Admin registration restriction failed: arbitrary user created admin account!"
    assert "restricted" in r.json()["detail"].lower()
    print("[PASS] Registration Security: Blocked arbitrary admin self-privilege escalation")

    admin_h = {"Authorization": f"Bearer {tokens['admin']}"}
    alumni_h = {"Authorization": f"Bearer {tokens['alumni']}"}
    student_h = {"Authorization": f"Bearer {tokens['student']}"}
    student_b_h = {"Authorization": f"Bearer {tokens['student_b']}"}

    # Verify auth/me
    r = client.get("/auth/me", headers=student_h)
    assert r.status_code == 200
    assert r.json()["email"] == test_users["student"]["email"]
    print("[PASS] GET /auth/me returns correct session context")

    # 3. Profile read & update
    # Update Student
    r = client.put("/profile/me", json={
        "name": "Maya 6H Update",
        "branch": "Mechanical Engineering",
        "year": "Third Year",
        "skills": "CAD, Thermodynamics, Python",
        "bio": "Mechanical engineering student with an interest in robotics."
    }, headers=student_h)
    assert r.status_code == 200, f"Student profile update failed: {r.text}"
    assert r.json()["profile"]["branch"] == "Mechanical Engineering"
    print("[PASS] Student profile updated successfully")

    # Update Alumni
    r = client.put("/profile/me", json={
        "name": "Alex 6H Update",
        "company": "NextGen Technologies",
        "job_role": "Senior Cloud Engineer",
        "department": "Computer Science",
        "graduation_year": "2020",
        "location": "Boston, MA",
        "skills": "AWS, Kubernetes, Go",
        "mentorship_available": True,
        "bio": "Senior Cloud Engineer willing to mentor students on architecture."
    }, headers=alumni_h)
    assert r.status_code == 200, f"Alumni profile update failed: {r.text}"
    assert r.json()["profile"]["company"] == "NextGen Technologies"
    assert r.json()["profile"]["mentorship_available"] is True
    print("[PASS] Alumni profile updated with mentorship availability successfully")

    # 4. Alumni list, search, filter, details
    # List all alumni
    r = client.get("/alumni/", headers=student_h)
    assert r.status_code == 200
    alumni_list = r.json()
    assert len(alumni_list) > 0
    # Search by company
    r = client.get("/alumni/?search=NextGen", headers=student_h)
    assert r.status_code == 200
    assert any(a["company"] == "NextGen Technologies" for a in r.json())
    # Filter by mentorship
    r = client.get("/alumni/?mentorship_available=true", headers=student_h)
    assert r.status_code == 200
    assert all(a["mentorship_available"] is True for a in r.json())
    # View details
    alumni_id = [a["id"] for a in alumni_list if a["email"] == test_users["alumni"]["email"]][0]
    r = client.get(f"/alumni/{alumni_id}", headers=student_h)
    assert r.status_code == 200
    assert r.json()["name"] == "Alex 6H Update"
    print("[PASS] Alumni Directory listing, search, filtering, and detail fetch verified")

    # 5. Events Module Tests
    # Create event (Alumni)
    event_data = {
        "title": "Cloud Networking Masterclass",
        "description": "Learn cloud infrastructure routing, VPC, and load balancers.",
        "event_date": "2026-10-15 14:00:00",
        "event_type": "Online",
        "location": "Zoom Meet Link",
        "organizer_id": 1 # ignored / auto-mapped to creator in routes
    }
    r = client.post("/events/", json=event_data, headers=alumni_h)
    assert r.status_code == 201, f"Event creation failed: {r.text}"
    event_id = r.json()["id"]
    print(f"[PASS] Event created successfully (ID: {event_id})")

    # List events
    r = client.get("/events/", headers=student_h)
    assert r.status_code == 200
    assert any(e["id"] == event_id for e in r.json())
    print("[PASS] Event listed in directory")

    # Register student
    r = client.post(f"/events/{event_id}/register", headers=student_h)
    assert r.status_code == 201, f"Registration failed: {r.text}"
    print("[PASS] Student registered for event successfully")

    # Register duplicate RSVP
    r = client.post(f"/events/{event_id}/register", headers=student_h)
    assert r.status_code == 400, "Duplicate registration was not blocked"
    print("[PASS] Duplicate event RSVP blocked correctly with 400 Bad Request")

    # Organizer views attendee list
    r = client.get(f"/events/{event_id}/registrations", headers=alumni_h)
    assert r.status_code == 200
    assert len(r.json()) == 1
    assert r.json()[0]["user_name"] == "Maya 6H Update"
    print("[PASS] Organizer accessed attendee roster successfully")

    # Cancel registration
    r = client.delete(f"/events/{event_id}/register", headers=student_h)
    assert r.status_code == 200
    # Confirm removed
    r = client.get(f"/events/{event_id}/registrations", headers=alumni_h)
    assert r.status_code == 200
    assert len(r.json()) == 0
    print("[PASS] Student canceled registration successfully")

    # 6. Mentorship Hub Module Tests
    # List mentors
    r = client.get("/mentorship/mentors", headers=student_h)
    assert r.status_code == 200
    mentors = r.json()
    assert len(mentors) > 0
    mentor_user_id = [m["user_id"] for m in mentors if m["email"] == test_users["alumni"]["email"]][0]

    # Student sends mentorship request
    req_data = {
        "mentor_id": mentor_user_id,
        "message": "Hi Alex, I want to learn Kubernetes. Please mentor me."
    }
    r = client.post("/mentorship/requests", json=req_data, headers=student_h)
    assert r.status_code == 201, f"Request failed: {r.text}"
    request_id = r.json()["id"]
    print(f"[PASS] Student sent mentorship request successfully (ID: {request_id})")

    # Verify duplicate mentorship request prevention (pending requests)
    r = client.post("/mentorship/requests", json=req_data, headers=student_h)
    assert r.status_code == 400
    print("[PASS] Duplicate mentorship request prevented successfully")

    # Student views sent requests
    r = client.get("/mentorship/requests/sent", headers=student_h)
    assert r.status_code == 200
    assert any(req["id"] == request_id for req in r.json())
    print("[PASS] Student verified request in Sent Requests folder")

    # Mentor views received requests
    r = client.get("/mentorship/requests/received", headers=alumni_h)
    assert r.status_code == 200
    assert any(req["id"] == request_id for req in r.json())
    print("[PASS] Mentor verified request in Received Requests folder")

    # Mentor accepts request
    r = client.put(f"/mentorship/requests/{request_id}/accept", json={"response_note": "Sure, let's schedule a Zoom call."}, headers=alumni_h)
    assert r.status_code == 200
    assert r.json()["status"] == "ACCEPTED"
    print("[PASS] Mentor accepted request with response note successfully")

    # Complete request
    r = client.put(f"/mentorship/requests/{request_id}/complete", headers=alumni_h)
    assert r.status_code == 200
    assert r.json()["status"] == "COMPLETED"
    print("[PASS] Mentorship completed successfully")

    # 7. Career Opportunities Module Tests
    # Create opportunity
    opp_data = {
        "title": "Senior Cloud Infrastructure Engineer",
        "company": "NextGen Technologies",
        "description": "Looking for Go/K8s/AWS expertise. Highly remote-friendly.",
        "opportunity_type": "Full-Time Job",
        "location": "Boston, MA / Remote",
        "deadline": "2026-12-31",
        "application_url": "https://nextgen.com/careers/senior-cloud"
    }
    r = client.post("/opportunities/", json=opp_data, headers=alumni_h)
    assert r.status_code == 201
    opp_id = r.json()["id"]
    print(f"[PASS] Alumni posted opportunity successfully (ID: {opp_id})")

    # List & filter
    r = client.get("/opportunities/", headers=student_h)
    assert r.status_code == 200
    assert any(o["id"] == opp_id for o in r.json())
    
    r = client.get("/opportunities/?opportunity_type=Full-Time Job", headers=student_h)
    assert r.status_code == 200
    assert all(o["opportunity_type"] == "Full-Time Job" for o in r.json())
    print("[PASS] Opportunity listing, search and filters verified")

    # Delete opportunity
    r = client.delete(f"/opportunities/{opp_id}", headers=alumni_h)
    assert r.status_code == 200
    # Confirm deletion
    r = client.get("/opportunities/", headers=student_h)
    assert not any(o["id"] == opp_id for o in r.json())
    print("[PASS] Creator deleted opportunity posting successfully")

    # 8. Notifications Module Tests
    # Trigger mentorship notification by sending a new request
    req_data2 = {
        "mentor_id": mentor_user_id,
        "message": "Hi Alex, mentorship request #2."
    }
    r = client.post("/mentorship/requests", json=req_data2, headers=student_h)
    assert r.status_code == 201
    request_id_2 = r.json()["id"]

    # Verify notifications inbox for Mentor
    r = client.get("/notifications/", headers=alumni_h)
    assert r.status_code == 200
    notifs = r.json()
    assert len(notifs) > 0
    mentor_notif = [n for n in notifs if n["notification_type"] == "MENTORSHIP"][0]
    notif_id = mentor_notif["id"]
    print(f"[PASS] Notification inbox retrieved. Found MENTORSHIP request trigger (ID: {notif_id})")

    # Get unread count
    r = client.get("/notifications/unread-count", headers=alumni_h)
    assert r.status_code == 200
    assert r.json()["unread_count"] > 0
    print(f"[PASS] Unread notifications count retrieved: {r.json()['unread_count']}")

    # Ownership isolation check: Student B must NOT see Alumni Alex's notification
    r = client.get(f"/notifications/", headers=student_b_h)
    assert not any(n["id"] == notif_id for n in r.json())
    print("[PASS] Security: Notification records isolated between users")

    # Ownership modification check: Student B must NOT be allowed to mark Alumni Alex's notification as read
    r = client.put(f"/notifications/{notif_id}/read", headers=student_b_h)
    assert r.status_code == 404 # route filters by current user, so not found is expected
    print("[PASS] Security: Prevented unauthorized cross-user notification read mutations")

    # Mark single notification as read
    r = client.put(f"/notifications/{notif_id}/read", headers=alumni_h)
    assert r.status_code == 200
    assert r.json()["is_read"] is True
    print("[PASS] Notification marked read successfully")

    # Mark all notifications read
    r = client.put("/notifications/read-all", headers=alumni_h)
    assert r.status_code == 200
    
    r = client.get("/notifications/unread-count", headers=alumni_h)
    assert r.status_code == 200
    assert r.json()["unread_count"] == 0
    print("[PASS] All notifications marked read successfully")

    # 9. Admin statistics, users, and RBAC
    # Student blocked from admin statistics (403)
    r = client.get("/admin/statistics", headers=student_h)
    assert r.status_code == 403
    print("[PASS] RBAC: Student access to statistics correctly blocked (403)")

    # Student blocked from user list (403)
    r = client.get("/admin/users", headers=student_h)
    assert r.status_code == 403
    print("[PASS] RBAC: Student access to user management correctly blocked (403)")

    # Admin accesses stats
    r = client.get("/admin/statistics", headers=admin_h)
    assert r.status_code == 200
    assert r.json()["total_users"] >= 4
    print(f"[PASS] Admin accessed live platform statistics: {r.json()}")

    # Admin accesses users list
    r = client.get("/admin/users", headers=admin_h)
    assert r.status_code == 200
    assert len(r.json()) >= 4
    print(f"[PASS] Admin accessed user list successfully ({len(r.json())} users)")

    print("=" * 60)
    print("ALL PHASE 6H SYSTEM INTEGRATION TESTS COMPLETED WITH 100% SUCCESS!")
    print("=" * 60)

if __name__ == "__main__":
    run_tests()

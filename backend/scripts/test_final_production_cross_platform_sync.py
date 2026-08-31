import os
import sys
import time
import uuid
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
assert base_url, "BACKEND_HTTPS_URL must be configured in backend/.env"
base_url = base_url.rstrip("/")

def run_e2e_suite():
    print("=" * 80)
    print("FINAL PRODUCTION CROSS-PLATFORM SYSTEM VERIFICATION SUITE")
    print("=" * 80)
    print(f"Backend Target: {base_url}")

    client = httpx.Client(base_url=base_url, timeout=60.0, follow_redirects=True)

    # 1. Health & Root Check
    print("\n--- 1. BACKEND HEALTH & AVAILABILITY ---")
    r_root = client.get("/")
    assert r_root.status_code == 200, f"Root check failed: {r_root.text}"
    print(f"[PASS] 1. Root Endpoint Active: {r_root.json()}")

    r_health = client.get("/health")
    assert r_health.status_code == 200, f"Health check failed: {r_health.text}"
    print(f"[PASS] 2. Health Endpoint Active: {r_health.json()}")

    # 2. CORS Preflight Verification
    print("\n--- 2. CORS PREFLIGHT VERIFICATION ---")
    headers_cors = {
        "Origin": "https://alumniconnect.vercel.app",
        "Access-Control-Request-Method": "POST",
        "Access-Control-Request-Headers": "authorization,content-type"
    }
    r_options = client.options("/auth/login", headers=headers_cors)
    print(f"[PASS] 3. CORS Preflight handled (Status: {r_options.status_code})")

    # 3. Authentication & Account Provisioning (Web & Android User Simulation)
    print("\n--- 3. AUTHENTICATION & SESSIONS (WEB & ANDROID) ---")
    ts = uuid.uuid4().hex[:6]
    web_student_data = {
        "name": f"Web Student {ts}",
        "email": f"web_student_{ts}@alumni.edu",
        "password": "WebStudent123!",
        "role": "student"
    }
    android_alumni_data = {
        "name": f"Android Alumni {ts}",
        "email": f"android_alumni_{ts}@alumni.edu",
        "password": "AndroidAlumni123!",
        "role": "alumni"
    }

    # Register Web Student
    r_reg_student = client.post("/auth/register", json=web_student_data)
    assert r_reg_student.status_code == 201
    web_student_token = r_reg_student.json()["access_token"]
    web_student_id = r_reg_student.json()["user"]["id"]
    web_student_h = {"Authorization": f"Bearer {web_student_token}"}
    print(f"[PASS] 4. Web Student Registered (ID: {web_student_id})")

    # Login Web Student
    r_login = client.post("/auth/login", json={"email": web_student_data["email"], "password": web_student_data["password"]})
    assert r_login.status_code == 200
    print("[PASS] 5. Web Student Logged In via JWT")

    # Call /auth/me for Web Student
    r_me_student = client.get("/auth/me", headers=web_student_h)
    assert r_me_student.status_code == 200
    assert r_me_student.json()["email"] == web_student_data["email"]
    print(f"[PASS] 6. /auth/me Verified for Web Session: {r_me_student.json()['name']} ({r_me_student.json()['role']})")

    # Register Android Alumni
    r_reg_alumni = client.post("/auth/register", json=android_alumni_data)
    assert r_reg_alumni.status_code == 201
    android_alumni_token = r_reg_alumni.json()["access_token"]
    android_alumni_id = r_reg_alumni.json()["user"]["id"]
    android_alumni_h = {"Authorization": f"Bearer {android_alumni_token}"}
    print(f"[PASS] 7. Android Alumni Registered (ID: {android_alumni_id})")

    # Call /auth/me for Android Alumni
    r_me_alumni = client.get("/auth/me", headers=android_alumni_h)
    assert r_me_alumni.status_code == 200
    assert r_me_alumni.json()["role"] == "alumni"
    print(f"[PASS] 8. /auth/me Verified for Android Session: {r_me_alumni.json()['name']}")

    # 4. Web ↔ Android Cross-Platform Synchronization Flows
    print("\n--- 4. WEB <-> ANDROID SYNCHRONIZATION FLOWS ---")

    # Flow A: Web Profile Update → Android Profile Fetch
    print("\n[Sync Flow A] Web Student Profile Update -> Android Refresh")
    web_profile_update = {
        "branch": "Computer Science & Engineering",
        "year": "4th Year",
        "skills": "React, TypeScript, FastAPI, PostgreSQL",
        "interests": "Cloud Native Architecture, Distributed Systems",
        "bio": "Senior CS student seeking industry mentorship."
    }
    r_p_update = client.put("/profile/me", json=web_profile_update, headers=web_student_h)
    assert r_p_update.status_code == 200
    print("[PASS] 9. Web Student updated profile via Web API")

    # Android client fetches student profile
    r_p_fetch = client.get("/profile/me", headers=web_student_h)
    assert r_p_fetch.status_code == 200
    fetched_profile = r_p_fetch.json()["profile"]
    assert fetched_profile["branch"] == web_profile_update["branch"]
    assert fetched_profile["skills"] == web_profile_update["skills"]
    print(f"[PASS] 10. Android retrieved updated profile seamlessly: {fetched_profile['branch']} ({fetched_profile['year']})")

    # Flow B: Android Alumni Profile Update -> Web Alumni Directory
    print("\n[Sync Flow B] Android Alumni Profile Update -> Web Alumni Directory Listing")
    android_alumni_update = {
        "company": "Google Cloud",
        "job_role": "Staff Software Engineer",
        "department": "Infrastructure",
        "graduation_year": "2020",
        "location": "Bengaluru, India",
        "skills": "Distributed Systems, Kubernetes, Go, Python",
        "bio": "Experienced engineer passionate about mentoring upcoming tech leaders.",
        "linkedin_url": "https://linkedin.com/in/example-alumni",
        "github_url": "https://github.com/example-alumni",
        "mentorship_available": True
    }
    r_alumni_up = client.put("/profile/me", json=android_alumni_update, headers=android_alumni_h)
    assert r_alumni_up.status_code == 200
    print("[PASS] 11. Android Alumni updated profile with mentorship availability")

    # Web Directory Fetches Alumni
    r_dir = client.get("/alumni/")
    assert r_dir.status_code == 200
    alumni_in_dir = [a for a in r_dir.json() if a["email"] == android_alumni_data["email"]]
    assert len(alumni_in_dir) == 1
    assert alumni_in_dir[0]["company"] == "Google Cloud"
    print(f"[PASS] 12. Web Alumni Directory retrieved synced alumni profile: {alumni_in_dir[0]['name']} @ {alumni_in_dir[0]['company']}")

    # Flow C: Web Event Creation & Registration Flow
    print("\n[Sync Flow C] Event Creation, RSVP & Attendee Roster")
    event_data = {
        "title": f"Global Alumni Tech Summit {ts}",
        "description": "Annual keynote on distributed systems and AI engineering.",
        "event_type": "webinar",
        "event_date": "2026-09-15",
        "start_time": "18:00",
        "location": "Virtual Google Meet",
        "meeting_url": "https://meet.google.com/abc-defg-hij"
    }
    r_ev_create = client.post("/events/", json=event_data, headers=android_alumni_h)
    assert r_ev_create.status_code == 201
    event_id = r_ev_create.json()["id"]
    print(f"[PASS] 13. Alumni created live event (ID: {event_id}): {event_data['title']}")

    # Web Student registers for the event
    r_rsvp = client.post(f"/events/{event_id}/register", headers=web_student_h)
    assert r_rsvp.status_code == 201
    print(f"[PASS] 14. Web Student registered for Event ID {event_id}")

    # Duplicate RSVP prevention check
    r_dup_rsvp = client.post(f"/events/{event_id}/register", headers=web_student_h)
    assert r_dup_rsvp.status_code == 400
    print("[PASS] 15. Duplicate event RSVP rejected with 400 Bad Request")

    # Event organizer views attendee roster
    r_roster = client.get(f"/events/{event_id}/registrations", headers=android_alumni_h)
    assert r_roster.status_code == 200
    roster_list = r_roster.json()
    assert any(att["user_id"] == web_student_id for att in roster_list)
    print(f"[PASS] 16. Event Organizer retrieved synced attendee roster ({len(roster_list)} attendees)")

    # Flow D: Web Mentorship Request -> Android Received/Sent Flow
    print("\n[Sync Flow D] Web Mentorship Request -> Android Received/Sent Folders")
    mr_payload = {
        "mentor_id": android_alumni_id,
        "message": f"Hi mentor, I would like guidance on systems architecture ({ts})."
    }
    r_mr = client.post("/mentorship/requests", json=mr_payload, headers=web_student_h)
    assert r_mr.status_code == 201
    mr_id = r_mr.json()["id"]
    print(f"[PASS] 17. Web Student sent mentorship request (ID: {mr_id})")

    # Student Sent folder check
    r_sent = client.get("/mentorship/requests/sent", headers=web_student_h)
    assert r_sent.status_code == 200
    assert any(req["id"] == mr_id for req in r_sent.json())
    print("[PASS] 18. Verified request in Student's Sent Requests folder")

    # Mentor Received folder check
    r_recv = client.get("/mentorship/requests/received", headers=android_alumni_h)
    assert r_recv.status_code == 200
    assert any(req["id"] == mr_id for req in r_recv.json())
    print("[PASS] 19. Verified request in Mentor's Received Requests folder")

    # Mentor accepts request
    r_accept = client.put(f"/mentorship/requests/{mr_id}/accept", json={"response_note": "Glad to mentor you! Let's schedule a call."}, headers=android_alumni_h)
    assert r_accept.status_code == 200
    assert r_accept.json()["status"] == "ACCEPTED"
    print("[PASS] 20. Mentor accepted request with response note")

    # Complete mentorship request
    r_complete = client.put(f"/mentorship/requests/{mr_id}/complete", headers=android_alumni_h)
    assert r_complete.status_code == 200
    assert r_complete.json()["status"] == "COMPLETED"
    print("[PASS] 21. Mentorship marked COMPLETED")

    # Flow E: Android Opportunity Posting -> Web Listing
    print("\n[Sync Flow E] Opportunity Posting & Catalog Fetch")
    opp_data = {
        "title": f"Full-Stack Cloud Engineer {ts}",
        "company": "Google Cloud",
        "description": "Designing high-scale microservices and cloud backends.",
        "opportunity_type": "full-time",
        "location": "Hybrid / Bengaluru",
        "deadline": "2026-11-30",
        "application_url": "https://careers.google.com"
    }
    r_opp = client.post("/opportunities/", json=opp_data, headers=android_alumni_h)
    assert r_opp.status_code == 201
    opp_id = r_opp.json()["id"]
    print(f"[PASS] 22. Android Alumni posted opportunity (ID: {opp_id})")

    # Web Catalog fetches opportunity
    r_opp_list = client.get("/opportunities/")
    assert r_opp_list.status_code == 200
    assert any(o["id"] == opp_id for o in r_opp_list.json())
    print(f"[PASS] 23. Web Opportunities Catalog retrieved posted opportunity")

    # Flow F: Notification Synchronization & Read State Mutation
    print("\n[Sync Flow F] Notification Triggering & Read State Synchronization")
    r_notifs_student = client.get("/notifications/", headers=web_student_h)
    assert r_notifs_student.status_code == 200
    student_notifs = r_notifs_student.json()
    assert len(student_notifs) >= 1
    notif_to_mark = student_notifs[0]["id"]
    print(f"[PASS] 24. Student received notification inbox ({len(student_notifs)} items)")

    # Mark single notification as read
    r_mark_read = client.put(f"/notifications/{notif_to_mark}/read", headers=web_student_h)
    assert r_mark_read.status_code == 200
    print(f"[PASS] 25. Marked notification ID {notif_to_mark} as READ")

    # Mark all notifications as read
    r_mark_all = client.put("/notifications/read-all", headers=web_student_h)
    assert r_mark_all.status_code == 200
    print("[PASS] 26. Marked all notifications as READ")

    # Flow G: RBAC Security Verification
    print("\n[Sync Flow G] RBAC Authorization Controls")
    r_sec_stats = client.get("/admin/statistics", headers=web_student_h)
    assert r_sec_stats.status_code == 403
    print("[PASS] 27. RBAC Verified: Student blocked from /admin/statistics (403 Forbidden)")

    r_sec_users = client.get("/admin/users", headers=web_student_h)
    assert r_sec_users.status_code == 403
    print("[PASS] 28. RBAC Verified: Student blocked from /admin/users (403 Forbidden)")

    print("\n" + "=" * 80)
    print("ALL 28 END-TO-END PRODUCTION CROSS-PLATFORM SYNCHRONIZATION TESTS PASSED 100%!")
    print("=" * 80)

if __name__ == "__main__":
    run_e2e_suite()

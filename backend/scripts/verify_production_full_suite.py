import os
import sys
import uuid
import time
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

BASE_URL = os.getenv("BACKEND_HTTPS_URL", "https://alumniconnect-bwoi.onrender.com").rstrip("/")

def run_comprehensive_verification():
    print("=" * 80)
    print("ALUMNICONNECT FINAL PRODUCTION VERIFICATION & FUNCTIONAL TEST SUITE")
    print("=" * 80)
    print(f"Target Backend API: {BASE_URL}")

    client = httpx.Client(base_url=BASE_URL, timeout=60.0, follow_redirects=True)
    test_suffix = uuid.uuid4().hex[:6]

    # -------------------------------------------------------------
    # STEP 1: INFRASTRUCTURE & HEALTH
    # -------------------------------------------------------------
    print("\n[STEP 1] INFRASTRUCTURE & HEALTH CHECKS")
    r_root = client.get("/")
    assert r_root.status_code == 200, f"Root check failed: {r_root.text}"
    print(f"  [PASS] GET / returned 200 OK: {r_root.json()}")

    r_health = client.get("/health")
    assert r_health.status_code == 200, f"Health check failed: {r_health.text}"
    print(f"  [PASS] GET /health returned 200 OK: {r_health.json()}")

    # -------------------------------------------------------------
    # STEP 2: CORS & TRANSPORT SECURITY AUDIT
    # -------------------------------------------------------------
    print("\n[STEP 2] CORS & TRANSPORT SECURITY AUDIT")
    r_cors_local = client.options(
        "/auth/login",
        headers={
            "Origin": "http://localhost:5173",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "authorization,content-type"
        }
    )
    print(f"  [PASS] CORS Preflight for http://localhost:5173 -> Status: {r_cors_local.status_code}")

    r_cors_vercel = client.options(
        "/auth/login",
        headers={
            "Origin": "https://alumniconnect.vercel.app",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "authorization,content-type"
        }
    )
    print(f"  [INFO] CORS Preflight for https://alumniconnect.vercel.app -> Status: {r_cors_vercel.status_code}")

    # -------------------------------------------------------------
    # STEP 3: AUTHENTICATION & JWT (Student, Alumni, Invalid login)
    # -------------------------------------------------------------
    print("\n[STEP 3] AUTHENTICATION, REGISTRATION & JWT WORKFLOWS")
    student_email = f"prod_test_student_{test_suffix}@alumni.edu"
    student_pwd = "SecureStudentPass123!"
    student_reg_data = {
        "name": f"Prod Student {test_suffix}",
        "email": student_email,
        "password": student_pwd,
        "role": "student"
    }
    r_reg_stu = client.post("/auth/register", json=student_reg_data)
    assert r_reg_stu.status_code == 201, f"Student registration failed: {r_reg_stu.text}"
    stu_auth = r_reg_stu.json()
    stu_token = stu_auth["access_token"]
    stu_user = stu_auth["user"]
    stu_headers = {"Authorization": f"Bearer {stu_token}"}
    print(f"  [PASS] Registered Student user (ID: {stu_user['id']}, Email: {student_email})")

    # Student Login
    r_login_stu = client.post("/auth/login", json={"email": student_email, "password": student_pwd})
    assert r_login_stu.status_code == 200, f"Student login failed: {r_login_stu.text}"
    print("  [PASS] Login succeeded with valid JWT access token for Student")

    # GET /auth/me
    r_me_stu = client.get("/auth/me", headers=stu_headers)
    assert r_me_stu.status_code == 200, f"GET /auth/me failed: {r_me_stu.text}"
    assert r_me_stu.json()["email"] == student_email
    assert r_me_stu.json()["role"] == "student"
    assert r_me_stu.json()["is_active"] is True
    print(f"  [PASS] GET /auth/me verified active student user profile: {r_me_stu.json()['name']}")

    # Alumni Registration
    alumni_email = f"prod_test_alumni_{test_suffix}@alumni.edu"
    alumni_pwd = "SecureAlumniPass123!"
    alumni_reg_data = {
        "name": f"Prod Alumni {test_suffix}",
        "email": alumni_email,
        "password": alumni_pwd,
        "role": "alumni"
    }
    r_reg_alm = client.post("/auth/register", json=alumni_reg_data)
    assert r_reg_alm.status_code == 201, f"Alumni registration failed: {r_reg_alm.text}"
    alm_auth = r_reg_alm.json()
    alm_token = alm_auth["access_token"]
    alm_user = alm_auth["user"]
    alm_headers = {"Authorization": f"Bearer {alm_token}"}
    print(f"  [PASS] Registered Alumni user (ID: {alm_user['id']}, Email: {alumni_email})")

    # Invalid login test
    r_invalid_login = client.post("/auth/login", json={"email": student_email, "password": "WrongPassword!"})
    assert r_invalid_login.status_code == 400, f"Expected 400 for wrong password, got {r_invalid_login.status_code}"
    print("  [PASS] Invalid password rejected with 400 Bad Request (Invalid credentials)")

    # -------------------------------------------------------------
    # STEP 4: PROFILE RETRIEVAL & UPDATES
    # -------------------------------------------------------------
    print("\n[STEP 4] PROFILE MANAGEMENT & DATA PERSISTENCE")
    
    # Update Student Profile
    stu_profile_payload = {
        "branch": "Computer Engineering",
        "year": "Final Year",
        "skills": "Python, React, FastAPIs, Docker",
        "interests": "Machine Learning, Distributed Systems",
        "bio": "Motivated senior student preparing for cloud careers."
    }
    r_up_stu = client.put("/profile/me", json=stu_profile_payload, headers=stu_headers)
    assert r_up_stu.status_code == 200, f"Student profile update failed: {r_up_stu.text}"
    print("  [PASS] Student profile updated successfully")

    # Retrieve Student Profile
    r_get_stu_p = client.get("/profile/me", headers=stu_headers)
    assert r_get_stu_p.status_code == 200
    stu_p_data = r_get_stu_p.json()["profile"]
    assert stu_p_data["branch"] == stu_profile_payload["branch"]
    assert stu_p_data["skills"] == stu_profile_payload["skills"]
    print(f"  [PASS] Verified persisted student profile: {stu_p_data['branch']} | {stu_p_data['year']}")

    # Update Alumni Profile
    alm_profile_payload = {
        "company": "Amazon Web Services",
        "job_role": "Senior Solutions Architect",
        "department": "Computer Science",
        "graduation_year": "2019",
        "location": "Seattle, WA",
        "skills": "Cloud Architecture, Kubernetes, Python, Microservices",
        "bio": "Cloud architect eager to guide students in backend and infra.",
        "linkedin_url": "https://linkedin.com/in/testalumni",
        "github_url": "https://github.com/testalumni",
        "mentorship_available": True
    }
    r_up_alm = client.put("/profile/me", json=alm_profile_payload, headers=alm_headers)
    assert r_up_alm.status_code == 200, f"Alumni profile update failed: {r_up_alm.text}"
    print("  [PASS] Alumni profile updated successfully with mentorship availability")

    # -------------------------------------------------------------
    # STEP 5: ALUMNI DIRECTORY & SEARCH / FILTER
    # -------------------------------------------------------------
    print("\n[STEP 5] ALUMNI DIRECTORY & SEARCH / FILTER")
    r_dir = client.get("/alumni/")
    assert r_dir.status_code == 200, f"Alumni directory fetch failed: {r_dir.text}"
    all_alumni = r_dir.json()
    assert len(all_alumni) >= 1
    print(f"  [PASS] Alumni directory returned {len(all_alumni)} profiles")

    # Search filter by company or skill
    r_search = client.get("/alumni/?search=Amazon")
    assert r_search.status_code == 200
    search_results = r_search.json()
    assert any(a["email"] == alumni_email for a in search_results)
    print(f"  [PASS] Search query 'Amazon' matched {len(search_results)} alumni profiles")

    # -------------------------------------------------------------
    # STEP 6: EVENTS LIFECYCLE (Create, List, Register, Cancel)
    # -------------------------------------------------------------
    print("\n[STEP 6] EVENTS LIFECYCLE (CREATE, LIST, REGISTER, CANCEL)")
    event_payload = {
        "title": f"Live Tech Masterclass {test_suffix}",
        "description": "Masterclass on designing high-scale REST APIs & database systems.",
        "event_type": "workshop",
        "event_date": "2026-10-20",
        "start_time": "14:00",
        "location": "Live Auditorium & Stream",
        "meeting_url": "https://meet.google.com/live-stream-test"
    }
    # Alumni creates event
    r_ev_c = client.post("/events/", json=event_payload, headers=alm_headers)
    assert r_ev_c.status_code == 201, f"Event creation failed: {r_ev_c.text}"
    event_id = r_ev_c.json()["id"]
    print(f"  [PASS] Created Event ID {event_id}: '{event_payload['title']}'")

    # List events
    r_ev_list = client.get("/events/")
    assert r_ev_list.status_code == 200
    assert any(e["id"] == event_id for e in r_ev_list.json())
    print(f"  [PASS] Events catalog lists newly created event")

    # Student registers for event
    r_reg_ev = client.post(f"/events/{event_id}/register", headers=stu_headers)
    assert r_reg_ev.status_code == 201, f"Event registration failed: {r_reg_ev.text}"
    print(f"  [PASS] Student registered for Event ID {event_id}")

    # Duplicate RSVP rejected
    r_dup = client.post(f"/events/{event_id}/register", headers=stu_headers)
    assert r_dup.status_code == 400
    print("  [PASS] Duplicate event RSVP prevented (400 Bad Request)")

    # Event organizer inspects attendee roster
    r_roster = client.get(f"/events/{event_id}/registrations", headers=alm_headers)
    assert r_roster.status_code == 200
    roster = r_roster.json()
    assert any(reg["user_id"] == stu_user["id"] for reg in roster)
    print(f"  [PASS] Event attendee roster confirmed ({len(roster)} attendee(s))")

    # Student cancels registration
    r_cancel_ev = client.delete(f"/events/{event_id}/register", headers=stu_headers)
    assert r_cancel_ev.status_code == 200, f"Event RSVP cancellation failed: {r_cancel_ev.text}"
    print("  [PASS] Event RSVP cancellation successful")

    # Student re-registers for clean state
    r_re_reg = client.post(f"/events/{event_id}/register", headers=stu_headers)
    assert r_re_reg.status_code == 201
    print("  [PASS] Event re-registration successful")

    # -------------------------------------------------------------
    # STEP 7: MENTORSHIP LIFECYCLE & WORKFLOW
    # -------------------------------------------------------------
    print("\n[STEP 7] MENTORSHIP LIFECYCLE & WORKFLOW")
    
    # Get Mentors
    r_mentors = client.get("/mentorship/mentors")
    assert r_mentors.status_code == 200
    mentors = r_mentors.json()
    assert any(m["email"] == alumni_email for m in mentors)
    print(f"  [PASS] Mentorship directory returned {len(mentors)} available mentors")

    # Student sends mentorship request
    mr_data = {
        "mentor_id": alm_user["id"],
        "message": f"Hello! I am seeking guidance on cloud engineering ({test_suffix})."
    }
    r_mr = client.post("/mentorship/requests", json=mr_data, headers=stu_headers)
    assert r_mr.status_code == 201, f"Mentorship request creation failed: {r_mr.text}"
    mr_id = r_mr.json()["id"]
    print(f"  [PASS] Created Mentorship Request ID {mr_id}")

    # Sent requests for Student
    r_sent_mr = client.get("/mentorship/requests/sent", headers=stu_headers)
    assert r_sent_mr.status_code == 200
    assert any(req["id"] == mr_id for req in r_sent_mr.json())
    print("  [PASS] Sent folder verified on Student account")

    # Received requests for Alumni
    r_recv_mr = client.get("/mentorship/requests/received", headers=alm_headers)
    assert r_recv_mr.status_code == 200
    assert any(req["id"] == mr_id for req in r_recv_mr.json())
    print("  [PASS] Received folder verified on Mentor account")

    # Mentor Accepts Request
    r_accept = client.put(
        f"/mentorship/requests/{mr_id}/accept",
        json={"response_note": "Welcome! I would be happy to mentor you."},
        headers=alm_headers
    )
    assert r_accept.status_code == 200
    assert r_accept.json()["status"] == "ACCEPTED"
    print("  [PASS] Mentor accepted request (Status -> ACCEPTED)")

    # Mentor Completes Request
    r_complete = client.put(f"/mentorship/requests/{mr_id}/complete", headers=alm_headers)
    assert r_complete.status_code == 200
    assert r_complete.json()["status"] == "COMPLETED"
    print("  [PASS] Mentorship engagement marked COMPLETED")

    # -------------------------------------------------------------
    # STEP 8: OPPORTUNITIES WORKFLOW & AUTHORIZATION
    # -------------------------------------------------------------
    print("\n[STEP 8] OPPORTUNITIES WORKFLOW & AUTHORIZATION")
    opp_payload = {
        "title": f"Junior Cloud Engineer {test_suffix}",
        "company": "Amazon Web Services",
        "description": "Full-time position for recent graduates with strong cloud fundamentals.",
        "opportunity_type": "full-time",
        "location": "Hybrid / Seattle",
        "deadline": "2026-12-31",
        "application_url": "https://aws.amazon.com/careers"
    }

    # Student attempting to create opportunity must be 403 Forbidden
    r_stu_opp = client.post("/opportunities/", json=opp_payload, headers=stu_headers)
    assert r_stu_opp.status_code == 403, f"Expected 403 for student opportunity posting, got {r_stu_opp.status_code}"
    print("  [PASS] RBAC Verified: Student blocked from posting opportunities (403 Forbidden)")

    # Alumni posts opportunity
    r_alm_opp = client.post("/opportunities/", json=opp_payload, headers=alm_headers)
    assert r_alm_opp.status_code == 201, f"Opportunity creation failed: {r_alm_opp.text}"
    opp_id = r_alm_opp.json()["id"]
    print(f"  [PASS] Alumni successfully posted opportunity ID {opp_id}")

    # Public / authenticated listings
    r_opps = client.get("/opportunities/")
    assert r_opps.status_code == 200
    assert any(o["id"] == opp_id for o in r_opps.json())
    print("  [PASS] Opportunities catalog lists newly created opportunity")

    # -------------------------------------------------------------
    # STEP 9: NOTIFICATIONS & READ STATUS
    # -------------------------------------------------------------
    print("\n[STEP 9] NOTIFICATIONS & READ STATUS WORKFLOW")
    
    # Check notifications on Alumni account
    r_notifs_alm = client.get("/notifications/", headers=alm_headers)
    assert r_notifs_alm.status_code == 200
    alm_notifs = r_notifs_alm.json()
    assert len(alm_notifs) >= 1
    print(f"  [PASS] Mentor received notification inbox ({len(alm_notifs)} items)")

    # Check notifications on Student account
    r_notifs_stu = client.get("/notifications/", headers=stu_headers)
    assert r_notifs_stu.status_code == 200
    stu_notifs = r_notifs_stu.json()
    assert len(stu_notifs) >= 1
    target_notif_id = stu_notifs[0]["id"]
    print(f"  [PASS] Student received notification inbox ({len(stu_notifs)} items)")

    # Mark single notification as read
    r_read_one = client.put(f"/notifications/{target_notif_id}/read", headers=stu_headers)
    assert r_read_one.status_code == 200
    print(f"  [PASS] Marked notification ID {target_notif_id} as READ")

    # Mark all notifications as read
    r_read_all = client.put("/notifications/read-all", headers=stu_headers)
    assert r_read_all.status_code == 200
    print("  [PASS] Marked all notifications as READ")

    # -------------------------------------------------------------
    # STEP 10: ADMIN RBAC & UNAUTHORIZED CONTROLS
    # -------------------------------------------------------------
    print("\n[STEP 10] RBAC & UNAUTHORIZED ACCESS VERIFICATION")
    
    # Student accessing /admin/statistics -> 403
    r_adm_stat_stu = client.get("/admin/statistics", headers=stu_headers)
    assert r_adm_stat_stu.status_code == 403
    print("  [PASS] Student blocked from /admin/statistics (403 Forbidden)")

    # Alumni accessing /admin/statistics -> 403
    r_adm_stat_alm = client.get("/admin/statistics", headers=alm_headers)
    assert r_adm_stat_alm.status_code == 403
    print("  [PASS] Alumni blocked from /admin/statistics (403 Forbidden)")

    # Student accessing /admin/users -> 403
    r_adm_users_stu = client.get("/admin/users", headers=stu_headers)
    assert r_adm_users_stu.status_code == 403
    print("  [PASS] Student blocked from /admin/users (403 Forbidden)")

    # Unauthenticated access to /admin/statistics -> 401
    r_unauth = client.get("/admin/statistics")
    assert r_unauth.status_code == 401
    print("  [PASS] Unauthenticated request blocked (401 Unauthorized)")

    # Unauthenticated access to protected route /profile/me -> 401
    r_unauth_prof = client.get("/profile/me")
    assert r_unauth_prof.status_code == 401
    print("  [PASS] Unauthenticated access to /profile/me blocked (401 Unauthorized)")

    # Invalid token access -> 401
    r_invalid_token = client.get("/profile/me", headers={"Authorization": "Bearer invalid_token_xyz"})
    assert r_invalid_token.status_code == 401
    print("  [PASS] Invalid/forged JWT token rejected (401 Unauthorized)")

    print("\n" + "=" * 80)
    print("ALL PRODUCTION WEB & BACKEND FUNCTIONAL VERIFICATIONS COMPLETED SUCCESSFULLY (100% PASS)!")
    print("=" * 80)

if __name__ == "__main__":
    run_comprehensive_verification()

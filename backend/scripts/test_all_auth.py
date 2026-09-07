import time
import httpx

for env_name, base_url in [
    ("LOCAL BACKEND (http://127.0.0.1:8000)", "http://127.0.0.1:8000"),
    ("CLOUD BACKEND (https://alumniconnect-bwoi.onrender.com)", "https://alumniconnect-bwoi.onrender.com")
]:
    print("=" * 80)
    print(f"TESTING {env_name}")
    print("=" * 80)
    client = httpx.Client(base_url=base_url, timeout=35.0)

    # 1. Fresh Registration
    ts = int(time.time() * 1000)
    fresh_email = f"fresh_{ts}@alumni.edu"
    r_reg = client.post("/auth/register", json={
        "name": "Fresh Student",
        "email": fresh_email,
        "password": "FreshPassword123!",
        "role": "student"
    })
    print(f"1. Fresh Registration: Status {r_reg.status_code}")

    # 2. Fresh Login
    r_log = client.post("/auth/login", json={"email": fresh_email, "password": "FreshPassword123!"})
    token = r_log.json().get("access_token") if r_log.status_code == 200 else None
    print(f"2. Fresh Login: Status {r_log.status_code}, Token Exists: {bool(token)}")

    # 3. GET /auth/me
    if token:
        r_me = client.get("/auth/me", headers={"Authorization": f"Bearer {token}"})
        print(f"3. GET /auth/me: Status {r_me.status_code}, Role: {r_me.json().get('role')}")

    # 4. Existing Accounts
    print("\nExisting Configured Accounts:")
    for em, pw in [
        ("admin@alumni.edu", "AdminPassword123!"),
        ("sujaltotala123@gmail.com", "Password123!"),
        ("student@alumni.edu", "StudentPassword123!"),
        ("alumni@alumni.edu", "AlumniPassword123!"),
    ]:
        r = client.post("/auth/login", json={"email": em, "password": pw})
        print(f"  Login {em} -> Status {r.status_code}")

    # 5. Invalid Password
    r_bad = client.post("/auth/login", json={"email": "admin@alumni.edu", "password": "WrongPasswordXYZ!"})
    print(f"\n5. Wrong password rejected: Status {r_bad.status_code} (Expected 400)")

    # 6. Duplicate Email
    r_dup = client.post("/auth/register", json={"name": "Dup", "email": fresh_email, "password": "FreshPassword123!", "role": "student"})
    print(f"6. Duplicate email rejected: Status {r_dup.status_code} (Expected 400)\n")

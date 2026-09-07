import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import httpx
from app.database.database import SessionLocal
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.auth.auth_handler import hash_password, verify_password

users_to_ensure = [
    {
        "name": "System Administrator",
        "email": "admin@alumni.edu",
        "password": "AdminPassword123!",
        "role": "admin"
    },
    {
        "name": "Sujal Totala",
        "email": "sujaltotala123@gmail.com",
        "password": "Password123!",
        "role": "student"
    },
    {
        "name": "Demo Student",
        "email": "student@alumni.edu",
        "password": "StudentPassword123!",
        "role": "student"
    },
    {
        "name": "Demo Alumni",
        "email": "alumni@alumni.edu",
        "password": "AlumniPassword123!",
        "role": "alumni"
    }
]

def main():
    print("=" * 80)
    print("1. LOCAL SQLITE DATABASE AUDIT & SYNCHRONIZATION")
    print("=" * 80)
    db = SessionLocal()
    for u in users_to_ensure:
        user = db.query(User).filter(User.email == u["email"]).first()
        if not user:
            user = User(
                name=u["name"],
                email=u["email"],
                password=hash_password(u["password"]),
                role=u["role"],
                is_active=True
            )
            db.add(user)
            db.commit()
            db.refresh(user)
            print(f"  [LOCAL ADDED] {u['role'].upper()}: {u['email']}")
        else:
            if not verify_password(u["password"], user.password):
                user.password = hash_password(u["password"])
                user.is_active = True
                db.commit()
                print(f"  [LOCAL PWD UPDATED] {u['role'].upper()}: {u['email']}")
            else:
                print(f"  [LOCAL VERIFIED] {u['role'].upper()}: {u['email']}")

        if u["role"] == "alumni":
            alm = db.query(Alumni).filter(Alumni.email == u["email"]).first()
            if not alm:
                db.add(Alumni(
                    user_id=user.id,
                    name=u["name"],
                    email=u["email"],
                    company="Google Cloud",
                    job_role="Staff Software Engineer",
                    mentorship_available=True
                ))
                db.commit()
    db.close()

    print("\n" + "=" * 80)
    print("2. TESTING LOCAL HTTP LOGIN (http://127.0.0.1:8000/auth/login)")
    print("=" * 80)
    client_local = httpx.Client(base_url="http://127.0.0.1:8000", timeout=10.0)
    for u in users_to_ensure:
        try:
            r = client_local.post("/auth/login", json={"email": u["email"], "password": u["password"]})
            status_text = "PASS (200 OK)" if r.status_code == 200 else f"FAIL ({r.status_code})"
            print(f"  {u['email']}: {status_text}")
        except Exception as e:
            print(f"  {u['email']}: Connection Error ({e})")

    print("\n" + "=" * 80)
    print("3. CLOUD DATABASE AUDIT & SYNCHRONIZATION (https://alumniconnect-bwoi.onrender.com)")
    print("=" * 80)
    client_cloud = httpx.Client(base_url="https://alumniconnect-bwoi.onrender.com", timeout=30.0)
    for u in users_to_ensure:
        r_login = client_cloud.post("/auth/login", json={"email": u["email"], "password": u["password"]})
        if r_login.status_code == 200:
            print(f"  [CLOUD VERIFIED] {u['role'].upper()}: {u['email']} -> Login 200 OK")
        else:
            print(f"  [CLOUD ATTEMPTING RE-REGISTRATION] {u['role'].upper()}: {u['email']} (Status {r_login.status_code})")
            r_reg = client_cloud.post("/auth/register", json=u)
            print(f"    -> Register response: {r_reg.status_code} {r_reg.text[:60]}")
            r_login2 = client_cloud.post("/auth/login", json={"email": u["email"], "password": u["password"]})
            print(f"    -> Re-test login: {r_login2.status_code}")

    print("\n" + "=" * 80)
    print("SYNCHRONIZATION & VERIFICATION COMPLETED")
    print("=" * 80)

if __name__ == "__main__":
    main()

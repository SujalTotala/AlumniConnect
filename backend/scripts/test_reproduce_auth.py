import time
import httpx

ts = int(time.time())
email = f"auth_test_{ts}@example.com"
pwd = "TestPassword123!"
payload = {
    "name": "Auth Test Student",
    "email": email,
    "password": pwd,
    "role": "student"
}

client = httpx.Client(base_url="https://alumniconnect-bwoi.onrender.com", timeout=35.0)

print("--- 1. POST /auth/register ---")
r_reg = client.post("/auth/register", json=payload)
print(f"Status: {r_reg.status_code}")
print(f"Body: {r_reg.text}")

print("\n--- 2. POST /auth/login ---")
r_login = client.post("/auth/login", json={"email": email, "password": pwd})
print(f"Status: {r_login.status_code}")
if r_login.status_code == 200:
    data = r_login.json()
    sanitized = {k: (v if k != "access_token" else "<TOKEN_EXISTS>") for k, v in data.items()}
    print(f"Body structure: {sanitized}")
    token = data.get("access_token")
    print(f"access_token exists: {bool(token)}")
else:
    print(f"Body: {r_login.text}")
    token = None

print("\n--- 3. GET /auth/me ---")
if token:
    r_me = client.get("/auth/me", headers={"Authorization": f"Bearer {token}"})
    print(f"Status: {r_me.status_code}")
    if r_me.status_code == 200:
        me_data = r_me.json()
        print(f"Email: {me_data.get('email')}, Role: {me_data.get('role')}")
    else:
        print(f"Body: {r_me.text}")
else:
    print("Skipped /auth/me: no token")

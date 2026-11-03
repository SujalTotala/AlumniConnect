import httpx

client = httpx.Client(base_url="https://alumniconnect-bwoi.onrender.com", timeout=35.0)

# 1. Login as Admin
r_adm = client.post("/auth/login", json={"email": "admin@alumni.edu", "password": "AdminPassword123!"})
print(f"Admin login status: {r_adm.status_code}")

if r_adm.status_code == 200:
    token = r_adm.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}
    
    # 2. Admin stats
    r_stats = client.get("/admin/statistics", headers=headers)
    print(f"Admin statistics status: {r_stats.status_code}")
    if r_stats.status_code == 200:
        stats = r_stats.json()
        print(f"Total users: {stats.get('total_users')}")
        print(f"Total alumni: {stats.get('total_alumni')}")
        print(f"Total students: {stats.get('total_students')}")
    
    # 3. Users list
    r_users = client.get("/admin/users", headers=headers)
    print(f"Admin users status: {r_users.status_code}")
    if r_users.status_code == 200:
        users = r_users.json()
        print(f"Total users returned: {len(users)}")
        print(f"User records: {[(u['email'], u['role']) for u in users]}")
else:
    print(f"Admin login failed: {r_adm.text}")

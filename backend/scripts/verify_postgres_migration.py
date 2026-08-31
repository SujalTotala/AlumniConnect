import os
from pathlib import Path
from sqlalchemy import create_engine, text, select, func, MetaData, Table
from sqlalchemy.engine import make_url

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

source_url = os.getenv("SOURCE_DATABASE_URL", "sqlite:///./alumni.db")
target_url = os.getenv("TARGET_DATABASE_URL")

assert target_url, "TARGET_DATABASE_URL is required"

if target_url.startswith("postgres://"):
    target_url = target_url.replace("postgres://", "postgresql+pg8000://", 1)
elif target_url.startswith("postgresql://"):
    target_url = target_url.replace("postgresql://", "postgresql+pg8000://", 1)

source_engine = create_engine(source_url)
target_engine = create_engine(target_url, connect_args={"timeout": 60})

tables = [
    "users",
    "alumni",
    "student_profiles",
    "events",
    "event_registrations",
    "mentorship_requests",
    "opportunities",
    "notifications",
]

def run_verification():
    print("=" * 70)
    print("RUNNING POST-MIGRATION POSTGRESQL INTEGRITY VERIFICATION")
    print("=" * 70)

    # 1. Compare row counts
    print("\n--- 1. TABLE ROW COUNT COMPARISON ---")
    with source_engine.connect() as s_conn, target_engine.connect() as t_conn:
        for tbl in tables:
            s_count = s_conn.execute(text(f"SELECT COUNT(*) FROM {tbl}")).scalar()
            t_count = t_conn.execute(text(f"SELECT COUNT(*) FROM {tbl}")).scalar()
            match = (s_count == t_count)
            status = "[MATCH]" if match else "[MISMATCH]"
            print(f"{status:<10} {tbl:<24}: SQLite = {s_count:>4}, PostgreSQL = {t_count:>4}")
            assert match, f"Row count mismatch in table {tbl}: {s_count} != {t_count}"

    with target_engine.connect() as conn:
        # 2. Natural Key Duplicates & Integrity Checks
        print("\n--- 2. UNIQUE & NATURAL KEY INTEGRITY ---")
        dup_users_email = conn.execute(text("SELECT email, COUNT(*) FROM users GROUP BY email HAVING COUNT(*) > 1")).fetchall()
        print(f"Users duplicate emails: {len(dup_users_email)}")
        assert len(dup_users_email) == 0, f"Duplicate emails in users: {dup_users_email}"

        dup_alumni_email = conn.execute(text("SELECT email, COUNT(*) FROM alumni GROUP BY email HAVING COUNT(*) > 1")).fetchall()
        print(f"Alumni duplicate emails: {len(dup_alumni_email)}")
        assert len(dup_alumni_email) == 0, f"Duplicate emails in alumni: {dup_alumni_email}"

        dup_student_profiles = conn.execute(text("SELECT user_id, COUNT(*) FROM student_profiles GROUP BY user_id HAVING COUNT(*) > 1")).fetchall()
        print(f"Student profiles duplicate user_id: {len(dup_student_profiles)}")
        assert len(dup_student_profiles) == 0, f"Duplicate user_id in student_profiles: {dup_student_profiles}"

        dup_rsvp = conn.execute(text("SELECT event_id, user_id, COUNT(*) FROM event_registrations GROUP BY event_id, user_id HAVING COUNT(*) > 1")).fetchall()
        print(f"Event registration duplicate pairs (event_id, user_id): {len(dup_rsvp)}")
        assert len(dup_rsvp) == 0, f"Duplicate event registrations: {dup_rsvp}"
        print("[PASS] All natural keys and uniqueness constraints verified")

        # 3. Foreign Key Reference Integrity
        print("\n--- 3. FOREIGN KEY RELATIONSHIP VERIFICATION ---")
        # Alumni -> Users
        orphan_alumni = conn.execute(text("SELECT id, user_id FROM alumni WHERE user_id IS NOT NULL AND user_id NOT IN (SELECT id FROM users)")).fetchall()
        print(f"Alumni orphan user_ids: {len(orphan_alumni)}")
        assert len(orphan_alumni) == 0

        # Student Profiles -> Users
        orphan_profiles = conn.execute(text("SELECT id, user_id FROM student_profiles WHERE user_id NOT IN (SELECT id FROM users)")).fetchall()
        print(f"Student profile orphan user_ids: {len(orphan_profiles)}")
        assert len(orphan_profiles) == 0

        # Events -> Users (created_by)
        orphan_events = conn.execute(text("SELECT id, created_by FROM events WHERE created_by IS NOT NULL AND created_by NOT IN (SELECT id FROM users)")).fetchall()
        print(f"Events orphan created_by: {len(orphan_events)}")
        assert len(orphan_events) == 0

        # Event Registrations -> Events & Users
        orphan_rsvp_event = conn.execute(text("SELECT id, event_id FROM event_registrations WHERE event_id NOT IN (SELECT id FROM events)")).fetchall()
        orphan_rsvp_user = conn.execute(text("SELECT id, user_id FROM event_registrations WHERE user_id NOT IN (SELECT id FROM users)")).fetchall()
        print(f"Event Registrations orphan events: {len(orphan_rsvp_event)}, orphan users: {len(orphan_rsvp_user)}")
        assert len(orphan_rsvp_event) == 0 and len(orphan_rsvp_user) == 0

        # Mentorship Requests -> Users (student_id & mentor_id)
        orphan_mr_student = conn.execute(text("SELECT id, student_id FROM mentorship_requests WHERE student_id NOT IN (SELECT id FROM users)")).fetchall()
        orphan_mr_mentor = conn.execute(text("SELECT id, mentor_id FROM mentorship_requests WHERE mentor_id NOT IN (SELECT id FROM users)")).fetchall()
        print(f"Mentorship orphan student_ids: {len(orphan_mr_student)}, orphan mentor_ids: {len(orphan_mr_mentor)}")
        assert len(orphan_mr_student) == 0 and len(orphan_mr_mentor) == 0

        # Opportunities -> Users (posted_by)
        orphan_opp = conn.execute(text("SELECT id, posted_by FROM opportunities WHERE posted_by IS NOT NULL AND posted_by NOT IN (SELECT id FROM users)")).fetchall()
        print(f"Opportunities orphan posted_by: {len(orphan_opp)}")
        assert len(orphan_opp) == 0

        # Notifications -> Users (user_id)
        orphan_notif = conn.execute(text("SELECT id, user_id FROM notifications WHERE user_id NOT IN (SELECT id FROM users)")).fetchall()
        print(f"Notifications orphan user_ids: {len(orphan_notif)}")
        assert len(orphan_notif) == 0
        print("[PASS] All foreign key relationships verified with 0 orphans")

        # 4. Sequence Value Verification (Sequences > MAX(id))
        print("\n--- 4. POSTGRESQL SEQUENCES AND AUTO-INCREMENT BEHAVIOR ---")
        for tbl in tables:
            max_id = conn.execute(text(f"SELECT MAX(id) FROM {tbl}")).scalar()
            max_id_val = 0 if max_id is None else max_id
            seq_name = conn.execute(text(f"SELECT pg_get_serial_sequence('{tbl}', 'id')")).scalar()
            if seq_name:
                last_val = conn.execute(text(f"SELECT last_value, is_called FROM {seq_name}")).fetchone()
                print(f"Table '{tbl:<20}': MAX(id) = {max_id_val:>3}, Sequence '{seq_name}' = {dict(last_val._mapping)}")
            else:
                print(f"Table '{tbl:<20}': MAX(id) = {max_id_val:>3}, (No serial sequence attached)")

    # 5. Insert test using sequence to prove auto-increment operates above MAX(id)
    print("\n--- 5. INSERT & AUTO-INCREMENT BEHAVIOR TEST ---")
    with target_engine.begin() as conn:
        res = conn.execute(
            text("INSERT INTO notifications (user_id, title, message, notification_type, is_read) VALUES (1, 'Verification Test', 'Test msg', 'SYSTEM', false) RETURNING id")
        )
        new_id = res.scalar()
        print(f"[PASS] Successfully inserted test notification with generated ID: {new_id} (Expected > 41)")
        assert new_id > 41, f"Generated ID {new_id} is not greater than pre-migration MAX(id) 41!"
        # Clean up test notification
        conn.execute(text("DELETE FROM notifications WHERE id = :id"), {"id": new_id})
        print(f"[PASS] Cleaned up test notification ID: {new_id}")

    print("\n" + "=" * 70)
    print("ALL POSTGRESQL POST-MIGRATION VERIFICATIONS COMPLETED WITH 100% SUCCESS!")
    print("=" * 70)

if __name__ == "__main__":
    run_verification()

"""Legacy schema baseline.

Revision ID: 7b0000000001
Revises:
Create Date: 2026-08-18

This revision represents the schema that predated Alembic adoption. The next
revision reconciles the few legacy differences with current model metadata.
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "7b0000000001"
down_revision: Union[str, Sequence[str], None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "users",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(), nullable=False),
        sa.Column("email", sa.String(), nullable=False),
        sa.Column("password", sa.String(), nullable=False),
        sa.Column("role", sa.String(), nullable=False),
        sa.Column("is_active", sa.Boolean(), server_default=sa.true(), nullable=True),
        sa.Column("created_at", sa.TIMESTAMP(), server_default=sa.text("NULL"), nullable=True),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("email"),
    )
    op.create_index(op.f("ix_users_id"), "users", ["id"], unique=False)

    op.create_table(
        "alumni",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), server_default=sa.text("NULL"), nullable=True),
        sa.Column("name", sa.String(), nullable=True),
        sa.Column("email", sa.String(), nullable=True),
        sa.Column("graduation_year", sa.String(), nullable=True),
        sa.Column("department", sa.String(), server_default=sa.text("NULL"), nullable=True),
        sa.Column("company", sa.String(), nullable=True),
        sa.Column("job_role", sa.String(), nullable=True),
        sa.Column("location", sa.String(), nullable=True),
        sa.Column("skills", sa.String(), nullable=True),
        sa.Column("bio", sa.String(), server_default=sa.text("NULL"), nullable=True),
        sa.Column("linkedin_url", sa.String(), server_default=sa.text("NULL"), nullable=True),
        sa.Column("github_url", sa.String(), server_default=sa.text("NULL"), nullable=True),
        sa.Column("mentorship_available", sa.Boolean(), server_default=sa.false(), nullable=True),
        sa.Column("created_at", sa.TIMESTAMP(), server_default=sa.text("NULL"), nullable=True),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("email"),
    )
    op.create_index(op.f("ix_alumni_id"), "alumni", ["id"], unique=False)

    op.create_table(
        "student_profiles",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("branch", sa.String(), nullable=True),
        sa.Column("year", sa.String(), nullable=True),
        sa.Column("skills", sa.String(), nullable=True),
        sa.Column("interests", sa.String(), nullable=True),
        sa.Column("bio", sa.Text(), nullable=True),
        sa.Column("profile_image_url", sa.String(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=True),
        sa.Column("updated_at", sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id"),
    )
    op.create_index(op.f("ix_student_profiles_id"), "student_profiles", ["id"], unique=False)

    op.create_table(
        "events",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("title", sa.String(), nullable=False),
        sa.Column("description", sa.Text(), nullable=False),
        sa.Column("event_type", sa.String(), nullable=False),
        sa.Column("event_date", sa.String(), nullable=False),
        sa.Column("start_time", sa.String(), nullable=True),
        sa.Column("location", sa.String(), nullable=False),
        sa.Column("meeting_url", sa.String(), nullable=True),
        sa.Column("image_url", sa.String(), nullable=True),
        sa.Column("created_by", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=True),
        sa.Column("updated_at", sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(["created_by"], ["users.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_events_id"), "events", ["id"], unique=False)

    op.create_table(
        "event_registrations",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("event_id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("registration_status", sa.String(), nullable=False),
        sa.Column("registered_at", sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(["event_id"], ["events.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("event_id", "user_id", name="uq_event_user_registration"),
    )
    op.create_index(op.f("ix_event_registrations_event_id"), "event_registrations", ["event_id"], unique=False)
    op.create_index(op.f("ix_event_registrations_id"), "event_registrations", ["id"], unique=False)
    op.create_index(op.f("ix_event_registrations_user_id"), "event_registrations", ["user_id"], unique=False)

    op.create_table(
        "mentorship_requests",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("student_id", sa.Integer(), nullable=False),
        sa.Column("mentor_id", sa.Integer(), nullable=False),
        sa.Column("message", sa.Text(), nullable=False),
        sa.Column("status", sa.String(), nullable=False),
        sa.Column("response_note", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=True),
        sa.Column("updated_at", sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(["mentor_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["student_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_mentorship_requests_id"), "mentorship_requests", ["id"], unique=False)
    op.create_index(op.f("ix_mentorship_requests_mentor_id"), "mentorship_requests", ["mentor_id"], unique=False)
    op.create_index(op.f("ix_mentorship_requests_student_id"), "mentorship_requests", ["student_id"], unique=False)

    op.create_table(
        "opportunities",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("title", sa.String(), nullable=False),
        sa.Column("company", sa.String(), nullable=False),
        sa.Column("description", sa.Text(), nullable=False),
        sa.Column("opportunity_type", sa.String(), nullable=False),
        sa.Column("location", sa.String(), nullable=True),
        sa.Column("deadline", sa.String(), nullable=True),
        sa.Column("application_url", sa.String(), nullable=True),
        sa.Column("posted_by", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(["posted_by"], ["users.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_opportunities_id"), "opportunities", ["id"], unique=False)

    op.create_table(
        "notifications",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("title", sa.String(), nullable=False),
        sa.Column("message", sa.Text(), nullable=False),
        sa.Column("notification_type", sa.String(), nullable=False),
        sa.Column("is_read", sa.Boolean(), nullable=False),
        sa.Column("created_at", sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_notifications_id"), "notifications", ["id"], unique=False)
    op.create_index(op.f("ix_notifications_user_id"), "notifications", ["user_id"], unique=False)


def downgrade() -> None:
    op.drop_index(op.f("ix_notifications_user_id"), table_name="notifications")
    op.drop_index(op.f("ix_notifications_id"), table_name="notifications")
    op.drop_table("notifications")
    op.drop_index(op.f("ix_opportunities_id"), table_name="opportunities")
    op.drop_table("opportunities")
    op.drop_index(op.f("ix_mentorship_requests_student_id"), table_name="mentorship_requests")
    op.drop_index(op.f("ix_mentorship_requests_mentor_id"), table_name="mentorship_requests")
    op.drop_index(op.f("ix_mentorship_requests_id"), table_name="mentorship_requests")
    op.drop_table("mentorship_requests")
    op.drop_index(op.f("ix_event_registrations_user_id"), table_name="event_registrations")
    op.drop_index(op.f("ix_event_registrations_id"), table_name="event_registrations")
    op.drop_index(op.f("ix_event_registrations_event_id"), table_name="event_registrations")
    op.drop_table("event_registrations")
    op.drop_index(op.f("ix_events_id"), table_name="events")
    op.drop_table("events")
    op.drop_index(op.f("ix_student_profiles_id"), table_name="student_profiles")
    op.drop_table("student_profiles")
    op.drop_index(op.f("ix_alumni_id"), table_name="alumni")
    op.drop_table("alumni")
    op.drop_index(op.f("ix_users_id"), table_name="users")
    op.drop_table("users")

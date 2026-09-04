"""Add feature expansion tables and alumni verification

Revision ID: a1b2c3d4e5f6
Revises: 2ea7c4d5b43b
Create Date: 2026-09-04 14:50:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = 'a1b2c3d4e5f6'
down_revision: Union[str, Sequence[str], None] = '2ea7c4d5b43b'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    bind = op.get_bind()
    is_postgres = bind.dialect.name == 'postgresql'
    naming_convention = {
        "fk": "fk_%(table_name)s_%(column_0_name)s_%(referred_table_name)s",
        "uq": "uq_%(table_name)s_%(column_0_name)s",
    }

    # 1. Add is_verified column to alumni table with safe default
    with op.batch_alter_table('alumni', naming_convention=naming_convention) as batch_op:
        batch_op.add_column(
            sa.Column('is_verified', sa.Boolean(), server_default=sa.false(), nullable=False)
        )

    # 2. Create user_bookmarks table
    op.create_table(
        'user_bookmarks',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('entity_type', sa.String(), nullable=False),
        sa.Column('entity_id', sa.Integer(), nullable=False),
        sa.Column('created_at', sa.DateTime(), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('user_id', 'entity_type', 'entity_id', name='uq_user_bookmark')
    )
    op.create_index('ix_user_bookmarks_id', 'user_bookmarks', ['id'], unique=False)
    op.create_index('ix_user_bookmarks_user_id', 'user_bookmarks', ['user_id'], unique=False)
    op.create_index('ix_bookmark_entity', 'user_bookmarks', ['entity_type', 'entity_id'], unique=False)

    # 3. Create announcements table
    op.create_table(
        'announcements',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('title', sa.String(), nullable=False),
        sa.Column('content', sa.Text(), nullable=False),
        sa.Column('category', sa.String(), server_default='GENERAL', nullable=False),
        sa.Column('priority', sa.String(), server_default='NORMAL', nullable=False),
        sa.Column('created_by', sa.Integer(), nullable=True),
        sa.Column('created_at', sa.DateTime(), server_default=sa.func.now(), nullable=False),
        sa.Column('expires_at', sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(['created_by'], ['users.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('ix_announcements_id', 'announcements', ['id'], unique=False)
    op.create_index('ix_announcements_priority', 'announcements', ['priority'], unique=False)
    op.create_index('ix_announcements_created_at', 'announcements', ['created_at'], unique=False)
    op.create_index('ix_announcements_expires_at', 'announcements', ['expires_at'], unique=False)

    # 4. Create notification_preferences table
    op.create_table(
        'notification_preferences',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('events', sa.Boolean(), server_default=sa.true(), nullable=False),
        sa.Column('mentorship', sa.Boolean(), server_default=sa.true(), nullable=False),
        sa.Column('opportunities', sa.Boolean(), server_default=sa.true(), nullable=False),
        sa.Column('announcements', sa.Boolean(), server_default=sa.true(), nullable=False),
        sa.Column('updated_at', sa.DateTime(), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('user_id', name='uq_notification_preferences_user_id')
    )
    op.create_index('ix_notification_preferences_id', 'notification_preferences', ['id'], unique=False)
    op.create_index('ix_notification_preferences_user_id', 'notification_preferences', ['user_id'], unique=True)


def downgrade() -> None:
    naming_convention = {
        "fk": "fk_%(table_name)s_%(column_0_name)s_%(referred_table_name)s",
        "uq": "uq_%(table_name)s_%(column_0_name)s",
    }
    op.drop_table('notification_preferences')
    op.drop_table('announcements')
    op.drop_table('user_bookmarks')
    with op.batch_alter_table('alumni', naming_convention=naming_convention) as batch_op:
        batch_op.drop_column('is_verified')

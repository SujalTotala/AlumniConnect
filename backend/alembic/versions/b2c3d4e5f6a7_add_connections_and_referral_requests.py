"""Add connections and referral requests tables

Revision ID: b2c3d4e5f6a7
Revises: a1b2c3d4e5f6
Create Date: 2026-09-06 16:25:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = 'b2c3d4e5f6a7'
down_revision: Union[str, Sequence[str], None] = 'a1b2c3d4e5f6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create connections table
    op.create_table(
        'connections',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('sender_id', sa.Integer(), nullable=False),
        sa.Column('receiver_id', sa.Integer(), nullable=False),
        sa.Column('status', sa.String(), server_default='PENDING', nullable=False),
        sa.Column('created_at', sa.DateTime(), server_default=sa.func.now(), nullable=False),
        sa.Column('updated_at', sa.DateTime(), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['sender_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['receiver_id'], ['users.id'], ondelete='CASCADE'),
        sa.CheckConstraint('sender_id != receiver_id', name='check_sender_not_receiver'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('ix_connections_id', 'connections', ['id'], unique=False)
    op.create_index('ix_connections_sender_id', 'connections', ['sender_id'], unique=False)
    op.create_index('ix_connections_receiver_id', 'connections', ['receiver_id'], unique=False)
    op.create_index('ix_connections_status', 'connections', ['status'], unique=False)

    # 2. Create referral_requests table
    op.create_table(
        'referral_requests',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('opportunity_id', sa.Integer(), nullable=False),
        sa.Column('requester_id', sa.Integer(), nullable=False),
        sa.Column('alumni_id', sa.Integer(), nullable=False),
        sa.Column('message', sa.Text(), nullable=True),
        sa.Column('status', sa.String(), server_default='PENDING', nullable=False),
        sa.Column('created_at', sa.DateTime(), server_default=sa.func.now(), nullable=False),
        sa.Column('updated_at', sa.DateTime(), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['opportunity_id'], ['opportunities.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['requester_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['alumni_id'], ['users.id'], ondelete='CASCADE'),
        sa.CheckConstraint('requester_id != alumni_id', name='check_requester_not_alumni'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('ix_referral_requests_id', 'referral_requests', ['id'], unique=False)
    op.create_index('ix_referral_requests_opportunity_id', 'referral_requests', ['opportunity_id'], unique=False)
    op.create_index('ix_referral_requests_requester_id', 'referral_requests', ['requester_id'], unique=False)
    op.create_index('ix_referral_requests_alumni_id', 'referral_requests', ['alumni_id'], unique=False)
    op.create_index('ix_referral_requests_status', 'referral_requests', ['status'], unique=False)


def downgrade() -> None:
    op.drop_index('ix_referral_requests_status', table_name='referral_requests')
    op.drop_index('ix_referral_requests_alumni_id', table_name='referral_requests')
    op.drop_index('ix_referral_requests_requester_id', table_name='referral_requests')
    op.drop_index('ix_referral_requests_opportunity_id', table_name='referral_requests')
    op.drop_index('ix_referral_requests_id', table_name='referral_requests')
    op.drop_table('referral_requests')

    op.drop_index('ix_connections_status', table_name='connections')
    op.drop_index('ix_connections_receiver_id', table_name='connections')
    op.drop_index('ix_connections_sender_id', table_name='connections')
    op.drop_index('ix_connections_id', table_name='connections')
    op.drop_table('connections')

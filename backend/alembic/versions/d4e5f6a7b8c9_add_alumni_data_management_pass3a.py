"""Add alumni data management tables (import jobs, segments, audit logs) and announcement targeting
Revision ID: d4e5f6a7b8c9
Revises: c3d4e5f6a7b8
Create Date: 2026-09-07 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = 'd4e5f6a7b8c9'
down_revision = 'c3d4e5f6a7b8'
branch_labels = None
depends_on = None

def upgrade():
    # 1. alumni_segments
    op.create_table(
        'alumni_segments',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('filters_json', sa.Text(), nullable=False),
        sa.Column('created_by', sa.Integer(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False),
        sa.Column('updated_at', sa.DateTime(), nullable=False),
        sa.ForeignKeyConstraint(['created_by'], ['users.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('name')
    )
    op.create_index(op.f('ix_alumni_segments_id'), 'alumni_segments', ['id'], unique=False)
    op.create_index(op.f('ix_alumni_segments_name'), 'alumni_segments', ['name'], unique=True)

    # 2. alumni_import_jobs
    op.create_table(
        'alumni_import_jobs',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('import_session_id', sa.String(), nullable=False),
        sa.Column('file_name', sa.String(), nullable=False),
        sa.Column('total_rows', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('created_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('updated_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('skipped_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('duplicate_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('status', sa.String(), nullable=False, server_default='PREVIEWED'),
        sa.Column('mode', sa.String(), nullable=True),
        sa.Column('raw_preview_json', sa.Text(), nullable=True),
        sa.Column('performed_by', sa.Integer(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False),
        sa.Column('completed_at', sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(['performed_by'], ['users.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('import_session_id')
    )
    op.create_index(op.f('ix_alumni_import_jobs_id'), 'alumni_import_jobs', ['id'], unique=False)
    op.create_index(op.f('ix_alumni_import_jobs_import_session_id'), 'alumni_import_jobs', ['import_session_id'], unique=True)
    op.create_index(op.f('ix_alumni_import_jobs_status'), 'alumni_import_jobs', ['status'], unique=False)
    op.create_index(op.f('ix_alumni_import_jobs_created_at'), 'alumni_import_jobs', ['created_at'], unique=False)

    # 3. admin_audit_logs
    op.create_table(
        'admin_audit_logs',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('admin_user_id', sa.Integer(), nullable=True),
        sa.Column('action', sa.String(), nullable=False),
        sa.Column('target_type', sa.String(), nullable=False),
        sa.Column('target_id', sa.String(), nullable=True),
        sa.Column('details', sa.Text(), nullable=True),
        sa.Column('ip_address', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False),
        sa.ForeignKeyConstraint(['admin_user_id'], ['users.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_admin_audit_logs_id'), 'admin_audit_logs', ['id'], unique=False)
    op.create_index(op.f('ix_admin_audit_logs_admin_user_id'), 'admin_audit_logs', ['admin_user_id'], unique=False)
    op.create_index(op.f('ix_admin_audit_logs_action'), 'admin_audit_logs', ['action'], unique=False)
    op.create_index(op.f('ix_admin_audit_logs_target_type'), 'admin_audit_logs', ['target_type'], unique=False)
    op.create_index(op.f('ix_admin_audit_logs_created_at'), 'admin_audit_logs', ['created_at'], unique=False)

    # 4. Alter announcements to add audience_type and segment_id
    with op.batch_alter_table('announcements', schema=None) as batch_op:
        batch_op.add_column(sa.Column('audience_type', sa.String(), nullable=False, server_default='ALL_USERS'))
        batch_op.add_column(sa.Column('segment_id', sa.Integer(), nullable=True))
        batch_op.create_foreign_key('fk_announcements_segment_id_alumni_segments', 'alumni_segments', ['segment_id'], ['id'], ondelete='SET NULL')
        batch_op.create_index('ix_announcements_audience_type', ['audience_type'], unique=False)
        batch_op.create_index('ix_announcements_segment_id', ['segment_id'], unique=False)

def downgrade():
    with op.batch_alter_table('announcements', schema=None) as batch_op:
        batch_op.drop_index('ix_announcements_segment_id')
        batch_op.drop_index('ix_announcements_audience_type')
        batch_op.drop_constraint('fk_announcements_segment_id_alumni_segments', type_='foreignkey')
        batch_op.drop_column('segment_id')
        batch_op.drop_column('audience_type')

    op.drop_table('admin_audit_logs')
    op.drop_table('alumni_import_jobs')
    op.drop_table('alumni_segments')

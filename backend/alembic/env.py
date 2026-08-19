import os
import sys
from logging.config import fileConfig
from pathlib import Path

from sqlalchemy import engine_from_config
from sqlalchemy import pool
from alembic import context

# Add project root directory to path to allow importing app
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.database.database import Base
from app.database.url_utils import normalize_database_url
# Import all models to ensure metadata registration
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.models.profile_model import StudentProfile
from app.models.event_model import Event, EventRegistration
from app.models.mentorship_model import MentorshipRequest
from app.models.opportunity_model import Opportunity
from app.models.notification_model import Notification

# this is the Alembic Config object, which provides
# access to the values within the .ini file in use.
config = context.config

# Interpret the config file for Python logging.
# This line sets up loggers basically.
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

target_metadata = Base.metadata

# Set database URL dynamically from app configuration
from app.config import DATABASE_URL
db_url = normalize_database_url(DATABASE_URL)

# ConfigParser treats percent signs as interpolation syntax. Escaping them here
# preserves ordinary percent-encoded URL credentials.
config.set_main_option("sqlalchemy.url", db_url.replace("%", "%%"))


def run_migrations_offline() -> None:
    """Run migrations in 'offline' mode.

    This configures the context with just a URL
    and not an Engine, though an Engine is acceptable
    here as well.  By skipping the Engine creation
    we don't even need a DBAPI to be available.

    Calls to context.execute() here emit the given string to the
    script output.

    """
    url = config.get_main_option("sqlalchemy.url")
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )

    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    """Run migrations in 'online' mode.

    In this scenario we need to create an Engine
    and associate a connection with the context.

    """
    connectable = engine_from_config(
        config.get_section(config.config_ini_section, {}),
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )

    with connectable.connect() as connection:
        context.configure(
            connection=connection, target_metadata=target_metadata
        )

        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()

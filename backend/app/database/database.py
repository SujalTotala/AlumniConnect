from sqlalchemy import create_engine, event
from sqlalchemy.engine import make_url
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from app.config import DATABASE_URL
from app.database.url_utils import normalize_database_url

db_url = normalize_database_url(DATABASE_URL)
database_backend = make_url(db_url).get_backend_name()

if database_backend == "sqlite":
    engine = create_engine(
        db_url,
        connect_args={"check_same_thread": False}
    )
else:
    engine = create_engine(
        db_url,
        pool_pre_ping=True
    )


if database_backend == "sqlite":
    @event.listens_for(engine, "connect")
    def enable_sqlite_foreign_keys(dbapi_connection, _connection_record):
        cursor = dbapi_connection.cursor()
        try:
            cursor.execute("PRAGMA foreign_keys=ON")
        finally:
            cursor.close()

SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine
)

Base = declarative_base()

# Database Dependency
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

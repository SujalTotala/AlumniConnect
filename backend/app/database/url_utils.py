from pathlib import Path

from sqlalchemy.engine import make_url


BACKEND_DIR = Path(__file__).resolve().parents[2]


def normalize_database_url(database_url: str) -> str:
    """Return a SQLAlchemy URL suitable for this project's installed drivers."""
    if not database_url or not database_url.strip():
        raise RuntimeError("DATABASE_URL must not be empty")

    url = make_url(database_url.strip())

    if url.drivername in {"postgres", "postgresql"}:
        url = url.set(drivername="postgresql+pg8000")

    if url.get_backend_name() == "sqlite" and url.database not in {None, ":memory:"}:
        database_path = Path(url.database)
        if not database_path.is_absolute():
            database_path = (BACKEND_DIR / database_path).resolve()
        url = url.set(database=str(database_path))

    return url.render_as_string(hide_password=False)

"""Explicit, transactional SQLite-to-PostgreSQL data migration utility.

The script never runs as part of application startup.  It reads connection URLs
only from SOURCE_DATABASE_URL and TARGET_DATABASE_URL.  Run a dry-run first:

    python scripts/migrate_sqlite_to_postgres.py --dry-run

The dry-run performs the planned inserts inside a real target transaction and
then rolls that transaction back.  A run without ``--dry-run`` commits all data
in one transaction after every source and conflict check has passed.
"""

from __future__ import annotations

import argparse
import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping, Sequence

from sqlalchemy import (
    BigInteger,
    Integer,
    MetaData,
    Table,
    create_engine,
    event,
    func,
    select,
    text,
)
from sqlalchemy.engine import Connection, Engine, URL, make_url
from sqlalchemy.exc import ArgumentError, SQLAlchemyError


SOURCE_URL_ENV = "SOURCE_DATABASE_URL"
TARGET_URL_ENV = "TARGET_DATABASE_URL"
POSTGRES_DRIVER = "postgresql+pg8000"
POSTGRES_INTEGER_MIN = -(2**31)
POSTGRES_INTEGER_MAX = 2**31 - 1
BACKEND_DIR = Path(__file__).resolve().parent.parent


@dataclass(frozen=True)
class ForeignKeyRule:
    column: str
    parent_table: str
    nullable: bool


@dataclass(frozen=True)
class TableSpec:
    name: str
    columns: tuple[str, ...]
    natural_key: tuple[str, ...] = ()
    foreign_keys: tuple[ForeignKeyRule, ...] = ()


# This order is derived from the current model foreign keys.  Mentorship rows
# reference users directly; event registrations are the only second-level rows.
TABLE_SPECS: tuple[TableSpec, ...] = (
    TableSpec(
        "users",
        ("id", "name", "email", "password", "role", "is_active", "created_at"),
        natural_key=("email",),
    ),
    TableSpec(
        "alumni",
        (
            "id",
            "user_id",
            "name",
            "email",
            "graduation_year",
            "department",
            "company",
            "job_role",
            "location",
            "skills",
            "bio",
            "linkedin_url",
            "github_url",
            "mentorship_available",
            "created_at",
        ),
        natural_key=("email",),
        foreign_keys=(ForeignKeyRule("user_id", "users", nullable=True),),
    ),
    TableSpec(
        "student_profiles",
        (
            "id",
            "user_id",
            "branch",
            "year",
            "skills",
            "interests",
            "bio",
            "profile_image_url",
            "created_at",
            "updated_at",
        ),
        natural_key=("user_id",),
        foreign_keys=(ForeignKeyRule("user_id", "users", nullable=False),),
    ),
    TableSpec(
        "events",
        (
            "id",
            "title",
            "description",
            "event_type",
            "event_date",
            "start_time",
            "location",
            "meeting_url",
            "image_url",
            "created_by",
            "created_at",
            "updated_at",
        ),
        foreign_keys=(ForeignKeyRule("created_by", "users", nullable=True),),
    ),
    TableSpec(
        "event_registrations",
        ("id", "event_id", "user_id", "registration_status", "registered_at"),
        natural_key=("event_id", "user_id"),
        foreign_keys=(
            ForeignKeyRule("event_id", "events", nullable=False),
            ForeignKeyRule("user_id", "users", nullable=False),
        ),
    ),
    TableSpec(
        "mentorship_requests",
        (
            "id",
            "student_id",
            "mentor_id",
            "message",
            "status",
            "response_note",
            "created_at",
            "updated_at",
        ),
        foreign_keys=(
            ForeignKeyRule("student_id", "users", nullable=False),
            ForeignKeyRule("mentor_id", "users", nullable=False),
        ),
    ),
    TableSpec(
        "opportunities",
        (
            "id",
            "title",
            "company",
            "description",
            "opportunity_type",
            "location",
            "deadline",
            "application_url",
            "posted_by",
            "created_at",
        ),
        foreign_keys=(ForeignKeyRule("posted_by", "users", nullable=True),),
    ),
    TableSpec(
        "notifications",
        (
            "id",
            "user_id",
            "title",
            "message",
            "notification_type",
            "is_read",
            "created_at",
        ),
        foreign_keys=(ForeignKeyRule("user_id", "users", nullable=False),),
    ),
)


class SafeMigrationError(RuntimeError):
    """An error whose message is safe to display without leaking row data."""


class ConfigurationError(SafeMigrationError):
    pass


class SchemaValidationError(SafeMigrationError):
    pass


class SourceIntegrityError(SafeMigrationError):
    pass


class MigrationConflictError(SafeMigrationError):
    pass


class TargetWriteError(SafeMigrationError):
    pass


@dataclass
class TableSummary:
    inserted: int = 0
    skipped: int = 0
    failed: int = 0
    conflicts: int = 0


@dataclass
class RunState:
    outcome: str = "NOT STARTED"
    integrity_checked: bool = False
    testing_override: bool = False
    sequence_count: int = 0


def _new_summaries() -> dict[str, TableSummary]:
    return {spec.name: TableSummary() for spec in TABLE_SPECS}


def _normalize_url(raw_url: str, env_name: str) -> URL:
    value = raw_url.strip()
    if not value:
        raise ConfigurationError(f"{env_name} must not be empty.")

    if value.startswith("postgres://"):
        value = value.replace("postgres://", f"{POSTGRES_DRIVER}://", 1)
    elif value.startswith("postgresql://"):
        value = value.replace("postgresql://", f"{POSTGRES_DRIVER}://", 1)

    try:
        url = make_url(value)
    except (ArgumentError, ValueError) as exc:
        raise ConfigurationError(f"{env_name} is not a valid database URL.") from exc

    if url.get_backend_name() == "sqlite" and url.database not in {None, ":memory:"}:
        database_path = Path(url.database)
        if not database_path.is_absolute():
            url = url.set(database=str((BACKEND_DIR / database_path).resolve()))

    return url


def _sqlite_database_identity(url: URL) -> str:
    database = url.database
    if not database or database == ":memory:":
        return database or ":memory:"
    path = Path(database)
    if not path.is_absolute():
        path = Path.cwd() / path
    return os.path.normcase(str(path.resolve()))


def _validate_urls(source_url: URL, target_url: URL, allow_non_postgres: bool) -> None:
    if source_url.get_backend_name() != "sqlite":
        raise ConfigurationError("SOURCE_DATABASE_URL must use SQLite.")

    target_is_postgres = target_url.get_backend_name() == "postgresql"
    if not target_is_postgres and not allow_non_postgres:
        raise ConfigurationError(
            "TARGET_DATABASE_URL must use PostgreSQL. "
            "The non-PostgreSQL override is for isolated tests only."
        )
    if target_is_postgres and target_url.drivername != POSTGRES_DRIVER:
        raise ConfigurationError("The PostgreSQL target must use the pg8000 driver.")

    if source_url == target_url:
        raise ConfigurationError("Source and target databases must be different.")
    if (
        target_url.get_backend_name() == "sqlite"
        and _sqlite_database_identity(source_url) == _sqlite_database_identity(target_url)
    ):
        raise ConfigurationError("Source and target SQLite files must be different.")


def _make_source_engine(url: URL) -> Engine:
    engine = create_engine(url, future=True)

    @event.listens_for(engine, "connect")
    def _make_sqlite_source_read_only(dbapi_connection: Any, _: Any) -> None:
        cursor = dbapi_connection.cursor()
        try:
            cursor.execute("PRAGMA query_only = ON")
        finally:
            cursor.close()

    return engine


def _make_target_engine(url: URL) -> Engine:
    if url.get_backend_name() == "sqlite":
        engine = create_engine(
            url,
            future=True,
            connect_args={"check_same_thread": False},
        )

        @event.listens_for(engine, "connect")
        def _enable_sqlite_foreign_keys(dbapi_connection: Any, _: Any) -> None:
            cursor = dbapi_connection.cursor()
            try:
                cursor.execute("PRAGMA foreign_keys = ON")
            finally:
                cursor.close()

        return engine

    return create_engine(
        url,
        future=True,
        pool_pre_ping=True,
        connect_args={"timeout": 60},
    )


def _reflect_tables(connection: Connection, database_label: str) -> dict[str, Table]:
    metadata = MetaData()
    tables: dict[str, Table] = {}

    for spec in TABLE_SPECS:
        try:
            table = Table(spec.name, metadata, autoload_with=connection)
        except SQLAlchemyError as exc:
            raise SchemaValidationError(
                f"The {database_label} schema is missing or cannot reflect table "
                f"'{spec.name}'."
            ) from exc

        missing_columns = sorted(set(spec.columns) - set(table.c.keys()))
        if missing_columns:
            raise SchemaValidationError(
                f"The {database_label} table '{spec.name}' is missing required "
                "migration columns."
            )
        if "id" not in {column.name for column in table.primary_key.columns}:
            raise SchemaValidationError(
                f"The {database_label} table '{spec.name}' must have primary key 'id'."
            )
        tables[spec.name] = table

    return tables


def _validate_target_foreign_keys(tables: Mapping[str, Table]) -> None:
    for spec in TABLE_SPECS:
        table = tables[spec.name]
        for rule in spec.foreign_keys:
            actual_targets = {
                (foreign_key.column.table.name, foreign_key.column.name)
                for foreign_key in table.c[rule.column].foreign_keys
            }
            if (rule.parent_table, "id") not in actual_targets:
                raise SchemaValidationError(
                    f"Target foreign key {spec.name}.{rule.column} -> "
                    f"{rule.parent_table}.id is missing."
                )


def _read_rows(connection: Connection, tables: Mapping[str, Table]) -> dict[str, list[dict[str, Any]]]:
    rows: dict[str, list[dict[str, Any]]] = {}
    for spec in TABLE_SPECS:
        table = tables[spec.name]
        statement = select(*(table.c[column] for column in spec.columns)).order_by(
            table.c.id
        )
        rows[spec.name] = [dict(row) for row in connection.execute(statement).mappings()]
    return rows


def _validate_primary_and_natural_keys(
    source_rows: Mapping[str, Sequence[Mapping[str, Any]]],
    summaries: Mapping[str, TableSummary],
) -> list[str]:
    errors: list[str] = []
    for spec in TABLE_SPECS:
        seen_ids: set[Any] = set()
        seen_natural_keys: set[tuple[Any, ...]] = set()
        primary_failures = 0
        natural_failures = 0

        for row in source_rows[spec.name]:
            source_id = row["id"]
            if source_id is None or source_id in seen_ids:
                primary_failures += 1
            else:
                seen_ids.add(source_id)

            if spec.natural_key:
                key = tuple(row[column] for column in spec.natural_key)
                if any(value is None for value in key) or key in seen_natural_keys:
                    natural_failures += 1
                else:
                    seen_natural_keys.add(key)

        if primary_failures:
            summaries[spec.name].failed += primary_failures
            errors.append(f"{spec.name}: invalid or duplicate primary IDs")
        if natural_failures:
            summaries[spec.name].failed += natural_failures
            errors.append(f"{spec.name}: invalid or duplicate natural keys")

    return errors


def _validate_source_foreign_keys(
    source_rows: Mapping[str, Sequence[Mapping[str, Any]]],
    summaries: Mapping[str, TableSummary],
) -> list[str]:
    parent_ids = {
        spec.name: {row["id"] for row in source_rows[spec.name] if row["id"] is not None}
        for spec in TABLE_SPECS
    }
    errors: list[str] = []

    for spec in TABLE_SPECS:
        for rule in spec.foreign_keys:
            orphan_count = 0
            for row in source_rows[spec.name]:
                value = row[rule.column]
                if value is None:
                    if not rule.nullable:
                        orphan_count += 1
                    continue
                if value not in parent_ids[rule.parent_table]:
                    orphan_count += 1

            if orphan_count:
                summaries[spec.name].failed += orphan_count
                errors.append(
                    f"{spec.name}.{rule.column} -> {rule.parent_table}.id "
                    f"({orphan_count} orphan(s))"
                )

    return errors


def _validate_target_nullability_and_integer_ranges(
    source_rows: Mapping[str, Sequence[Mapping[str, Any]]],
    target_tables: Mapping[str, Table],
    summaries: Mapping[str, TableSummary],
    postgres_target: bool,
) -> list[str]:
    errors: list[str] = []

    for spec in TABLE_SPECS:
        table = target_tables[spec.name]
        invalid_nulls = 0
        out_of_range = 0
        for row in source_rows[spec.name]:
            for column_name in spec.columns:
                column = table.c[column_name]
                value = row[column_name]
                if value is None and not column.nullable and not column.primary_key:
                    invalid_nulls += 1
                if (
                    postgres_target
                    and value is not None
                    and isinstance(column.type, Integer)
                    and not isinstance(column.type, BigInteger)
                    and not isinstance(value, bool)
                    and not (POSTGRES_INTEGER_MIN <= value <= POSTGRES_INTEGER_MAX)
                ):
                    out_of_range += 1

        if invalid_nulls:
            summaries[spec.name].failed += invalid_nulls
            errors.append(f"{spec.name}: values violate target nullability")
        if out_of_range:
            summaries[spec.name].failed += out_of_range
            errors.append(f"{spec.name}: integers exceed PostgreSQL INTEGER range")

    return errors


def _validate_source_integrity(
    source_rows: Mapping[str, Sequence[Mapping[str, Any]]],
    target_tables: Mapping[str, Table],
    summaries: Mapping[str, TableSummary],
    postgres_target: bool,
) -> None:
    errors = _validate_primary_and_natural_keys(source_rows, summaries)
    errors.extend(_validate_source_foreign_keys(source_rows, summaries))
    errors.extend(
        _validate_target_nullability_and_integer_ranges(
            source_rows,
            target_tables,
            summaries,
            postgres_target,
        )
    )
    if errors:
        raise SourceIntegrityError(
            "Source integrity validation failed: " + "; ".join(errors) + "."
        )


def _build_target_indexes(
    spec: TableSpec,
    target_rows: Sequence[Mapping[str, Any]],
) -> tuple[dict[Any, dict[str, Any]], dict[tuple[Any, ...], dict[str, Any]]]:
    by_id: dict[Any, dict[str, Any]] = {}
    by_natural_key: dict[tuple[Any, ...], dict[str, Any]] = {}

    for raw_row in target_rows:
        row = dict(raw_row)
        target_id = row["id"]
        if target_id in by_id:
            raise SchemaValidationError(
                f"Target table '{spec.name}' contains duplicate primary IDs."
            )
        by_id[target_id] = row

        if spec.natural_key:
            key = tuple(row[column] for column in spec.natural_key)
            if any(value is None for value in key):
                raise SchemaValidationError(
                    f"Target table '{spec.name}' contains an invalid natural key."
                )
            if key in by_natural_key:
                raise SchemaValidationError(
                    f"Target table '{spec.name}' contains duplicate natural keys."
                )
            by_natural_key[key] = row

    return by_id, by_natural_key


def _rows_equal(
    spec: TableSpec,
    source_row: Mapping[str, Any],
    target_row: Mapping[str, Any],
) -> bool:
    return all(source_row[column] == target_row[column] for column in spec.columns)


def _plan_migration(
    source_rows: Mapping[str, Sequence[Mapping[str, Any]]],
    target_rows: Mapping[str, Sequence[Mapping[str, Any]]],
    summaries: Mapping[str, TableSummary],
) -> dict[str, list[dict[str, Any]]]:
    id_maps: dict[str, dict[Any, Any]] = {spec.name: {} for spec in TABLE_SPECS}
    planned_inserts: dict[str, list[dict[str, Any]]] = {
        spec.name: [] for spec in TABLE_SPECS
    }

    for spec in TABLE_SPECS:
        target_by_id, target_by_natural_key = _build_target_indexes(
            spec, target_rows[spec.name]
        )

        for raw_source_row in source_rows[spec.name]:
            source_row = {column: raw_source_row[column] for column in spec.columns}
            source_id = source_row["id"]

            for rule in spec.foreign_keys:
                source_parent_id = source_row[rule.column]
                if source_parent_id is None:
                    continue
                try:
                    source_row[rule.column] = id_maps[rule.parent_table][source_parent_id]
                except KeyError as exc:
                    summaries[spec.name].failed += 1
                    raise SourceIntegrityError(
                        f"Unable to map foreign key {spec.name}.{rule.column}."
                    ) from exc

            primary_match = target_by_id.get(source_id)
            natural_match = None
            if spec.natural_key:
                natural_key = tuple(source_row[column] for column in spec.natural_key)
                natural_match = target_by_natural_key.get(natural_key)

            if primary_match is not None:
                if natural_match is not None and natural_match["id"] != source_id:
                    summaries[spec.name].conflicts += 1
                elif _rows_equal(spec, source_row, primary_match):
                    summaries[spec.name].skipped += 1
                else:
                    summaries[spec.name].conflicts += 1
                id_maps[spec.name][source_id] = source_id
                continue

            if natural_match is not None:
                summaries[spec.name].skipped += 1
                id_maps[spec.name][source_id] = natural_match["id"]
                continue

            planned_inserts[spec.name].append(source_row)
            id_maps[spec.name][source_id] = source_id
            target_by_id[source_id] = source_row
            if spec.natural_key:
                key = tuple(source_row[column] for column in spec.natural_key)
                target_by_natural_key[key] = source_row

    conflict_count = sum(summary.conflicts for summary in summaries.values())
    if conflict_count:
        raise MigrationConflictError(
            f"Migration stopped before inserts because {conflict_count} "
            "same-primary-key or ambiguous conflict(s) were detected."
        )

    return planned_inserts


def _insert_planned_rows(
    connection: Connection,
    target_tables: Mapping[str, Table],
    planned_inserts: Mapping[str, Sequence[Mapping[str, Any]]],
    summaries: Mapping[str, TableSummary],
) -> None:
    for spec in TABLE_SPECS:
        rows = list(planned_inserts[spec.name])
        if not rows:
            continue
        try:
            connection.execute(target_tables[spec.name].insert(), rows)
        except SQLAlchemyError as exc:
            summaries[spec.name].failed += len(rows)
            raise TargetWriteError(
                f"Target rejected rows for table '{spec.name}'; the transaction "
                "will be rolled back."
            ) from exc
        summaries[spec.name].inserted += len(rows)


def _discover_postgres_sequence(
    connection: Connection,
    table: Table,
) -> tuple[str, str] | None:
    table_name = table.fullname
    sequence_name = connection.execute(
        text("SELECT pg_get_serial_sequence(:table_name, :column_name)"),
        {"table_name": table_name, "column_name": "id"},
    ).scalar_one_or_none()
    if not sequence_name:
        return None

    sequence_row = connection.execute(
        text(
            "SELECT namespace.nspname, sequence.relname "
            "FROM pg_class AS sequence "
            "JOIN pg_namespace AS namespace "
            "ON namespace.oid = sequence.relnamespace "
            "WHERE sequence.oid = CAST(:sequence_name AS regclass)"
        ),
        {"sequence_name": sequence_name},
    ).one_or_none()
    if sequence_row is None:
        raise TargetWriteError("A PostgreSQL ID sequence could not be resolved safely.")
    return str(sequence_row[0]), str(sequence_row[1])


def _count_postgres_sequences(
    connection: Connection,
    target_tables: Mapping[str, Table],
) -> int:
    return sum(
        _discover_postgres_sequence(connection, target_tables[spec.name]) is not None
        for spec in TABLE_SPECS
    )


def _reset_postgres_sequences(
    connection: Connection,
    target_tables: Mapping[str, Table],
) -> int:
    """Reset serial/identity sequences transactionally after explicit-ID inserts.

    PostgreSQL ``ALTER SEQUENCE ... RESTART`` is used instead of ``setval`` so
    the restart participates in the same transaction as the imported rows.
    Identifiers come from PostgreSQL's catalogs and are quoted by the dialect.
    """

    reset_count = 0
    preparer = connection.dialect.identifier_preparer

    for spec in TABLE_SPECS:
        table = target_tables[spec.name]
        sequence_parts = _discover_postgres_sequence(connection, table)
        if sequence_parts is None:
            continue

        schema_name, sequence_name = sequence_parts
        maximum_id = connection.execute(select(func.max(table.c.id))).scalar_one()
        restart_value = 1 if maximum_id is None else int(maximum_id) + 1
        quoted_sequence = (
            f"{preparer.quote_identifier(schema_name)}."
            f"{preparer.quote_identifier(sequence_name)}"
        )
        connection.exec_driver_sql(
            f"ALTER SEQUENCE {quoted_sequence} RESTART WITH {restart_value}"
        )
        reset_count += 1

    return reset_count


def _execute_migration(
    source_url: URL,
    target_url: URL,
    dry_run: bool,
    summaries: Mapping[str, TableSummary],
    state: RunState,
) -> None:
    source_engine = _make_source_engine(source_url)
    target_engine = _make_target_engine(target_url)
    postgres_target = target_url.get_backend_name() == "postgresql"

    try:
        with source_engine.connect() as source_connection:
            source_tables = _reflect_tables(source_connection, "source")
            source_rows = _read_rows(source_connection, source_tables)

            with target_engine.connect() as target_connection:
                transaction = target_connection.begin()
                state.outcome = "TARGET TRANSACTION OPEN"
                try:
                    target_tables = _reflect_tables(target_connection, "target")
                    _validate_target_foreign_keys(target_tables)
                    target_rows = _read_rows(target_connection, target_tables)
                    _validate_source_integrity(
                        source_rows,
                        target_tables,
                        summaries,
                        postgres_target,
                    )
                    state.integrity_checked = True

                    planned_inserts = _plan_migration(
                        source_rows,
                        target_rows,
                        summaries,
                    )
                    _insert_planned_rows(
                        target_connection,
                        target_tables,
                        planned_inserts,
                        summaries,
                    )

                    if dry_run:
                        if postgres_target:
                            state.sequence_count = _count_postgres_sequences(
                                target_connection, target_tables
                            )
                        transaction.rollback()
                        state.outcome = "DRY RUN ROLLED BACK"
                        return

                    if postgres_target:
                        state.sequence_count = _reset_postgres_sequences(
                            target_connection, target_tables
                        )
                    transaction.commit()
                    state.outcome = "COMMITTED"
                except Exception:
                    if transaction.is_active:
                        transaction.rollback()
                    state.outcome = "ROLLED BACK"
                    raise
    finally:
        source_engine.dispose()
        target_engine.dispose()


def _print_summary(
    summaries: Mapping[str, TableSummary],
    state: RunState,
    dry_run: bool,
) -> None:
    print("\nMigration summary")
    print("-" * 78)
    print(f"{'table':<24} {'inserted':>10} {'skipped':>10} {'failed':>10} {'conflicts':>10}")
    for spec in TABLE_SPECS:
        summary = summaries[spec.name]
        print(
            f"{spec.name:<24} {summary.inserted:>10} {summary.skipped:>10} "
            f"{summary.failed:>10} {summary.conflicts:>10}"
        )
    print("-" * 78)
    print(
        "Foreign-key integrity: "
        + ("PASSED" if state.integrity_checked else "NOT COMPLETED")
    )
    if state.sequence_count:
        sequence_action = "would reset" if dry_run else "reset"
        print(f"PostgreSQL sequences {sequence_action}: {state.sequence_count}")
    if state.testing_override:
        print("Target mode: NON-POSTGRESQL TESTING OVERRIDE")
    print(f"Outcome: {state.outcome}")
    if dry_run and state.outcome == "DRY RUN ROLLED BACK":
        print("Inserted counts were exercised in the transaction; no rows were persisted.")
    elif state.outcome == "ROLLED BACK":
        print("Inserted counts, if any, were attempted only; no rows were committed.")


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Copy AlumniConnect data from SQLite to an already-migrated "
            "PostgreSQL database. Connection URLs are read only from "
            "SOURCE_DATABASE_URL and TARGET_DATABASE_URL."
        )
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="exercise the migration in a target transaction, then roll it back",
    )
    parser.add_argument(
        "--allow-non-postgres-target",
        action="store_true",
        help="allow a non-PostgreSQL target for isolated utility tests only",
    )
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    args = _build_parser().parse_args(argv)
    summaries = _new_summaries()
    state = RunState(testing_override=args.allow_non_postgres_target)

    env_path = BACKEND_DIR / ".env"
    if env_path.exists():
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, val = line.split("=", 1)
                    os.environ.setdefault(key.strip(), val.strip())

    source_raw = os.getenv(SOURCE_URL_ENV)
    target_raw = os.getenv(TARGET_URL_ENV)
    if source_raw is None or target_raw is None:
        print(
            f"ERROR: Set both {SOURCE_URL_ENV} and {TARGET_URL_ENV} before running.",
            file=sys.stderr,
        )
        return 2

    try:
        source_url = _normalize_url(source_raw, SOURCE_URL_ENV)
        target_url = _normalize_url(target_raw, TARGET_URL_ENV)
        _validate_urls(source_url, target_url, args.allow_non_postgres_target)

        print("Mode:", "DRY RUN" if args.dry_run else "COMMIT")
        print("Connection URLs validated; credentials and row values will not be logged.")
        _execute_migration(
            source_url,
            target_url,
            args.dry_run,
            summaries,
            state,
        )
    except SafeMigrationError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        _print_summary(summaries, state, args.dry_run)
        return 1
    except Exception as exc:  # Never print DBAPI messages that may contain parameters.
        print(
            f"ERROR: Migration failed safely ({type(exc).__name__}); "
            "connection details and row values were suppressed.",
            file=sys.stderr,
        )
        _print_summary(summaries, state, args.dry_run)
        return 1

    _print_summary(summaries, state, args.dry_run)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env bash
# Start a local PostgreSQL (Homebrew) matching application.yml defaults.
# Use this when Docker is not available in the environment.

set -euo pipefail

export PATH="/home/linuxbrew/.linuxbrew/opt/postgresql@16/bin:${PATH}"
export PGDATA="${PGDATA:-/home/linuxbrew/.linuxbrew/var/postgresql@16}"

if ! command -v pg_ctl >/dev/null 2>&1; then
  echo "postgresql@16 not found. Install with: brew install postgresql@16" >&2
  exit 1
fi

if ! pg_ctl status >/dev/null 2>&1; then
  pg_ctl -l /tmp/pg-fms.log start
fi

psql -d postgres -v ON_ERROR_STOP=1 <<'SQL'
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'fms') THEN
    CREATE ROLE fms LOGIN PASSWORD 'fms';
  END IF;
END
$$;
SQL

psql -d postgres -tc "SELECT 1 FROM pg_database WHERE datname='feature_management'" | grep -q 1 || \
  psql -d postgres -c "CREATE DATABASE feature_management OWNER fms;"

psql -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE feature_management TO fms;" >/dev/null
psql -d feature_management -c "GRANT ALL ON SCHEMA public TO fms;" >/dev/null

echo "PostgreSQL ready: jdbc:postgresql://localhost:5432/feature_management (fms/fms)"

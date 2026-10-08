#!/usr/bin/env bash
# Start a local Redis (Homebrew) matching application.yml defaults.
# Use this when Docker / brew services are not available.

set -euo pipefail

export PATH="/home/linuxbrew/.linuxbrew/opt/redis/bin:${PATH}"

if ! command -v redis-server >/dev/null 2>&1; then
  echo "redis not found. Install with: brew install redis" >&2
  exit 1
fi

REDIS_DIR="${REDIS_DIR:-/tmp/fms-redis}"
REDIS_PORT="${REDIS_PORT:-6379}"
mkdir -p "${REDIS_DIR}"

if redis-cli -p "${REDIS_PORT}" ping >/dev/null 2>&1; then
  echo "Redis already running on port ${REDIS_PORT}"
  exit 0
fi

redis-server \
  --port "${REDIS_PORT}" \
  --daemonize yes \
  --dir "${REDIS_DIR}" \
  --logfile "${REDIS_DIR}/redis.log" \
  --pidfile "${REDIS_DIR}/redis.pid" \
  --bind 127.0.0.1

if ! redis-cli -p "${REDIS_PORT}" ping >/dev/null 2>&1; then
  echo "Failed to start Redis. See ${REDIS_DIR}/redis.log" >&2
  exit 1
fi

echo "Redis ready: localhost:${REDIS_PORT}"

# Feature Management Service

Spring Boot service for boolean feature flags with global defaults, per-user overrides, PostgreSQL as source of truth, and Redis read-through caching for evaluation.

Design docs:

- [High-Level Design](./docs/high-level-design.md) — including the [evaluation-path architecture diagram](./docs/high-level-design.md#06--critical-flow-a--read-path-and-cache-consistency)
- [Phased Development Plan](./docs/phased-development-plan.md)

## Prerequisites

- Java 21
- PostgreSQL 16 (`brew install postgresql@16` or Docker Compose)
- Redis 7+ (`brew install redis` or Docker Compose)

## Local setup

```bash
# From repo root / this module
./scripts/start-local-postgres.sh
./scripts/start-local-redis.sh

./gradlew :feature-management-service:bootRun
# or from this directory:
./gradlew bootRun
```

Health:

```bash
curl -s localhost:8080/actuator/health
```

Metrics (Micrometer):

```bash
curl -s localhost:8080/actuator/metrics/fms.cache.hits
curl -s localhost:8080/actuator/prometheus   # when prometheus registry is on the classpath
```

Docker Compose (optional) starts Postgres + Redis — see `compose.yaml`. Prefer Homebrew scripts when Docker is unavailable.

## Quick API examples

```bash
# Create flag
curl -s -X POST localhost:8080/api/v1/flags \
  -H 'Content-Type: application/json' \
  -H 'X-Actor-Id: admin-1' \
  -H 'Idempotency-Key: create-checkout-1' \
  -d '{"name":"new-checkout","globalEnabled":true,"state":"ACTIVE"}'

# Override for a user
curl -s -X PUT localhost:8080/api/v1/flags/new-checkout/overrides/user-123 \
  -H 'Content-Type: application/json' \
  -H 'X-Actor-Id: admin-1' \
  -d '{"enabled":false}'

# Evaluate
curl -s 'localhost:8080/api/v1/evaluations/new-checkout?userId=user-123'
```

Actor attribution uses `X-Actor-Id` (default `local-admin`). Spring Security is deferred; do not expose management APIs publicly without a gateway/auth layer.

## Flag evaluation rules

| Global | Override | Result | `reason` |
| --- | --- | --- | --- |
| `true` / `false` | missing or `null` (INHERIT) | global value | `GLOBAL` |
| any | explicit `true` / `false` | override value | `USER_OVERRIDE` |
| — | flag name unknown | HTTP `404` | — |

Invalid flag names (spaces, uppercase, etc.) return HTTP `400`. When evaluation cannot safely read PostgreSQL after a cache miss (circuit open / capacity exceeded / DB error), the API returns HTTP `503`.

## How caching works

Keys (Redis hashes):

| Key | Fields | TTL |
| --- | --- | --- |
| `ff:flag:{name}` | `flagId`, `name`, `globalEnabled`, `updatedAt` | 30s |
| `ff:override:{flagId}:{userId}` | `value` (`TRUE`/`FALSE`/`INHERIT`), `updatedAt` | 30s (15s for `INHERIT`) |

Read-through evaluation:

1. Read flag metadata from Redis; on miss, load PostgreSQL (bounded fallback) and populate cache.
2. Read override from Redis; on miss, load PostgreSQL (or write `INHERIT` negative cache).
3. Apply precedence above.

Writes: after a successful DB commit, the service best-effort updates/evicts Redis (`updatedAt` wins via Lua). A transactional outbox worker is **deferred** (Phase 8 skipped); TTL bounds staleness if Redis put fails.

Redis down: cache ops fail soft; evaluation falls back to PostgreSQL through `DbFallbackGuard` (concurrency limit + short circuit breaker) so the DB is not stampeded.

## Tests & CI

```bash
./gradlew :feature-management-service:test
```

GitHub Actions runs the same on pull requests and `main` (see `.github/workflows/ci.yml`).

## Helm

Chart under `orchestration/helm/feature-management-service/` with `values-dev.yaml` for datasource/Redis env wiring.

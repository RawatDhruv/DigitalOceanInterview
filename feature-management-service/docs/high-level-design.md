# Feature Management System

**High-Level Design (HLD) and Requirements Specification**

| | |
|---|---|
| **Document** | HLD v1.0 \| 08 Oct 2026 |
| **Technology** | Java 21 • Spring Boot • PostgreSQL • Redis |
| **Deployment model** | 1 Spring Boot application + background worker |
| **Audience** | Backend engineers, reviewers, and interviewers |
| **Label** | INTERVIEW-READY / SYSTEM DESIGN / V1.0 |

> All performance figures in this document are proposed design targets, not measured results.

---

## Executive summary

Design a reliable, low-latency feature flag platform supporting administrative CRUD, global boolean values, optional per-user overrides, runtime evaluation, and an immutable audit trail. PostgreSQL is the source of truth; Redis accelerates read-heavy evaluation. All committed mutations write audit and outbox events atomically.

### Architecture at a glance

| Decision | Recommendation |
|---|---|
| **Deployables** | One Spring Boot codebase; application replicas serve management and evaluation APIs. A scheduled outbox worker starts in the same deployment and may be scaled separately. |
| **Logical services** | `FeatureFlagService`, `FeatureOverrideService`, `FeatureEvaluationService`, `AuditService`; plus `CacheSyncWorker`. |
| **Data & cache** | Managed PostgreSQL for authoritative state; Redis for cached entries keyed by flag name / override. |
| **Consistency** | Writes and audit events are ACID. Cache-backed reads are eventually consistent with bounded TTL and outbox propagation. |
| **Identity of a flag** | Flags are uniquely identified by `name` (no `project_id` / `environment` in V1). |

> **Primary business rule:** An explicit per-user value (`true` or `false`) wins over the global value. Missing or removed overrides inherit `global_enabled`.

---

## 01 / Requirements

### Functional requirements

| ID | Must-have requirement | Acceptance behavior |
|---|---|---|
| **FR-01** | Flag CRUD | Create, fetch, list, update and delete flags; unique `name`. |
| **FR-02** | Flag attributes | Store name, description, `globalEnabled`, and audit metadata. |
| **FR-03** | Global state control | Authorized admins can enable/disable a flag globally. |
| **FR-04** | Per-user overrides | Set `true`/`false` for a `(flag, user)` pair; remove to inherit global. |
| **FR-05** | Precedence | Explicit user override > global value; a `false` override remains explicit. |
| **FR-06** | Evaluation endpoint | Accept flag name and user ID; return effective boolean and decision reason. |
| **FR-07** | Caching | Cache metadata and overrides; read-through miss to PostgreSQL; negative caching. |
| **FR-08** | Audit trail | Record actor, time, action and field-level previous/new values for each committed change. |
| **FR-09** | Security | Only authorized users/services may call management and evaluation APIs. |
| **FR-10** | Operational views | Paginated flag listing, override listing and audit history retrieval. |

### Scope boundaries

| In scope (V1) | Out of scope (V1) |
|---|---|
| Boolean flags; global and user-level controls | Percentage rollouts, targeting expressions, segments |
| REST APIs and optional admin UI | Client-side SDK streaming and edge evaluation |
| Auditing, cache consistency and basic metrics | Experiment analytics or A/B testing |
| Globally unique flag names | Multi-tenant project/environment isolation; lifecycle states; optimistic locking versions |
| | Multi-region active/active writes |

---

## 02 / Non-functional requirements and assumptions

### Non-functional requirements

| ID | Quality attribute | Proposed target / design control |
|---|---|---|
| **NFR-01** | Read latency | p99 under 30 ms for warm-cache API requests within one region. |
| **NFR-02** | Throughput | Initial capacity target: 10k evaluations/s horizontally scaled; load-test before sign-off. |
| **NFR-03** | Availability | Target ≥99.9% service availability, subject to service-level objectives and budget. |
| **NFR-04** | Update responsiveness | Healthy-path p99 cache propagation < 5 seconds; eventual consistency remains explicit. |
| **NFR-05** | Durability | PostgreSQL durable transactions, backups and tested recovery; no audit gap on commit. |
| **NFR-06** | Security | TLS, OIDC/JWT or mTLS, RBAC, secret management, least-privilege DB access. |
| **NFR-07** | Observability | Metrics, traces and structured logs; alert on outbox lag and DB fallbacks. |
| **NFR-08** | Operability | Containerized build, migrations, rolling rollout, liveness/readiness checks. |
| **NFR-09** | Audit | Immutable application-level change records; retention per policy. |
| **NFR-10** | Scale isolation | Stateless API replicas; bounded connections and fallback concurrency. |

### Sizing assumptions used in this HLD

| Dimension | Working assumption | Implication |
|---|---|---|
| Flags | Up to 100,000 unique names | Indexed name lookup and paginated browsing. |
| Evaluation traffic | 10,000 requests/second, read-dominant | Keep hot path cache-oriented; autoscale replicas. |
| Administrative writes | Typically <100 mutations/second | Transactional writes can use primary database. |
| Overrides | Sparse but potentially large per flag | Separate relational table and bounded cache memory. |
| Region | Single primary region at launch | No active/active conflict-resolution requirement. |

> **Contract caveat:** The evaluation API guarantees an effective value based on the state it observes, not necessarily the latest committed state. Emergency shutdown requires a separate strict-consistency mode or stronger control path.

---

## 03 / Architecture

### System architecture and ownership

One codebase and one primary deployable service. The diagram separates the management and evaluation execution paths. The worker is a background component, not an independently required microservice.

```text
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot Application                     │
│                                                                 │
│  ┌──────────────┐  ┌────────────────────┐  ┌─────────────────┐  │
│  │ Management   │  │ Evaluation API     │  │ Outbox / Cache  │  │
│  │ REST API     │  │ (read path)        │  │ Sync Worker     │  │
│  └──────┬───────┘  └─────────┬──────────┘  └────────┬────────┘  │
│         │                    │                      │           │
│  ┌──────▼────────────────────▼──────────────────────▼────────┐  │
│  │ FeatureFlag / Override / Evaluation / Audit Services      │  │
│  └──────┬───────────────────────────────────┬────────────────┘  │
└─────────┼───────────────────────────────────┼───────────────────┘
          │                                   │
          ▼                                   ▼
   ┌─────────────┐                     ┌─────────────┐
   │ PostgreSQL  │                     │    Redis    │
   │ (source of  │                     │   cache)    │
   │  truth)     │                     │             │
   └─────────────┘                     └─────────────┘
```

### Component responsibilities

| Component | Responsibility |
|---|---|
| REST controllers | Request validation, authentication context, response/error contracts. |
| `FeatureFlagService` | Create/update/delete flags. |
| `FeatureOverrideService` | Create/update/remove per-user values; maintain `INHERIT` tombstones when removed. |
| `FeatureEvaluationService` | Apply override precedence; expose read-only decisions. |
| `AuditService` | Persist immutable `changeMap` records within the mutation transaction. |
| Cache service | Read-through metadata/override lookups with TTL and freshness timestamps. |
| Outbox worker | Poll, claim, retry and apply committed events idempotently to Redis. |

> **Deployment count:** 1 application service + PostgreSQL + Redis. The worker begins inside the Spring Boot application and can later become a separate deployment without adding a new business service.

---

## 04 / Data model

### Canonical entities

| Entity | Key attributes / constraints |
|---|---|
| `feature_flags` | `id` (**BIGINT** identity PK); `name`; `description`; `global_enabled` BOOLEAN; created/updated actor and time; **UNIQUE(`name`)**. No `project_id` / `environment` in V1. |
| `feature_overrides` | (`flag_id` **BIGINT** FK, `user_id`) composite PK; `enabled` BOOLEAN NULL; updated actor and time. `NULL` is an inheritance tombstone (override removed). |
| `feature_audits` | `id` (**BIGINT** identity PK); `flag_id` (**BIGINT**); `action`; `actor_id`; `created_at`; `request_id`; `change_map` JSONB. Append-only for application roles; index (`flag_id`, `created_at` DESC). No FK to flags so history is retained after delete. |
| `outbox_events` | `id` (**BIGINT** identity PK); `aggregate_id` (**BIGINT** flag id); `event_type`; `payload` JSONB; `created_at`; `processed_at`; `attempts`; pending-event index. |

### Evaluation decision table

| Global | User override | Result | Reason |
|---|---|---|---|
| `true` | absent / `NULL` | `true` | `GLOBAL` |
| `false` | absent / `NULL` | `false` | `GLOBAL` |
| `true` | `false` | `false` | `USER_OVERRIDE` |
| `false` | `true` | `true` | `USER_OVERRIDE` |
| — | flag not found | HTTP `404` | — |

> **Enablement model:** `global_enabled` is the default boolean. An explicit per-user override wins when present and non-null. There is no separate lifecycle `state` in V1.

### Key invariants

- Flag names are immutable after creation and unique globally.
- Surrogate keys are `BIGINT` identity values (API `id` / `flagId` fields are numbers, not UUIDs).
- Delete removes the flag (and its overrides); audit history is retained.
- User overrides do not store a complete user list inside the flag record.

---

## 05 / Interfaces

### Administrative endpoints (authenticated role-based access)

| Method | Route | Behavior |
|---|---|---|
| `POST` | `/v1/flags` | Create flag; 201, 409 duplicate name. |
| `GET` | `/v1/flags` | List flags; cursor/page-size filters. |
| `GET` | `/v1/flags/{name}` | Get flag by unique name. |
| `PATCH` | `/v1/flags/{name}` | Change description or `globalEnabled`. |
| `DELETE` | `/v1/flags/{name}` | Delete flag (overrides removed; audit retained). |
| `PUT` | `/v1/flags/{name}/overrides/{userId}` | Upsert explicit `true`/`false`. |
| `DELETE` | `/v1/flags/{name}/overrides/{userId}` | Remove override to inherit; retain `INHERIT` tombstone as needed for cache. |
| `GET` | `/v1/flags/{name}/overrides` | Paginated effective override records. |
| `GET` | `/v1/flags/{name}/audits` | Paginated immutable change records. |

### Runtime evaluation endpoint

```http
GET /v1/evaluations/new-checkout?userId=user-123
Authorization: Bearer <service-token>
```

```json
{
  "flag": "new-checkout",
  "userId": "user-123",
  "enabled": true,
  "reason": "USER_OVERRIDE"
}
```

Runtime evaluation is read-only and requires a trusted service credential. Flag identity is the unique `name` path parameter.

### Flag name validation

| Rule | Expectation |
|---|---|
| No spaces | Reject names containing whitespace. |
| Character set | Conservative pattern (e.g. lowercase alphanumeric, hyphens, underscores). |
| Length | Enforce reasonable min/max (aligned with schema `VARCHAR(150)`). |
| Immutability | Name is set at create time and not renamed via update. |

### Error behavior

| Status | When / client guidance |
|---|---|
| `400` | Malformed flag key or user ID (including spaces / length violations). |
| `401` / `403` | Missing identity or insufficient permission. |
| `404` | Unknown / non-existent flag in the caller's allowed scope. |
| `409` | Duplicate flag name on create. |
| `429` | Caller exceeded the applicable rate limit. |
| `503` | Authoritative lookup unavailable and no usable cache entry; SDK uses configured safe default. |

Use a global `@RestControllerAdvice` so error bodies are consistent across management and evaluation APIs.

---

## 06 / Critical flow A — Read path and cache consistency

### Evaluation path architecture

The diagram below is a required deliverable for the service: it must show the request evaluation path, where Redis sits, when it is consulted, and when it is bypassed or updated/invalidated.

```mermaid
flowchart LR
  Client[Client / SDK] -->|GET /api/v1/evaluations/{flag}?userId=...| API[Evaluation API]
  API --> Auth[AuthN + validation]
  Auth --> CacheMeta[Redis<br/>flag metadata]
  CacheMeta -->|hit| Exists{Flag found?}
  CacheMeta -->|miss| PGMeta[(PostgreSQL)]
  PGMeta -->|populate cache| CacheMeta
  Exists -->|no| NotFound[404 Not Found]
  Exists -->|yes| CacheOvr[Redis<br/>user override]
  CacheOvr -->|TRUE / FALSE| ResultOvr[Return override]
  CacheOvr -->|INHERIT / miss→INHERIT| ResultGlobal[Return global_enabled]
  CacheOvr -->|Redis unavailable| PGOvr[(PostgreSQL)<br/>bounded fallback]
  PGOvr --> ResultOvr
  PGOvr --> ResultGlobal
```

| Situation | Cache behavior |
|---|---|
| Warm evaluation, Redis healthy | Consult Redis for metadata, then override (or negative `INHERIT`). |
| Cache miss | Read PostgreSQL, populate Redis with entry + `updatedAt` + TTL, continue evaluation. |
| Flag missing | Return `404`; do not invent a disabled value. |
| Redis unavailable | Bypass Redis; bounded read-through to PostgreSQL with rate limiting / circuit breaker. |
| Admin mutation committed | Do not require synchronous Redis write in the request path; insert outbox event atomically with DB + audit. |
| Outbox worker processes event | Timestamp-aware Redis update / invalidation; reject stale `updatedAt` values. |
| Override removed | Store `INHERIT` tombstone in DB and cache so old values cannot resurrect until TTL/outbox catches up. |
| TTL expiry | Entry expires; next read is a miss and reloads from PostgreSQL. |

### Read-through evaluation algorithm

| Step | Process |
|---|---|
| 1 | Authenticate; validate flag name and user ID. |
| 2 | `GET ff:flag:{name}`; cache miss → read PostgreSQL primary and populate with `updatedAt`. |
| 3 | If missing flag → `404`. |
| 4 | `GET ff:override:{flagId}:{userId}` (`flagId` is the BIGINT surrogate key); cache miss → read database and populate `TRUE`/`FALSE`/`INHERIT`. |
| 5 | If override is `TRUE` or `FALSE` → use it. Otherwise use `global_enabled`. Return reason and value. |

### Cache contract

| Entry | Model | Starting TTL |
|---|---|---|
| Flag metadata | `{flagId (BIGINT), globalEnabled, updatedAt}`; cached by name. | 30 s |
| User override | `{value: TRUE\|FALSE\|INHERIT, updatedAt}`; cached per flag id and user id. | 30 s |
| Absent override | `INHERIT`; explicit negative cache entry. | 15 s |

Use atomic compare-and-set (Lua script or equivalent): apply a cache update only if the incoming `updatedAt` is newer than the cached value. Store override removals as `INHERIT` tombstones in PostgreSQL and cache. TTL expiry can still permit short stale windows; the design is intentionally eventually consistent.

### Cache failure and freshness

- **Redis unavailable:** bounded read-through to PostgreSQL; apply rate limits and circuit-breaking to avoid cascading DB overload.
- **Database unavailable, cache hit:** optionally return an unexpired cached decision under a documented staleness policy.
- **Both unavailable or cache miss without DB:** return 503; applications apply their configured fail-safe fallback.
- **Outbox event delay:** stale reads are possible until propagation or TTL-based reload; track lag against the freshness SLO.

> **Correctness limitation:** Individual metadata and override cache reads are not an atomic snapshot. Version-aware writes prevent many stale-overwrite races but do not provide linearizable evaluations.

---

## 07 / Critical flow B — Write path, audit and outbox

### Transactional mutation protocol

| Order | Action | Atomicity |
|---|---|---|
| 1 | Authorize role. | Before transaction |
| 2 | `BEGIN`; load current entity; validate and compute `changeMap`. | PostgreSQL transaction |
| 3 | Update flag or override. | Same transaction |
| 4 | Insert append-only audit event containing actor, time and previous/new fields. | Same transaction |
| 5 | Insert outbox event for cache refresh/invalidation. | Same transaction |
| 6 | `COMMIT`; send success. Worker claims event, retries and conditionally updates cache. | Async after commit |

### Sample audit event

```json
{
  "action": "FLAG_UPDATED",
  "actorId": "admin-104",
  "changedAt": "2026-10-08T10:30:00Z",
  "requestId": "req-7f80",
  "changeMap": {
    "globalEnabled": { "previous": false, "new": true }
  }
}
```

### Worker delivery guarantees

- Polling workers claim records using `SKIP LOCKED` or an equivalent lease to avoid duplicated in-flight work.
- Retries are safe because cache updates are idempotent and compare incoming `updatedAt` values.
- Only mark an outbox event processed after successful cache application; alert on retries and oldest-event age.
- Audit is written in the same transaction as business changes; failure of either write rolls back the mutation.

> **Concurrency note:** V1 has no optimistic-lock `version` field. Concurrent admin edits use last-commit-wins; audit still records each committed change.

---

## 08 / Operations

### Security model

| Control | Design |
|---|---|
| Authentication | OIDC/OAuth2 JWT for admins/service callers; mTLS where required. |
| Authorization | RBAC: `FLAG_READ`, `FLAG_WRITE`, `OVERRIDE_WRITE`, `AUDIT_READ`; scope enforced server-side. |
| Data protection | TLS end to end; encryption at rest; hashed user identifiers in cache keys; audit access restricted. |
| Secrets | Managed secret storage; rotate credentials; never embed tokens in code or logs. |
| Audit integrity | Append-only application permissions; backups, retention and export controls. |

### Failure-mode expectations

| Incident | Expected behavior |
|---|---|
| Redis down | DB fallback under a concurrency/rate limit; degrade gracefully rather than saturate DB. |
| PostgreSQL down | Serve unexpired cache hits only if policy allows; otherwise 503. Writes fail closed. |
| Outbox worker down | Writes may continue and queue outbox records; freshness SLO at risk; alert immediately. |
| Cache event reordered | Reject older `updatedAt` values; retain newer cached values. |
| Audit insert fails | Database transaction rolls back; no unlogged committed change. |
| Pod/instance fails | Other stateless replicas continue behind load balancer; in-flight requests retry safely. |

### Golden signals and alerts

Measure evaluation request rate, p50/p95/p99 latency, error rate, cache hit ratio, DB fallback QPS, Redis timeouts, PostgreSQL pool saturation, outbox lag, oldest pending outbox age, and audit insertion failures.

> **Key alert:** Growing outbox lag indicates that committed flags may not have reached the cache; expose a freshness dashboard as part of production readiness.

---

## 09 / Platform

### Recommended DigitalOcean mapping

| Workload | DigitalOcean service | Notes |
|---|---|---|
| Spring Boot API | DigitalOcean Kubernetes (DOKS) | 2+ application replicas; readiness, HPA and rolling releases. |
| Primary relational store | Managed PostgreSQL | Private networking, backup/PITR as configured, constrained connection pool. |
| Cache | Managed Redis | Use TLS and trusted-source restrictions; cache treated as disposable. |
| Load balancing | DOKS ingress / load balancer | Terminate/reencrypt TLS; protect admin routes. |
| Background sync | Spring Boot scheduled worker initially | Split into independent Kubernetes worker deployment when scale warrants. |
| Observability | Micrometer/Actuator + metrics backend | Export service metrics, logs and traces; alert on outbox age. |

### Initial scaling strategy

- Scale stateless API pods based on CPU, request rate and p99 latency; keep evaluation workloads isolated via application-level bulkheads.
- Keep PostgreSQL pool sizes small and calculate total connections across all pods and workers; consider a pooler at scale.
- Apply jittered TTLs; protect against cache stampedes with per-key single flight / short mutex where practical.
- Use load testing with warm, cold and high-cardinality override traffic; avoid assuming a 10k RPS target is met without proof.
- Run Flyway migrations in controlled CI/CD before application rollout; preserve backward-compatible schema changes.

### Primary deployment decision

> **Start simple:** One business application is easier to deploy, test and audit. Split management and evaluation into two deployments only if independent traffic, SLOs or team ownership justify it.

Platform notes: Managed Redis with TLS and trusted-source restrictions is available on DigitalOcean. DOKS is a managed Kubernetes offering. Links appear in References.

---

## 10 / Delivery

### Final readiness expectations (required for the service)

These are mandatory for considering the service interview-ready / demo-ready:

| # | Expectation | Detail |
|---|---|---|
| 1 | **Architecture diagram** | Document the evaluation request path, where Redis sits, when it is consulted, and when it is bypassed or invalidated/updated (see §06). Keep the diagram in the README and HLD. |
| 2 | **Validation & error handling** | Validate flag names (no spaces, reasonable length limits). Return appropriate status codes (`400`, `404` for non-existent flags, `409` duplicate name, `503`, etc.) via consistent global error handling. |
| 3 | **Tests** | Unit tests for evaluation precedence and core rules, plus at least one HTTP end-to-end integration test. |
| 4 | **CI/CD** | GitHub Actions workflow that builds and runs `./gradlew test` on pull requests and main. |
| 5 | **Documentation** | `README.md` covering local setup, flag evaluation rules, and how caching is implemented (keys, TTLs, read-through, outbox sync, Redis-down behavior). |

### Release acceptance checklist

| Area | Pass criterion |
|---|---|
| CRUD | Unique flag `name` enforced; delete removes the flag while audit history is retained. |
| Precedence | Evaluation cases in the decision table return expected values. |
| Override removal | `TRUE` or `FALSE` → `INHERIT`; old values do not immediately resurrect in cache. |
| Audit | For every committed change, one complete audit event exists with actor and `changeMap`. |
| Reliability | Outbox worker retries and does not corrupt newer cached values. |
| Resilience | Failure-injection tests cover Redis, DB and worker outages. |
| Performance | Load test verifies cache-hit latency and DB fallback stability at selected load. |
| Security | Unauthenticated and cross-scope requests are denied. |
| Observability | Dashboards and alerts expose cache hit ratio, errors, outbox lag and saturation. |
| Validation | Invalid flag names rejected; non-existent flags return `404`. |
| Tests & CI | Unit + HTTP e2e coverage green in GitHub Actions. |
| README | Setup, evaluation rules, and caching behavior documented. |

### Main risks / decisions to revisit

- **Cache freshness vs. latency:** strict emergency disable needs a distinct strongly consistent path.
- **Override cardinality:** predominantly one-off user lookups may make per-user negative cache inefficient.
- **Audit retention and privacy:** agree retention, export, access and user-ID pseudonymization policy.
- **Multi-tenancy / lifecycle:** V1 has no `project_id`, `environment`, or `state`; add later if product needs isolation or draft/archive workflows.
- **Optimistic locking:** V1 is last-commit-wins; add a `version` column later if concurrent admin edits become painful.

### Official platform references

- [DigitalOcean Managed Redis](https://docs.digitalocean.com/products/databases/redis/)
- [DigitalOcean Kubernetes](https://docs.digitalocean.com/products/kubernetes/)

### Interview closing statement

The design prioritizes correctness at write time, low-latency reads, explicit eventual consistency and operational simplicity. Four logical Spring Boot services operate within one application, backed by PostgreSQL and Redis caching, with a durable outbox worker for synchronization.

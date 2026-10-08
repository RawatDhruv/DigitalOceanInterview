# Feature Management Service — API Test Report

**Generated:** `2026-10-08T08:56:00.799467+00:00`  
**Updated:** `2026-10-08T09:03:58.338569+00:00`  
**Base URL:** `http://localhost:8080`  
**Test flag:** `api-test-412810eb`  

## Summary

| Metric | Value |
| --- | --- |
| Total tests | 55 |
| Passed | 55 |
| Failed | 0 |
| Pass rate | 100.0% |

### By category

| Category | Total | Passed | Failed |
| --- | --- | --- | --- |
| Audits | 3 | 3 | 0 |
| Cache | 2 | 2 | 0 |
| Evaluations | 11 | 11 | 0 |
| Flags | 14 | 14 | 0 |
| Ops | 6 | 6 | 0 |
| Overrides | 8 | 8 | 0 |
| SeededFeatures | 11 | 11 | 0 |

All tests passed. Prior Ops/Flags failures were harness issues (Accept: application/json on prometheus; unencoded spaces in flag path).

---

## 1. [PASS] Actuator health

- **Category:** Ops
- **Elapsed:** 55.06 ms

### Request

```http
GET /actuator/health HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:55:59 GMT

{
  "groups": [
    "liveness",
    "readiness"
  ],
  "status": "UP"
}
```

---

## 2. [PASS] Actuator info

- **Category:** Ops
- **Elapsed:** 22.13 ms

### Request

```http
GET /actuator/info HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 2
Date: Thu, 08 Oct 2026 08:56:00 GMT

{}
```

---

## 3. [PASS] Prometheus metrics scrape

- **Category:** Ops

### Request

```http
GET /actuator/prometheus HTTP/1.1
Host: localhost:8080
Accept: text/plain

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: text/plain;version=0.0.4;charset=utf-8
Content-Length: 53709

# HELP application_ready_time_seconds Time taken for the application to be ready to service requests
# TYPE application_ready_time_seconds gauge
application_ready_time_seconds{application="feature-management-service",main_application_class="com.example.featuremanagement.FeatureManagementServiceApplication"} 5.546
# HELP application_started_time_seconds Time taken to start the application
# TYPE application_started_time_seconds gauge
application_started_time_seconds{application="feature-management-service",main_application_class="com.example.featuremanagement.FeatureManagementServiceApplication"} 5.488
# HELP disk_free_bytes Usable space for path
# TYPE disk_free_bytes gauge
disk_free_bytes{application="feature-management-service",path="/workspaces/feature-management-service/."} 3.97711147008E11
# HELP disk_total_bytes Total space for path
# TYPE disk_total_bytes gauge
disk_total_bytes{application="feature-management-service",path="/workspaces/feature-management-service/."} 4.11645771776E11
# HELP executor_active_threads The approximate number of threads that are actively executing tasks
# TYPE executor_active_threads gauge
executor_active_threads{application="feature-management-service",name="applicationTaskExecutor"} 0.0
# HELP executor_completed_tasks_total The approximate total number of tasks that have completed execution
# TYPE executor_completed_tasks_total counter
executor_completed_tasks_total{application="feature-management-service",name="applicationTaskExecutor"} 0.0
# HELP executor_pool_core_threads The core number of threads for the pool
# TYPE executor_pool_core_threads gauge
executor_pool_core_threads{application="feature-management-service",name="applicationTaskExecutor"} 8.0
# HELP executor_pool_max_threads The maximum allowed number of threads in the pool
# TYPE executor_pool_max_threads gauge
executor_pool_max_threads{application="feature-management-service",name="applicationTaskExecutor"} 2.147483647E9
# HELP executor_pool_size_threads The current
... [truncated] ...
```

---

## 4. [PASS] OpenAPI YAML served

- **Category:** Ops
- **Elapsed:** 18.2 ms

### Request

```http
GET /openapi.yaml HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/octet-stream
Content-Length: 17425
Date: Thu, 08 Oct 2026 08:56:00 GMT

openapi: 3.0.3
info:
  title: Feature Management Service API
  description: |
    Boolean feature flags with global defaults, per-user overrides, evaluation,
    and an immutable audit trail.

    - Surrogate IDs are **BIGINT** (JSON numbers), not UUIDs.
    - Actor attribution uses `X-Actor-Id` (default `local-admin`). Spring Security is deferred.
    - Create supports idempotency via `Idempotency-Key` or `X-Request-Id`.
  version: 0.0.1
  contact:
    name: Feature Management Service

servers:
  - url: http://localhost:8080
    description: Local development

tags:
  - name: Flags
    description: Feature flag CRUD
  - name: Overrides
    description: Per-user targeting overrides
  - name: Evaluations
    description: Runtime flag evaluation
  - name: Audits
    description: Immutable change history

paths:
  /api/v1/flags:
    get:
      tags: [Flags]
      summary: List feature flags
      operationId: listFlags
      parameters:
        - $ref: '#/components/parameters/Page'
        - $ref: '#/components/parameters/Size'
        - $ref: '#/components/parameters/Sort'
      responses:
        '200':
          description: Paginated flags
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FeatureFlagPage'
        '400':
          $ref: '#/components/responses/BadRequest'
    post:
      tags: [Flags]
      summary: Create a feature flag
      operationId: createFlag
      parameters:
        - $ref: '#/components/parameters/ActorId'
        - $ref: '#/components/parameters/IdempotencyKey'
        - $ref: '#/components/parameters/RequestId'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateFlagRequest'
            example:
              name: new-checkout
              description: Checkout redesign
              globalEnabled: false
              state: ACTIVE
      responses:
        '201':
          description: Flag created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FeatureFlagResponse'
        '200':
          description: Idempotent replay of a prior create with the same key
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FeatureFlagResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '409':
          $ref: '#/components/responses/Conflict'
        '503':
          $ref: '#/components/responses/ServiceUnavailable'

  /api/v1/flags/{name}:
    parameters:
      - $ref: '#/components/parameters/FlagName'
    get:
      tags: [Flags]
      summary: Get a feature flag by name
      operationId: getFlag
      responses:
        '200':
          description: Flag found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FeatureFlagResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '404':
          $ref: '#/components/responses/NotFound'
    patch:
      tags: [Flags]
      summary: Partially update a feature flag
      operationId: updateFlag
      parameters:
        - $ref: '#/components/parameters/ActorId'
        - $ref: '#/components/parameters/RequestId'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateFlagRequest'
            example:
              description: Updated checkout
              globalEnabled: true
      responses:
        '200':
          description: Flag updated
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FeatureFlagResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '404':
          $ref: '#/components/responses/NotFound'
        '409':
          description: Optimistic lock conflict
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '503':
          $ref: '#/components/responses/ServiceUnavailable'
    delete:
      tags: [Flags]
      summary: Delete a feature flag
      operationId: deleteFlag
      parameters:
        - $ref: '#/components/parameters/ActorId'
        - $ref: '#/components/parameters/RequestId'
      responses:
        '204':
          description: Flag deleted (overrides cascade; audit history retained)
        '400':
          $ref: '#/components/responses/BadRequest'
        '404':
          $ref: '#/components/responses/NotFound'
        '503':
          $ref: '#/components/responses/ServiceUnavailable'

  /api/v1/flags/{name}/overrides:
    parameters:
      - $ref: '#/components/parameters/FlagName'
    get:
      tags: [Overrides]
      summary: List active overrides for a flag
      operationId: listOverrides
      parameters:
        - $ref: '#/components/parameters/Page'
        - $ref: '#/components/parameters/Size'
        - $ref: '#/components/parameters/Sort'
      responses:
        '200':
          description: Paginated active overrides (`enabled` is non-null)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FeatureOverridePage'
        '400':
          $ref: '#/components/responses/BadRequest'
        '404':
          $ref: '#/components/responses/NotFound'

  /api/v1/flags/{name}/overrides/{userId}:
    parameters:
      - $ref: '#/components/parameters/FlagName'
      - $ref: '#/components/parameters/UserId'
    put:
      tags: [Overrides]
      summary: Upsert a user override
      operationId: upsertOverride
      parameters:
        - $ref: '#/components/parameters/ActorId'
        - $ref: '#/components/parameters/RequestId'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpsertOverrideRequest'
            example:
              enabled: false
      responses:
        '200':
          description: Override upserted
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FeatureOverrideResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '404':
          $ref: '#/components/responses/NotFound'
        '503':
          $ref: '#/components/responses/ServiceUnavailable'
    delete:
      tags: [Overrides]
      summary: Remove a user override (INHERIT tombstone)
      operationId: removeOverride
      parameters:
        - $ref: '#/components/parameters/ActorId'
        - $ref: '#/components/parameters/RequestId'
      responses:
        '204':
          description: Override removed; evaluation falls back to global
        '400':
          $ref: '#/components/responses/BadRequest'
        '404':
          $ref: '#/components/responses/NotFound'
        '503':
          $ref: '#/components/responses/ServiceUnavailable'

  /api/v1/flags/{name}/audits:
    parameters:
      - $ref: '#/components/parameters/FlagName'
    get:
      tags: [Audits]
      summary: List audit events for a flag
      operationId: listAudits
      parameters:
        - $ref: '#/components/parameters/Page'
        - $ref: '#/components/parameters/Size'
        - $ref: '#/components/parameters/Sort'
      responses:
        '200':
          description: Paginated audit records (newest first by default)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FeatureAuditPage'
        '400':
          $ref: '#/components/responses/BadRequest'
        '404':
          $ref: '#/components/responses/NotFound'

  /api/v1/evaluations/{flagName}:
    get:
      tags: [Evaluations]
      summary: Evaluate a flag for a user
      description: |
        Precedence: explicit non-null user override (`USER_OVERRIDE`) wins over
        `globalEnabled` (`GLOBAL`). Missing or null override inherits global.
      operationId: evaluateFlag
      parameters:
        - name: flagName
          in: path
          required: true
          schema:
            $ref: '#/components/schemas/FlagName'
        - name: userId
          in: query
          required: true
          schema:
            type: string
            minLength: 1
            maxLength: 150
          example: user-123
      responses:
        '200':
          description: Evaluation result
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EvaluationResponse'
              examples:
                global:
                  value:
                    flag: new-checkout
                    userId: user-123
                    enabled: true
                    reason: GLOBAL
                override:
                  value:
                    flag: new-checkout
                    userId: user-123
                    enabled: false
                    reason: USER_OVERRIDE
        '400':
          $ref: '#/components/responses/BadRequest'
        '404':
          $ref: '#/components/responses/NotFound'
        '503':
          $ref: '#/components/responses/ServiceUnavailable'

components:
  parameters:
    FlagName:
      name: name
      in: path
      required: true
      schema:
        $ref: '#/components/schemas/FlagName'
    UserId:
      name: userId
      in: path
      required: true
      schema:
        type: string
        minLength: 1
        maxLength: 150
      example: user-123
    ActorId:
      name: X-Actor-Id
      in: header
      required: false
      description: Actor performing the mutation (defaults to `local-admin`)
      schema:
        type: string
        default: local-admin
        maxLength: 100
    RequestId:
      name: X-Request-Id
      in: header
      required: false
      description: Optional request correlation id stored on audit records
      schema:
        type: string
        maxLength: 100
    IdempotencyKey:
      name: Idempotency-Key
      in: header
      required: false
      description: |
        Optional create idempotency key. Retries with the same key return the
        original flag with HTTP 200 instead of creating a duplicate.
      schema:
        type: string
        maxLength: 100
    Page:
      name: page
      in: query
      required: false
      schema:
        type: integer
        minimum: 0
        default: 0
    Size:
      name: size
      in: query
      required: false
      schema:
        type: integer
        minimum: 1
        default: 20
    Sort:
      name: sort
      in: query
      required: false
      description: Spring Data sort (`property,asc|desc`)
      schema:
        type: string
        example: name,asc

  responses:
    BadRequest:
      description: Invalid input
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    NotFound:
      description: Flag not found
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    Conflict:
      description: Duplicate flag name or concurrent update conflict
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    ServiceUnavailable:
      description: Data store or evaluation fallback unavailable
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'

  schemas:
    FlagName:
      type: string
      minLength: 1
      maxLength: 150
      pattern: '^[a-z0-9]+(?:[_-][a-z0-9]+)*$'
      description: Lowercase alphanumeric with optional hyphens/underscores
      example: new-checkout

    FlagState:
      type: string
      enum: [DRAFT, ACTIVE, ARCHIVED]

    EvaluationReason:
      type: string
      enum: [GLOBAL, USER_OVERRIDE]

    CreateFlagRequest:
      type: object
      required: [name, globalEnabled]
      properties:
        name:
          $ref: '#/components/schemas/FlagName'
        description:
          type: string
          maxLength: 2000
          nullable: true
        globalEnabled:
          type: boolean
        state:
          allOf:
            - $ref: '#/components/schemas/FlagState'
          nullable: true
          description: Defaults to DRAFT when omitted

    UpdateFlagRequest:
      type: object
      properties:
        description:
          type: string
          maxLength: 2000
          nullable: true
        globalEnabled:
          type: boolean
          nullable: true
        state:
          allOf:
            - $ref: '#/components/schemas/FlagState'
          nullable: true

    UpsertOverrideRequest:
      type: object
      required: [enabled]
      properties:
        enabled:
          type: boolean

    FeatureFlagResponse:
      type: object
      required:
        - id
        - name
        - globalEnabled
        - state
        - version
        - createdBy
        - createdAt
        - updatedBy
        - updatedAt
      properties:
        id:
          type: integer
          format: int64
          description: BIGINT identity
        name:
          $ref: '#/components/schemas/FlagName'
        description:
          type: string
          nullable: true
        globalEnabled:
          type: boolean
        state:
          $ref: '#/components/schemas/FlagState'
        version:
          type: integer
          format: int64
        createdBy:
          type: string
        createdAt:
          type: string
          format: date-time
        updatedBy:
          type: string
        updatedAt:
          type: string
          format: date-time

    FeatureOverrideResponse:
      type: object
      required: [flagId, flagName, userId, updatedBy, updatedAt]
      properties:
        flagId:
          type: integer
          format: int64
        flagName:
          $ref: '#/components/schemas/FlagName'
        userId:
          type: string
        enabled:
          type: boolean
          nullable: true
          description: Null means INHERIT tombstone (not returned in list of active overrides)
        updatedBy:
          type: string
        updatedAt:
          type: string
          format: date-time

    FeatureAuditResponse:
      type: object
      required: [id, flagId, action, actorId, changeMap, createdAt]
      properties:
        id:
          type: integer
          format: int64
        flagId:
          type: integer
          format: int64
        action:
          type: string
          example: FLAG_UPDATED
        actorId:
          type: string
        changeMap:
          type: object
          additionalProperties:
            type: object
            properties:
              previous:
                description: Previous field value (any JSON type)
              new:
                description: New field value (any JSON type)
          example:
            globalEnabled:
              previous: false
              new: true
        requestId:
          type: string
          nullable: true
        createdAt:
          type: string
          format: date-time

    EvaluationResponse:
      type: object
      required: [flag, userId, enabled, reason]
      properties:
        flag:
          $ref: '#/components/schemas/FlagName'
        userId:
          type: string
        enabled:
          type: boolean
        reason:
          $ref: '#/components/schemas/EvaluationReason'

    ErrorResponse:
      type: object
      required: [timestamp, status, error, message, path]
      properties:
        timestamp:
          type: string
          format: date-time
        status:
          type: integer
          example: 404
        error:
          type: string
          example: Not Found
        message:
          type: string
        path:
          type: string

    PageMeta:
      type: object
      properties:
        page:
          type: integer
        size:
          type: integer
        totalElements:
          type: integer
          format: int64
        totalPages:
          type: integer

    FeatureFlagPage:
      type: object
      properties:
        content:
          type: array
          items:
            $ref: '#/components/schemas/FeatureFlagResponse'
        page:
          $ref: '#/components/schemas/PageMeta'
        totalElements:
          type: integer
          format: int64
        totalPages:
          type: integer
        size:
          type: integer
        number:
          type: integer

    FeatureOverridePage:
      type: object
      properties:
        content:
          type: array
          items:
            $ref: '#/components/schemas/FeatureOverrideResponse'
        totalElements:
          type: integer
          format: int64
        totalPages:
          type: integer
        size:
          type: integer
        number:
          type: integer

    FeatureAuditPage:
      type: object
      properties:
        content:
          type: array
          items:
            $ref: '#/components/schemas/FeatureAuditResponse'
        totalElements:
          type: integer
          format: int64
        totalPages:
          type: integer
        size:
          type: integer
        number:
          type: integer

```

---

## 5. [PASS] List flags (seeded)

- **Category:** Flags
- **Elapsed:** 16.41 ms

### Request

```http
GET /api/v1/flags?page=0&size=20&sort=name,asc HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 1541
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "content": [
    {
      "id": 3,
      "name": "ai-assistant",
      "description": "In-product AI assistant",
      "globalEnabled": true,
      "state": "ACTIVE",
      "version": 0,
      "createdBy": "seed",
      "createdAt": "2026-10-08T08:47:23.380906Z",
      "updatedBy": "seed",
      "updatedAt": "2026-10-08T08:47:23.380906Z"
    },
    {
      "id": 1,
      "name": "beta-dashboard",
      "description": "Beta dashboard experience",
      "globalEnabled": true,
      "state": "ACTIVE",
      "version": 0,
      "createdBy": "seed",
      "createdAt": "2026-10-08T08:47:23.380906Z",
      "updatedBy": "seed",
      "updatedAt": "2026-10-08T08:47:23.380906Z"
    },
    {
      "id": 4,
      "name": "mobile-push",
      "description": "Mobile push notifications",
      "globalEnabled": false,
      "state": "DRAFT",
      "version": 0,
      "createdBy": "seed",
      "createdAt": "2026-10-08T08:47:23.380906Z",
      "updatedBy": "seed",
      "updatedAt": "2026-10-08T08:47:23.380906Z"
    },
    {
      "id": 2,
      "name": "new-billing",
      "description": "New billing checkout flow",
      "globalEnabled": false,
      "state": "ACTIVE",
      "version": 0,
      "createdBy": "seed",
      "createdAt": "2026-10-08T08:47:23.380906Z",
      "updatedBy": "seed",
      "updatedAt": "2026-10-08T08:47:23.380906Z"
    },
    {
      "id": 5,
      "name": "referral-bonus",
      "description": "Referral bonus campaign",
      "globalEnabled": true,
      "state": "ACTIVE",
      "version": 0,
      "createdBy": "seed",
      "createdAt": "2026-10-08T08:47:23.380906Z",
      "updatedBy": "seed",
      "updatedAt": "2026-10-08T08:47:23.380906Z"
    }
  ],
  "empty": false,
  "first": true,
  "last": true,
  "number": 0,
  "numberOfElements": 5,
  "pageable": {
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 20,
    "paged": true,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "unpaged": false
  },
  "size": 20,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "totalElements": 5,
  "totalPages": 1
}
```

---

## 6. [PASS] Get seeded flag beta-dashboard

- **Category:** Flags
- **Elapsed:** 69.2 ms

### Request

```http
GET /api/v1/flags/beta-dashboard HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 246
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "id": 1,
  "name": "beta-dashboard",
  "description": "Beta dashboard experience",
  "globalEnabled": true,
  "state": "ACTIVE",
  "version": 0,
  "createdBy": "seed",
  "createdAt": "2026-10-08T08:47:23.380906Z",
  "updatedBy": "seed",
  "updatedAt": "2026-10-08T08:47:23.380906Z"
}
```

---

## 7. [PASS] Get unknown flag → 404

- **Category:** Flags
- **Elapsed:** 8.34 ms

### Request

```http
GET /api/v1/flags/does-not-exist-zzz HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 404
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.130710458Z",
  "status": 404,
  "error": "Not Found",
  "message": "Feature flag not found: does-not-exist-zzz",
  "path": "/api/v1/flags/does-not-exist-zzz"
}
```

---

## 8. [PASS] Get invalid flag name (URL-encoded) → 400

- **Category:** Flags

### Request

```http
GET /api/v1/flags/Invalid%20Name%21 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 400
Content-Type: application/json

{
  "timestamp": "2026-10-08T09:03:58.332485129Z",
  "status": 400,
  "error": "Bad Request",
  "message": "get.name: must match \"^[a-z0-9]+(?:[_-][a-z0-9]+)*$\"",
  "path": "/api/v1/flags/Invalid%20Name%21"
}
```

---

## 9. [PASS] Create flag

- **Category:** Flags
- **Elapsed:** 237.91 ms

### Request

```http
POST /api/v1/flags HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
Idempotency-Key: idem-9b6dc2f92b68
X-Request-Id: req-73530f01
Content-Type: application/json

{
  "name": "api-test-412810eb",
  "description": "API test flag created by automated suite",
  "globalEnabled": false,
  "state": "ACTIVE"
}
```

### Response

```http
HTTP/1.1 201
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "id": 6,
  "name": "api-test-412810eb",
  "description": "API test flag created by automated suite",
  "globalEnabled": false,
  "state": "ACTIVE",
  "version": 0,
  "createdBy": "api-tester",
  "createdAt": "2026-10-08T08:56:00.216440636Z",
  "updatedBy": "api-tester",
  "updatedAt": "2026-10-08T08:56:00.216440636Z"
}
```

---

## 10. [PASS] Create flag idempotent replay → 200

- **Category:** Flags
- **Elapsed:** 30.88 ms

### Request

```http
POST /api/v1/flags HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
Idempotency-Key: idem-9b6dc2f92b68
Content-Type: application/json

{
  "name": "api-test-412810eb",
  "description": "API test flag created by automated suite",
  "globalEnabled": false,
  "state": "ACTIVE"
}
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "id": 6,
  "name": "api-test-412810eb",
  "description": "API test flag created by automated suite",
  "globalEnabled": false,
  "state": "ACTIVE",
  "version": 0,
  "createdBy": "api-tester",
  "createdAt": "2026-10-08T08:56:00.216441Z",
  "updatedBy": "api-tester",
  "updatedAt": "2026-10-08T08:56:00.216441Z"
}
```

---

## 11. [PASS] Create duplicate flag name → 409

- **Category:** Flags
- **Elapsed:** 7.22 ms

### Request

```http
POST /api/v1/flags HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
Idempotency-Key: other-8794dd95
Content-Type: application/json

{
  "name": "api-test-412810eb",
  "description": "dup",
  "globalEnabled": false,
  "state": "ACTIVE"
}
```

### Response

```http
HTTP/1.1 409
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.407379953Z",
  "status": 409,
  "error": "Conflict",
  "message": "Feature flag already exists: api-test-412810eb",
  "path": "/api/v1/flags"
}
```

---

## 12. [PASS] Create flag missing globalEnabled → 400

- **Category:** Flags
- **Elapsed:** 13.73 ms

### Request

```http
POST /api/v1/flags HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
Content-Type: application/json

{
  "name": "bad-flag-missing-enabled"
}
```

### Response

```http
HTTP/1.1 400
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.420106705Z",
  "status": 400,
  "error": "Bad Request",
  "message": "globalEnabled: must not be null",
  "path": "/api/v1/flags"
}
```

---

## 13. [PASS] Get created flag api-test-412810eb

- **Category:** Flags
- **Elapsed:** 4.55 ms

### Request

```http
GET /api/v1/flags/api-test-412810eb HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 277
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "id": 6,
  "name": "api-test-412810eb",
  "description": "API test flag created by automated suite",
  "globalEnabled": false,
  "state": "ACTIVE",
  "version": 0,
  "createdBy": "api-tester",
  "createdAt": "2026-10-08T08:56:00.216441Z",
  "updatedBy": "api-tester",
  "updatedAt": "2026-10-08T08:56:00.216441Z"
}
```

---

## 14. [PASS] Patch flag (enable + description)

- **Category:** Flags
- **Elapsed:** 26.55 ms

### Request

```http
PATCH /api/v1/flags/api-test-412810eb HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
X-Request-Id: patch-1
Content-Type: application/json

{
  "globalEnabled": true,
  "description": "Updated by API test suite",
  "state": "ACTIVE"
}
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 264
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "id": 6,
  "name": "api-test-412810eb",
  "description": "Updated by API test suite",
  "globalEnabled": true,
  "state": "ACTIVE",
  "version": 1,
  "createdBy": "api-tester",
  "createdAt": "2026-10-08T08:56:00.216441Z",
  "updatedBy": "api-tester",
  "updatedAt": "2026-10-08T08:56:00.435921124Z"
}
```

---

## 15. [PASS] Patch unknown flag → 404

- **Category:** Flags
- **Elapsed:** 5.85 ms

### Request

```http
PATCH /api/v1/flags/no-such-flag-xyz HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
Content-Type: application/json

{
  "globalEnabled": true
}
```

### Response

```http
HTTP/1.1 404
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.458460085Z",
  "status": 404,
  "error": "Not Found",
  "message": "Feature flag not found: no-such-flag-xyz",
  "path": "/api/v1/flags/no-such-flag-xyz"
}
```

---

## 16. [PASS] Evaluate GLOBAL (no override, global true)

- **Category:** Evaluations
- **Elapsed:** 23.88 ms

### Request

```http
GET /api/v1/evaluations/api-test-412810eb?userId=test-user-api HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 86
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "api-test-412810eb",
  "userId": "test-user-api",
  "enabled": true,
  "reason": "GLOBAL"
}
```

---

## 17. [PASS] Evaluate seeded beta-dashboard GLOBAL for stranger

- **Category:** Evaluations
- **Elapsed:** 10.31 ms

### Request

```http
GET /api/v1/evaluations/beta-dashboard?userId=stranger-999 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 82
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "beta-dashboard",
  "userId": "stranger-999",
  "enabled": true,
  "reason": "GLOBAL"
}
```

---

## 18. [PASS] Evaluate seeded beta-dashboard USER_OVERRIDE user-001

- **Category:** Evaluations
- **Elapsed:** 11.8 ms

### Request

```http
GET /api/v1/evaluations/beta-dashboard?userId=user-001 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 86
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "beta-dashboard",
  "userId": "user-001",
  "enabled": false,
  "reason": "USER_OVERRIDE"
}
```

---

## 19. [PASS] Evaluate seeded beta-dashboard USER_OVERRIDE user-002

- **Category:** Evaluations
- **Elapsed:** 9.0 ms

### Request

```http
GET /api/v1/evaluations/beta-dashboard?userId=user-002 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 85
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "beta-dashboard",
  "userId": "user-002",
  "enabled": true,
  "reason": "USER_OVERRIDE"
}
```

---

## 20. [PASS] Evaluate seeded new-billing GLOBAL false

- **Category:** Evaluations
- **Elapsed:** 9.53 ms

### Request

```http
GET /api/v1/evaluations/new-billing?userId=stranger-999 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 80
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "new-billing",
  "userId": "stranger-999",
  "enabled": false,
  "reason": "GLOBAL"
}
```

---

## 21. [PASS] Evaluate missing userId → 400

- **Category:** Evaluations
- **Elapsed:** 4.15 ms

### Request

```http
GET /api/v1/evaluations/api-test-412810eb HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 400
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.527647428Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Required request parameter 'userId' for method parameter type String is not present",
  "path": "/api/v1/evaluations/api-test-412810eb"
}
```

---

## 22. [PASS] Evaluate unknown flag → 404

- **Category:** Evaluations
- **Elapsed:** 7.03 ms

### Request

```http
GET /api/v1/evaluations/unknown-flag-zzz?userId=u1 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 404
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.535192220Z",
  "status": 404,
  "error": "Not Found",
  "message": "Feature flag not found: unknown-flag-zzz",
  "path": "/api/v1/evaluations/unknown-flag-zzz"
}
```

---

## 23. [PASS] Upsert override enabled=false

- **Category:** Overrides
- **Elapsed:** 23.85 ms

### Request

```http
PUT /api/v1/flags/api-test-412810eb/overrides/test-user-api HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
X-Request-Id: ovr-1
Content-Type: application/json

{
  "enabled": false
}
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 154
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flagId": 6,
  "flagName": "api-test-412810eb",
  "userId": "test-user-api",
  "enabled": false,
  "updatedBy": "api-tester",
  "updatedAt": "2026-10-08T08:56:00.549061764Z"
}
```

---

## 24. [PASS] Evaluate after override → USER_OVERRIDE false

- **Category:** Evaluations
- **Elapsed:** 5.02 ms

### Request

```http
GET /api/v1/evaluations/api-test-412810eb?userId=test-user-api HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 94
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "api-test-412810eb",
  "userId": "test-user-api",
  "enabled": false,
  "reason": "USER_OVERRIDE"
}
```

---

## 25. [PASS] Upsert override enabled=true

- **Category:** Overrides
- **Elapsed:** 12.15 ms

### Request

```http
PUT /api/v1/flags/api-test-412810eb/overrides/test-user-api HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
Content-Type: application/json

{
  "enabled": true
}
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 153
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flagId": 6,
  "flagName": "api-test-412810eb",
  "userId": "test-user-api",
  "enabled": true,
  "updatedBy": "api-tester",
  "updatedAt": "2026-10-08T08:56:00.570018433Z"
}
```

---

## 26. [PASS] Evaluate after override flip → USER_OVERRIDE true

- **Category:** Evaluations
- **Elapsed:** 4.69 ms

### Request

```http
GET /api/v1/evaluations/api-test-412810eb?userId=test-user-api HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 93
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "api-test-412810eb",
  "userId": "test-user-api",
  "enabled": true,
  "reason": "USER_OVERRIDE"
}
```

---

## 27. [PASS] Upsert override for other user

- **Category:** Overrides
- **Elapsed:** 11.8 ms

### Request

```http
PUT /api/v1/flags/api-test-412810eb/overrides/other-user HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
Content-Type: application/json

{
  "enabled": false
}
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 151
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flagId": 6,
  "flagName": "api-test-412810eb",
  "userId": "other-user",
  "enabled": false,
  "updatedBy": "api-tester",
  "updatedAt": "2026-10-08T08:56:00.587891144Z"
}
```

---

## 28. [PASS] List overrides

- **Category:** Overrides
- **Elapsed:** 13.95 ms

### Request

```http
GET /api/v1/flags/api-test-412810eb/overrides?page=0&size=20 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 617
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "content": [
    {
      "flagId": 6,
      "flagName": "api-test-412810eb",
      "userId": "other-user",
      "enabled": false,
      "updatedBy": "api-tester",
      "updatedAt": "2026-10-08T08:56:00.587891Z"
    },
    {
      "flagId": 6,
      "flagName": "api-test-412810eb",
      "userId": "test-user-api",
      "enabled": true,
      "updatedBy": "api-tester",
      "updatedAt": "2026-10-08T08:56:00.570018Z"
    }
  ],
  "empty": false,
  "first": true,
  "last": true,
  "number": 0,
  "numberOfElements": 2,
  "pageable": {
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 20,
    "paged": true,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "unpaged": false
  },
  "size": 20,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "totalElements": 2,
  "totalPages": 1
}
```

---

## 29. [PASS] List overrides for unknown flag → 404

- **Category:** Overrides
- **Elapsed:** 4.97 ms

### Request

```http
GET /api/v1/flags/no-such-flag/overrides HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 404
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.611964314Z",
  "status": 404,
  "error": "Not Found",
  "message": "Feature flag not found: no-such-flag",
  "path": "/api/v1/flags/no-such-flag/overrides"
}
```

---

## 30. [PASS] Remove override (INHERIT tombstone)

- **Category:** Overrides
- **Elapsed:** 13.58 ms

### Request

```http
DELETE /api/v1/flags/api-test-412810eb/overrides/test-user-api HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
X-Request-Id: del-ovr

(no body)
```

### Response

```http
HTTP/1.1 204
Date: Thu, 08 Oct 2026 08:56:00 GMT

(empty body)
```

---

## 31. [PASS] Evaluate after remove override → GLOBAL

- **Category:** Evaluations
- **Elapsed:** 5.38 ms

### Request

```http
GET /api/v1/evaluations/api-test-412810eb?userId=test-user-api HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 86
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "api-test-412810eb",
  "userId": "test-user-api",
  "enabled": true,
  "reason": "GLOBAL"
}
```

---

## 32. [PASS] List overrides after one removed

- **Category:** Overrides
- **Elapsed:** 5.86 ms

### Request

```http
GET /api/v1/flags/api-test-412810eb/overrides HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 466
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "content": [
    {
      "flagId": 6,
      "flagName": "api-test-412810eb",
      "userId": "other-user",
      "enabled": false,
      "updatedBy": "api-tester",
      "updatedAt": "2026-10-08T08:56:00.587891Z"
    }
  ],
  "empty": false,
  "first": true,
  "last": true,
  "number": 0,
  "numberOfElements": 1,
  "pageable": {
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 20,
    "paged": true,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "unpaged": false
  },
  "size": 20,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 33. [PASS] Remove override unknown flag → 404

- **Category:** Overrides
- **Elapsed:** 5.54 ms

### Request

```http
DELETE /api/v1/flags/no-such-flag/overrides/u1 HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester

(no body)
```

### Response

```http
HTTP/1.1 404
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.641842360Z",
  "status": 404,
  "error": "Not Found",
  "message": "Feature flag not found: no-such-flag",
  "path": "/api/v1/flags/no-such-flag/overrides/u1"
}
```

---

## 34. [PASS] List audits for test flag

- **Category:** Audits
- **Elapsed:** 15.74 ms

### Request

```http
GET /api/v1/flags/api-test-412810eb/audits?page=0&size=50&sort=createdAt,desc HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 1993
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "content": [
    {
      "id": 6,
      "flagId": 6,
      "action": "OVERRIDE_REMOVED",
      "actorId": "api-tester",
      "changeMap": {
        "userId": {
          "new": "test-user-api",
          "previous": null
        },
        "enabled": {
          "new": null,
          "previous": true
        }
      },
      "requestId": "del-ovr",
      "createdAt": "2026-10-08T08:56:00.620201Z"
    },
    {
      "id": 5,
      "flagId": 6,
      "action": "OVERRIDE_UPSERTED",
      "actorId": "api-tester",
      "changeMap": {
        "userId": {
          "new": "other-user",
          "previous": null
        },
        "enabled": {
          "new": false,
          "previous": null
        }
      },
      "requestId": "5fb6c815-45da-4e74-9d1c-31825782c3d3",
      "createdAt": "2026-10-08T08:56:00.588562Z"
    },
    {
      "id": 4,
      "flagId": 6,
      "action": "OVERRIDE_UPSERTED",
      "actorId": "api-tester",
      "changeMap": {
        "userId": {
          "new": "test-user-api",
          "previous": null
        },
        "enabled": {
          "new": true,
          "previous": false
        }
      },
      "requestId": "ddeb8a35-2981-4cbd-abeb-9cc5377c2964",
      "createdAt": "2026-10-08T08:56:00.570647Z"
    },
    {
      "id": 3,
      "flagId": 6,
      "action": "OVERRIDE_UPSERTED",
      "actorId": "api-tester",
      "changeMap": {
        "userId": {
          "new": "test-user-api",
          "previous": null
        },
        "enabled": {
          "new": false,
          "previous": null
        }
      },
      "requestId": "ovr-1",
      "createdAt": "2026-10-08T08:56:00.552717Z"
    },
    {
      "id": 2,
      "flagId": 6,
      "action": "FLAG_UPDATED",
      "actorId": "api-tester",
      "changeMap": {
        "description": {
          "new": "Updated by API test suite",
          "previous": "API test flag created by automated suite"
        },
        "globalEnabled": {
          "new": true,
          "previous": false
        }
      },
      "requestId": "patch-1",
      "createdAt": "2026-10-08T08:56:00.445174Z"
    },
    {
      "id": 1,
      "flagId": 6,
      "action": "FLAG_CREATED",
      "actorId": "api-tester",
      "changeMap": {
        "name": {
          "new": "api-test-412810eb",
          "previous": null
        },
        "state": {
          "new": "ACTIVE",
          "previous": null
        },
        "description": {
          "new": "API test flag created by automated suite",
          "previous": null
        },
        "globalEnabled": {
          "new": false,
          "previous": null
        }
      },
      "requestId": "idem-9b6dc2f92b68",
      "createdAt": "2026-10-08T08:56:00.240216Z"
    }
  ],
  "empty": false,
  "first": true,
  "last": true,
  "number": 0,
  "numberOfElements": 6,
  "pageable": {
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 50,
    "paged": true,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "unpaged": false
  },
  "size": 50,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "totalElements": 6,
  "totalPages": 1
}
```

---

## 35. [PASS] List audits for seeded flag

- **Category:** Audits
- **Elapsed:** 5.93 ms

### Request

```http
GET /api/v1/flags/beta-dashboard/audits?page=0&size=10 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 317
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "content": [],
  "empty": true,
  "first": true,
  "last": true,
  "number": 0,
  "numberOfElements": 0,
  "pageable": {
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 10,
    "paged": true,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "unpaged": false
  },
  "size": 10,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "totalElements": 0,
  "totalPages": 0
}
```

---

## 36. [PASS] List audits unknown flag → 404

- **Category:** Audits
- **Elapsed:** 4.93 ms

### Request

```http
GET /api/v1/flags/no-such-flag/audits HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 404
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.669713239Z",
  "status": 404,
  "error": "Not Found",
  "message": "Feature flag not found: no-such-flag",
  "path": "/api/v1/flags/no-such-flag/audits"
}
```

---

## 37. [PASS] Evaluate cache warm #1

- **Category:** Cache
- **Elapsed:** 3.84 ms

### Request

```http
GET /api/v1/evaluations/api-test-412810eb?userId=test-user-api HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 86
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "api-test-412810eb",
  "userId": "test-user-api",
  "enabled": true,
  "reason": "GLOBAL"
}
```

---

## 38. [PASS] Evaluate cache warm #2 (should be fast)

- **Category:** Cache
- **Elapsed:** 4.81 ms

### Request

```http
GET /api/v1/evaluations/api-test-412810eb?userId=test-user-api HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 86
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "api-test-412810eb",
  "userId": "test-user-api",
  "enabled": true,
  "reason": "GLOBAL"
}
```

---

## 39. [PASS] Cache hits metric

- **Category:** Ops
- **Elapsed:** 12.44 ms

### Request

```http
GET /actuator/metrics/fms.cache.hits HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 190
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "availableTags": [
    {
      "tag": "application",
      "values": [
        "feature-management-service"
      ]
    }
  ],
  "description": "Redis cache hits",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 13.0
    }
  ],
  "name": "fms.cache.hits"
}
```

---

## 40. [PASS] Cache misses metric

- **Category:** Ops
- **Elapsed:** 2.22 ms

### Request

```http
GET /actuator/metrics/fms.cache.misses HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 193
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "availableTags": [
    {
      "tag": "application",
      "values": [
        "feature-management-service"
      ]
    }
  ],
  "description": "Redis cache misses",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 8.0
    }
  ],
  "name": "fms.cache.misses"
}
```

---

## 41. [PASS] Delete flag

- **Category:** Flags
- **Elapsed:** 13.87 ms

### Request

```http
DELETE /api/v1/flags/api-test-412810eb HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester
X-Request-Id: del-flag

(no body)
```

### Response

```http
HTTP/1.1 204
Date: Thu, 08 Oct 2026 08:56:00 GMT

(empty body)
```

---

## 42. [PASS] Get deleted flag → 404

- **Category:** Flags
- **Elapsed:** 3.49 ms

### Request

```http
GET /api/v1/flags/api-test-412810eb HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 404
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.710931119Z",
  "status": 404,
  "error": "Not Found",
  "message": "Feature flag not found: api-test-412810eb",
  "path": "/api/v1/flags/api-test-412810eb"
}
```

---

## 43. [PASS] Evaluate deleted flag → 404

- **Category:** Evaluations
- **Elapsed:** 5.63 ms

### Request

```http
GET /api/v1/evaluations/api-test-412810eb?userId=test-user-api HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 404
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.716754787Z",
  "status": 404,
  "error": "Not Found",
  "message": "Feature flag not found: api-test-412810eb",
  "path": "/api/v1/evaluations/api-test-412810eb"
}
```

---

## 44. [PASS] Delete already-deleted flag → 404

- **Category:** Flags
- **Elapsed:** 3.84 ms

### Request

```http
DELETE /api/v1/flags/api-test-412810eb HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Actor-Id: api-tester

(no body)
```

### Response

```http
HTTP/1.1 404
Content-Type: application/json
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "timestamp": "2026-10-08T08:56:00.720555995Z",
  "status": 404,
  "error": "Not Found",
  "message": "Feature flag not found: api-test-412810eb",
  "path": "/api/v1/flags/api-test-412810eb"
}
```

---

## 45. [PASS] Seed eval beta-dashboard / user-001

- **Category:** SeededFeatures
- **Elapsed:** 4.4 ms

### Request

```http
GET /api/v1/evaluations/beta-dashboard?userId=user-001 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 86
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "beta-dashboard",
  "userId": "user-001",
  "enabled": false,
  "reason": "USER_OVERRIDE"
}
```

---

## 46. [PASS] Seed eval beta-dashboard / user-002

- **Category:** SeededFeatures
- **Elapsed:** 4.55 ms

### Request

```http
GET /api/v1/evaluations/beta-dashboard?userId=user-002 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 85
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "beta-dashboard",
  "userId": "user-002",
  "enabled": true,
  "reason": "USER_OVERRIDE"
}
```

---

## 47. [PASS] Seed eval beta-dashboard / user-003

- **Category:** SeededFeatures
- **Elapsed:** 6.2 ms

### Request

```http
GET /api/v1/evaluations/beta-dashboard?userId=user-003 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 86
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "beta-dashboard",
  "userId": "user-003",
  "enabled": false,
  "reason": "USER_OVERRIDE"
}
```

---

## 48. [PASS] Seed eval beta-dashboard / user-004

- **Category:** SeededFeatures
- **Elapsed:** 7.75 ms

### Request

```http
GET /api/v1/evaluations/beta-dashboard?userId=user-004 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 85
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "beta-dashboard",
  "userId": "user-004",
  "enabled": true,
  "reason": "USER_OVERRIDE"
}
```

---

## 49. [PASS] Seed eval new-billing / user-001

- **Category:** SeededFeatures
- **Elapsed:** 7.04 ms

### Request

```http
GET /api/v1/evaluations/new-billing?userId=user-001 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 82
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "new-billing",
  "userId": "user-001",
  "enabled": true,
  "reason": "USER_OVERRIDE"
}
```

---

## 50. [PASS] Seed eval new-billing / user-002

- **Category:** SeededFeatures
- **Elapsed:** 8.7 ms

### Request

```http
GET /api/v1/evaluations/new-billing?userId=user-002 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 83
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "new-billing",
  "userId": "user-002",
  "enabled": false,
  "reason": "USER_OVERRIDE"
}
```

---

## 51. [PASS] Seed eval new-billing / user-003

- **Category:** SeededFeatures
- **Elapsed:** 7.27 ms

### Request

```http
GET /api/v1/evaluations/new-billing?userId=user-003 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 82
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "new-billing",
  "userId": "user-003",
  "enabled": true,
  "reason": "USER_OVERRIDE"
}
```

---

## 52. [PASS] Seed eval new-billing / user-004

- **Category:** SeededFeatures
- **Elapsed:** 6.86 ms

### Request

```http
GET /api/v1/evaluations/new-billing?userId=user-004 HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 82
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "new-billing",
  "userId": "user-004",
  "enabled": true,
  "reason": "USER_OVERRIDE"
}
```

---

## 53. [PASS] Seed eval ai-assistant / any-user

- **Category:** SeededFeatures
- **Elapsed:** 8.26 ms

### Request

```http
GET /api/v1/evaluations/ai-assistant?userId=any-user HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 76
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "ai-assistant",
  "userId": "any-user",
  "enabled": true,
  "reason": "GLOBAL"
}
```

---

## 54. [PASS] Seed eval mobile-push / any-user

- **Category:** SeededFeatures
- **Elapsed:** 7.81 ms

### Request

```http
GET /api/v1/evaluations/mobile-push?userId=any-user HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 76
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "mobile-push",
  "userId": "any-user",
  "enabled": false,
  "reason": "GLOBAL"
}
```

---

## 55. [PASS] Seed eval referral-bonus / any-user

- **Category:** SeededFeatures
- **Elapsed:** 8.49 ms

### Request

```http
GET /api/v1/evaluations/referral-bonus?userId=any-user HTTP/1.1
Host: localhost:8080
Accept: application/json

(no body)
```

### Response

```http
HTTP/1.1 200
Content-Type: application/json
Content-Length: 78
Date: Thu, 08 Oct 2026 08:56:00 GMT

{
  "flag": "referral-bonus",
  "userId": "any-user",
  "enabled": true,
  "reason": "GLOBAL"
}
```

---

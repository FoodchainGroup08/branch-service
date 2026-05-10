# Branch Service

Spring Boot microservice for **restaurant branch** data: locations, GPS coordinates, operating hours, and active/inactive status. Used by the customer app (branch list / nearby), admin tooling, and other services that need branch metadata.

## Port & base URL

| | |
|--|--|
| **Port** | **8081** in local `application.yml` (override with `SERVER_PORT` if it clashes with **user-service**) |
| **Context path** | `/api` |
| **Example base** | `http://localhost:8081/api` |
| **Through gateway** | `http://localhost:8080/api/v1/branches/**` |

Service id in Eureka: **`branch-service`**.

---

## API overview

Controller base path: **`/v1/branches`** (full path with context: `/api/v1/branches`).

### Read (mostly public via gateway)

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/v1/branches` | Paginated list (`page`, `size`, optional `active` filter). |
| `GET` | `/v1/branches/nearby` | Branches near `lat`, `lng`, optional `radiusKm`. |
| `GET` | `/v1/branches/{id}` | Branch detail. |
| `GET` | `/v1/branches/{id}/hours` | Weekly operating hours. |

The **API gateway** allows **anonymous** `GET` only for:

- `GET /api/v1/branches` (list)
- `GET /api/v1/branches/nearby`

Other reads (e.g. branch by id) require a **Bearer token** at the gateway.

### Writes (admin)

Create, update, delete, hours, activate/deactivate/status require an admin role. Authorization is enforced using the **`X-User-Role`** header set by the gateway after JWT validation (the gateway strips `Authorization`).

Accepted admin roles (case-insensitive): **`HEAD_OFFICE_ADMIN`**, **`OFFICE_ADMIN`**, **`Admin`** (display name).

When calling this service **without** the gateway (e.g. local Swagger), set **`X-User-Role: HEAD_OFFICE_ADMIN`** manually for admin routes.

| Method | Path | Notes |
|--------|------|--------|
| `POST` | `/v1/branches` | Create branch |
| `PUT` | `/v1/branches/{id}` | Update |
| `DELETE` | `/v1/branches/{id}` | Returns **200** JSON `{ "message", "id" }` on success |
| `PUT` | `/v1/branches/{id}/hours` | Replace weekly hours |
| `PATCH` | `/v1/branches/{id}/activate` \| `/deactivate` \| `/status` | Toggle active |

---

## Integration

- **MySQL** persistence for branches and hours.
- **Eureka** registration.
- Optional **Spring Cloud Config** (`optional:configserver:http://localhost:8888`).
- **No Spring Security** in-process — trust boundary is the **gateway** + explicit **`X-User-Role`** checks on mutating endpoints.

---

## Error responses

JSON errors via `@RestControllerAdvice`: `status`, `error`, `message`, `path`, `timestamp`; validation and constraint violations return **400** with `fields` when applicable.

---

## Running locally

Prerequisites: Java 17+, MySQL, Eureka (and Config server if used).

```bash
./mvnw spring-boot:run
```

Tests: `./mvnw test` (often use H2 / mocks).

Swagger (direct): `http://localhost:8081/api/swagger-ui.html`

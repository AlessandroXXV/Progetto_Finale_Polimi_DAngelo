# Eably — Backend

REST + WebSocket API for the Eably platform: a marketplace connecting clients with service providers, including booking, real-time chat, reviews, identity verification and Stripe payments.

## Tech stack

| Area            | Technology                                  |
|-----------------|---------------------------------------------|
| Language        | Java 25                                     |
| Framework       | Spring Boot 4.0.3                           |
| Persistence     | Spring Data JPA + Hibernate, PostgreSQL 17  |
| Security        | Spring Security + JWT (jjwt)                |
| Real-time       | Spring WebSocket + STOMP                     |
| Mapping         | MapStruct                                   |
| Payments        | Stripe (PaymentIntent + Connect payouts)    |
| Mail            | Spring Mail (SMTP)                          |
| Monitoring      | Spring Actuator (health, metrics, prometheus)|
| Build           | Maven (`mvnw` wrapper)                       |

## Package layout

```
it.eably.backend
├── config       # Spring configuration (security, websocket, ...)
├── controller   # REST + WebSocket controllers (see ENDPOINTS.md)
├── dto          # Request/response DTOs
├── exception    # Custom exceptions + GlobalExceptionHandler
├── mapper       # MapStruct mappers
├── model        # JPA entities and enums
├── repository   # Spring Data repositories
├── security     # JWT filter, auth provider, principal handling
└── service      # Business logic (def + impl)
```

## API documentation

All endpoints are documented in [ENDPOINTS.md](ENDPOINTS.md) — auth, profiles, availability, bookings, chat (WebSocket + history), reviews, payments, admin and verification.

## Configuration & profiles

Configuration lives in `src/main/resources/application*.properties`. The base file activates `mail`, a machine profile, and `stripe-test` by default.

| Profile         | Purpose                                              |
|-----------------|------------------------------------------------------|
| `macos`/`linux` | Local DB connection (selected via `MACHINE_PROFILE`) |
| `docker`        | DB host `db` for docker-compose                      |
| `mail`          | SMTP settings                                        |
| `stripe-test`   | Stripe test keys                                     |
| `stripe-live`   | Stripe live keys                                     |

### Required environment variables

No secrets are committed. Supply these via environment (see [`../.env.example`](../.env.example)):

| Variable                 | Description                          |
|--------------------------|--------------------------------------|
| `DB_PASSWORD`            | PostgreSQL password                  |
| `JWT_SECRET`             | JWT signing secret (≥ 256 bits)      |
| `SMTP_HOST` / `SMTP_PORT`| Mail server                          |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | Mail credentials            |
| `STRIPE_SECRET_KEY` / `STRIPE_PUBLISHABLE_KEY` | Stripe keys     |

## Running

### With Docker (recommended)

From the repository root, `docker-compose.yml` starts PostgreSQL, the backend and the frontend together:

```bash
cp .env.example .env   # fill in the values
docker compose up --build
```

Backend is then available on `http://localhost:8080`.

### Locally

Requires Java 25 and a running PostgreSQL instance matching your machine profile.

```bash
# Create the database (one-time setup):
createdb eably_spring
# or: psql -c "CREATE DATABASE eably_spring;"

cd backend
./mvnw spring-boot:run
```

Set `MACHINE_PROFILE=linux` (or `macos`) and export the required environment variables first.

## Tests

```bash
cd backend

# Unit tests (default — no external dependencies)
./mvnw test

# Integration tests that require a real PostgreSQL connection (e.g. BookingConcurrencyTest)
# 1. Create the test database (one-time setup):
createdb eably_test
# or: psql -c "CREATE DATABASE eably_test;"

# 2. Run with the flag (test profile reads application-test.properties; schema is auto-created):
./mvnw test -DrunPostgresIT=true
```

The suite covers controllers, services, security, WebSocket config and booking concurrency.
Integration tests are guarded by `@EnabledIfSystemProperty(named = "runPostgresIT", matches = "true")` and are skipped unless the flag is passed explicitly.

## Health & monitoring

- Health: `GET /actuator/health` (public)
- Metrics / Prometheus: exposed under `/actuator` (auth required for details)

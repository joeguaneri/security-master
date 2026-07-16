# security-master

A JWT-secured Spring Boot securities mastering service. Securities are cross-referenced by internal
id, PACESEC ID, SEDOL, CUSIP, and TICKER, and cover both cash instruments and derivatives (options,
futures, swaps, FX). Persistence is hand-written JDBC (no Hibernate/JPA); reference-data updates
flow in and out over JMS (embedded ActiveMQ Artemis) alongside a REST API.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the design and [AGENTS.md](AGENTS.md) for repo
conventions (TDD is mandatory, no ORM).

## Prerequisites

- Java 21
- Maven
- Docker (for Postgres, and for Testcontainers-backed tests)

## Running locally

```bash
docker compose up -d          # starts Postgres on localhost:5433 (5432 is often already in use locally)
mvn spring-boot:run           # runs the app; Flyway migrates the schema on startup
```

## Seeded demo credentials

Dev-only, created by a Flyway seed migration — do not reuse in any real environment:

| username | password | roles |
|---|---|---|
| `admin`  | `admin`  | `ROLE_READ, ROLE_WRITE, ROLE_ADMIN` |
| `reader` | `reader` | `ROLE_READ` |

## Trying it out

```bash
# 1. Log in
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r .accessToken)

# 2. Create a security
curl -X POST localhost:8080/api/securities \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"assetType":"EQUITY","name":"Example Corp","ticker":"EXMP","currency":"USD"}'

# 3. Look it up by any identifier, without knowing which scheme it is
curl -H "Authorization: Bearer $TOKEN" \
  "localhost:8080/api/securities/lookup?value=EXMP"

# 4. Simulate an inbound feed update over JMS via REST
curl -X POST localhost:8080/api/admin/feed/simulate \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"ticker":"EXMP","name":"Example Corp (Renamed)","assetType":"EQUITY","currency":"USD"}'
```

## Tests

```bash
mvn test
```

Runs the full unit test suite plus Testcontainers-backed integration tests (Postgres + embedded
Artemis) — Docker must be running.

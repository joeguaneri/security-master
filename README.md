# security-master

A JWT-secured Spring Boot securities master in the style of BNY Eagle PACE. Each instrument is one
row, keyed by **PACESECID**, with a single flat security type. Securities are also cross-referenced
by SEDOL, CUSIP, and TICKER. An equity swap is the type `EQUITYSWAP`: the equity underlier
(`underlyingIdentifier`) and the swap notional currency sit on that same row. Persistence is
hand-written JDBC (no Hibernate/JPA). Reference-data updates flow in and out over JMS (embedded
ActiveMQ Artemis) alongside a REST API.

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

# 2. Create the underlier, then the equity swap as one flat PACE type
curl -X POST localhost:8080/api/securities \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"assetType":"EQUITY","name":"Example Corp","ticker":"EXMP","paceSecId":"PACE-EXMP","currency":"USD"}'

curl -X POST localhost:8080/api/securities \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"assetType":"EQUITYSWAP","name":"Example Corp Equity Swap","ticker":"EXMPSWAP","paceSecId":"PACE-EQSWAP-1","currency":"USD","notionalCurrency":"USD","underlyingIdentifier":"EXMP"}'

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

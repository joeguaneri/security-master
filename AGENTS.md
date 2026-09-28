# Working in this repo

## Non-negotiable conventions

1. **Test-driven development.** For every unit of behavior, write the test first, run it and confirm
   it fails for the right reason, then write the implementation. Do not write a source file before
   its corresponding test exists. This applies at every layer: service, repository, security filter,
   controller, JMS listener.
2. **No ORM.** Persistence is hand-written JDBC (`NamedParameterJdbcTemplate` + `RowMapper`). Do not
   add Hibernate, JPA, or Spring Data JPA. Schema is owned entirely by Flyway migrations under
   `src/main/resources/db/migration`; there is no `ddl-auto` anywhere.
3. **SQL correctness is verified against real Postgres**, not H2 — repository tests use Testcontainers.

## Package layout

```
com.guaneri.securitymaster
├── domain      # plain records/enums, no framework annotations
├── repository  # SecurityRepository/AppUserRepository interfaces + Jdbc* implementations + RowMappers
├── service     # business logic, validation, JMS event publishing
├── web         # controllers, DTOs, exception handling
├── security    # JWT issuance/validation, Spring Security config
└── jms         # Artemis config, inbound listener, message/event types
```

## Build & test

- `mvn test` — full suite (unit + Testcontainers integration tests; Docker must be running)
- `mvn spring-boot:run` — run the app (requires `docker compose up -d` for Postgres first)
- `docker compose up -d` / `docker compose down` — local Postgres

## Identifiers

A `Security` row is cross-referenced by up to five identifiers: the generated internal `id` (UUID),
`paceSecId`, `sedol`, `cusip`, and `ticker`. Any of the four external identifiers may be `null`, but
when populated must be unique (enforced by partial unique indexes, not application code). See
`IdentifierType` and the two lookup endpoints in `ARCHITECTURE.md`.

## Security type (Eagle PACE)

Classification is one flat `AssetType` on the security row. `EQUITYSWAP` is a single type code: the
equity underlier is `underlyingIdentifier` on that same row, and `notionalCurrency` is required with
it.

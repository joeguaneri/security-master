# Architecture

## Purpose

`security-master` is the system of record for security reference data, modeled on BNY Eagle PACE.
Each security is one PACE record: a generated internal id plus **PACESECID** (the Eagle book-of-record
identifier), SEDOL, CUSIP, and TICKER. Classification is a single flat security type on that row.
Cash instruments (equity, bond, fund, commodity) and derivatives (options, futures, swaps, FX) are
separate type codes, not a parent/child tree.

## Domain model

`Security` is a single flat record (see `domain/Security.java`) rather than a class hierarchy per
asset type. It holds core fields common to every security (identifiers, name, issuer, currency,
country of risk, exchange, status) plus a set of instrument-specific fields that are populated only
for the asset types they apply to (e.g. `strikePrice` for options, `couponRate`/`maturityDate` for
bonds, `notionalCurrency` for swaps, `baseCurrency`/`quoteCurrency` for FX). This mirrors the
persistence model directly: one wide table, no ORM inheritance mapping to lean on.

`AssetType` is a single flat enum with one explicit constant per instrument type:

```
EQUITY, BOND, FUND, COMMODITY,
EQUITYOPTION, INDEXOPTION, FUTUREOPTION, FUTURE,
EQUITYSWAP, INTERESTRATESWAP, CURRENCYSWAP, CREDITDEFAULTSWAP, TOTALRETURNSWAP,
FXSPOT, FXFORWARD,
OTHER
```

`EQUITYSWAP` is one PACE security type. The equity it references is `underlyingIdentifier` on the
swap row (a ticker or other identifier string), and `notionalCurrency` is required on that same row.
Creating or feeding an `EQUITYSWAP` without either field is rejected. The underlier, when mastered
at all, is its own `EQUITY` security with its own PACESECID — it is not a classification level on
the swap.

`SecurityMasterService` enforces which instrument-specific fields are required for which
`AssetType` (e.g. an `EQUITYOPTION` must carry a `strikePrice`, an `EQUITYSWAP` must carry
`underlyingIdentifier` and `notionalCurrency`) and throws `InvalidSecurityFieldsException` on
violation — this is a runtime, service-layer check, not a compile-time type distinction, since the
persistence layer doesn't support one without an ORM.

`IdentifierType` (`INTERNAL_ID, PACESEC_ID, SEDOL, CUSIP, TICKER`) is used by the generic lookup
endpoint and the JMS feed-upsert matcher. It's mapped to a physical column name via a fixed
`Map<IdentifierType, String>` inside the DAO — never by concatenating caller-supplied input into
SQL — so the lookup query's target column always comes from an allow-list.

## Persistence (no ORM)

Schema is owned entirely by Flyway migrations (`src/main/resources/db/migration`). The `securities`
table has partial unique indexes on `pacesec_id`, `sedol`, `cusip`, `ticker` (`WHERE x IS NOT NULL`)
so multiple `NULL`s are allowed but populated values must be unique.

`JdbcSecurityRepository` uses `NamedParameterJdbcTemplate` with hand-written SQL for every
operation — insert, version-checked update, `findById`, `findByIdentifier(type, value)`,
`findByAnyIdentifier(value)` (a single `OR`-across-columns query for the universal lookup), and
paged search. `SecurityRowMapper` reads a full row (including whichever instrument-specific columns
are non-null) into one `Security` record.

Optimistic locking is manual, not `@Version`-annotation-driven:

```sql
UPDATE securities SET ..., version = version + 1 WHERE id = :id AND version = :expectedVersion
```

If the affected-row count isn't exactly 1, the repository throws `OptimisticLockException`.

## REST API

Base path `/api/securities`:

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/securities` | `ROLE_WRITE`/`ROLE_ADMIN` | create; publishes outbound JMS event |
| PUT | `/api/securities/{id}` | `ROLE_WRITE`/`ROLE_ADMIN` | update; requires `version` in body; publishes event |
| GET | `/api/securities/{id}` | any authenticated | fetch by internal id |
| GET | `/api/securities/lookup?idType={type}&value={value}` | any authenticated | lookup by a specific identifier type |
| GET | `/api/securities/lookup?value={value}` | any authenticated | universal lookup across all 5 identifier columns |
| GET | `/api/securities?assetType=&status=&page=&size=` | any authenticated | paged search |
| DELETE | `/api/securities/{id}` | `ROLE_ADMIN` | soft-delete (status → INACTIVE); publishes event |

`POST /api/auth/login` (permit-all) exchanges username/password for a JWT.

`POST /api/admin/feed/simulate` (`ROLE_ADMIN`) publishes a caller-supplied feed message onto the
inbound JMS queue, so the full inbound loop can be exercised over REST without a separate JMS
client.

## Security

Self-issued JWTs (HS256, `io.jsonwebtoken`), stateless sessions, CSRF disabled (pure API, no
cookies). `JwtAuthenticationFilter` reads `Authorization: Bearer <token>`, validates it via
`JwtService`, and populates the `SecurityContext` directly from the token's claims — there's no
`UserDetailsService`/`AuthenticationManager` lookup on the request path, since a valid JWT is
self-contained proof of identity and roles. Role checks (`ROLE_READ`/`ROLE_WRITE`/`ROLE_ADMIN`) are
enforced with `@PreAuthorize` on individual controller methods.

`AuthController` (the *login* path, not request authentication) depends directly on
`AppUserRepository` (JDBC) and `PasswordEncoder`: it looks the user up, checks
`passwordEncoder.matches(...)` against the stored BCrypt hash, and on success issues a JWT carrying
that user's roles. This is simpler than wiring a `DaoAuthenticationProvider`/`AuthenticationManager`
for a case that doesn't need one, and it keeps the JWT-validation path (every other request)
completely decoupled from the datasource.

One easy-to-miss wrinkle: `response.sendError(403)` from the `AccessDeniedHandler` triggers an
internal Servlet **error dispatch** to `/error`, which re-enters the security filter chain as a
second pseudo-request. If `/error` isn't itself permitted, that second pass has no bearer token
context and fails with `InsufficientAuthenticationException`, silently overwriting the original 403
with a 401. `SecurityConfig` permits `/error` alongside `/api/auth/**` and the actuator health
endpoints for exactly this reason.

## JMS (Artemis)

Runs as an embedded broker (`spring.artemis.mode=embedded`) — no external broker install required.

- **Inbound**: `SecurityFeedListener` consumes `SecurityFeedMessage` from queue
  `securities.feed.inbound` and calls `SecurityMasterService.upsertFromFeed(...)`, which matches an
  existing row by whichever identifier is populated on the incoming message, updates it, or creates
  a new security if no match is found.
- **Outbound**: on create/update/deactivate, the service publishes a `SecurityChangedEvent`
  (`internalId, paceSecId, sedol, cusip, ticker, changeType, timestamp`) to topic
  `securities.events` for downstream consumers.

## Testing strategy

- Fast, mock-based unit tests for pure logic and business rules (`JwtServiceTest`,
  `SecurityMasterServiceTest`).
- Testcontainers-backed tests wherever hand-written SQL or the real JMS broker behavior actually
  matters (`JdbcSecurityRepositoryTest`, `JdbcAppUserRepositoryTest`, `SecurityFeedListenerIT`).
- MockMvc tests for the web layer with the service mocked out (`SecurityControllerTest`,
  `AuthControllerTest`, `AdminFeedControllerTest`, `SecurityConfigIT`).

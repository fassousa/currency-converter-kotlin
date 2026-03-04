# Implementation Plan — Kotlin/Spring Boot Migration

**Reference**: `KOTLIN_MIGRATION_BLUEPRINT.md`
**Schema notes**: `KOTLIN_DATABASE_SCHEMA_AUDIT.md`
**Deployment notes**: `KOTLIN_DEPLOYMENT_STRATEGY.md`
**Audited**: 2026-03-02

---

## Senior Architect's Audit

### 1. Logical Sequencing & The Critical Path

**Finding: One sequencing risk identified — Security was too late.**

The original Phase 6 (Security) came *after* Phase 5 (REST Layer). This means the controller tests in task 5.6 would have been written against an unauthenticated surface and then broken when JWT filtering was added. Authentication is a cross-cutting concern that must be established before any controller test is written.

**Fix applied**: Security (JWT filter + `SecurityConfig`) is now Phase 5, immediately before the REST layer (Phase 6). Controller tests in Phase 6 are written against an already-secured surface from day one.

The rest of the critical path is sound:
```
Domain (pure, no deps) → DB Schema (Flyway + JPA entities) → Application Service
(@Transactional + idempotency) → Security (JWT) → REST (controllers + tests)
→ Exchange Rate Gateway (Resilience4j) → Observability → Docs + Hardening
```

This ordering means Phases 0–3 are entirely testable without running a real HTTP server. The first `@SpringBootTest` does not appear until Phase 4 (the concurrency test), and even that uses a `@MockBean` for the exchange rate gateway. Integration is incremental, not big-bang.

---

### 2. Redundancy & Scope Creep

**Findings:**

| Item | Verdict | Action |
|---|---|---|
| `DuplicateTransactionException.kt` (task 1.2) | ❌ **Redundant** — idempotency hits return the existing record silently; they never throw. The `409` comes from `DataIntegrityViolationException` on the DB constraint, not from a domain exception. | **Removed.** Do not create this class. |
| `TransactionCreatedEvent` + `TransactionEventListener` (task 3.2) | ⚠️ **Borderline** — adds two classes for a `log.info()` that the service already emits. However, the `@TransactionalEventListener(AFTER_COMMIT)` pattern is an explicit P1 interview signal (ADR-007). | **Kept, but scoped**: implement the event + listener with only the log line. Document the Kafka upgrade path. Do not implement any actual event publishing infrastructure. |
| `RateLimitingFilter` + `RateLimitingConfig` (tasks 7.3–7.4) | ⚠️ **Gold-plating risk** — Bucket4j with Redis adds two new infrastructure classes and a Redis dependency for a feature the demo will never stress-test end-to-end. | **Kept at P2** — but explicitly deferred until Phases 0–6 are green. Rate limiting is a legitimate banking concern; skip it only if time pressure is real. |
| `springdoc-openapi` Phase 8 | ✅ **Justified** — Swagger UI is how a reviewer executes the demo. Keep. |
| `wiremock-jre8` (task 4.8) | ✅ **Justified** — the only way to test circuit-breaker fallback without a live API. Keep. |
| `H2` test dependency | ✅ **Justified** — pure domain unit tests run in milliseconds with no Postgres container. Keep. |
| Detekt (task 9.2) | ✅ **Justified** — one `./gradlew detekt` line in CI signals code quality discipline. Keep. |

---

### 3. Spring Boot Setup Verification

**Findings on `build.gradle.kts`:**

| Issue | Severity | Fix |
|---|---|---|
| `wiremock-jre8` targets JRE 8 embedded Jetty — incompatible with Java 21 module system | 🔴 **Critical** | Replace with `wiremock-standalone:3.x` or `org.wiremock:wiremock:3.5.4` |
| Missing `kotlin-allopen` / `kotlin-noarg` compiler plugins — Spring `@Component` and JPA `@Entity` require open classes and no-arg constructors | 🔴 **Critical** | `kotlin("plugin.spring")` handles `@Component`/`@Service`; `kotlin("plugin.jpa")` handles `@Entity`. Both are already in the build file — ✅ confirmed. |
| `resilience4j-feign:2.2.0` — the Feign decorator is a separate artifact. If annotations (`@CircuitBreaker`) are used on a Spring bean method, `resilience4j-spring-boot3` is sufficient; `resilience4j-feign` is only needed if using the `FeignDecorators` API directly | 🟡 **Unnecessary** | Remove `resilience4j-feign` unless `FeignDecorators` is explicitly used. The annotation-driven approach from `resilience4j-spring-boot3` + `spring-boot-starter-aop` is the correct choice here. |
| `bucket4j-redis:8.9.0` requires a compatible Lettuce/Jedis version — ensure the Redis starter's Lettuce version is compatible | 🟡 **Verify** | Spring Boot 3.2.x ships Lettuce 6.3.x. Bucket4j 8.9.0 supports Lettuce 6.x. ✅ compatible. |
| `com.github.tomakehurst:wiremock-jre8` → must be replaced | 🔴 **Critical** | `testImplementation("org.wiremock:wiremock:3.5.4")` |

**One missing dependency:**
```kotlin
// Required for @Transactional on non-Spring-managed objects and for AOP proxy debugging
testImplementation("org.springframework.boot:spring-boot-starter-test") // already present ✅
// Missing: Testcontainers BOM for consistent versions across all TC artifacts
testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.4"))
```

---

### 4. Feasibility Check

**Honest assessment of the ~8–10 session estimate:**

| Phase Group | Sessions | Risk |
|---|---|---|
| Phases 0–3 (Scaffold + Domain + DB + App Service) | 3–4 | Low — all pure Kotlin, no framework fights |
| Phase 4 (Security) | 1 | Medium — JJWT 0.12.x API is different from 0.11.x; allow extra time |
| Phase 5 (REST + Controller tests) | 1–2 | Low once security is in place |
| Phase 6 (Exchange Rate Gateway + Resilience4j) | 1–2 | Medium — Resilience4j annotation ordering and `@Cacheable` interaction require careful testing |
| Phases 7–9 (Observability + Docs + Hardening) | 1–2 | Low |

**Total: 8–12 sessions is realistic *if* tests are written alongside each phase, not at the end.**

The single biggest risk is writing all code first and testing last. That approach will not produce 80% coverage — it will produce untestable code that was never designed to be tested. **The DoD for every task below requires a passing test before the task is checked off.**

---

## Refined Task List — Definition of Done

Tasks are grouped into four tracks. Within each track, tasks are numbered sequentially. **Each task is scoped to at most one production file + one test file — the right granularity for a single LLM prompt.**

---

## Track A — Infrastructure & Setup

**Goal**: A running, empty Spring Boot app. The entire stack boots. Nothing crashes.

- [x] **A.1** — **`build.gradle.kts` + `settings.gradle.kts`**
  Create `currency-converter-kotlin/` with `settings.gradle.kts` and `build.gradle.kts`.
  Apply fixes from the audit: replace `wiremock-jre8` with `org.wiremock:wiremock:3.5.4`; remove `resilience4j-feign`; add `testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.4"))`.
  *DoD*: `./gradlew build` compiles with zero errors and zero warnings. No application code yet.

- [x] **A.2** — **Application entry point**
  Create `CurrencyConverterApplication.kt` with `@SpringBootApplication` + `@EnableFeignClients`.
  *DoD*: `./gradlew bootRun` starts without error (DB connection failure is expected at this stage).

- [x] **A.3** — **`application.yml` (base config)**
  Create `application.yml` with the base profile — datasource placeholders, JPA settings, Flyway enabled, Actuator endpoints, JWT/Redis/cache settings all referencing `${ENV_VAR}` — no hardcoded secrets.
  *DoD*: `./gradlew test` with H2 profile passes (context loads, no binding errors).

- [x] **A.4** — **`application-dev.yml` + `application-prod.yml`**
  Create the two profile overlays: `dev` enables DDL auto-create, SQL logging, readable log pattern; `prod` disables auto-DDL, sets `ddl-auto=validate`, Logstash JSON encoder.
  *DoD*: `./gradlew build` succeeds; both profiles have PostgreSQL configuration with proper defaults.

- [x] **A.5** — **`Dockerfile` (multi-stage)**
  Create a multi-stage `Dockerfile`: `gradle:8-jdk21` build stage → `eclipse-temurin:21-jre-alpine` runtime stage, non-root user, health-check instruction (Blueprint §20). Uses layered JAR extraction for optimal Docker cache reuse.
  *DoD*: `docker build -t currency-converter-kotlin .` succeeds; image size ≈ 380 MB (Spring Boot 3 + JRE 21; sub-200 MB breaks JDBC dynamic class-loading — distroless/jlink not viable here).

- [x] **A.6** — **`docker-compose.yml` + `.env.example`**
  Create `docker-compose.yml` with three services: `app` (depends on `db` + `redis`), `db:postgres:16-alpine`, `redis:7-alpine`. Create `.env.example` documenting every required variable.
  *DoD*: `docker compose up db redis` starts both containers healthy.

- [x] **A.7** — **`Makefile`**
  Create `Makefile` with targets: `setup`, `build`, `up`, `down`, `logs`, `shell`, `test`, `migrate`.
  *DoD*: `make setup` from a clean checkout brings up all three containers; `GET /actuator/health` returns `{"status":"UP"}`.

- [x] **A.8** — **`V1__create_users.sql`**
  Write the users migration: `UUID PK`, `email VARCHAR(255) NOT NULL UNIQUE`, `password_digest VARCHAR(255) NOT NULL`, `created_at`/`updated_at TIMESTAMP WITH TIME ZONE NOT NULL` (Schema Audit findings).
  *DoD*: `make up` shows `Successfully applied 1 migration`; `psql \d users` matches spec.

- [x] **A.9** — **`V2__create_transactions.sql`**
  Write the transactions migration: `UUID PK`, `user_id UUID NOT NULL REFERENCES users ON DELETE RESTRICT`, `idempotency_key UUID NOT NULL UNIQUE`, `source_currency CHAR(3) NOT NULL`, `source_amount NUMERIC(19,4) NOT NULL`, `target_currency CHAR(3) NOT NULL`, `target_amount NUMERIC(19,4) NOT NULL`, `exchange_rate NUMERIC(27,8) NOT NULL`, `created_at TIMESTAMP WITH TIME ZONE NOT NULL` (Blueprint §18 + Schema Audit).
  *DoD*: `make up` shows `Successfully applied 2 migrations`; `psql \d transactions` matches spec.

- [x] **A.10** — **`V3__add_constraints.sql`**
  Add `CHECK` constraints: `source_amount > 0`, `target_amount > 0`, `exchange_rate > 0`, `source_currency ~ '^[A-Z]{3}$'`, `target_currency ~ '^[A-Z]{3}$'`, `source_currency <> target_currency`. Add all named indexes (Blueprint §18).
  *DoD*: `make up` shows `Successfully applied 3 migrations`; inserting a row violating any constraint is rejected by Postgres.

---

## Track B — Core Domain

**Goal**: The domain model is complete, all invariants are enforced, and every invariant has a test. Zero Spring annotations anywhere in `domain/`.

- [ ] **B.1** — **Domain exceptions**
  Create `domain/exception/CurrencyNotSupportedException.kt` and `domain/exception/ExchangeRateUnavailableException.kt`.
  *(Note: `DuplicateTransactionException` is removed — it is redundant. See Audit §2.)*
  *DoD*: Both exceptions extend `RuntimeException`; no Spring imports; both compile cleanly.

- [ ] **B.2** — **`SupportedCurrency` enum**
  Create `domain/model/SupportedCurrency.kt` — enum `BRL, USD, EUR, JPY` with `fromCode()` companion factory that throws `CurrencyNotSupportedException` for unknown codes (Blueprint §7.1).
  *DoD*: Unit test `SupportedCurrencyTest` covers: `fromCode("USD")` returns enum; `fromCode("XYZ")` throws `CurrencyNotSupportedException`; `fromCode("usd")` succeeds (case-insensitive). Zero framework imports in production file.

- [ ] **B.3** — **`Money` value object**
  Create `domain/model/Money.kt` — immutable `data class`, `BigDecimal` amount, `HALF_EVEN` rounding, `convertTo(rate: BigDecimal): Money`, companion constants `MONETARY_SCALE=4`, `RATE_SCALE=8`, `Money.of()` factory that normalises scale (Blueprint §7.2).
  *DoD*: Unit test `MoneyTest` covers: `convertTo()` result uses `HALF_EVEN` (not `HALF_UP`) for a `.5` boundary value; `init` block throws for negative amount; `Money.of()` normalises to 4 dp.

- [ ] **B.4** — **`Transaction` aggregate root**
  Create `domain/model/Transaction.kt` — `data class`, all `val`, `Transaction.create()` companion factory enforces: currencies differ, rate > 0, amount > 0, delegates conversion to `Money.convertTo()` (Blueprint §7.3).
  *DoD*: Unit test `TransactionTest` covers: happy path produces correct `targetAmount`; same-currency pair throws `IllegalArgumentException`; negative amount throws; zero rate throws. All 4 tests run in < 50 ms with no Spring context.

- [ ] **B.5** — **Outbound port interfaces**
  Create `domain/port/out/TransactionRepository.kt` — `findById`, `findByIdempotencyKey`, `findByUserId(userId, pageable)`, `save`.
  Create `domain/port/out/ExchangeRateGateway.kt` — `fetchRate(from: SupportedCurrency, to: SupportedCurrency): BigDecimal`.
  *DoD*: Both files contain only pure Kotlin interfaces. `./gradlew compileKotlin` passes. No Spring imports.

- [ ] **B.6** — **Inbound port interfaces + command/query objects**
  Create `domain/port/in/ConvertCurrencyUseCase.kt` with `ConvertCurrencyCommand` data class.
  Create `domain/port/in/ListTransactionsUseCase.kt` with `ListTransactionsQuery` data class.
  *DoD*: Both files contain only pure Kotlin interfaces and data classes. `./gradlew compileKotlin` passes. No Spring imports.

- [ ] **B.7** — **`UserEntity` JPA entity**
  Create `adapter/out/persistence/entity/UserEntity.kt` with all `@Column` mappings, `@Table`, `@Entity` (Blueprint §9.1). No domain logic.
  *DoD*: `./gradlew compileKotlin` passes. Verify `kotlin("plugin.jpa")` is enabling the no-arg constructor (no manual secondary constructor needed).

- [ ] **B.8** — **`TransactionEntity` JPA entity**
  Create `adapter/out/persistence/entity/TransactionEntity.kt` with all `@Column` mappings, `@ManyToOne(fetch = LAZY)` to `UserEntity`, `@Table` index annotations, `idempotency_key` unique constraint (Blueprint §9.1).
  *DoD*: `./gradlew compileKotlin` passes; Flyway schema and entity column names are in sync (no rename mismatch).

- [ ] **B.9** — **`TransactionEntityMapper`**
  Create `adapter/out/persistence/mapper/TransactionEntityMapper.kt` — `toDomain(entity: TransactionEntity): Transaction` and `toEntity(domain: Transaction): TransactionEntity`.
  *DoD*: Unit test `TransactionEntityMapperTest` — round-trip: `toDomain(toEntity(transaction))` produces a transaction equal to the original; `HALF_EVEN`-scaled amounts survive the round-trip.

- [ ] **B.10** — **Spring Data repository interfaces**
  Create `adapter/out/persistence/SpringJpaTransactionRepository` (internal, package-private Spring Data JPA interface with `findByIdempotencyKey` and `findByUserId` query methods).
  Create `adapter/out/persistence/JpaUserRepository` (internal Spring Data JPA interface).
  *DoD*: `./gradlew compileKotlin` passes. Both interfaces are `internal` — not accessible outside the adapter package.

- [ ] **B.11** — **`JpaTransactionRepositoryAdapter`**
  Create `adapter/out/persistence/JpaTransactionRepositoryAdapter` implementing `domain/port/out/TransactionRepository`. Delegates all methods to `SpringJpaTransactionRepository` via the mapper.
  *DoD*: Testcontainers integration test `JpaTransactionRepositoryAdapterIT` — saves a `Transaction` via the domain port, reads it back via `findByIdempotencyKey`, asserts all fields round-trip correctly. Test touches only the domain port; the Spring Data interface is never referenced directly.

- [ ] **B.12** — **`ConvertCurrencyService` — structure + idempotency**
  Create `application/ConvertCurrencyService.kt` with `@Transactional` and the idempotency check only (step 1: find existing → return early). Stubs for steps 2–6 with `TODO()`.
  *DoD*: Unit test `ConvertCurrencyServiceTest` — idempotency hit: `findByIdempotencyKey` returns existing → `fetchRate` called zero times, `save` called zero times, existing transaction returned.

- [ ] **B.13** — **`ConvertCurrencyService` — happy path**
  Fill in steps 2–5 of `ConvertCurrencyService`: `SupportedCurrency.fromCode()`, `exchangeRateGateway.fetchRate()`, `Transaction.create()`, `transactionRepository.save()`. Remove the `TODO()` stubs.
  *DoD*: Unit test additions to `ConvertCurrencyServiceTest` — happy path: `fetchRate` called once, `save` called once, returned transaction has correct currencies and `targetAmount`. `CurrencyNotSupportedException` path: thrown before `fetchRate` is called, `save` never called.

- [ ] **B.14** — **`TransactionCreatedEvent` + `TransactionEventListener`**
  Create `application/TransactionCreatedEvent.kt` (simple data class wrapping the saved `Transaction`).
  Create `application/TransactionEventListener.kt` with `@TransactionalEventListener(phase = AFTER_COMMIT)` that logs the committed fact (Blueprint §8, ADR-007).
  Wire `eventPublisher.publishEvent(TransactionCreatedEvent(saved))` as step 6 in `ConvertCurrencyService`.
  *DoD*: Unit test addition to `ConvertCurrencyServiceTest` — `eventPublisher.publishEvent()` called exactly once on happy path; called zero times on idempotency-hit path.

- [ ] **B.15** — **`ListTransactionsService`**
  Create `application/ListTransactionsService.kt` implementing `ListTransactionsUseCase`. Single method: delegates to `transactionRepository.findByUserId()` with the correct `Pageable`.
  *DoD*: Unit test `ListTransactionsServiceTest` — verifies delegation to the repository with the `Pageable` from the query; returns the `Page<Transaction>` unchanged.

- [ ] **B.16** — **Concurrency test**
  Create `ConvertCurrencyServiceConcurrencyTest.kt` — Testcontainers Postgres + real `JpaTransactionRepositoryAdapter` + `CountDownLatch(1)` releases 10 threads simultaneously with the same `idempotency_key` (Blueprint §19.5). Exchange rate gateway is `@MockBean`.
  *DoD*: All 10 threads return successfully; all 10 return the **same** `transaction.id`; zero `DataIntegrityViolationException` exceptions leak to the caller; exactly **one** row in `transactions` table after the test.

---

## Track C — Resilience

**Goal**: The exchange rate gateway is protected by a circuit breaker and retry. The fallback always fails loudly. The idempotency path is closed at the HTTP surface.

- [ ] **C.1** — **`SecurityConfig`**
  Create `config/SecurityConfig.kt` — `SecurityFilterChain`: CSRF disabled, `STATELESS` session, permit `POST /api/v1/auth/**`, `/actuator/health`, `/actuator/info`, `/swagger-ui/**`, `/v3/api-docs/**`; authenticate everything else (Blueprint §11.1).
  *DoD*: `@SpringBootTest` slice — `GET /api/v1/transactions` without a token returns `401`; `POST /api/v1/auth/sign_in` without a token returns `200` or `400`, never `401`.

- [ ] **C.2** — **`JwtService`**
  Create `adapter/in/security/JwtService.kt` — JJWT 0.12.x: `generateToken(userId)`, `parseAndValidate(token)` returning claims, `extractJti(token)`, `extractExpiry(token)`. HS256, configurable secret + expiry from `application.yml`.
  *DoD*: Unit test `JwtServiceTest` — generated token parses back to correct `sub` and `userId` claims; expired token throws `ExpiredJwtException`; tampered signature throws `JwtException`.

- [ ] **C.3** — **`TokenRevocationService`**
  Create `adapter/in/security/TokenRevocationService.kt` — Redis-backed; `revoke(jti: String, ttl: Duration)` writes key; `isRevoked(jti: String): Boolean` checks existence.
  *DoD*: Unit test `TokenRevocationServiceTest` — uses `EmbeddedRedis` or `MockRedisTemplate`; revoked JTI returns `true`; non-revoked JTI returns `false`.

- [ ] **C.4** — **Security principals + token model**
  Create `adapter/in/security/UserPrincipal.kt` (implements `UserDetails`) and `adapter/in/security/JwtAuthenticationToken.kt` (extends `AbstractAuthenticationToken`).
  *DoD*: `./gradlew compileKotlin` passes. Both classes are pure data-holders with no business logic; unit test `UserPrincipalTest` constructs one and asserts `isAuthenticated()` returns `true` when credentials are populated.

- [ ] **C.5** — **`CustomAuthEntryPoint` + `CustomAccessDeniedHandler`**
  Create `adapter/in/security/CustomAuthEntryPoint.kt` (returns JSON `{"error":"Unauthorized"}` with `401`) and `CustomAccessDeniedHandler.kt` (returns JSON `{"error":"Forbidden"}` with `403`).
  *DoD*: Unit test — call `commence()` / `handle()` directly with a mock `HttpServletResponse`; assert status codes and response bodies.

- [ ] **C.6** — **`JwtAuthenticationFilter`**
  Create `adapter/in/security/JwtAuthenticationFilter.kt` (`OncePerRequestFilter`) — extracts Bearer token, calls `JwtService.parseAndValidate()`, checks `TokenRevocationService.isRevoked()`, sets `SecurityContextHolder` (Blueprint §11.2).
  *DoD*: `@WebMvcTest` `JwtAuthenticationFilterTest` with mocked `JwtService` + `TokenRevocationService` — valid token sets `SecurityContext`; revoked token returns `401`; missing header returns `401`; malformed token returns `401`.

- [ ] **C.7** — **`GlobalExceptionHandler` — domain exceptions**
  Create `adapter/in/rest/GlobalExceptionHandler.kt` (`@RestControllerAdvice`) with handlers for: `CurrencyNotSupportedException` → `422`, `ExchangeRateUnavailableException` → `503`.
  *DoD*: Unit test `GlobalExceptionHandlerTest` — call each handler method directly; assert HTTP status and `{"error":"...","code":"..."}` response body shape.

- [ ] **C.8** — **`GlobalExceptionHandler` — infrastructure + validation exceptions**
  Add handlers to `GlobalExceptionHandler.kt` for: `MethodArgumentNotValidException` → `422` (field errors list), `DataIntegrityViolationException` (idempotency key constraint) → `409`, `MissingRequestHeaderException` → `400`, fallback `Exception` → `500` (Blueprint §15).
  *DoD*: Unit test additions to `GlobalExceptionHandlerTest` — one test per new handler branch; assert status and body for each.

- [ ] **C.9** — **Transaction DTOs + `TransactionMapper`**
  Create `adapter/in/rest/dto/CreateTransactionRequest.kt` (with `@field:` Bean Validation annotations) and `TransactionResponse.kt`.
  Create `adapter/in/rest/mapper/TransactionMapper.kt` — `toResponse(domain: Transaction): TransactionResponse`.
  *DoD*: Unit test `TransactionMapperTest` — maps a domain `Transaction` to `TransactionResponse`; all fields present; no null leaks.

- [ ] **C.10** — **`TransactionController` — `POST /api/v1/transactions`**
  Create `adapter/in/rest/TransactionController.kt` with only the `POST` endpoint — reads `Idempotency-Key: UUID` header (required), validates body, delegates to `ConvertCurrencyUseCase`, returns `201` (Blueprint §10.2).
  *DoD*: `@WebMvcTest` `TransactionControllerTest` —
  - Valid body + valid JWT + valid header → `201` with correct response shape.
  - Missing `Idempotency-Key` header → `400`.
  - Invalid body (missing required field) → `422`.

- [ ] **C.11** — **`TransactionController` — `GET /api/v1/transactions`**
  Add `GET /api/v1/transactions` to `TransactionController` — `page`/`perPage` query params clamped to 1–100, delegates to `ListTransactionsUseCase`, returns `200` with `data` + `meta.totalCount` (Blueprint §10.2).
  *DoD*: `@WebMvcTest` additions to `TransactionControllerTest` —
  - `GET` without token → `401`.
  - `GET` with valid token → `200` + `meta.totalCount` matches.
  - `page` > 100 is clamped to 100.

- [ ] **C.12** — **`AuthController` — sign-up + sign-in**
  Create `adapter/in/rest/AuthController.kt` with `POST /api/v1/auth/sign_up` (BCrypt, unique email → `201`) and `POST /api/v1/auth/sign_in` (verify password, return JWT) (Blueprint §11.3).
  *DoD*: `@WebMvcTest` `AuthControllerTest` — sign-up with duplicate email → `409`; sign-in with wrong password → `401`; sign-in with correct credentials → `200` + non-empty `token` field.

- [ ] **C.13** — **`AuthController` — sign-out + full auth flow integration test**
  Add `POST /api/v1/auth/sign_out` to `AuthController` (revokes JTI → `204`).
  *DoD*: `@SpringBootTest` integration test `AuthFlowIT` — sign up → sign in → call `GET /api/v1/transactions` with token → sign out → same token on `GET /api/v1/transactions` returns `401`.

- [ ] **C.14** — **`CurrencyApiClient` + `CurrencyApiClientConfig`**
  Create `adapter/out/gateway/CurrencyApiClient.kt` (`@FeignClient`) with the `getLatestRates` method signature.
  Create `adapter/out/gateway/CurrencyApiClientConfig.kt` — injects `apikey` request header from `${CURRENCY_API_KEY}` (Blueprint §9.2).
  *DoD*: `./gradlew compileKotlin` passes; WireMock stub returns a valid JSON fixture; Feign client deserialises it correctly in a minimal `@SpringBootTest`.

- [ ] **C.15** — **`CurrencyApiGateway` — happy path + Resilience4j config**
  Create `adapter/out/gateway/CurrencyApiGateway.kt` implementing `ExchangeRateGateway`. Method decorated `@Cacheable` → `@CircuitBreaker` → `@Retry`; fallback method **always throws** `ExchangeRateUnavailableException`.
  Add Resilience4j YAML config: circuit breaker sliding window 10, failure rate 50%, wait 30s; retry max 3, exponential backoff ×2, ignore `CurrencyNotSupportedException` (Blueprint §12).
  *DoD*: WireMock test `CurrencyApiGatewayTest` — valid `200` response returns correct `BigDecimal`; `503` with retries exhausted throws `ExchangeRateUnavailableException`.

- [ ] **C.16** — **`CurrencyApiGateway` — circuit breaker test**
  Add circuit-breaker scenario to `CurrencyApiGatewayTest`.
  *DoD*: After 5 consecutive `503` responses the circuit opens; subsequent calls fail immediately (within < 5 ms) without hitting WireMock — verified via WireMock call count assertion.

- [ ] **C.17** — **`CacheConfig` + Redis cache integration test**
  Create `config/CacheConfig.kt` — `RedisCacheManager` with `exchangeRates` cache, 24h TTL, Jackson serializer (Blueprint §13).
  *DoD*: Integration test `ExchangeRateCacheIT` — `fetchRate(USD, BRL)` called twice; WireMock stub receives **exactly one** HTTP request (second call served from Redis cache). Cache key must include the calendar date.

---

## Track D — Senior Bonuses

**Goal**: Observability, rate limiting, and documentation that demonstrate production-ops thinking.

- [ ] **D.1** — **`MdcFilter`**
  Create `adapter/in/filter/MdcFilter.kt` (`OncePerRequestFilter` at `HIGHEST_PRECEDENCE`) — reads or generates `X-Correlation-ID`; populates MDC `correlationId`, `requestPath`, `requestMethod`; echoes header in response; clears MDC in `finally` block (Blueprint §16.1).
  *DoD*: Unit test — mock request with `X-Correlation-ID: test-123`; assert response header echoed; assert MDC populated correctly before `doFilterInternal` returns.

- [ ] **D.2** — **`MdcFilter` integration test**
  Add `MdcFilterIT` — full `@SpringBootTest` with `ListAppender` capturing Logback events.
  *DoD*: Assert response header contains `X-Correlation-ID: test-123`; assert at least one log event contains `correlationId=test-123` in MDC.

- [ ] **D.3** — **`logback-spring.xml`**
  Create `src/main/resources/logback-spring.xml` — `LogstashEncoder` for `prod` profile; human-readable pattern encoder for `dev` (Blueprint §16.2).
  *DoD*: Start the app with `spring.profiles.active=prod`; one request produces a log line that is valid JSON with fields `correlationId`, `level`, `message`, `timestamp`.

- [ ] **D.4** — **Actuator & circuit breaker health config**
  Verify/update `application.yml` — expose `health`, `metrics`, `prometheus`, `circuitbreakers` endpoints; `management.health.circuitbreakers.enabled=true` (Blueprint §16.3).
  *DoD*: `GET /actuator/health` returns `{"status":"UP","components":{"circuitBreakers":{"status":"UP",...}}}`. `GET /actuator/prometheus` contains `resilience4j_circuitbreaker_state`.

- [ ] **D.5** — **`RateLimitingConfig` + `RateLimitingFilter`**
  Create `config/RateLimitingConfig.kt` (Bucket4j bandwidths: 100 req/min global, 5 per 20s for auth paths) and `adapter/in/filter/RateLimitingFilter.kt` (returns `429` with `X-RateLimit-Limit` / `X-RateLimit-Remaining` headers) (Blueprint §17).
  *DoD*: Integration test — fire 6 `POST /api/v1/auth/sign_in` requests in < 1 second from the same IP; assert the 6th returns `429` with the correct headers.

- [ ] **D.6** — **`OpenApiConfig` + controller annotations**
  Create `config/OpenApiConfig.kt` — `OpenAPI` bean with `SecurityScheme(bearerAuth)` (Blueprint §22).
  Add `@Operation` + `@SecurityRequirement(name = "bearerAuth")` to all `TransactionController` and `AuthController` methods.
  *DoD*: `GET /swagger-ui/index.html` renders; `POST /api/v1/auth/sign_in` executable from UI; token from sign-in authorises `POST /api/v1/transactions` in the UI.

- [ ] **D.7** — **Coverage + Detekt**
  Run `./gradlew test jacocoTestReport` — identify and fill gaps until line coverage ≥ 80%.
  Run `./gradlew detekt` — fix all violations.
  *DoD*: `jacocoTestReport` HTML shows ≥ 80% line coverage; `./gradlew detekt` exits 0.

- [ ] **D.8** — **`README.md` + smoke test**
  Write `README.md` — setup instructions, environment variable table, `make` targets, architecture summary, link to `KOTLIN_MIGRATION_BLUEPRINT.md`.
  Run `make setup` from a clean clone and execute:
  ```
  POST /api/v1/auth/sign_up   → 201
  POST /api/v1/auth/sign_in   → 200 + JWT
  POST /api/v1/transactions   → 201  (with Idempotency-Key header)
  POST /api/v1/transactions   → 409  (same Idempotency-Key)
  GET  /api/v1/transactions   → 200 + paginated list
  GET  /actuator/health       → {"status":"UP", circuitBreaker: CLOSED}
  ```
  *DoD*: All smoke tests pass; README is readable by someone who has never seen the project.

---

## Summary & Priority Reference

| Track | Tasks | Count | Interview Signal | Do First? |
|---|---|---|---|---|
| **A — Infra/Setup** | A.1–A.10 | 10 | Foundation — nothing works without it | ✅ Yes |
| **B — Core Domain** | B.1–B.16 | 16 | **P0** — the primary differentiator | ✅ Yes |
| **C — Resilience** | C.1–C.17 | 17 | **P1** — closes the banking-grade story | ✅ Yes |
| **D — Senior Bonuses** | D.1–D.8 | 8 | **P2/P3** — polish and production-ops | Only after A–C are green |
| **Total** | | **51** | | |

### Why this granularity?
Each task is scoped to **one production file + one test file** — the right size for a single LLM prompt or a single focused commit. Prompting with a task this size means:
- The LLM has a single, unambiguous goal.
- The DoD is immediately verifiable (one `./gradlew test` run).
- A failure is isolated and cheap to retry.
- No task requires holding more than ~2 files in context simultaneously.

**Minimum viable demo (if time-constrained)**: Complete A + B.1–B.16 + C.1–C.16.
That covers: Hexagonal architecture, zero-framework domain, `@Transactional` boundary, idempotency with concurrency proof, JWT auth, circuit breaker, and the full test pyramid. Everything else is polish.

**Build file fix required before writing a single line of application code** (from Audit §3):
```kotlin
// REMOVE:
testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")
implementation("io.github.resilience4j:resilience4j-feign:2.2.0")

// ADD:
testImplementation("org.wiremock:wiremock:3.5.4")
testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.4"))
```

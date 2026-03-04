# Currency Converter — Kotlin/Spring Boot Migration Blueprint

**Source**: Ruby on Rails 7.1 (Mid-level 3 challenge)  
**Target**: Kotlin 1.9 + Spring Boot 3.x  
**Purpose**: Architectural-grade rewrite demonstrating Senior/Principal-level engineering for a financial services platform  
**Date**: February 2026

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [What the Rails Version Got Right](#2-what-the-rails-version-got-right)
3. [Architecture Decision: Hexagonal vs. Layered](#3-architecture-decision-hexagonal-vs-layered)
4. [Architecture Decision: RDBMS vs. NoSQL](#4-architecture-decision-rdbms-vs-nosql)
5. [Target Project Structure](#5-target-project-structure)
6. [Build File — build.gradle.kts](#6-build-file--buildgradlekts)
7. [Domain Layer — The Core](#7-domain-layer--the-core)
8. [Application Layer — Use Cases](#8-application-layer--use-cases)
9. [Infrastructure Layer — Outbound Adapters](#9-infrastructure-layer--outbound-adapters)
10. [REST Adapter — Inbound Layer](#10-rest-adapter--inbound-layer)
11. [Security Configuration](#11-security-configuration)
12. [Resilience — Circuit Breaker, Retry, Rate Limiter](#12-resilience--circuit-breaker-retry-rate-limiter)
13. [Caching](#13-caching)
14. [Idempotency — Banking-Grade Critical](#14-idempotency--banking-grade-critical)
15. [Error Handling](#15-error-handling)
16. [Observability — Logging, MDC, Actuator](#16-observability--logging-mdc-actuator)
17. [Rate Limiting](#17-rate-limiting)
18. [Database Schema and Migrations](#18-database-schema-and-migrations)
19. [Testing Strategy](#19-testing-strategy)
20. [Docker and Infrastructure](#20-docker-and-infrastructure)
21. [Application Properties](#21-application-properties)
22. [Architecture Decision Records (ADRs)](#22-architecture-decision-records-adrs)
23. [Implementation Roadmap](#23-implementation-roadmap)

---

## 1. Executive Summary

This document is the complete blueprint for rewriting the Currency Converter API from Ruby on Rails into Kotlin with Spring Boot 3. It is designed not merely to produce a working clone, but to demonstrate **Senior Principal-level architectural reasoning** — the kind required at major digital banks like C6.

The Rails version is clean and readable. It correctly uses Service Objects, `BigDecimal` for monetary precision, JWT revocation, structured logging, and retry logic. These are strong foundations. The gaps are not in what exists but in **what it stops short of**:

- No **idempotency guarantees** on `POST /transactions` (a retry creates a duplicate)
- Business logic is not **isolated from the framework** (service objects depend on ActiveRecord directly)
- **Retry without circuit breaking** can amplify a downstream outage into thread pool exhaustion
- **No `@Transactional` boundary** wrapping rate-fetch + persist atomically
- No **domain events** for auditability or future event-driven extension
- No **correlation IDs** to trace a single request across all log lines
- **`HALF_UP` rounding** (Rails default) instead of `HALF_EVEN` (banker's rounding)

Each of these is addressed in this blueprint with an explanation of the trade-off, not just the solution.

---

## 2. What the Rails Version Got Right

Before the critique, name what is already senior-grade. These decisions should be preserved and their reasoning articulated explicitly in the Kotlin version:

| Decision | Why it demonstrates seniority |
|---|---|
| Immutable transactions (append-only, no update) | Natural audit trail; a core banking primitive |
| `BigDecimal` for all monetary values | Correct — `Float` is forbidden for money |
| Service objects (`Transactions::Create`) | Separates business logic from HTTP concerns |
| JTI token revocation in the database | Stateful JWT — enables instant sign-out, unlike pure stateless JWT |
| Custom error hierarchy (`ApplicationError`) | Structured, consistent error responses with HTTP codes |
| Structured JSON logs with event names | Grep-able from day one; ready for Datadog/ELK without changes |
| Exponential backoff on HTTP retries | Avoids thundering-herd on upstream recovery |
| Redis cache with daily TTL keys | Correc cache-key strategy (`rate:USD:BRL:2026-02-28`) |
| Database indexes on `(user_id, timestamp)` | Shows understanding of query patterns, not just data shape |
| API versioning via `/api/v1/` namespace | Future-proofs against breaking changes |

**The interview narrative**: "I preserved all of these decisions because they are correct. The rewrite is not a repudiation — it is a translation to a stricter type system and a richer framework ecosystem, plus closing specific gaps in resilience, idempotency, and domain isolation."

---

## 3. Architecture Decision: Hexagonal vs. Layered

### The Trade-off

| | Classic Layered | Hexagonal / Ports & Adapters |
|---|---|---|
| Cognitive overhead | Near zero for CRUD | Higher initial setup |
| Framework coupling | Business logic may import `@Entity`, `@Repository` | Domain layer has zero framework imports |
| Testability | Mock the repository | Swap the adapter; no mocking framework needed |
| Adapter flexibility | One deployment target assumed | Multiple adapters (REST, gRPC, CLI, event consumer) are natural |
| When it pays off | Single, stable infrastructure | Infra change likely; multiple input/output channels |
| Risk | Refactoring cost grows as framework coupling accumulates | Over-engineering risk if domain never changes |

### The Decision

Adopt **lightweight Hexagonal Architecture** — not full CQRS/Event Sourcing. The domain has one aggregate (`Transaction`), one write operation, and one external dependency (CurrencyAPI). A full event-sourced design would be over-engineering. However, isolating the `ExchangeRateGateway` behind a port interface is the single abstraction that earns its cost immediately: it enables testing the use case with a pure stub and makes swapping CurrencyAPI.com for any other provider a one-class change.

### The Senior Narrative

> "I chose Hexagonal because the domain invariant — *a recorded conversion must survive any infrastructure change* — must not be coupled to any framework. The `Transaction` aggregate is a Kotlin data class with zero Spring annotations. The use case owns the `@Transactional` boundary. The adapters (REST controller, JPA repository, Feign client) are implementation details. If we needed to add a gRPC interface tomorrow, we'd write one new inbound adapter and touch nothing else. That is the concrete, measurable payoff of the abstraction."

---

## 4. Architecture Decision: RDBMS vs. NoSQL

### The Trade-off

| | PostgreSQL (RDBMS) | MongoDB / DynamoDB (NoSQL) |
|---|---|---|
| ACID transactions | Native, multi-row | Limited (MongoDB 4+) / Single-item only |
| Schema enforcement | Strong, DB-level constraints | Flexible but requires app-level enforcement |
| Complex queries | Native SQL, joins, aggregations | Requires denormalization or multiple reads |
| Horizontal write scaling | Vertical scaling primary path | Native horizontal sharding |
| Operability | Mature, well-understood | Requires expertise |
| For *this* problem | ✅ Strong fit | ❌ Trading guarantees for scale not needed here |

### The Decision

PostgreSQL. The transaction records in this system are the canonical use case for relational databases: they must be atomic (rate capture + record persist), consistent (schema constraints), isolated (no phantom reads during listing), and durable (survives crashes). A document store removes the atomicity guarantee we cannot afford to lose.

### The Senior Narrative

> "The question 'RDBMS or NoSQL?' is only answerable after you define your consistency model. Our primary invariant is that a `Transaction` record, once written, is immutable and represents a fact. Facts must be durable and consistent. PostgreSQL with a proper `@Transactional` boundary gives us that. I would revisit this only if the write pattern demanded horizontal sharding across hundreds of nodes — a problem this application will not have unless it becomes a core banking ledger, which is a fundamentally different domain and would warrant event sourcing, not just NoSQL."

---

## 5. Target Project Structure

```
currency-converter-kotlin/
├── build.gradle.kts
├── settings.gradle.kts
├── docker-compose.yml
├── Makefile
│
└── src/
    ├── main/
    │   ├── kotlin/
    │   │   └── com/fintech/currencyconverter/
    │   │       │
    │   │       ├── domain/                         # Zero framework dependencies
    │   │       │   ├── model/
    │   │       │   │   ├── Transaction.kt           # Pure data class — the aggregate
    │   │       │   │   ├── Money.kt                 # Value object: amount + currency
    │   │       │   │   └── SupportedCurrency.kt     # Enum — not raw String
    │   │       │   ├── port/
    │   │       │   │   ├── in/
    │   │       │   │   │   ├── ConvertCurrencyUseCase.kt   # Interface driven by controller
    │   │       │   │   │   └── ListTransactionsUseCase.kt  # Interface driven by controller
    │   │       │   │   └── out/
    │   │       │   │       ├── TransactionRepository.kt    # Interface driven by service
    │   │       │   │       └── ExchangeRateGateway.kt      # Interface driven by service
    │   │       │   └── exception/
    │   │       │       ├── CurrencyNotSupportedException.kt
    │   │       │       ├── ExchangeRateUnavailableException.kt
    │   │       │       └── DuplicateTransactionException.kt
    │   │       │
    │   │       ├── application/                    # Orchestration — owns @Transactional
    │   │       │   ├── ConvertCurrencyService.kt   # Implements ConvertCurrencyUseCase
    │   │       │   └── ListTransactionsService.kt  # Implements ListTransactionsUseCase
    │   │       │
    │   │       └── adapter/
    │   │           ├── in/
    │   │           │   └── rest/
    │   │           │       ├── TransactionController.kt
    │   │           │       ├── AuthController.kt
    │   │           │       ├── dto/
    │   │           │       │   ├── ConvertCurrencyRequest.kt
    │   │           │       │   ├── ConvertCurrencyResponse.kt
    │   │           │       │   ├── LoginRequest.kt
    │   │           │       │   ├── LoginResponse.kt
    │   │           │       │   ├── RegisterRequest.kt
    │   │           │       │   └── PagedResponse.kt
    │   │           │       └── mapper/
    │   │           │           └── TransactionMapper.kt
    │   │           └── out/
    │   │               ├── persistence/
    │   │               │   ├── JpaTransactionRepository.kt  # Spring Data impl of domain port
    │   │               │   ├── JpaUserRepository.kt
    │   │               │   ├── entity/
    │   │               │   │   ├── TransactionEntity.kt
    │   │               │   │   └── UserEntity.kt
    │   │               │   └── mapper/
    │   │               │       └── TransactionEntityMapper.kt
    │   │               └── exchange/
    │   │                   ├── CurrencyApiGateway.kt        # Feign impl of domain port
    │   │                   ├── CurrencyApiClient.kt         # Feign interface
    │   │                   └── dto/
    │   │                       └── CurrencyApiResponse.kt
    │   │
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    │           ├── V1__create_users.sql
    │           ├── V2__create_transactions.sql
    │           └── V3__add_idempotency_key.sql
    │
    └── test/
        └── kotlin/
            └── com/fintech/currencyconverter/
                ├── application/
                │   └── ConvertCurrencyServiceTest.kt
                ├── adapter/
                │   ├── in/rest/
                │   │   └── TransactionControllerTest.kt
                │   └── out/exchange/
                │       └── CurrencyApiGatewayTest.kt
                └── domain/
                    ├── model/
                    │   └── TransactionTest.kt
                    └── model/
                        └── MoneyTest.kt
```

---

## 6. Build File — build.gradle.kts

```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
    id("org.flywaydb.flyway") version "10.8.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.5"
    jacoco
}

group = "com.fintech"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // HTTP Client (Feign for external API calls)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Resilience4j — Circuit Breaker, Retry, Rate Limiter
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    // resilience4j-feign removed: annotation-driven approach (@CircuitBreaker, @Retry)
    // via resilience4j-spring-boot3 + spring-boot-starter-aop is sufficient.
    // resilience4j-feign is only needed if using the FeignDecorators builder API directly.
    implementation("org.springframework.boot:spring-boot-starter-aop") // Required by Resilience4j

    // Rate Limiting (Bucket4j)
    implementation("com.bucket4j:bucket4j-core:8.9.0")
    implementation("com.bucket4j:bucket4j-redis:8.9.0")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    // Testcontainers BOM keeps all TC artifact versions in sync
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.4"))
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    // wiremock-jre8 is incompatible with Java 21's module system — use standalone instead
    testImplementation("org.wiremock:wiremock:3.5.4")
    testRuntimeOnly("com.h2database:h2") // Fast unit tests only
}

// Spring Cloud BOM for Feign
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"   // Strict null safety for Spring annotations
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    dependsOn(tasks.test)
}
```

---

## 7. Domain Layer — The Core

This layer has **zero Spring annotations** and **zero framework imports**. This is the architectural boundary. If you need to import `org.springframework.*` here, you have violated the boundary.

### 7.1 SupportedCurrency — Enum, Not String

**Why not `String`**: The Rails version validates currencies against a constant array. This is a runtime check. Kotlin's type system can make an invalid currency **unrepresentable** at compile time.

```kotlin
// domain/model/SupportedCurrency.kt
package com.fintech.currencyconverter.domain.model

enum class SupportedCurrency {
    BRL, USD, EUR, JPY;

    companion object {
        fun fromCode(code: String): SupportedCurrency =
            values().firstOrNull { it.name == code.uppercase() }
                ?: throw CurrencyNotSupportedException(code)
    }
}
```

**Interview point**: "The type-level guarantee replaces a runtime array inclusion check. A `SupportedCurrency.UNKNOWN` is a compile error, not a `422` at runtime. The validation logic moves from the service layer into the type system itself."

### 7.2 Money — Value Object

```kotlin
// domain/model/Money.kt
package com.fintech.currencyconverter.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Value object representing a monetary amount in a specific currency.
 *
 * Design notes:
 * - Immutable: all operations return a new Money instance
 * - Uses HALF_EVEN (banker's rounding) to eliminate systematic bias
 *   when aggregating large volumes of conversions. HALF_UP introduces
 *   a predictable upward bias that compounds over millions of records.
 * - Scale is fixed at 4 decimal places — sufficient for all supported
 *   currency pairs and their standard tick sizes.
 */
data class Money(
    val amount: BigDecimal,
    val currency: SupportedCurrency
) {
    init {
        require(amount >= BigDecimal.ZERO) { "Monetary amount cannot be negative: $amount" }
    }

    fun convertTo(targetCurrency: SupportedCurrency, rate: BigDecimal): Money {
        require(rate > BigDecimal.ZERO) { "Conversion rate must be positive: $rate" }
        val converted = amount.multiply(rate).setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN)
        return Money(converted, targetCurrency)
    }

    override fun toString(): String = "${amount.toPlainString()} ${currency.name}"

    companion object {
        const val MONETARY_SCALE = 4
        const val RATE_SCALE = 8

        fun of(amount: BigDecimal, currency: SupportedCurrency) = Money(
            amount.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN),
            currency
        )
    }
}
```

### 7.3 Transaction — The Aggregate Root

```kotlin
// domain/model/Transaction.kt
package com.fintech.currencyconverter.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Transaction aggregate root.
 *
 * Invariants enforced in the factory function (not the constructor):
 * - Source and target currencies must differ
 * - Rate must be positive
 * - Amount must be positive
 * - Converted amount is derived deterministically from amount × rate
 *
 * Immutability:
 * Transactions are facts — once created, they are never updated.
 * This is enforced at the persistence layer (no update methods exposed)
 * and reinforced here by using a data class with only val properties.
 */
data class Transaction(
    val id: UUID = UUID.randomUUID(),
    val userId: Long,
    val idempotencyKey: UUID,
    val sourceMoney: Money,
    val targetMoney: Money,
    val rate: BigDecimal,
    val convertedAt: Instant = Instant.now(),
) {
    companion object {
        /**
         * The single creation path for a Transaction.
         * All domain validation is concentrated here.
         *
         * Note: 'id' and 'convertedAt' are not arguments — the domain
         * assigns identity, not the caller.
         */
        fun create(
            userId: Long,
            idempotencyKey: UUID,
            amount: BigDecimal,
            fromCurrency: SupportedCurrency,
            toCurrency: SupportedCurrency,
            rate: BigDecimal,
        ): Transaction {
            require(fromCurrency != toCurrency) {
                "Source and target currencies must differ: $fromCurrency"
            }
            require(rate > BigDecimal.ZERO) { "Rate must be positive: $rate" }
            require(amount > BigDecimal.ZERO) { "Amount must be positive: $amount" }

            val sourceMoney = Money.of(amount, fromCurrency)
            val targetMoney = sourceMoney.convertTo(toCurrency, rate)

            return Transaction(
                userId          = userId,
                idempotencyKey  = idempotencyKey,
                sourceMoney     = sourceMoney,
                targetMoney     = targetMoney,
                rate            = rate.setScale(Money.RATE_SCALE, java.math.RoundingMode.HALF_EVEN),
            )
        }
    }
}
```

### 7.4 Domain Exceptions

```kotlin
// domain/exception/CurrencyNotSupportedException.kt
package com.fintech.currencyconverter.domain.exception

class CurrencyNotSupportedException(
    val currencyCode: String,
    val supportedCurrencies: List<String> = listOf("BRL", "USD", "EUR", "JPY")
) : RuntimeException("Currency '$currencyCode' is not supported. Supported: $supportedCurrencies")

// domain/exception/ExchangeRateUnavailableException.kt
class ExchangeRateUnavailableException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// domain/exception/DuplicateTransactionException.kt
class DuplicateTransactionException(
    val idempotencyKey: java.util.UUID
) : RuntimeException("Transaction with idempotency key '$idempotencyKey' already exists")
```

### 7.5 Outbound Ports (Interfaces)

```kotlin
// domain/port/out/ExchangeRateGateway.kt
package com.fintech.currencyconverter.domain.port.out

import com.fintech.currencyconverter.domain.model.SupportedCurrency
import java.math.BigDecimal

/**
 * Port for fetching live exchange rates.
 *
 * This interface is what the application layer depends on.
 * The actual implementation (CurrencyAPI.com, a mock, a DB snapshot)
 * is an infrastructure concern injected from outside.
 *
 * Contract: must always return a positive rate or throw ExchangeRateUnavailableException.
 */
interface ExchangeRateGateway {
    fun fetchRate(from: SupportedCurrency, to: SupportedCurrency): BigDecimal
}

// domain/port/out/TransactionRepository.kt
import com.fintech.currencyconverter.domain.model.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface TransactionRepository {
    fun save(transaction: Transaction): Transaction
    fun findByIdempotencyKey(key: UUID): Transaction?
    fun findByUserId(userId: Long, pageable: Pageable): Page<Transaction>
}
```

### 7.6 Inbound Ports (Use Case Interfaces)

```kotlin
// domain/port/in/ConvertCurrencyUseCase.kt
package com.fintech.currencyconverter.domain.port.in

import com.fintech.currencyconverter.domain.model.Transaction
import java.math.BigDecimal
import java.util.UUID

data class ConvertCurrencyCommand(
    val userId: Long,
    val idempotencyKey: UUID,
    val fromCurrencyCode: String,
    val toCurrencyCode: String,
    val amount: BigDecimal,
)

interface ConvertCurrencyUseCase {
    fun convert(command: ConvertCurrencyCommand): Transaction
}

// domain/port/in/ListTransactionsUseCase.kt
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

data class ListTransactionsQuery(val userId: Long, val pageable: Pageable)

interface ListTransactionsUseCase {
    fun list(query: ListTransactionsQuery): Page<Transaction>
}
```

---

## 8. Application Layer — Use Cases

The application layer owns the `@Transactional` boundary. It orchestrates domain objects and outbound ports. It has no knowledge of HTTP, JSON, or database technology.

```kotlin
// application/ConvertCurrencyService.kt
package com.fintech.currencyconverter.application

import com.fintech.currencyconverter.domain.model.SupportedCurrency
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.port.`in`.ConvertCurrencyCommand
import com.fintech.currencyconverter.domain.port.`in`.ConvertCurrencyUseCase
import com.fintech.currencyconverter.domain.port.out.ExchangeRateGateway
import com.fintech.currencyconverter.domain.port.out.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConvertCurrencyService(
    private val transactionRepository: TransactionRepository,
    private val exchangeRateGateway: ExchangeRateGateway,
    private val eventPublisher: ApplicationEventPublisher,
) : ConvertCurrencyUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * @Transactional wraps the entire unit of work:
     *   1. Idempotency check
     *   2. Rate fetch (result passed into transaction, not the fetch itself)
     *   3. Domain object creation
     *   4. Persistence
     *
     * The @Transactional boundary is here — at the USE CASE level — not at the
     * repository level. The service owns the business invariant, therefore the
     * service owns the unit of work.
     *
     * If the DB write fails after the rate is fetched, the entire operation rolls
     * back. The Rails version lacked this guarantee: save! was called outside any
     * explicit transaction, meaning a DB failure after a successful API call would
     * silently lose the conversion event.
     */
    @Transactional
    override fun convert(command: ConvertCurrencyCommand): Transaction {
        // Step 1: Idempotency check — return the cached result if seen before
        transactionRepository.findByIdempotencyKey(command.idempotencyKey)?.let { existing ->
            log.info("idempotency.hit idempotencyKey={} transactionId={}", command.idempotencyKey, existing.id)
            return existing
        }

        // Step 2: Resolve currency types (throws CurrencyNotSupportedException if invalid)
        val fromCurrency = SupportedCurrency.fromCode(command.fromCurrencyCode)
        val toCurrency = SupportedCurrency.fromCode(command.toCurrencyCode)

        // Step 3: Fetch live rate — this may throw ExchangeRateUnavailableException
        val rate = exchangeRateGateway.fetchRate(fromCurrency, toCurrency)
        log.info("rate.fetched from={} to={} rate={}", fromCurrency, toCurrency, rate)

        // Step 4: Create domain object — all invariants validated inside Transaction.create()
        val transaction = Transaction.create(
            userId         = command.userId,
            idempotencyKey = command.idempotencyKey,
            amount         = command.amount,
            fromCurrency   = fromCurrency,
            toCurrency     = toCurrency,
            rate           = rate,
        )

        // Step 5: Persist
        val saved = transactionRepository.save(transaction)

        // Step 6: Publish domain event AFTER commit (see @TransactionalEventListener below)
        eventPublisher.publishEvent(TransactionCreatedEvent(saved))

        return saved
    }
}
```

### Domain Event — After-Commit Only

```kotlin
// application/TransactionCreatedEvent.kt
data class TransactionCreatedEvent(val transaction: Transaction)

// application/TransactionEventListener.kt
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class TransactionEventListener {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * AFTER_COMMIT is critical here.
     *
     * Firing at BEFORE_COMMIT would risk publishing an event for a transaction
     * that then fails to persist (e.g., DB constraint violation). The event
     * would describe an intent, not a fact. Downstream consumers that act on
     * intents introduce very difficult consistency bugs.
     *
     * This annotation guarantees: "this event represents something that happened."
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onTransactionCreated(event: TransactionCreatedEvent) {
        val t = event.transaction
        log.info(
            "transaction.created id={} userId={} pair={}/{} amount={} rate={}",
            t.id, t.userId, t.sourceMoney.currency, t.targetMoney.currency,
            t.sourceMoney.amount, t.rate
        )
        // Future: eventPublisher.send("currency.conversions", event)
        // The @TransactionalEventListener makes this upgrade path safe:
        // the Kafka send would also only happen post-commit.
    }
}
```

---

## 9. Infrastructure Layer — Outbound Adapters

### 9.1 JPA Persistence Adapter

```kotlin
// adapter/out/persistence/entity/TransactionEntity.kt
package com.fintech.currencyconverter.adapter.out.persistence.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "transactions",
    indexes = [
        Index(name = "idx_transactions_user_timestamp", columnList = "user_id, converted_at"),
        Index(name = "idx_transactions_idempotency_key", columnList = "idempotency_key", unique = true),
        Index(name = "idx_transactions_currency_pair", columnList = "from_currency, to_currency"),
    ]
)
class TransactionEntity(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "idempotency_key", nullable = false, unique = true, columnDefinition = "UUID")
    val idempotencyKey: UUID,

    @Column(name = "from_currency", nullable = false, length = 3)
    val fromCurrency: String,

    @Column(name = "to_currency", nullable = false, length = 3)
    val toCurrency: String,

    @Column(name = "from_amount", nullable = false, precision = 19, scale = 4)
    val fromAmount: BigDecimal,

    @Column(name = "to_amount", nullable = false, precision = 19, scale = 4)
    val toAmount: BigDecimal,

    // Scale 8 for rate — sufficient for all forex pairs (e.g., USD/JPY ~= 148.12345678)
    @Column(name = "rate", nullable = false, precision = 19, scale = 8)
    val rate: BigDecimal,

    @Column(name = "converted_at", nullable = false)
    val convertedAt: Instant = Instant.now(),
)
```

```kotlin
// adapter/out/persistence/JpaTransactionRepository.kt
@Repository
class JpaTransactionRepositoryAdapter(
    private val springRepo: SpringJpaTransactionRepository,
    private val mapper: TransactionEntityMapper,
) : TransactionRepository {  // <-- implements the DOMAIN port, not Spring Data directly

    override fun save(transaction: Transaction): Transaction {
        val entity = mapper.toEntity(transaction)
        return mapper.toDomain(springRepo.save(entity))
    }

    override fun findByIdempotencyKey(key: UUID): Transaction? =
        springRepo.findByIdempotencyKey(key)?.let(mapper::toDomain)

    override fun findByUserId(userId: Long, pageable: Pageable): Page<Transaction> =
        springRepo.findByUserIdOrderByConvertedAtDesc(userId, pageable).map(mapper::toDomain)
}

// Internal Spring Data interface — never exposed to the domain
interface SpringJpaTransactionRepository : JpaRepository<TransactionEntity, UUID> {
    fun findByIdempotencyKey(key: UUID): TransactionEntity?
    fun findByUserIdOrderByConvertedAtDesc(userId: Long, pageable: Pageable): Page<TransactionEntity>
}
```

### 9.2 Exchange Rate Gateway with Resilience4j

```kotlin
// adapter/out/exchange/CurrencyApiGateway.kt
package com.fintech.currencyconverter.adapter.out.exchange

import com.fintech.currencyconverter.domain.exception.ExchangeRateUnavailableException
import com.fintech.currencyconverter.domain.model.SupportedCurrency
import com.fintech.currencyconverter.domain.port.out.ExchangeRateGateway
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class CurrencyApiGateway(
    private val currencyApiClient: CurrencyApiClient,
) : ExchangeRateGateway {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Annotation order is significant:
     * @CircuitBreaker wraps @Retry. If the circuit is OPEN, Retry is never attempted.
     * This is correct — retrying against an open circuit is pointless and harmful.
     *
     * @Cacheable is applied first (outer-most). A cache hit never reaches the circuit
     * breaker or retry at all.
     *
     * Cache key strategy: currency pair + calendar day.
     * Rationale: exchange rates change daily; a 24-hour TTL aligns with market close.
     * A more sophisticated approach would use a shorter TTL during market hours
     * and a daily key when markets are closed. This is documented as a known trade-off.
     */
    @Cacheable(
        value = ["exchangeRates"],
        key = "#from.name() + ':' + #to.name() + ':' + T(java.time.LocalDate).now()"
    )
    @CircuitBreaker(name = "currencyApi", fallbackMethod = "fetchRateFallback")
    @Retry(name = "currencyApi")
    override fun fetchRate(from: SupportedCurrency, to: SupportedCurrency): BigDecimal {
        if (from == to) return BigDecimal.ONE

        log.info("exchange_rate.fetch.start from={} to={}", from, to)
        val start = System.currentTimeMillis()

        return try {
            val response = currencyApiClient.getLatest(
                baseCurrency = from.name,
                currencies   = to.name
            )
            val rate = response.data[to.name]?.value?.toBigDecimal()
                ?: throw ExchangeRateUnavailableException("No rate returned for $from → $to")

            log.info(
                "exchange_rate.fetch.success from={} to={} rate={} durationMs={}",
                from, to, rate, System.currentTimeMillis() - start
            )
            rate
        } catch (ex: ExchangeRateUnavailableException) {
            throw ex  // domain exception — don't wrap again
        } catch (ex: Exception) {
            log.error(
                "exchange_rate.fetch.error from={} to={} durationMs={} error={}",
                from, to, System.currentTimeMillis() - start, ex.message
            )
            throw ExchangeRateUnavailableException("Failed to fetch rate $from → $to: ${ex.message}", ex)
        }
    }

    /**
     * Circuit breaker fallback.
     * Called when the circuit is OPEN or when all retry attempts are exhausted.
     *
     * We do NOT return a stale/cached rate here. Serving a potentially wrong rate
     * is worse than refusing to serve — a customer's financial decision would be
     * based on incorrect data. Fail loudly instead.
     */
    @Suppress("UNUSED_PARAMETER")
    fun fetchRateFallback(
        from: SupportedCurrency,
        to: SupportedCurrency,
        ex: Exception
    ): BigDecimal {
        log.error("exchange_rate.circuit_open from={} to={} cause={}", from, to, ex.message)
        throw ExchangeRateUnavailableException(
            "Exchange rate service is temporarily unavailable. Please try again later."
        )
    }
}
```

```kotlin
// adapter/out/exchange/CurrencyApiClient.kt
package com.c6bank.currencyconverter.adapter.out.exchange

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.math.BigDecimal

@FeignClient(
    name   = "currencyApi",
    url    = "\${currency-api.base-url}",
    configuration = [CurrencyApiClientConfig::class]
)
interface CurrencyApiClient {
    @GetMapping("/latest")
    fun getLatest(
        @RequestParam("base_currency") baseCurrency: String,
        @RequestParam("currencies") currencies: String,
    ): CurrencyApiResponse
}

data class CurrencyApiResponse(
    val data: Map<String, CurrencyData>
)

data class CurrencyData(
    val code: String,
    val value: Double,
)

// Configuration: injects the API key into every request
@Configuration
class CurrencyApiClientConfig(
    @Value("\${currency-api.api-key}") private val apiKey: String
) {
    @Bean
    fun requestInterceptor(): RequestInterceptor = RequestInterceptor { template ->
        template.header("apikey", apiKey)
    }
}
```

---

## 10. REST Adapter — Inbound Layer

### 10.1 Request / Response DTOs

```kotlin
// adapter/in/rest/dto/ConvertCurrencyRequest.kt
package com.c6bank.currencyconverter.adapter.`in`.rest.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class ConvertCurrencyRequest(
    @field:NotBlank(message = "Source currency is required")
    val fromCurrency: String,

    @field:NotBlank(message = "Target currency is required")
    val toCurrency: String,

    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    val amount: BigDecimal,
)

// adapter/in/rest/dto/ConvertCurrencyResponse.kt
data class ConvertCurrencyResponse(
    val id: String,
    val userId: Long,
    val fromCurrency: String,
    val toCurrency: String,
    val fromAmount: String,   // String — never Double for monetary values in JSON
    val toAmount: String,
    val rate: String,
    val convertedAt: String,
)

// adapter/in/rest/dto/PagedResponse.kt
data class PagedResponse<T>(
    val data: List<T>,
    val meta: PageMeta,
)

data class PageMeta(
    val page: Int,
    val perPage: Int,
    val totalPages: Int,
    val totalCount: Long,
)
```

### 10.2 Transaction Controller

```kotlin
// adapter/in/rest/TransactionController.kt
package com.c6bank.currencyconverter.adapter.`in`.rest

import com.c6bank.currencyconverter.adapter.`in`.rest.dto.*
import com.c6bank.currencyconverter.domain.port.`in`.ConvertCurrencyCommand
import com.c6bank.currencyconverter.domain.port.`in`.ConvertCurrencyUseCase
import com.c6bank.currencyconverter.domain.port.`in`.ListTransactionsQuery
import com.c6bank.currencyconverter.domain.port.`in`.ListTransactionsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/transactions")
@SecurityRequirement(name = "bearerAuth")
class TransactionController(
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val listTransactionsUseCase: ListTransactionsUseCase,
    private val mapper: TransactionMapper,
) {

    @GetMapping
    @Operation(summary = "List all transactions for the authenticated user")
    fun list(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") perPage: Int,
    ): ResponseEntity<PagedResponse<ConvertCurrencyResponse>> {
        val clampedPerPage = perPage.coerceIn(1, 100)
        val pageResult = listTransactionsUseCase.list(
            ListTransactionsQuery(
                userId   = user.id,
                pageable = PageRequest.of(page - 1, clampedPerPage),
            )
        )
        return ResponseEntity.ok(
            PagedResponse(
                data = pageResult.content.map(mapper::toResponse),
                meta = PageMeta(
                    page       = page,
                    perPage    = clampedPerPage,
                    totalPages = pageResult.totalPages,
                    totalCount = pageResult.totalElements,
                ),
            )
        )
    }

    @PostMapping
    @Operation(summary = "Create a new currency conversion transaction")
    fun create(
        @RequestHeader("Idempotency-Key") idempotencyKey: UUID,
        @Valid @RequestBody request: ConvertCurrencyRequest,
        @AuthenticationPrincipal user: UserPrincipal,
    ): ResponseEntity<ConvertCurrencyResponse> {
        val transaction = convertCurrencyUseCase.convert(
            ConvertCurrencyCommand(
                userId           = user.id,
                idempotencyKey   = idempotencyKey,
                fromCurrencyCode = request.fromCurrency,
                toCurrencyCode   = request.toCurrency,
                amount           = request.amount,
            )
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(mapper.toResponse(transaction))
    }
}
```

---

## 11. Security Configuration

### 11.1 Spring Security

```kotlin
// config/SecurityConfig.kt
package com.c6bank.currencyconverter.config

import com.c6bank.currencyconverter.adapter.`in`.rest.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtFilter: JwtAuthenticationFilter,
    private val authEntryPoint: CustomAuthEntryPoint,
    private val accessDeniedHandler: CustomAccessDeniedHandler,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(authEntryPoint)
                ex.accessDeniedHandler(accessDeniedHandler)
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
```

### 11.2 JWT Authentication Filter with JTI Revocation

```kotlin
// adapter/in/rest/security/JwtAuthenticationFilter.kt
package com.c6bank.currencyconverter.adapter.`in`.rest.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val tokenRevocationService: TokenRevocationService,  // Redis-backed
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractBearerToken(request)
            ?: return filterChain.doFilter(request, response)

        runCatching {
            val claims = jwtService.parseAndValidate(token)

            // JTI revocation check — equivalent to Rails' JTIMatcher strategy.
            // Stored in Redis with TTL = token expiry so the store never grows unboundedly.
            if (tokenRevocationService.isRevoked(claims.jti)) {
                writeUnauthorized(response, "Token has been revoked")
                return
            }

            val auth = JwtAuthenticationToken(
                principal = UserPrincipal(id = claims.userId, email = claims.email),
                token     = token,
            ).apply {
                details = WebAuthenticationDetailsSource().buildDetails(request)
            }
            SecurityContextHolder.getContext().authentication = auth

        }.onFailure { ex ->
            writeUnauthorized(response, "Invalid or expired token")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun extractBearerToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)

    private fun writeUnauthorized(response: HttpServletResponse, message: String) {
        response.status = 401
        response.contentType = "application/json"
        response.writer.write("""{"error":"unauthorized","message":"$message"}""")
    }
}
```

---

## 12. Resilience — Circuit Breaker, Retry, Rate Limiter

```yaml
# In application.yml
resilience4j:
  circuitbreaker:
    instances:
      currencyApi:
        # Sliding window of last 10 calls
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        # Open the circuit when 50% of calls fail
        failure-rate-threshold: 50
        # Wait 30 seconds before allowing test calls (HALF_OPEN)
        wait-duration-in-open-state: 30s
        # Allow 3 test calls in HALF_OPEN before deciding to CLOSE again
        permitted-number-of-calls-in-half-open-state: 3
        # Count these as failures (e.g. the Feign timeout)
        record-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - feign.FeignException
        # Expose state on /actuator/health
        register-health-indicator: true

  retry:
    instances:
      currencyApi:
        max-attempts: 3
        wait-duration: 500ms
        # Doubles wait: 500ms -> 1000ms -> 2000ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2.0
        retry-exceptions:
          - java.io.IOException
          - feign.RetryableException
        # Do NOT retry on domain errors — they won't resolve with more attempts
        ignore-exceptions:
          - com.c6bank.currencyconverter.domain.exception.CurrencyNotSupportedException

  timelimiter:
    instances:
      currencyApi:
        timeout-duration: 10s
```

**Interview narrative on circuit breaker**:

> "Retry without a circuit breaker converts a downstream outage into a self-inflicted thread exhaustion incident. If CurrencyAPI.com is down and `MAX_RETRIES = 3` with `wait = 500ms`, each failing request holds its thread for at least 1.5 seconds before giving up. Under moderate load, the thread pool exhausts in seconds and our service stops responding entirely — even to requests that don't need the exchange rate. The circuit breaker's `OPEN` state makes failures immediate: zero thread hold time, zero wasted capacity. The `HALF_OPEN` probe is how the circuit heals automatically without manual intervention."

---

## 13. Caching

```kotlin
// config/CacheConfig.kt
@Configuration
@EnableCaching
class CacheConfig(
    @Value("\${spring.data.redis.host}") private val redisHost: String,
    @Value("\${spring.data.redis.port}") private val redisPort: Int,
) {
    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val exchangeRateConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24))
            .serializeValuesWith(
                // Jackson serialization — survives app restarts unlike Java serialization
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer()
                )
            )
            .disableCachingNullValues()

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(exchangeRateConfig)
            .withCacheConfiguration("exchangeRates", exchangeRateConfig)
            .build()
    }
}
```

The `@Cacheable` key strategy mirrors the Rails version's `exchange_rate:USD:BRL:2026-02-28` — a daily key ensures we fetch fresh rates at market open each day.

---

## 14. Idempotency — Banking-Grade Critical

This is the most important gap in the Rails version and the most impactful thing to demonstrate in the Kotlin version.

### The Problem

The Rails `POST /transactions` endpoint is not idempotent. A client that retries a request due to a network timeout will create a duplicate conversion. This is unacceptable in any financial system.

### The Solution: Client-Supplied Idempotency Key

The pattern comes from Stripe's API design, which is the industry standard:

1. The client generates a UUID per *intent* (not per retry)
2. The server stores the key with the transaction
3. A unique database index provides the final safety net

**Defense in depth**:
- Layer 1: Application-level check (`findByIdempotencyKey`) — returns the existing result immediately
- Layer 2: `@Transactional` boundary — prevents a race between two concurrent identical requests from both passing the application check
- Layer 3: `UNIQUE` database constraint — even if two threads pass layers 1 and 2 simultaneously, the DB rejects the second write
- Layer 4: `@ExceptionHandler(DataIntegrityViolationException)` — converts the DB signal into a `409 Conflict` with a clear message

> **Why NOT Pessimistic Locking (`@Lock(PESSIMISTIC_WRITE)`):**
> Pessimistic locking protects a **mutable shared resource** — a row that multiple threads are competing
> to *update* (e.g., a wallet balance row). `Transaction` records are **immutable facts** (append-only).
> There is no shared mutable row, so there is nothing to lock. Applying `PESSIMISTIC_WRITE` to the
> idempotency check would serialize all reads for zero correctness gain and dramatically reduce
> throughput. The `UNIQUE` constraint is the infallible guard; the application-level check is the
> performance optimization. This is a **uniqueness constraint problem, not a locking problem**.
> (See ADR-008.)

### Migration from Rails

The schema requires one new column:

```sql
-- V3__add_idempotency_key.sql
ALTER TABLE transactions
    ADD COLUMN idempotency_key UUID NOT NULL;

CREATE UNIQUE INDEX idx_transactions_idempotency_key
    ON transactions(idempotency_key);
```

---

## 15. Error Handling

```kotlin
// adapter/in/rest/GlobalExceptionHandler.kt
package com.c6bank.currencyconverter.adapter.`in`.rest

import com.c6bank.currencyconverter.domain.exception.CurrencyNotSupportedException
import com.c6bank.currencyconverter.domain.exception.ExchangeRateUnavailableException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(CurrencyNotSupportedException::class)
    fun handleCurrencyNotSupported(ex: CurrencyNotSupportedException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(errorBody("currency_not_supported", ex.message, mapOf(
                "currency" to ex.currencyCode,
                "supportedCurrencies" to ex.supportedCurrencies,
            )))

    @ExceptionHandler(ExchangeRateUnavailableException::class)
    fun handleRateUnavailable(ex: ExchangeRateUnavailableException): ResponseEntity<Map<String, Any>> {
        log.error("exchange_rate.unavailable error={}", ex.message)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(errorBody("exchange_rate_unavailable", ex.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val details = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(errorBody("validation_error", "Request validation failed", mapOf("errors" to details)))
    }

    /**
     * DataIntegrityViolationException is the Spring signal for a unique constraint violation.
     * We inspect the message to identify which constraint fired:
     * - idempotency_key violation → 409 Conflict (the intent was already processed)
     * - any other → 409 Conflict with generic message
     *
     * This closes the idempotency defense-in-depth loop: even if the application-level
     * idempotency check races, the DB constraint fires and we return a clean 409.
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ResponseEntity<Map<String, Any>> {
        return if (ex.message?.contains("idempotency_key", ignoreCase = true) == true) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody("duplicate_request", "This request has already been processed"))
        } else {
            log.error("data.integrity.violation error={}", ex.message)
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody("conflict", "Data conflict"))
        }
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(ex: MissingRequestHeaderException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorBody("missing_header", "Required header '${ex.headerName}' is missing"))

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<Map<String, Any>> {
        log.error("unexpected.error", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorBody("internal_error", "An unexpected error occurred"))
    }

    private fun errorBody(
        type: String,
        message: String?,
        details: Map<String, Any>? = null,
    ): Map<String, Any> = buildMap {
        put("error", buildMap {
            put("type", type)
            put("message", message ?: "An error occurred")
            details?.let { put("details", it) }
        })
    }
}
```

---

## 16. Observability — Logging, MDC, Actuator

### 16.1 MDC Correlation ID Filter

The Rails version logs have `timestamp` but no request-scoped identifier. In production with concurrent requests, log lines from different requests are interleaved, making debugging very difficult.

```kotlin
// adapter/in/rest/MdcFilter.kt
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MdcFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        // Honour an existing correlation ID from a gateway/load balancer, or generate one
        val correlationId = request.getHeader("X-Correlation-ID") ?: UUID.randomUUID().toString()

        MDC.put("correlationId", correlationId)
        MDC.put("requestPath", request.requestURI)
        MDC.put("requestMethod", request.method)

        try {
            // Echo back so client can trace their request
            response.addHeader("X-Correlation-ID", correlationId)
            chain.doFilter(request, response)
        } finally {
            // CRITICAL: clear MDC after the request. Spring uses a thread pool —
            // without this, a thread's MDC context leaks into the next request it serves.
            MDC.clear()
        }
    }
}
```

### 16.2 Structured Logging Configuration (logback-spring.xml)

```xml
<configuration>
    <springProfile name="prod">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <!-- Automatically includes MDC fields (correlationId, userId, etc.) -->
                <includeMdcKeyName>correlationId</includeMdcKeyName>
                <includeMdcKeyName>requestPath</includeMdcKeyName>
                <includeMdcKeyName>requestMethod</includeMdcKeyName>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="STDOUT"/>
        </root>
    </springProfile>
</configuration>
```

### 16.3 Actuator Configuration

Replaces the manually-coded `GET /api/v1/health` endpoint in Rails with a richer, auto-managed endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus, circuitbreakers, retries
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      show-components: when-authorized
  health:
    circuitbreakers:
      enabled: true   # Shows CLOSED/OPEN/HALF_OPEN for currencyApi
    redis:
      enabled: true
    db:
      enabled: true
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true   # P50/P95/P99 latency histograms
```

This exposes the Resilience4j circuit breaker state on `/actuator/health`, meaning an SRE can see in real time if CurrencyAPI.com is causing problems without tailing application logs.

---

## 17. Rate Limiting

Replaces Rack::Attack with Bucket4j, which integrates natively with Spring and Redis:

```kotlin
// config/RateLimitingConfig.kt
@Configuration
class RateLimitingConfig {

    @Bean
    fun rateLimitFilter(redisClient: RedisClient): RateLimitingFilter {
        val globalBandwidth = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)))
        val authBandwidth   = Bandwidth.classic(5,   Refill.greedy(5,   Duration.ofSeconds(20)))

        return RateLimitingFilter(
            redisClient     = redisClient,
            globalBandwidth = globalBandwidth,
            authBandwidth   = authBandwidth,
        )
    }
}

@Component
class RateLimitingFilter(
    private val redisClient: RedisClient,
    private val globalBandwidth: Bandwidth,
    private val authBandwidth: Bandwidth,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val ip = request.remoteAddr
        val isAuthEndpoint = request.requestURI.startsWith("/api/v1/auth")

        val bandwidth = if (isAuthEndpoint) authBandwidth else globalBandwidth
        val bucketKey = if (isAuthEndpoint) "auth:$ip" else "global:$ip"

        val bucket = Bucket.builder()
            .addLimit(bandwidth)
            .build()  // In production: backed by Redis via ProxyManager

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response)
        } else {
            response.status = 429
            response.contentType = "application/json"
            response.addHeader("X-RateLimit-Limit", bandwidth.capacity.toString())
            response.addHeader("X-RateLimit-Remaining", "0")
            response.writer.write(
                """{"error":{"type":"rate_limit_exceeded","message":"Too many requests. Please try again later."}}"""
            )
        }
    }
}
```

---

## 18. Database Schema and Migrations

Using Flyway for version-controlled, repeatable migrations. Explicit SQL over ORM auto-generation — you can read and audit what's in the database without reverse-engineering annotations.

```sql
-- V1__create_users.sql
CREATE TABLE users (
    id                     BIGSERIAL PRIMARY KEY,
    email                  VARCHAR(255) NOT NULL,
    encrypted_password     VARCHAR(255) NOT NULL DEFAULT '',
    jti                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    reset_password_token   VARCHAR(255),
    reset_password_sent_at TIMESTAMP WITH TIME ZONE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_jti   UNIQUE (jti)
);

-- V2__create_transactions.sql
CREATE TABLE transactions (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         BIGINT  NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    idempotency_key UUID    NOT NULL,
    from_currency   CHAR(3) NOT NULL,
    to_currency     CHAR(3) NOT NULL,
    from_amount     NUMERIC(19, 4) NOT NULL,
    to_amount       NUMERIC(19, 4) NOT NULL,
    rate            NUMERIC(19, 8) NOT NULL,
    converted_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_transactions_positive_amounts
        CHECK (from_amount > 0 AND to_amount > 0 AND rate > 0),
    CONSTRAINT chk_transactions_different_currencies
        CHECK (from_currency != to_currency),
    CONSTRAINT uk_transactions_idempotency_key
        UNIQUE (idempotency_key)
);

-- Indexes — chosen based on the actual query patterns, not guesswork
CREATE INDEX idx_transactions_user_converted_at
    ON transactions(user_id, converted_at DESC);  -- Primary list query

CREATE INDEX idx_transactions_currency_pair
    ON transactions(from_currency, to_currency);  -- Analytics / reporting

-- V3__add_transactions_constraints.sql
-- Enforce currency codes at DB level — belt + suspenders with app-level validation
ALTER TABLE transactions
    ADD CONSTRAINT chk_from_currency CHECK (from_currency IN ('BRL','USD','EUR','JPY')),
    ADD CONSTRAINT chk_to_currency   CHECK (to_currency   IN ('BRL','USD','EUR','JPY'));
```

**Note on `ON DELETE RESTRICT`**: The Rails version uses `dependent: :destroy`, which deletes all transactions when a user is deleted. In a banking context this is wrong — transactions are financial records that must be retained for regulatory and audit purposes. `RESTRICT` prevents silent data loss: if you want to delete a user, you must explicitly archive or reassign their transactions first.

---

## 19. Testing Strategy

The Hexagonal structure directly enables superior testing:

### 19.1 Domain Tests — Pure Unit Tests, Zero Framework

```kotlin
// domain/model/TransactionTest.kt
class TransactionTest {
    @Test
    fun `create - applies HALF_EVEN rounding correctly`() {
        // 1.005 USD at rate 5.5 = 5.5275 BRL (rounds to 5.5275)
        // HALF_EVEN: 5.52750 → 5.5275 (no rounding needed here)
        // Test the banker's rounding specifically:
        // 2.5 at rate 1.0 → 2.5000 rounded HALF_EVEN → 2.5000 (stable)
        val transaction = Transaction.create(
            userId         = 1L,
            idempotencyKey = UUID.randomUUID(),
            amount         = BigDecimal("100.00"),
            fromCurrency   = SupportedCurrency.USD,
            toCurrency     = SupportedCurrency.BRL,
            rate           = BigDecimal("5.12345678"),
        )
        assertEquals(BigDecimal("512.3457"), transaction.targetMoney.amount)  // HALF_EVEN
    }

    @Test
    fun `create - rejects same currency conversion`() {
        assertThrows<IllegalArgumentException> {
            Transaction.create(
                userId = 1L, idempotencyKey = UUID.randomUUID(),
                amount = BigDecimal("100"), fromCurrency = SupportedCurrency.USD,
                toCurrency = SupportedCurrency.USD, rate = BigDecimal("1.0"),
            )
        }
    }
}
```

### 19.2 Application Service Tests — Use MockK, No Spring Context

```kotlin
// application/ConvertCurrencyServiceTest.kt
class ConvertCurrencyServiceTest {
    private val repository     = mockk<TransactionRepository>()
    private val gateway        = mockk<ExchangeRateGateway>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val service        = ConvertCurrencyService(repository, gateway, eventPublisher)

    @Test
    fun `convert - returns existing transaction on duplicate idempotency key`() {
        val key = UUID.randomUUID()
        val existing = buildTransaction(idempotencyKey = key)
        every { repository.findByIdempotencyKey(key) } returns existing

        val result = service.convert(buildCommand(idempotencyKey = key))

        assertEquals(existing.id, result.id)
        verify(exactly = 0) { gateway.fetchRate(any(), any()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `convert - creates and saves new transaction`() {
        val key = UUID.randomUUID()
        every { repository.findByIdempotencyKey(key) } returns null
        every { gateway.fetchRate(SupportedCurrency.USD, SupportedCurrency.BRL) } returns BigDecimal("5.0")
        every { repository.save(any()) } answers { firstArg() }

        val result = service.convert(buildCommand(idempotencyKey = key))

        assertEquals(SupportedCurrency.USD, result.sourceMoney.currency)
        verify(exactly = 1) { repository.save(any()) }
    }
}
```

### 19.3 Integration Tests — Testcontainers

```kotlin
// adapter/out/persistence/JpaTransactionRepositoryAdapterIT.kt
@SpringBootTest
@Testcontainers
class JpaTransactionRepositoryAdapterIT {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("currency_converter_test")
    }

    @Autowired lateinit var repository: TransactionRepository

    @Test
    fun `save and findByIdempotencyKey roundtrip`() {
        val key = UUID.randomUUID()
        val transaction = Transaction.create(
            userId = 1L, idempotencyKey = key, amount = BigDecimal("100"),
            fromCurrency = SupportedCurrency.USD, toCurrency = SupportedCurrency.BRL,
            rate = BigDecimal("5.0"),
        )
        repository.save(transaction)

        val found = repository.findByIdempotencyKey(key)
        assertNotNull(found)
        assertEquals(transaction.idempotencyKey, found!!.idempotencyKey)
    }
}
```

### 19.4 Gateway Tests — WireMock

```kotlin
// adapter/out/exchange/CurrencyApiGatewayTest.kt
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@WireMockTest(httpPort = 8089)
class CurrencyApiGatewayTest {

    @Autowired lateinit var gateway: ExchangeRateGateway

    @Test
    fun `fetchRate - parses valid response`() {
        stubFor(
            get(urlPathEqualTo("/latest"))
                .willReturn(okJson("""
                    { "data": { "BRL": { "code": "BRL", "value": 5.1234 } } }
                """.trimIndent()))
        )

        val rate = gateway.fetchRate(SupportedCurrency.USD, SupportedCurrency.BRL)
        assertEquals(0, BigDecimal("5.1234").compareTo(rate))
    }

    @Test
    fun `fetchRate - throws ExchangeRateUnavailableException on 503`() {
        stubFor(get(urlPathEqualTo("/latest")).willReturn(serviceUnavailable()))

        assertThrows<ExchangeRateUnavailableException> {
            gateway.fetchRate(SupportedCurrency.USD, SupportedCurrency.BRL)
        }
    }
}
```

### 19.5 Concurrency Test — Proving Idempotency Under Load

This test proves that the defense-in-depth idempotency pattern holds when multiple threads fire the same
request simultaneously. It does **not** test pessimistic locking (which is not used here) — it tests
that the `UNIQUE` constraint + application-level check together guarantee exactly one record is created
and all callers receive the same result.

Use `CountDownLatch` (not Coroutines) for this test: it gives deterministic control over the exact
moment all threads are released simultaneously, which is the precise condition you need to provoke
a race.

```kotlin
// application/ConvertCurrencyServiceConcurrencyTest.kt
@SpringBootTest
@Testcontainers
class ConvertCurrencyServiceConcurrencyTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("currency_converter_test")
    }

    @Autowired lateinit var convertCurrencyUseCase: ConvertCurrencyUseCase
    @Autowired lateinit var transactionRepository: TransactionRepository

    @MockBean lateinit var exchangeRateGateway: ExchangeRateGateway

    @BeforeEach
    fun setup() {
        every { exchangeRateGateway.fetchRate(any(), any()) } returns BigDecimal("5.00")
    }

    @Test
    fun `same idempotency key under concurrent requests creates exactly one transaction`() {
        val idempotencyKey = UUID.randomUUID()
        val threadCount = 10

        val startLatch = CountDownLatch(1)      // holds all threads until we say go
        val doneLatch  = CountDownLatch(threadCount)
        val results    = CopyOnWriteArrayList<Result<Transaction>>()

        repeat(threadCount) {
            Thread {
                try {
                    startLatch.await()           // all threads wait here simultaneously
                    results.add(runCatching {
                        convertCurrencyUseCase.convert(
                            ConvertCurrencyCommand(
                                userId           = 1L,
                                idempotencyKey   = idempotencyKey,
                                fromCurrencyCode = "USD",
                                toCurrencyCode   = "BRL",
                                amount           = BigDecimal("100.00"),
                            )
                        )
                    })
                } finally {
                    doneLatch.countDown()
                }
            }.start()
        }

        startLatch.countDown()           // release all 10 threads simultaneously
        doneLatch.await(10, TimeUnit.SECONDS)

        val successes = results.filter { it.isSuccess }
        val failures  = results.filter { it.isFailure }

        // Every caller must receive a successful response (idempotent, not an error)
        assertThat(successes).hasSize(threadCount)

        // But all responses must point to the SAME underlying transaction record
        val uniqueIds = successes.map { it.getOrNull()!!.id }.toSet()
        assertThat(uniqueIds).hasSize(1)

        // No unhandled exceptions — DataIntegrityViolationException must be handled upstream
        assertThat(failures).isEmpty()
    }
}
```

**Why this test is sufficient without pessimistic locking:**

The `assertThat(uniqueIds).hasSize(1)` assertion passes because:
1. The first thread to commit inserts the row with the unique `idempotency_key`
2. All subsequent concurrent threads either hit the application-level `findByIdempotencyKey` check
   (which returns the existing record) or hit the DB unique constraint (which the
   `DataIntegrityViolationException → 409` handler converts to a clean `ConflictException`)
3. No thread creates a second record

**Interview narrative:**
> "I wrote a concurrency test using `CountDownLatch` to release 10 threads at exactly the same moment
> against the same idempotency key. The test asserts two things: every caller succeeds (no crashes),
> and every caller gets back the same transaction ID. That proves the uniqueness guarantee.
> I deliberately chose not to use pessimistic locking because `Transaction` records are immutable —
> there is no mutable shared state to protect. The combination of a `UNIQUE` constraint,
> `@Transactional`, and the `DataIntegrityViolationException` handler is the correct and
> more performant solution."

---

## 20. Docker and Infrastructure

```yaml
# docker-compose.yml
services:
  app:
    build: currency-converter-ruby
    ports:
      - "3000:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/currency_converter
      SPRING_DATA_REDIS_HOST: redis
      CURRENCY_API_KEY: ${CURRENCY_API_KEY}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:8080/actuator/health" ]
      interval: 30s
      timeout: 5s
      retries: 3

  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: currency_converter
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U ${DB_USER}" ]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    healthcheck:
      test: [ "CMD", "redis-cli", "ping" ]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
  redis_data:
```

```dockerfile
# Dockerfile — multi-stage for minimal production image
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

---

## 21. Application Properties

```yaml
# application.yml
spring:
  application:
    name: currency-converter

  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000

  jpa:
    hibernate:
      ddl-auto: validate   # Flyway owns schema — Hibernate only validates
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: 6379

  cache:
    type: redis

currency-api:
  base-url: https://api.currencyapi.com/v3
  api-key: ${CURRENCY_API_KEY}

jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 3600000  # 1 hour

feign:
  client:
    config:
      currencyApi:
        connect-timeout: 5000
        read-timeout: 10000
        logger-level: BASIC

logging:
  level:
    com.c6bank: INFO
    org.springframework.security: WARN
```

---

## 22. Architecture Decision Records (ADRs)

### ADR-001: Hexagonal Architecture

**Status**: Accepted  
**Context**: The application depends on two external systems (PostgreSQL and CurrencyAPI.com). The domain logic — create a conversion record with validated rate and amount — must remain correct regardless of which provider or database is used.  
**Decision**: Adopt Hexagonal Architecture with explicit port interfaces.  
**Consequences**: Higher initial setup; domain layer is framework-free and purely unit-testable; infrastructure changes (e.g., swap CurrencyAPI.com for another provider) require changing one class.

### ADR-002: PostgreSQL over NoSQL

**Status**: Accepted  
**Context**: Transactions are financial records requiring ACID guarantees.  
**Decision**: PostgreSQL.  
**Consequences**: Vertical scaling path; excellent consistency; `@Transactional` boundaries are reliable; not suitable for >100M rows/day without sharding.

### ADR-003: Idempotency Keys

**Status**: Accepted  
**Context**: `POST /transactions` is a non-idempotent operation executed over an unreliable network.  
**Decision**: Require clients to supply a `UUID Idempotency-Key` header. Store the key with a unique constraint. Return `409 Conflict` on duplicate.  
**Consequences**: Client must generate and retry-with-same-key; adds one column and one index to `transactions`.

### ADR-004: Resilience4j over Manual Retry

**Status**: Accepted  
**Context**: CurrencyAPI.com has variable availability.  
**Decision**: Use Resilience4j Circuit Breaker + Retry.  
**Consequences**: Automatic circuit opening prevents thread pool exhaustion during outages; Resilience4j metrics are exposed on Actuator automatically; adds a dependency.

### ADR-005: HALF_EVEN (Banker's Rounding)

**Status**: Accepted  
**Context**: Rounding mode affects accumulated error in aggregations.  
**Decision**: Use `RoundingMode.HALF_EVEN` everywhere.  
**Consequences**: Correct financial rounding; eliminates systematic upward bias; slightly different results than `HALF_UP` for values ending in exactly .5.

### ADR-006: ON DELETE RESTRICT for transactions

**Status**: Accepted  
**Context**: Rails version used `dependent: :destroy`.  
**Decision**: `ON DELETE RESTRICT` at the database level.  
**Consequences**: Deleting a user requires explicitly handling their transactions first. This prevents silent financial record deletion that would violate audit requirements.

### ADR-007: @TransactionalEventListener(AFTER_COMMIT)

**Status**: Accepted  
**Context**: Domain events should represent facts, not intentions.  
**Decision**: Always publish events in `AFTER_COMMIT` phase.  
**Consequences**: Events are only published if the DB transaction committed successfully; listeners never act on data that failed to persist; this is the prerequisite for safe Kafka integration.

### ADR-008: No Pessimistic Locking on Transaction Writes

**Status**: Accepted  
**Context**: Considered using `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the idempotency key lookup to prevent race conditions and "double-spending."  
**Decision**: Do not use pessimistic locking. Use `UNIQUE` database constraint + `@Transactional` + `DataIntegrityViolationException → 409` instead.  
**Rationale**: Pessimistic locking (`SELECT ... FOR UPDATE`) is the correct tool when protecting a **mutable shared row** — for example, a `wallet_balances` row that is being decremented. `Transaction` records are **immutable facts** (append-only). No row is mutated after insert, so there is no mutable shared state to lock. Applying `PESSIMISTIC_WRITE` to the idempotency check would serialize all reads without providing any additional correctness guarantee — the `UNIQUE` constraint at the database level is already infallible. Pessimistic locking would reduce write throughput significantly under concurrent load for zero benefit.  
**When to revisit**: If a `wallet_balances` table is introduced with a mutable `balance` column, pessimistic locking (`findByUserIdForUpdate`) would be required to prevent overdraft under concurrent deduction requests.  
**Consequences**: Higher concurrent write throughput; no lock wait latency; idempotency guarantee remains complete via the constraint + handler chain.

---

## 23. Implementation Roadmap

Ordered by impact at a Senior-level interview:

| Priority | Deliverable | Architectural Signal |
|---|---|---|
| **P0** | Domain layer (Transaction, Money, SupportedCurrency, ports) | "Zero framework in domain" — shows Hexagonal understanding |
| **P0** | `@Transactional` at use case boundary | ACID correctness — the most common senior interview question |
| **P0** | Idempotency key (header + DB constraint + 409 handler) | Core banking primitive — very likely to be asked directly |
| **P1** | Hexagonal adapter structure (separate in/out packages) | Structural evidence of the architecture, not just the narrative |
| **P1** | Resilience4j Circuit Breaker + Retry on `CurrencyApiGateway` | Production ops thinking — replaces naive retry |
| **P1** | `@TransactionalEventListener(AFTER_COMMIT)` | Shows understanding of transactional semantics |
| **P1** | `@RestControllerAdvice` with `DataIntegrityViolationException` | Closes idempotency loop at API surface |
| **P1** | `HALF_EVEN` rounding + `Money` value object | Domain precision — signals financial domain knowledge |
| **P1** | Concurrency test (`CountDownLatch` × 10, same idempotency key) | Proves idempotency under race conditions; explicitly rejects pessimistic locking with rationale |
| **P2** | MDC correlation IDs | Observability fundamentals |
| **P2** | Actuator + Resilience4j health indicators | Replaces bespoke health endpoint with richer equivalent |
| **P2** | Flyway migrations with explicit SQL | Shows preference for explicitness over convention in schema management |
| **P2** | `ON DELETE RESTRICT` with audit rationale | Shows understanding of financial data retention requirements |
| **P3** | Testcontainers integration test | Evidence of confidence in persistence layer |
| **P3** | WireMock gateway test | Evidence of confidence in external dependency handling |
| **P3** | SpringDoc/OpenAPI annotations | API-first professionalism |

---

*This blueprint is the complete reference for the Kotlin/Spring Boot rewrite. Every decision in it has a documented trade-off. When asked "why did you choose X?", the answer is always comparative: what was the alternative, what does each cost, and why did the specific constraints of this problem favour this choice.*

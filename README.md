# Currency Converter API 💱

> 🌍 **Language:** **English** | [Português](README.pt-BR.md)

> **Senior Kotlin/Spring Boot Developer - Technical Assessment for Jaya Tech**

Production-ready Spring Boot API for real-time currency conversion with JWT authentication, comprehensive test coverage, and automated CI/CD deployment.

🌐 **Live:** https://kotlin-converter.duckdns.org | 📚 **API Docs:** https://kotlin-converter.duckdns.org/api/v1/swagger-ui/index.html | ✅ **80% coverage** (JaCoCo)

---

## ✅ Assessment Requirements Met

| Requirement | Implementation | Evidence |
|------------|----------------|----------|
| **Spring Boot 3.2+** | ✅ Spring Boot 3.2.3 | [build.gradle.kts](build.gradle.kts) |
| **Kotlin 1.9+** | ✅ Kotlin 1.9.22 | [build.gradle.kts](build.gradle.kts) |
| **PostgreSQL** | ✅ Production DB | Flyway migrations in `src/main/resources/db/migration` |
| **Redis** | ✅ Cache & Rate Limiting | [application.yml](src/main/resources/application.yml) |
| **JUnit Tests** | ✅ Comprehensive coverage, 80% line coverage | `./gradlew test` |
| **CI/CD** | ✅ GitHub Actions | `.github/workflows/ci-cd.yml` |
| **Git & Agile** | ✅ PRs, conventional commits | [Commit history](https://github.com/your-org/currency-converter-kotlin/commits/main) |

**Bonus:** Docker ✅ | Static Analysis (Detekt) ✅ | Security (Spring Security + JWT) ✅ | API Documentation (Springdoc OpenAPI) ✅ | Production Deployment ✅ | HTTPS/SSL ✅ | Resilience4j (Circuit Breaker) ✅ | Rate Limiting (Bucket4j) ✅

---

## 🚀 Quick Start (30 seconds)

```bash
cd currency-converter-kotlin
./gradlew bootRun
```

Or with Docker:

```bash
docker-compose up
```

**Test login:** `admin@example.com` / `password`  
**Try it:** Visit http://localhost:8080/api/v1/swagger-ui/index.html for interactive API documentation

---

## 🏗️ Architecture & Technical Decisions

**Design Patterns:**
- Service Objects for business logic isolation
- OpenFeign Client for external API calls with Resilience4j decorators
- Custom exception hierarchy with proper HTTP status code mapping
- Repository pattern with Spring Data JPA

**Performance:**
- Redis caching (24hr TTL for exchange rates)
- Database indexes on foreign keys and search columns
- N+1 query prevention with eager loading and projections
- Connection pooling with HikariCP

**Security:**
- JWT authentication (Spring Security + JJWT)
- Rate limiting: 100 req/min per user (Bucket4j + Redis)
- Static analysis: Detekt with custom ruleset
- HTTPS with Let's Encrypt SSL
- Resilience4j Circuit Breaker for external API calls

**Quality Assurance:**
- JUnit 5 with MockK (idiomatic Kotlin mocking)
- Testcontainers for integration tests (PostgreSQL, Redis)
- WireMock for external API mocking
- 80% code coverage (JaCoCo)
- CI/CD pipeline with automated tests, linting, and deployment

📖 **Deep dive:** [Architecture Decisions](KOTLIN_ARCHITECTURE_DECISIONS.md) | [Development Guide](DEVELOPMENT.md) | [Deployment Guide](DEPLOYMENT.md)

---

## 📋 Core Features

- ✅ **10+ currencies** with real-time exchange rates ([CurrencyAPI](https://currencyapi.com))
- ✅ **JWT authentication** for secure API access (Spring Security)
- ✅ **Idempotent transactions** — Server auto-generates Idempotency-Keys, prevents duplicates on retries
- ✅ **Transaction history** with pagination and user isolation
- ✅ **Comprehensive logging** with structured JSON (Logstash Logback)
- ✅ **Health checks** for monitoring (database, cache, external API)
- ✅ **Swagger/OpenAPI documentation** auto-generated from annotations
- ✅ **Resilience4j Circuit Breaker** for fault tolerance
- ✅ **Rate limiting** with Bucket4j (100 req/min per user)
- ✅ **Prometheus metrics** for monitoring (Micrometer)

---

## 🧪 Testing & Quality

```bash
./gradlew test              # Run all tests (JUnit 5)
./gradlew detekt            # Lint code style with Detekt
./gradlew jacocoTestReport  # Generate coverage report
open build/reports/jacoco/test/html/index.html  # View coverage
```

**Test Coverage Breakdown:**
- Controllers: Integration tests with Testcontainers (PostgreSQL, Redis)
- Services: Unit tests with MockK
- OpenFeign Clients: WireMock stub server
- Repositories: Database tests with Testcontainers
- Exception handling: Custom exception mapping specs

**Test Framework Stack:**
- JUnit 5 (Jupiter)
- MockK + SpringMockK
- Testcontainers (PostgreSQL, Redis)
- WireMock (HTTP stubs)
- AssertJ (fluent assertions)

---

## 📚 Documentation

- 📖 [API Examples](src/main/resources/docs/API_EXAMPLES.md) - Request/response samples
- 🔑 [Idempotency-Key Guide](IDEMPOTENCY_KEY_GUIDE.md) - Understand & use idempotent requests
- 📖 [Architecture Decisions](KOTLIN_IMPLEMENTATION_PLAN.md) - Technical choices & rationale
- 📖 [Development Guide](DEVELOPMENT.md) - Local setup & Docker workflows
- 📖 [Deployment Guide](DEPLOYMENT.md) - Production setup with HTTPS
- 📖 [Interactive API Docs](https://kotlin-converter.duckdns.org/api/v1/swagger-ui/index.html) - Swagger UI
- 📖 [OpenAPI Schema](https://kotlin-converter.duckdns.org/api/v1/api-docs) - JSON specification

---

## 🛠️ Tech Stack

**Language & Framework:** Kotlin 1.9 | Spring Boot 3.2 | Spring Security  
**Database:** PostgreSQL | Flyway (migrations) | Spring Data JPA  
**Cache & Rate Limiting:** Redis | Bucket4j | Lettuce  
**HTTP Client:** OpenFeign | Resilience4j (Circuit Breaker, Retry)  
**Testing:** JUnit 5 | MockK | Testcontainers | WireMock | AssertJ  
**Quality:** Detekt | JaCoCo (80% coverage) | Micrometer (Prometheus)  
**DevOps:** GitHub Actions | Docker | Docker Compose | Nginx + Spring Boot  
**API Documentation:** Springdoc OpenAPI | Swagger UI  
**Logging:** SLF4J | Logstash Logback Encoder (JSON structured logs)  

---

## 🚀 Development Workflow

### Prerequisites
- Java 21+
- Kotlin 1.9+
- PostgreSQL 14+ (or use Docker Compose)
- Redis 7+ (or use Docker Compose)

### Local Development

**1. Start infrastructure (PostgreSQL + Redis):**
```bash
docker-compose up -d postgres redis
```

**2. Run the application:**
```bash
./gradlew bootRun
```

**3. Run tests:**
```bash
./gradlew test
```

**4. Run static analysis:**
```bash
./gradlew detekt
```

**5. Generate coverage report:**
```bash
./gradlew jacocoTestReport
```

---

## 🌟 Why This Implementation?

**For Jaya Tech's "Conscious Software Engineering":**

1. **Data-Driven Decisions:** 80% code coverage with JaCoCo ensures quality metrics
2. **Healthy Relationships:** Clean architecture enables team collaboration and maintainability
3. **Impact Understanding:** Documentation explains *why*, not just *what*
4. **Self-Awareness:** Each commit follows conventions, tests validate assumptions

**Production-Ready Features:**
- Deployed with CI/CD, not just "works on my machine"
- Static analysis (Detekt) catches code quality issues early
- Comprehensive test suite with Testcontainers for realistic scenarios
- Real SSL certificate, not self-signed placeholders
- Structured JSON logging for debugging and monitoring
- Resilience4j for handling external API failures gracefully
- Prometheus metrics for observability and alerting

---

## 🔧 Configuration

All configuration is externalized via `application.yml`:

```yaml
spring:
  application:
    name: currency-converter-api
  datasource:
    url: jdbc:postgresql://postgres:5432/currency_converter
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
  redis:
    host: redis
    port: 6379
  cache:
    type: redis
    redis:
      time-to-live: 86400000  # 24 hours in ms

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000  # 24 hours in ms
  currency-api:
    key: ${CURRENCY_API_KEY}
    base-url: https://api.currencyapi.com/v3
```

---

## 📊 Monitoring & Observability

**Health Checks:** `GET /api/v1/health`
- Database connectivity
- Redis connectivity  
- External API configuration

**Prometheus Metrics:** `GET /actuator/prometheus`
- HTTP request latency and count
- JVM memory and GC metrics
- Database connection pool metrics
- Custom business metrics

**Structured Logging:**
- All logs are JSON-formatted (Logstash Logback)
- Includes request ID for tracing
- Automatic context propagation

---

## 🐳 Docker Deployment

**Build image:**
```bash
docker build -t currency-converter-kotlin:latest .
```

**Run with Docker Compose:**
```bash
docker-compose -f docker-compose.prod.yml up
```

The Dockerfile uses a multi-stage build:
1. **Builder stage:** Compiles Kotlin, runs tests, generates coverage
2. **Runtime stage:** Minimal JRE 21 image with app JAR

---

**Built with ❤️ using Kotlin + Spring Boot** | [View Live Application →](https://kotlin-converter.duckdns.org/api/v1/swagger-ui/index.html)



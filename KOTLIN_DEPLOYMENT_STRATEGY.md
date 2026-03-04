# Deployment Strategy — Kotlin Migration Reference

**Source**: Ruby on Rails deployment (extracted from `DEPLOYMENT.md`, `docker-compose.yml`, `Makefile`, `Dockerfile`, `Procfile`, `puma.rb`, `config/environments/production.rb`)  
**Target**: Kotlin/Spring Boot 3 adaptation  
**Date**: 2026-03-01

---

## 1. Ruby Deployment — As-Built

### 1.1 Infrastructure Overview

| Component | Technology | Notes |
|---|---|---|
| Platform | DigitalOcean Droplet (Ubuntu 22.04) | Single server |
| Web server | Nginx | Reverse proxy + TLS termination |
| App server | Puma | 2 workers × 5 threads |
| Database | PostgreSQL 14 | Local on droplet |
| Cache / Rate limit | Redis 7 | Local on droplet |
| SSL | Let's Encrypt via Certbot | Auto-renewing, 90-day cert |
| Domain | DuckDNS (`currencyconverter.duckdns.org`) | Free dynamic DNS |
| Live URL | `https://currencyconverter.duckdns.org` | |

### 1.2 Process Model

```
Internet → Nginx (443 / TLS) → Unix socket → Puma (2 workers, 5 threads) → Rails app
                                              PostgreSQL (local)
                                              Redis (local)
```

Puma in production uses a **Unix socket** (`tmp/sockets/puma.sock`), not a TCP port, for lower overhead. Nginx proxies to the socket path.

**`Procfile`** (used by Heroku-style runners):
```
web:     bundle exec puma -C config/puma.rb
release: bundle exec rails db:migrate
```
The `release` process ensures migrations run before the new app version starts serving traffic — the deployment pipeline is **migrate-then-deploy**.

### 1.3 Docker Setup (Local / Staging)

**`docker-compose.yml`** brings up three services:

```yaml
services:
  db:    postgres:15          # port not exposed externally
  redis: redis:7              # no persistence (--save "" --appendonly no)
  web:   ./backend (build)    # port 3000:3000, mounts ./backend as volume
```

**`docker-compose.override.yml`** adds development overrides (hot-reload volume mounts, sets `RAILS_ENV=development`).

**`Makefile` targets**:

| Target | Command | What it does |
|---|---|---|
| `make setup` | copies `.env.example` → `.env`, builds, starts, runs `db:prepare` | One-command first-time setup |
| `make build` | `docker-compose build` | Rebuild image |
| `make up` | `docker-compose up -d --remove-orphans` | Start stack detached |
| `make down` | `docker-compose down` | Stop and remove containers |
| `make migrate` | `docker-compose exec web bin/rails db:migrate` | Run pending migrations |
| `make console` | `docker-compose exec web bin/rails console` | Rails console |
| `make dbshell` | `docker-compose exec db psql -U postgres -d ...` | Direct DB access |
| `make logs` | `docker-compose logs -f` | Tail all logs |

### 1.4 Dockerfile — Multi-Stage (Ruby)

```
Stage 1 (build): ruby:3.3.5-slim
  → apt: build-essential, git, libpq-dev, libvips, pkg-config
  → bundle install (production gems only, BUNDLE_WITHOUT=development)
  → bootsnap precompile (gem cache + app code)

Stage 2 (runtime): ruby:3.3.5-slim
  → apt: curl, libvips, postgresql-client
  → copy gems + app from build stage
  → non-root user: rails:rails
  → ENTRYPOINT: bin/docker-entrypoint
  → CMD: ./bin/rails server
  → EXPOSE 3000
```

**`bin/docker-entrypoint`**:
```bash
#!/bin/bash -e
if [ "${1}" == "./bin/rails" ] && [ "${2}" == "server" ]; then
  ./bin/rails db:prepare   # creates DB if missing, runs pending migrations
fi
exec "${@}"
```
`db:prepare` is idempotent — safe to run on every container start.

### 1.5 Environment Variables

| Variable | Required | Description |
|---|---|---|
| `RAILS_ENV` | ✅ | `production` in prod, `development` locally |
| `DATABASE_URL` | ✅ | `postgresql://user:pass@host/db` |
| `REDIS_URL` | ✅ prod | `redis://localhost:6379/0` |
| `CURRENCY_API_KEY` | ✅ | CurrencyAPI.com key |
| `DEVISE_JWT_SECRET_KEY` | ✅ | JWT signing secret (Devise) |
| `SECRET_KEY_BASE` | ✅ prod | Rails encryption key |
| `RAILS_MAX_THREADS` | optional | Puma thread count (default: 5) |
| `WEB_CONCURRENCY` | optional | Puma worker count (default: 1) |
| `PORT` | optional dev | HTTP port (default: 3000) |
| `JWT_SECRET_KEY` | dev only | Used in docker-compose for local dev |

### 1.6 CI/CD Pipeline

Defined in `.github/workflows/ci-cd.yml` (GitHub Actions):

```
On push to any branch:
  1. Run RSpec (190 tests)
  2. Run RuboCop (linting)
  3. Run Brakeman (static security analysis)
  4. Run Bundler Audit (CVE scan on Gemfile.lock)

On push to main branch (additional):
  5. SSH into DigitalOcean droplet
  6. git pull origin main
  7. bundle install
  8. RAILS_ENV=production rails db:migrate
  9. sudo systemctl restart puma
  10. Health check: curl /api/v1/health
```

**SSH deploy user**: `deploy@161.35.142.103`  
**Deploy path**: `/home/deploy/currency-converter-ruby/backend`

### 1.7 Manual Deployment (Emergency)

```bash
ssh deploy@161.35.142.103
cd /home/deploy/currency-converter-ruby/backend
git pull origin main
bundle install
RAILS_ENV=production rails db:migrate
sudo systemctl restart puma
```

### 1.8 SSL Management

```bash
# Initial setup
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d currencyconverter.duckdns.org
# Certbot auto-patches the Nginx config for HTTPS + redirect

# Manual renewal (auto-renewal is configured by Certbot on install)
sudo certbot renew
sudo systemctl reload nginx

# Dry-run test
sudo certbot renew --dry-run
```

Cert location: `/etc/letsencrypt/live/currencyconverter.duckdns.org/`

### 1.9 Health Check Endpoint

`GET /api/v1/health`

```json
{
  "status": "healthy",
  "services": {
    "database": { "status": "up" },
    "cache":    { "status": "up" },
    "external_api": { "status": "configured" }
  }
}
```

### 1.10 Monitoring & Logging

- **Lograge**: structured JSON logs per request with event name, duration, status
- **Performance filter**: flags requests > 1s as slow
- **Log level**: `info` in production (`RAILS_LOG_LEVEL` env override)
- **Log output**: STDOUT (container-friendly, captured by Docker / systemd)
- No external APM configured (Datadog/New Relic would be the next step)

---

## 2. Kotlin Deployment — Adapted Strategy

### 2.1 What Changes

| Aspect | Ruby | Kotlin |
|---|---|---|
| App server | Puma (separate process) | Embedded Tomcat (inside the JAR) |
| Process model | `bundle exec puma` | `java -jar app.jar` |
| Health check | Custom `GET /api/v1/health` | Spring Actuator `/actuator/health` (richer) |
| Migration runner | `rails db:migrate` (ActiveRecord) | Flyway (auto-runs on startup) |
| Build artifact | Source code + gems | Single fat JAR via `./gradlew bootJar` |
| Container base | `ruby:3.3.5-slim` | `eclipse-temurin:21-jre-alpine` |
| Config format | `config/environments/*.rb` + `.env` | `application.yml` + env variable overrides |
| Rate limiting | Rack::Attack (middleware) | Bucket4j + Redis |
| Circuit breaker | Manual retry with `faraday` | Resilience4j (declarative, metric-exposed) |

### 2.2 Docker Setup (Kotlin)

**`docker-compose.yml`** — adapted from Ruby:

```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/currency_converter
      SPRING_DATA_REDIS_HOST: redis
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      CURRENCY_API_KEY: ${CURRENCY_API_KEY}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
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
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
  redis_data:
```

**Key difference from Ruby**: Redis now has `--appendonly yes` (persistence enabled). The Ruby compose disabled persistence (`--save "" --appendonly no`). In Kotlin, Redis stores Bucket4j rate-limit buckets and the JWT revocation list — both must survive a Redis restart or rate limits reset and revoked tokens become valid again.

### 2.3 Dockerfile (Kotlin Multi-Stage)

```dockerfile
# Stage 1: Build the fat JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon   # cache dependency layer
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

**No entrypoint migration script needed**: Flyway runs automatically on startup via `spring.flyway.enabled=true` in `application.yml`. The Spring context will not start if migrations fail — this is a safer guarantee than the Rails `db:prepare` approach, which can succeed even with a failed migration in some edge cases.

### 2.4 Makefile (Kotlin)

```makefile
DC=docker-compose

.PHONY: setup build up down logs shell migrate test

setup:
	@test -f .env || (cp .env.example .env && echo "Copied .env.example → .env")
	$(DC) build
	$(DC) up -d

build:
	$(DC) build

up:
	$(DC) up -d --remove-orphans

down:
	$(DC) down

logs:
	$(DC) logs -f

shell:
	$(DC) exec app sh

# Flyway manages migrations automatically on startup.
# Use this target to trigger a manual migration check.
migrate:
	$(DC) exec app java -jar app.jar --spring.flyway.enabled=true

test:
	./gradlew test

# Trigger production deploy (mirrors CI/CD)
deploy:
	ssh deploy@<SERVER_IP> \
	  "cd /home/deploy/currency-converter-kotlin && \
	   git pull origin main && \
	   docker-compose build app && \
	   docker-compose up -d --no-deps app"
```

### 2.5 Environment Variables (Kotlin)

| Variable | Required | Ruby Equivalent | Notes |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | ✅ | `DATABASE_URL` | JDBC format: `jdbc:postgresql://...` |
| `DB_USER` | ✅ | embedded in `DATABASE_URL` | |
| `DB_PASSWORD` | ✅ | embedded in `DATABASE_URL` | |
| `SPRING_DATA_REDIS_HOST` | ✅ prod | `REDIS_URL` | Just hostname; port defaults to 6379 |
| `CURRENCY_API_KEY` | ✅ | `CURRENCY_API_KEY` | Same |
| `JWT_SECRET` | ✅ | `DEVISE_JWT_SECRET_KEY` | Min 256-bit key for HS256 |
| `SPRING_PROFILES_ACTIVE` | ✅ prod | `RAILS_ENV` | `prod` activates `application-prod.yml` |

### 2.6 CI/CD Pipeline (Kotlin)

Adapts the Ruby GitHub Actions workflow:

```
On push to any branch:
  1. ./gradlew test         (unit + integration tests via Testcontainers)
  2. ./gradlew detekt       (static analysis — replaces RuboCop)
  3. ./gradlew dependencyCheckAnalyze  (CVE scan — replaces Bundler Audit)

On push to main:
  4. ./gradlew bootJar      (build fat JAR)
  5. docker build + push to registry (or direct SSH deploy)
  6. SSH: docker-compose pull && docker-compose up -d --no-deps app
  7. Health check: curl http://<SERVER>:8080/actuator/health
```

**Migrate-before-deploy**: Flyway runs on app startup inside the new container. If migrations fail, the new container exits immediately — the old container (if still running) continues serving traffic. This gives a safer rollout window compared to running `rails db:migrate` as a separate step that can leave the schema in an intermediate state.

### 2.7 Production Process Model (Kotlin)

```
Internet → Nginx (443 / TLS) → localhost:8080 → Spring Boot (embedded Tomcat)
                                                  ↓
                                              PostgreSQL
                                              Redis
```

No Unix socket needed — Spring Boot's embedded Tomcat listens on TCP 8080 by default. Nginx proxies `proxy_pass http://localhost:8080`.

**Nginx config snippet** (replaces the Puma socket config):
```nginx
upstream spring_app {
    server localhost:8080;
}

server {
    listen 443 ssl;
    server_name currencyconverter.duckdns.org;

    ssl_certificate     /etc/letsencrypt/live/currencyconverter.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/currencyconverter.duckdns.org/privkey.pem;

    location / {
        proxy_pass         http://spring_app;
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 30s;
    }
}

server {
    listen 80;
    server_name currencyconverter.duckdns.org;
    return 301 https://$host$request_uri;
}
```

### 2.8 Health Check (Kotlin — Richer Than Ruby)

Spring Actuator `/actuator/health` automatically aggregates:

```json
{
  "status": "UP",
  "components": {
    "db":           { "status": "UP", "details": { "database": "PostgreSQL", "validationQuery": "isValid()" } },
    "redis":        { "status": "UP", "details": { "version": "7.2.4" } },
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "currencyApi": { "status": "UP", "details": { "state": "CLOSED", "failureRate": "0.0%" } }
      }
    },
    "diskSpace":    { "status": "UP" },
    "ping":         { "status": "UP" }
  }
}
```

The circuit breaker state is visible at a glance — an SRE can determine if CurrencyAPI.com is degraded without grepping logs.

### 2.9 Gotchas & Lessons from the Ruby Deployment

| Gotcha | Ruby | Kotlin mitigation |
|---|---|---|
| **Redis restarts lose rate-limit state** | Ruby compose: Redis has no persistence; all buckets reset on restart | Kotlin compose: `--appendonly yes` — buckets survive restart |
| **`db:prepare` masks migration errors** | `bin/docker-entrypoint` calls `db:prepare` which swallows some errors | Flyway throws on startup failure; container exits with non-zero — Docker marks unhealthy |
| **No JTI TTL in Redis** | JWT revocation entries stored in DB — no eviction | Store JTI in Redis with TTL = token expiry; Redis evicts automatically |
| **Puma socket path hardcoded** | `unix:///home/rails/currency-converter-ruby/...` — breaks if app path changes | Embedded Tomcat uses TCP; path is irrelevant |
| **`ON DELETE CASCADE` risk** | Rails FK default silently deletes transactions with user | Kotlin schema: `ON DELETE RESTRICT` — enforced at DB level |
| **`updated_at` on transactions** | Triggers on any UPDATE — signals mutability | No `updated_at` on transactions; JPA entity is `val`-only |
| **Integer PKs are enumerable** | Transaction IDs are sequential integers | UUID PKs make enumeration attacks infeasible |
| **`TIMESTAMP` without timezone** | Server-timezone-dependent; DST bugs in non-UTC environments | `TIMESTAMP WITH TIME ZONE` throughout; Jackson serializes as ISO-8601 with `Z` |
| **Health endpoint manually coded** | Custom `/api/v1/health` controller | Actuator auto-manages health, circuit states, and Prometheus metrics |
| **No correlation IDs in logs** | Request ID via `log_tags` — but not propagated to downstream | `X-Correlation-ID` in MDC filter; echoed in response header; propagated to all log lines |


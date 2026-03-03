.PHONY: help setup build up down logs shell test migrate clean purge \
        wait-infra-healthy wait-app-healthy

# ─────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────
APP_URL        ?= http://localhost:8080
HEALTH_URL     := $(APP_URL)/actuator/health
POSTGRES_USER  ?= postgres

# Maximum attempts × interval = total wait time
INFRA_RETRIES  ?= 12   # 12 × 5 s = 60 s
APP_RETRIES    ?= 24   # 24 × 5 s = 120 s
RETRY_INTERVAL ?= 5

help:
	@echo ""
	@echo "  Currency Converter Kotlin — available targets"
	@echo ""
	@echo "  setup    Initialise project: build image, start all services,"
	@echo "           run Flyway migrations, verify /actuator/health"
	@echo "  build    Compile and package the application (skips tests)"
	@echo "  up       Start all services (app, db, redis)"
	@echo "  down     Stop all services"
	@echo "  logs     Stream logs from all services  (Ctrl-C to quit)"
	@echo "  shell    Open a shell inside the running app container"
	@echo "  test     Run the full test suite via Gradle"
	@echo "  migrate  Show applied Flyway migrations (migrations run on startup)"
	@echo "  clean    Remove Gradle build artefacts"
	@echo "  purge    Stop services and delete all volumes  ⚠ DATA LOSS"
	@echo ""
	@echo "  Examples:"
	@echo "    make setup   # first-time checkout"
	@echo "    make up      # start after a previous 'make down'"
	@echo "    make logs    # watch live output"
	@echo "    make test    # run tests locally (no Docker required)"
	@echo ""

# ─────────────────────────────────────────────
# Primary targets
# ─────────────────────────────────────────────

## First-time setup: build image → start services → wait for app health
setup: build up wait-infra-healthy wait-app-healthy
	@echo ""
	@echo "✓ Setup complete!"
	@echo "  Application : $(APP_URL)"
	@echo "  Health      : $(HEALTH_URL)"
	@echo ""
	@curl -sf $(HEALTH_URL) | python3 -m json.tool 2>/dev/null || curl -sf $(HEALTH_URL)
	@echo ""

## Compile and package (skips tests for speed)
build:
	@echo "→ Building application..."
	./gradlew build -x test
	@echo "✓ Build complete"

## Start all Docker Compose services in detached mode
up:
	@echo "→ Starting services (app, db, redis)..."
	docker compose up -d
	@echo "✓ Services started"

## Stop all services (containers are removed; volumes are kept)
down:
	@echo "→ Stopping services..."
	docker compose down
	@echo "✓ Services stopped"

## Stream logs from all services
logs:
	docker compose logs -f

## Open an interactive shell inside the running app container
shell:
	docker compose exec app sh

## Run the full Gradle test suite
test:
	@echo "→ Running tests..."
	./gradlew test
	@echo "✓ Tests complete"

## Show Flyway migration status (migrations execute automatically on startup)
migrate:
	@echo "→ Flyway migration status:"
	@docker compose exec -T db \
		psql -U $(POSTGRES_USER) -d currency_converter \
		-c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;" \
		2>/dev/null || echo "  (Flyway migrations run automatically when the app starts)"

## Remove Gradle build artefacts
clean:
	@echo "→ Cleaning build artefacts..."
	./gradlew clean
	@rm -rf build/
	@echo "✓ Clean complete"

## Stop services AND delete all Docker volumes — ⚠ permanent data loss
purge: down
	@echo "⚠  Removing all volumes and persistent data..."
	docker compose down -v
	@echo "✓ Volumes removed"

# ─────────────────────────────────────────────
# Internal helper targets
# ─────────────────────────────────────────────

## Wait for Postgres and Redis to be ready before starting the app
wait-infra-healthy:
	@echo "→ Waiting for db and redis to be healthy..."
	@i=1; while [ $$i -le $(INFRA_RETRIES) ]; do \
		DB_OK=0; REDIS_OK=0; \
		docker compose exec -T db pg_isready -U $(POSTGRES_USER) > /dev/null 2>&1 && DB_OK=1; \
		docker compose exec -T redis redis-cli ping > /dev/null 2>&1 && REDIS_OK=1; \
		if [ $$DB_OK -eq 1 ] && [ $$REDIS_OK -eq 1 ]; then \
			echo "✓ db and redis are healthy"; \
			exit 0; \
		fi; \
		echo "  [$$i/$(INFRA_RETRIES)] not ready yet — retrying in $(RETRY_INTERVAL)s..."; \
		sleep $(RETRY_INTERVAL); \
		i=$$((i + 1)); \
	done; \
	echo "✗ db or redis did not become healthy in time"; \
	docker compose logs db redis; \
	exit 1

## Poll /actuator/health until Spring Boot reports {"status":"UP"}
wait-app-healthy:
	@echo "→ Waiting for application to report healthy at $(HEALTH_URL)..."
	@i=1; while [ $$i -le $(APP_RETRIES) ]; do \
		STATUS=$$(curl -sf $(HEALTH_URL) 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" 2>/dev/null); \
		if [ "$$STATUS" = "UP" ]; then \
			echo "✓ Application is UP"; \
			exit 0; \
		fi; \
		echo "  [$$i/$(APP_RETRIES)] status='$$STATUS' — retrying in $(RETRY_INTERVAL)s..."; \
		sleep $(RETRY_INTERVAL); \
		i=$$((i + 1)); \
	done; \
	echo "✗ Application did not become healthy in time"; \
	docker compose logs app; \
	exit 1

.DEFAULT_GOAL := help


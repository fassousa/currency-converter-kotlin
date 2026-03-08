#!/usr/bin/env bash
# deploy.sh — called by CI/CD on the production server
# Usage: ./scripts/deploy.sh
set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env.production"
APP_CONTAINER="currency-converter-app"
HEALTH_URL="http://localhost:8080/api/v1/actuator/health"
MAX_WAIT=120   # seconds to wait for app to become healthy
RETRY_INTERVAL=5

# ── Save current image for rollback ────────────────────────────────────────
docker tag currency-converter-kotlin:latest currency-converter-kotlin:rollback 2>/dev/null || true

# ── Load new image ──────────────────────────────────────────────────────────
echo "📦 Loading new image..."
docker load < image.tar.gz

# ── Write .env.production ──────────────────────────────────────────────────
echo "🗄️  Writing .env.production..."
cat > "$ENV_FILE" << EOF
SPRING_PROFILES_ACTIVE=prod
POSTGRES_USER=${POSTGRES_USER}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
POSTGRES_DB=${POSTGRES_DB:-currency_converter}
JWT_SECRET=${JWT_SECRET}
CURRENCY_API_KEY=${CURRENCY_API_KEY}
EOF

# ── Start containers ────────────────────────────────────────────────────────
echo "🚀 Starting containers..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --no-build

# ── Wait for app Docker healthcheck to pass ─────────────────────────────────
echo "⏳ Waiting for app container to become healthy (up to ${MAX_WAIT}s)..."
elapsed=0
while [ "$elapsed" -lt "$MAX_WAIT" ]; do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$APP_CONTAINER" 2>/dev/null || echo "unknown")
  if [ "$STATUS" = "healthy" ]; then
    break
  fi
  echo "  Container status: $STATUS (${elapsed}s elapsed)..."
  sleep "$RETRY_INTERVAL"
  elapsed=$((elapsed + RETRY_INTERVAL))
done

STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$APP_CONTAINER" 2>/dev/null || echo "unknown")
if [ "$STATUS" != "healthy" ]; then
  echo "❌ Container never became healthy (status: $STATUS). Rolling back..."
  docker compose -f "$COMPOSE_FILE" down
  docker tag currency-converter-kotlin:rollback currency-converter-kotlin:latest 2>/dev/null || true
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --no-build
  exit 1
fi

# ── Final HTTP health check ─────────────────────────────────────────────────
echo "🏥 HTTP health check..."
RESPONSE=$(curl -sf "$HEALTH_URL" || true)
if echo "$RESPONSE" | grep -q '"status":"UP"'; then
  echo "✅ Deploy successful!"
  docker image prune -f
  exit 0
fi

echo "❌ HTTP health check failed. Rolling back..."
docker compose -f "$COMPOSE_FILE" down
docker tag currency-converter-kotlin:rollback currency-converter-kotlin:latest 2>/dev/null || true
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --no-build
exit 1


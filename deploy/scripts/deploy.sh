#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_PATH:-/opt/xebia-lms}"
cd "$DEPLOY_DIR"

: "${IMAGE_TAG:?IMAGE_TAG must be set}"
: "${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME must be set}"
: "${DOCKERHUB_TOKEN:?DOCKERHUB_TOKEN must be set}"

# Load server secrets first; CI-provided IMAGE_TAG always wins over .env
if [ -f .env ]; then
  set -a
  # shellcheck source=/dev/null
  source .env
  set +a
fi
export IMAGE_TAG
export DOCKERHUB_USERNAME

echo "==> Deploying Xebia LMS with image tag: ${IMAGE_TAG}"

echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin

wait_for_healthy() {
  local container="$1"
  local max_attempts="${2:-60}"
  local attempt=1

  while [ "$attempt" -le "$max_attempts" ]; do
    status="$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container" 2>/dev/null || echo "missing")"
    if [ "$status" = "healthy" ]; then
      echo "    ${container} is healthy"
      return 0
    fi
    echo "    waiting for ${container} (status: ${status})..."
    sleep 2
    attempt=$((attempt + 1))
  done

  echo "ERROR: ${container} did not become healthy in time" >&2
  docker logs --tail 50 "$container" || true
  return 1
}

echo "==> Starting PostgreSQL and Redis..."
docker compose pull postgres redis
docker compose up -d postgres redis

wait_for_healthy "xebia-lms-postgres"
wait_for_healthy "xebia-lms-redis"

echo "==> Pulling and deploying application images..."
docker compose pull backend frontend
docker compose up -d --remove-orphans

wait_for_healthy "xebia-lms-backend" 90

docker image prune -f

echo "==> Deployment complete"
docker compose ps

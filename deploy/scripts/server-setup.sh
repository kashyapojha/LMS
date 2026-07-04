#!/usr/bin/env bash
# One-time setup on the AWS EC2 instance (Ubuntu 22.04 / Amazon Linux 2023).
set -euo pipefail

DEPLOY_DIR="${1:-/home/ubuntu/xebia-lms}"

echo "==> Installing Docker..."
if command -v apt-get &>/dev/null; then
  sudo apt-get update -qq
  sudo apt-get install -y -qq ca-certificates curl
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
elif command -v dnf &>/dev/null; then
  sudo dnf install -y docker
  sudo systemctl enable --now docker
  sudo usermod -aG docker "$USER"
fi

echo "==> Creating deploy directory: ${DEPLOY_DIR}"
sudo mkdir -p "$DEPLOY_DIR/deploy/scripts"
sudo chown -R "$USER:$USER" "$DEPLOY_DIR"

if [ ! -f "$DEPLOY_DIR/.env" ]; then
  cat > "$DEPLOY_DIR/.env" <<'EOF'
# Docker Hub (username only — token is passed by CI at deploy time)
DOCKERHUB_USERNAME=your-dockerhub-username

# Image tag is set automatically by CI on each deploy
IMAGE_TAG=latest

# App database (bundled postgres container — data in postgres_data volume)
POSTGRES_DB=lms
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me-strong-password

# Redis cache (bundled redis container — data in redis_data volume)
REDIS_HOST=redis
REDIS_PORT=6379

# Or point backend at RDS instead (then run: docker compose stop postgres):
# SPRING_DATASOURCE_URL=jdbc:postgresql://your-rds-endpoint:5432/lms
# SPRING_DATASOURCE_USERNAME=postgres
# SPRING_DATASOURCE_PASSWORD=change-me

# Cloudinary
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=

FRONTEND_PORT=80
EOF
  echo "==> Created ${DEPLOY_DIR}/.env — edit it before first deploy"
fi

echo "==> Server setup done. Next steps:"
echo "  1. Edit ${DEPLOY_DIR}/.env with your secrets"
echo "  2. Copy docker-compose.yml and deploy/scripts/deploy.sh into ${DEPLOY_DIR}"
echo "  3. Add SSH_HOST, SSH_USER, SSH_PRIVATE_KEY GitHub secrets"
echo "  4. Push to main to trigger CI/CD"

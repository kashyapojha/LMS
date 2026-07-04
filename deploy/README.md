# AWS Deployment Guide — Xebia LMS (SSH + Docker Compose)

The CI/CD pipeline builds images, pushes them to Docker Hub, then SSHs into an AWS EC2 instance to pull and run the **new image tag** on every deploy.

## Architecture

```
GitHub Actions
    │
    ├─ Build & Test (Maven + npm)
    ├─ Docker Build & Push → Docker Hub (tag: <git-sha> + latest)
    └─ SSH Deploy → EC2 instance
                        │
                        1. start postgres + redis (wait until healthy)
                        2. pull backend + frontend (new IMAGE_TAG)
                        3. docker compose up -d
                        │
                        ├─ frontend  (Docker Hub image)
                        ├─ backend   (Docker Hub image) → postgres + redis
                        ├─ postgres  (persistent volume)
                        └─ redis     (persistent volume)
```

Each push to `main` builds a new image tagged with the commit SHA (e.g. `a1b2c3d`). The deploy step passes that tag to the server so containers always run the freshly built image.

## 1. EC2 Instance Setup

Launch an EC2 instance (Ubuntu 22.04 or Amazon Linux 2023):

- **Instance type:** `t3.small` or larger
- **Security group:** allow inbound `22` (SSH) and `80` (HTTP) from your IP / `0.0.0.0/0`
- **Storage:** 20 GB+ recommended

SSH in and run the one-time setup script:

```bash
curl -fsSL https://raw.githubusercontent.com/<your-org>/<your-repo>/main/deploy/scripts/server-setup.sh | bash
```

Or copy `deploy/scripts/server-setup.sh` manually and run it.

Edit `/home/ubuntu/xebia-lms/.env` with your database and Cloudinary credentials.

Copy `docker-compose.yml` and `deploy/scripts/deploy.sh` into `/home/ubuntu/xebia-lms` (the CI pipeline uploads these on every deploy).

## 2. Docker Hub

Create two repositories:

- `xebia-lms-backend`
- `xebia-lms-frontend`

Generate an access token at [Docker Hub → Security](https://hub.docker.com/settings/security).

## 3. GitHub Secrets

Add these in **Settings → Secrets and variables → Actions**:

| Secret | Description |
|--------|-------------|
| `DOCKERHUB_USERNAME` | Docker Hub username |
| `DOCKERHUB_TOKEN` | Docker Hub access token |
| `SSH_HOST` | EC2 public IP or DNS (e.g. `3.110.x.x`) |
| `SSH_USER` | `ubuntu` (Ubuntu) or `ec2-user` (Amazon Linux) |
| `SSH_PRIVATE_KEY` | Full contents of your `.pem` key file |
| `SSH_PORT` | Optional, default `22` |
| `DEPLOY_PATH` | Optional, default `/home/ubuntu/xebia-lms` |

App secrets (`POSTGRES_PASSWORD`, Cloudinary keys, etc.) live in the **server** `.env` file at `${DEPLOY_PATH}/.env`, not in GitHub.

## 4. Pipeline Behavior

| Trigger | Build & Test | Docker Push | SSH Deploy |
|---------|-------------|-------------|------------|
| Pull request to `main` | Yes | No | No |
| Push to `main` | Yes | Yes | Yes |

## 5. What Happens on Deploy

1. CI builds and pushes `username/xebia-lms-backend:<sha>` and `username/xebia-lms-frontend:<sha>`
2. `docker-compose.yml` and `deploy.sh` are copied to the server
3. SSH runs `deploy.sh` with `IMAGE_TAG=<sha>`
4. **PostgreSQL and Redis start first** and must pass health checks
5. Backend and frontend images are pulled and containers recreated with the new tag
6. Backend connects to Postgres at `postgres:5432` and Redis at `redis:6379` via the Docker network

### PostgreSQL & Redis in production

| Service | Container | Persistence | Backend connection |
|---------|-----------|-------------|-------------------|
| PostgreSQL | `xebia-lms-postgres` | `postgres_data` volume | `jdbc:postgresql://postgres:5432/lms` |
| Redis | `xebia-lms-redis` | `redis_data` volume (AOF) | `REDIS_HOST=redis`, `REDIS_PORT=6379` |

- Postgres and Redis are **not** redeployed on each CI run — only `backend` and `frontend` images are updated.
- Backend waits for both Postgres and Redis to be healthy before starting.
- If Redis is temporarily unavailable at runtime, the app falls back to PostgreSQL (cache miss).

## 6. Manual Deploy (on server)

```bash
cd /home/ubuntu/xebia-lms
export IMAGE_TAG=a1b2c3d        # specific build tag
export DOCKERHUB_USERNAME=your-user
export DOCKERHUB_TOKEN=your-token
./deploy/scripts/deploy.sh
```

## 7. Using Amazon RDS (optional)

To use RDS instead of the bundled Postgres container, set in `/home/ubuntu/xebia-lms/.env`:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://your-rds-endpoint:5432/lms
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-password
```

Then stop the local postgres container:

```bash
docker compose stop postgres
```

Ensure the RDS security group allows inbound `5432` from the EC2 instance.

## 8. Troubleshooting

- **Permission denied (docker):** log out and back in after `server-setup.sh`, or run `newgrp docker`
- **502 from frontend:** backend may still be starting — check `docker compose logs backend`
- **Old image still running:** verify `IMAGE_TAG` in deploy logs matches the latest push tag on Docker Hub
- **SSH fails in CI:** confirm `SSH_PRIVATE_KEY` includes the full PEM block with newlines
- **Backend can't connect to Postgres:** ensure `POSTGRES_PASSWORD` in `.env` matches the value used when the volume was first created; if you changed it, reset with `docker compose down -v` (destroys data) or update the password inside the container
- **Redis cache not working:** check `docker compose logs redis` and confirm `REDIS_HOST=redis` in `.env` (not `localhost`)

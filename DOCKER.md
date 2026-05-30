# Pmitz Docker Setup

Docker support in this repository targets the standalone `pmitz-remoteserver` application. The reusable
server-side remote-mode implementation lives in `pmitz-spring-boot-starter-remoteserver`; the Docker image packages
the standalone server built on top of that starter.

## Quick Start: Standalone Remote Server with PostgreSQL

The root [docker-compose.yml](docker-compose.yml) is the default Docker path for running the published Pmitz remote
server image with PostgreSQL.

```bash
docker compose up -d

# If PMITZ_API_KEY was not set, the container generates one at startup.
docker compose logs pmitz | grep "Generated API Key"
```

This compose file:

- pulls `terpomo/pmitz-remoteserver:0.9.0`
- starts PostgreSQL alongside the server
- activates the `postgresql` Spring profile
- reads `PMITZ_API_KEY`, `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` from your shell or `.env` file

## Run Against an Existing PostgreSQL Database

Use this when PostgreSQL is already managed outside Docker:

```bash
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=postgresql \
  -e PMITZ_API_KEY=my-secure-api-key \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/pmitz \
  -e SPRING_DATASOURCE_USERNAME=pmitz \
  -e SPRING_DATASOURCE_PASSWORD=your-db-password \
  terpomo/pmitz-remoteserver:0.9.0
```

The same release is also available from GHCR as `ghcr.io/terpomo-io/pmitz-remoteserver:0.9.0`.

## Build a Local Image from Source

Use this when you are changing the standalone server or Docker packaging locally:

```bash
docker build -t pmitz-local:dev .

docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=postgresql \
  -e PMITZ_API_KEY=my-secure-api-key \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/pmitz \
  -e SPRING_DATASOURCE_USERNAME=pmitz \
  -e SPRING_DATASOURCE_PASSWORD=your-db-password \
  pmitz-local:dev
```

## Configuration

Required environment variables for the standalone server:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Recommended environment variables:

- `SPRING_PROFILES_ACTIVE=postgresql`
- `PMITZ_API_KEY`

Optional repository table overrides:

- `PMITZ_REMOTESERVER_REPOSITORY_RDB_SCHEMA_NAME`
- `PMITZ_REMOTESERVER_REPOSITORY_RDB_USER_USAGE_TABLE_NAME`
- `PMITZ_REMOTESERVER_REPOSITORY_RDB_USER_LIMIT_TABLE_NAME`
- `PMITZ_REMOTESERVER_REPOSITORY_RDB_SUBSCRIPTION_TABLE_NAME`
- `PMITZ_REMOTESERVER_REPOSITORY_RDB_SUBSCRIPTION_PLAN_TABLE_NAME`

Example `.env` file for the root compose setup:

```bash
cat > .env <<'EOF'
PMITZ_API_KEY=my-secure-api-key
POSTGRES_DB=pmitz
POSTGRES_USER=pmitz
POSTGRES_PASSWORD=my-secure-db-password
EOF

docker compose up -d
```

## API Examples

Replace `YOUR_API_KEY` with the configured value or the generated key from the container logs.

### Add Product

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "productId": "Library",
    "features": [{
      "featureId": "Books",
      "limits": [{
        "type": "CountLimit",
        "id": "Max books",
        "count": 5,
        "unit": "books"
      }]
    }]
  }'
```

### Check Usage

```bash
# Users
curl -H "X-Api-Key: YOUR_API_KEY" \
  "http://localhost:8080/users/user1/usage/Library/Books"

# Subscriptions
curl -H "X-Api-Key: YOUR_API_KEY" \
  "http://localhost:8080/subscriptions/sub1/usage/Library/Books"

# Directory groups
curl -H "X-Api-Key: YOUR_API_KEY" \
  "http://localhost:8080/directory-groups/group1/usage/Library/Books"
```

### Record Usage

```bash
curl -X POST "http://localhost:8080/users/user1/usage/Library/Books" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{"units": {"Max books": 2}, "reduceUnits": false}'
```

## Database Access

```bash
# Connect to PostgreSQL started by docker compose
docker compose exec postgres psql -U "${POSTGRES_USER:-pmitz}" -d "${POSTGRES_DB:-pmitz}"

# View tables
\dt dbo.*

# View usage data
SELECT * FROM dbo.usage;

# View subscription data
SELECT * FROM dbo.subscription;
SELECT * FROM dbo.subscription_plan;
```

## Management

```bash
# View status
docker compose ps

# View logs
docker compose logs pmitz

# Stop
docker compose down

# Stop and remove data
docker compose down -v
```

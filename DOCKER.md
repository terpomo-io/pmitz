# pmitz Docker Setup

## Scenario 1: Development (Build from Source)

**What happens:** Builds pmitz from your local source code + includes PostgreSQL

**Use when:** You're developing pmitz or want to test local changes

```bash
# Uses existing docker-compose.yml which contains:
#   pmitz:
#     build: .    # This builds from Dockerfile in current directory
docker compose up -d

# Get API key
docker compose logs pmitz | grep "Generated API Key"
```

## Scenario 2: Production (Docker Hub Image + Database)

**What happens:** Downloads pre-built pmitz image + includes PostgreSQL

**Use when:** You want the official pmitz release with included database

Create new `docker-compose.yml`:
```yaml
services:
  pmitz:
    image: pmitz/pmitz:latest  # Downloads from Docker Hub
    ports:
      - "8080:8080"
    environment:
      - PMITZ_API_KEY=my-secure-api-key
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pmitz
      - SPRING_DATASOURCE_USERNAME=pmitz
      - SPRING_DATASOURCE_PASSWORD=my-secure-password
    depends_on:
      - postgres

  postgres:
    image: postgres:13-alpine
    environment:
      - POSTGRES_DB=pmitz
      - POSTGRES_USER=pmitz
      - POSTGRES_PASSWORD=my-secure-password
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

```bash
docker compose up -d
```

## Scenario 3: Enterprise (Docker Hub Image Only)

**What happens:** Downloads pre-built pmitz image only

**Use when:** You have your own PostgreSQL database
```bash
# Run pmitz container only
docker run -d \
  -p 8080:8080 \
  -e PMITZ_API_KEY=my-secure-api-key \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/pmitz \
  -e SPRING_DATASOURCE_USERNAME=pmitz \
  -e SPRING_DATASOURCE_PASSWORD=your-db-password \
  pmitz/pmitz:latest
```

## API Examples

Replace `YOUR_API_KEY` with the key from the logs.

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

# Directory Groups
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

## Custom API Key

```bash
PMITZ_API_KEY=my-key docker compose up -d
```

## Custom Configuration

Set environment variables to customize the setup:

```bash
# Custom API key and database password
PMITZ_API_KEY=my-secure-api-key \
POSTGRES_PASSWORD=my-secure-db-password \
docker compose up -d
```

Or use `.env` file:
```bash
cat > .env << EOF
PMITZ_API_KEY=my-secure-api-key
POSTGRES_PASSWORD=my-secure-db-password
POSTGRES_USER=pmitz
POSTGRES_DB=pmitz
EOF

docker compose up -d
```

**For Docker Hub image usage:**
```bash
# Run pmitz container with custom settings
docker run -d \
  -p 8080:8080 \
  -e PMITZ_API_KEY=my-secure-api-key \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/pmitz \
  -e SPRING_DATASOURCE_USERNAME=pmitz \
  -e SPRING_DATASOURCE_PASSWORD=your-db-password \
  pmitz/pmitz:latest
```

## Database Access

```bash
# Connect to database
docker compose exec postgres psql -U pmitz -d pmitz

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

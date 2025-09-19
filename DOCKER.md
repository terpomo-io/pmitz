# Docker Setup for pmitz Remote Server

This guide shows how to run the pmitz Remote Server as a Docker service with PostgreSQL.

## Prerequisites

- Docker and Docker Compose installed
- Git (to clone the repository)

## Quick Start

1. **Clone and navigate to the project:**
   ```bash
   git clone https://github.com/terpomo-io/pmitz.git
   cd pmitz
   ```

2. **Start the services:**
   ```bash
   docker-compose up --build
   ```

3. **Find your API key in the logs:**
   Look for the startup banner that displays your generated API key:
   ```
   ========================================
   🚀 pmitz Remote Server is starting...
   ========================================
   🔑 API Key: a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456
   📡 Server URL: http://localhost:8080
   
   📋 Usage Examples:
   curl -H "X-Api-Key: a1b2c3d4..." http://localhost:8080/products
   ========================================
   ```

4. **Test the API** (replace `YOUR_API_KEY` with the key from the logs):

## Complete API Testing Commands

### 1. Health Check
```bash
curl -f http://localhost:8080/actuator/health
```

### 2. Add Products
```bash
# Add Library product
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "id": "Library",
    "name": "Library Management System",
    "description": "A system for managing library resources"
  }'

# Add VideoStreaming product
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "id": "VideoStreaming",
    "name": "Video Streaming Service",
    "description": "A platform for streaming videos"
  }'
```

### 3. Test User Usage Tracking
```bash
# Get current usage for a user
curl -H "X-Api-Key: YOUR_API_KEY" \
  "http://localhost:8080/users/user001/usage/Library/Reserving%20books"

# Record usage (increment by 1)
curl -X POST http://localhost:8080/users/user001/usage/Library/Reserving%20books \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "amount": 1,
    "operation": "ADD"
  }'

# Check usage again to see the increment
curl -H "X-Api-Key: YOUR_API_KEY" \
  "http://localhost:8080/users/user001/usage/Library/Reserving%20books"

# Reduce usage (decrement by 1)
curl -X POST http://localhost:8080/users/user001/usage/Library/Reserving%20books \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "amount": 1,
    "operation": "SUBTRACT"
  }'
```

### 4. Test Subscription Usage Tracking
```bash
# Get subscription usage
curl -H "X-Api-Key: YOUR_API_KEY" \
  "http://localhost:8080/subscriptions/sub001/usage/VideoStreaming/Streaming%20hours"

# Record subscription usage
curl -X POST http://localhost:8080/subscriptions/sub001/usage/VideoStreaming/Streaming%20hours \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "amount": 5,
    "operation": "ADD"
  }'
```

### 5. Test Directory Group Usage
```bash
# Get directory group usage
curl -H "X-Api-Key: YOUR_API_KEY" \
  "http://localhost:8080/directory-groups/group001/usage/Library/Book%20checkouts"

# Record directory group usage
curl -X POST http://localhost:8080/directory-groups/group001/usage/Library/Book%20checkouts \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "amount": 3,
    "operation": "ADD"
  }'
```

### 6. Test Limits Checking
```bash
# Check if user can perform an action (limits verification)
curl -X POST http://localhost:8080/users/user001/limits-check/Library/Reserving%20books \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "amount": 1
  }'

# Check subscription limits
curl -X POST http://localhost:8080/subscriptions/sub001/limits-check/VideoStreaming/Streaming%20hours \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "amount": 2
  }'
```

### 7. Remove Products (Cleanup)
```bash
# Remove Library product
curl -X DELETE http://localhost:8080/products/Library \
  -H "X-Api-Key: YOUR_API_KEY"

# Remove VideoStreaming product
curl -X DELETE http://localhost:8080/products/VideoStreaming \
  -H "X-Api-Key: YOUR_API_KEY"
```

## Configuration

### Custom API Key

You can provide your own API key instead of using the generated one:

**Option 1: Environment variable in docker-compose.yml**
```yaml
services:
  pmitz:
    # ... other config ...
    environment:
      - PMITZ_API_KEY=your-custom-api-key-here
```

**Option 2: Command line**
```bash
docker-compose up --build -e PMITZ_API_KEY=your-custom-api-key-here
```

### Custom Database Credentials

For security reasons, you should change the default database credentials in production:

**Option 1: Edit docker-compose.yml**
```yaml
services:
  pmitz:
    environment:
      - SPRING_DATASOURCE_USERNAME=your_db_user
      - SPRING_DATASOURCE_PASSWORD=your_secure_password
      # ... other config ...
  
  postgres:
    environment:
      - POSTGRES_DB=pmitz
      - POSTGRES_USER=your_db_user
      - POSTGRES_PASSWORD=your_secure_password
```

**Option 2: Environment file (.env)**

Create a `.env` file in the project root:
```bash
# .env file
POSTGRES_USER=your_db_user
POSTGRES_PASSWORD=your_secure_password
PMITZ_API_KEY=your-custom-api-key
```

Then update docker-compose.yml to use these variables:
```yaml
services:
  pmitz:
    environment:
      - SPRING_DATASOURCE_USERNAME=${POSTGRES_USER}
      - SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}
      - PMITZ_API_KEY=${PMITZ_API_KEY}
      # ... other config ...
  
  postgres:
    environment:
      - POSTGRES_DB=pmitz
      - POSTGRES_USER=${POSTGRES_USER}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
```

**Option 3: Command line override**
```bash
POSTGRES_USER=myuser POSTGRES_PASSWORD=mysecurepass123 docker-compose up --build
```

### Database Access

The PostgreSQL database is accessible on `localhost:5432` with the default credentials:
- **Database**: `pmitz`
- **Username**: `pmitz` (or your custom `POSTGRES_USER`)
- **Password**: `pmitz123` (or your custom `POSTGRES_PASSWORD`)

```bash
# Connect to the database (with default credentials)
docker-compose exec postgres psql -U pmitz -d pmitz

# Connect with custom credentials
docker-compose exec postgres psql -U your_db_user -d pmitz
```

## Stopping the Services

```bash
# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes (removes all data)
docker-compose down -v
```

## Troubleshooting

### Check container logs
```bash
# pmitz server logs
docker-compose logs pmitz

# PostgreSQL logs
docker-compose logs postgres

# Follow logs in real-time
docker-compose logs -f
```

### Rebuild after code changes
```bash
docker-compose down
docker-compose up --build
```

### Reset everything (including database)
```bash
docker-compose down -v
docker-compose up --build
```

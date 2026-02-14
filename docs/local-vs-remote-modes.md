# Local vs Remote Modes

Pmitz supports two operational modes for subscription and limit verification: **Local Mode** and **Remote Mode**. This document explains the differences, use cases, and architecture of each approach.

## Overview

| Aspect | Local Mode | Remote Mode |
|--------|-----------|-------------|
| **Deployment** | Embedded in application | Centralized Spring Boot server |
| **Database Access** | Direct JDBC connection | Server manages database |
| **Network** | None (in-process) | HTTP/HTTPS required |
| **Latency** | Minimal (same JVM) | Network dependent |
| **Scalability** | Per-application instance | Shared across applications |
| **Best For** | Monolithic apps, single instance | Microservices, multi-tenant |

## Local Mode

In Local Mode, verification runs directly within your application process using JDBC database connections.

### Architecture

```mermaid
flowchart TB
    subgraph Application["Your Application"]
        App[Application Code]
        LV[LimitVerifier]
        SV[SubscriptionVerifier]
        PR[ProductRepository]
    end

    subgraph Database["Database"]
        UT[usage table]
        ULT[user_limit table]
        ST[subscription table]
        SPT[subscription_plan table]
    end

    App --> LV
    App --> SV
    LV --> PR
    SV --> PR
    LV -->|JDBC| UT
    LV -->|JDBC| ULT
    SV -->|JDBC| ST
    SV -->|JDBC| SPT
```

### Verification Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant LV as LimitVerifier
    participant LRR as LimitRuleResolver
    participant UR as UsageRepository
    participant DB as Database

    App->>LV: recordFeatureUsage(feature, user, limits)
    LV->>LRR: resolveLimits(feature, user)
    LRR-->>LV: resolved limits
    LV->>UR: getCurrentUsage(feature, user)
    UR->>DB: SELECT usage
    DB-->>UR: usage data
    UR-->>LV: current usage
    LV->>LV: verify limits not exceeded
    alt Within Limits
        LV->>UR: incrementUsage(feature, user)
        UR->>DB: UPDATE usage
        LV-->>App: success
    else Limit Exceeded
        LV-->>App: LimitExceededException
    end
```

### Configuration

```java
// Load product definitions
ProductRepository productRepo = new InMemoryProductRepository();
productRepo.load(getClass().getResourceAsStream("/products.json"));

// Build LimitVerifier with local database
LimitVerifier limitVerifier = LimitVerifierBuilder.of(productRepo)
    .withDefaultLimitRuleResolver()
    .withJdbcUsageRepository(dataSource, "dbo", "usage")
    .build();

// Build SubscriptionVerifier with local database
SubscriptionVerifier subscriptionVerifier = SubscriptionVerifierBuilder
    .withJdbcSubscriptionRepository(dataSource, "dbo", "subscription", "subscription_plan")
    .withDefaultSubscriptionFeatureManager(productRepo)
    .build();
```

### When to Use Local Mode

- Single application instance
- Monolithic architecture
- Low latency requirements
- Application already has database access
- Simple deployment without additional services

## Remote Mode

In Remote Mode, verification is delegated to a centralized Pmitz server via HTTP/HTTPS REST API.

### Architecture

```mermaid
flowchart TB
    subgraph ClientApp["Client Application"]
        App[Application Code]
        RC[LimitVerifierRemoteClient]
    end

    subgraph PmitzServer["Pmitz Server"]
        API[REST API Controller]
        FUT[FeatureUsageTracker]
        LV[LimitVerifier]
        SV[SubscriptionVerifier]
    end

    subgraph Database["Database"]
        UT[usage table]
        ST[subscription table]
    end

    App --> RC
    RC -->|HTTP/HTTPS| API
    API --> FUT
    FUT --> LV
    FUT --> SV
    LV -->|JDBC| UT
    SV -->|JDBC| ST
```

### Verification Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant RC as RemoteClient
    participant API as Pmitz Server API
    participant LV as LimitVerifier
    participant DB as Database

    App->>RC: recordFeatureUsage(feature, user, limits)
    RC->>API: POST /users/{userId}/usage/{productId}/{featureId}
    Note over RC,API: X-Api-Key header for auth
    API->>LV: recordFeatureUsage(feature, user, limits)
    LV->>DB: Check and update usage
    alt Within Limits
        DB-->>LV: success
        LV-->>API: success
        API-->>RC: HTTP 200 OK
        RC-->>App: success
    else Limit Exceeded
        LV-->>API: LimitExceededException
        API-->>RC: HTTP 400 + error details
        RC-->>App: LimitExceededException
    end
```

### Configuration

**Client Side:**

```java
// Create remote client pointing to Pmitz server
LimitVerifier limitVerifier = new LimitVerifierRemoteClient("http://localhost:8080");

// Upload product definitions to server
limitVerifier.uploadProduct(getClass().getResourceAsStream("/products.json"));
```

**Server Side (Environment Variables):**

```bash
# Database configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pmitz
SPRING_DATASOURCE_USERNAME=pmitz
SPRING_DATASOURCE_PASSWORD=secret

# API authentication
PMITZ_API_KEY=your-api-key
```

### REST API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/{userGroupingType}/{id}/usage/{productId}/{featureId}` | Get current usage |
| `POST` | `/{userGroupingType}/{id}/usage/{productId}/{featureId}` | Record usage |
| `POST` | `/{userGroupingType}/{id}/limits-check/{productId}/{featureId}` | Check if within limits |
| `POST` | `/products` | Upload product definition |
| `DELETE` | `/products/{productId}` | Remove product |

**User Grouping Types:**
- `users` - Individual users
- `subscriptions` - Subscription-based grouping
- `directory-groups` - Directory/team groups

### When to Use Remote Mode

- Microservices architecture
- Multiple applications sharing usage data
- Centralized usage tracking across services
- Multi-tenant SaaS applications
- Need for shared rate limiting

## Multi-Application Architecture (Remote Mode)

```mermaid
flowchart TB
    subgraph Apps["Applications"]
        A1[Web App]
        A2[Mobile API]
        A3[Background Jobs]
    end

    subgraph Clients["Remote Clients"]
        C1[RemoteClient]
        C2[RemoteClient]
        C3[RemoteClient]
    end

    subgraph Server["Pmitz Server"]
        API[REST API]
        Core[Verification Core]
    end

    subgraph DB["Shared Database"]
        Usage[(Usage Data)]
        Subs[(Subscriptions)]
    end

    A1 --> C1
    A2 --> C2
    A3 --> C3
    C1 --> API
    C2 --> API
    C3 --> API
    API --> Core
    Core --> Usage
    Core --> Subs
```

## Choosing Between Modes

```mermaid
flowchart TD
    Start([Start]) --> Q1{Multiple applications<br/>share usage data?}
    Q1 -->|Yes| Remote[Use Remote Mode]
    Q1 -->|No| Q2{Microservices<br/>architecture?}
    Q2 -->|Yes| Remote
    Q2 -->|No| Q3{Need centralized<br/>rate limiting?}
    Q3 -->|Yes| Remote
    Q3 -->|No| Q4{Low latency<br/>critical?}
    Q4 -->|Yes| Local[Use Local Mode]
    Q4 -->|No| Q5{Simple single-app<br/>deployment?}
    Q5 -->|Yes| Local
    Q5 -->|No| Remote
```

## Code Example Comparison

Both modes implement the same `LimitVerifier` interface, making them interchangeable:

```java
// Same usage code works with both modes
Feature feature = productRepo.getFeature("Library", "Reserving books");
UserGrouping user = new IndividualUser("user123");

// Record usage
limitVerifier.recordFeatureUsage(feature, user, Map.of("Maximum books reserved", 5L));

// Check remaining quota
Map<String, Long> remaining = limitVerifier.getLimitsRemainingUnits(feature, user);

// Check if within limits (without recording)
boolean withinLimits = limitVerifier.isWithinLimits(feature, user, Map.of("Maximum books reserved", 1L));
```

## Database Requirements

Both modes require the same database schema:

| Table | Purpose |
|-------|---------|
| `usage` | Tracks usage counts per user/feature/limit |
| `user_limit` | Per-user limit overrides (optional) |
| `subscription` | Subscription records |
| `subscription_plan` | Maps subscriptions to product plans |

**Supported Databases:** PostgreSQL, MySQL, SQL Server, H2 (dev/test)

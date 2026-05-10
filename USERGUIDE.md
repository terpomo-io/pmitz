# Pmitz User Guide

## Overview

**Pmitz** is a Java library for controlling user access to application features based on subscriptions and usage limits. It simplifies implementing subscription models and configurable usage quotas in your applications.

- **Recommended Maven Coordinates:** `io.terpomo.pmitz:pmitz-all`
- **Version:** 0.9.0
- **Java Version:** 17+
- **License:** Apache 2.0

---

## Module Structure

| Module | Purpose |
|--------|---------|
| `core` | Domain models, interfaces, and base abstractions |
| `limits` | Usage limit verification and tracking |
| `subscriptions` | Subscription management and verification |
| `all` | Aggregates core + limits modules |
| `remoteserver` | Standalone Spring Boot REST API server |
| `spring-boot-starter-remoteserver` | Embeddable Spring Boot starter for remote mode |
| `remoteclient` | HTTP client for remote server |
| `examples` | Sample applications |

---

## Core Concepts

### User Types

Pmitz supports different user abstractions through `UserGrouping`:

| Class | Description |
|-------|-------------|
| `IndividualUser` | Single user entity |
| `DirectoryGroup` | Group of users from organization/directory |
| `Subscription` | User with subscription status, expiration, and product-plan mappings |

### Product & Features

```
Product
├── productId
├── features[]
│   ├── featureId
│   └── limits[]
└── plans[]
    ├── planId
    └── includedFeatures[]
```

### Limit Types

| Type | Description | Example |
|------|-------------|---------|
| `CountLimit` | Simple counter limit | Max 5 books reserved |
| `CalendarPeriodRateLimit` | Calendar-aligned rate limit | 100 API calls per month |

### Feature Status

| Status | Meaning |
|--------|---------|
| `AVAILABLE` | Feature available within limits |
| `LIMIT_EXCEEDED` | Feature available but usage limit reached |
| `NOT_ALLOWED` | User not entitled to this feature |

---

## Quick Start

### 1. Add Dependencies

**Gradle:**
```groovy
implementation 'io.terpomo.pmitz:pmitz-all:0.9.0'
// Or specific modules:
implementation 'io.terpomo.pmitz:pmitz-core:0.9.0'
implementation 'io.terpomo.pmitz:pmitz-limits:0.9.0'
implementation 'io.terpomo.pmitz:pmitz-subscriptions:0.9.0'
```

### 2. Define Your Product (JSON)

```json
{
  "productId": "Library",
  "features": [
    {
      "featureId": "Reserving books",
      "limits": [
        {
          "type": "CountLimit",
          "id": "Maximum books reserved",
          "count": 5
        }
      ]
    },
    {
      "featureId": "API calls",
      "limits": [
        {
          "type": "CalendarPeriodRateLimit",
          "id": "Monthly API quota",
          "quota": 1000,
          "periodicity": "MONTH"
        }
      ]
    }
  ]
}
```

### 3. Load Product Configuration

```java
InMemoryProductRepository productRepo = new InMemoryProductRepository();
productRepo.load(getClass().getResourceAsStream("/product.json"));
```

---

## Limit Verification

### Building a LimitVerifier

```java
// Basic setup with JDBC storage
LimitVerifier limitVerifier = LimitVerifierBuilder.of(productRepo)
    .withDefaultLimitRuleResolver()
    .withJdbcUsageRepository(dataSource, "dbo", "usage")
    .build();

// With user-specific limit overrides
UserLimitRepository userLimitRepo = UserLimitRepository.builder()
    .jdbcRepository(dataSource, "dbo", "user_limit");

LimitVerifier limitVerifier = LimitVerifierBuilder.of(productRepo)
    .withUserLimitRepository(userLimitRepo)
    .withJdbcUsageRepository(dataSource, "dbo", "usage")
    .build();
```

### Checking Limits

```java
Feature feature = productRepo.getProductById("Library")
    .flatMap(p -> p.getFeature("Reserving books"))
    .orElseThrow();

IndividualUser user = new IndividualUser("user123");

// Check if within limits
boolean withinLimits = limitVerifier.isWithinLimits(feature, user, Map.of("Maximum books reserved", 1L));

// Get remaining units
Map<String, Long> remaining = limitVerifier.getLimitsRemainingUnits(feature, user);
```

### Recording Usage

```java
try {
    // Records usage and throws if limit exceeded
    limitVerifier.recordFeatureUsage(feature, user, Map.of("Maximum books reserved", 1L));
} catch (LimitExceededException e) {
    // Handle limit exceeded
}
```

### Reducing Usage (e.g., when user returns a book)

```java
limitVerifier.reduceFeatureUsage(feature, user, Map.of("Maximum books reserved", 1L));
```

---

## Subscription Verification

### Building a SubscriptionVerifier

```java
SubscriptionVerifier subscriptionVerifier = SubscriptionVerifierBuilder
    .withJdbcSubscriptionRepository(dataSource, "dbo", "subscription", "subscription_plan")
    .withDefaultSubscriptionFeatureManager(productRepo)
    .build();
```

### Verifying Entitlement

```java
Subscription subscription = subscriptionRepo.find("subscription-id").orElseThrow();
Feature feature = productRepo.getFeature("productId", "featureId");

SubscriptionVerifDetail detail = subscriptionVerifier.verifyEntitlement(feature, subscription);

if (detail.isAllowed()) {
    // Feature is allowed
} else {
    // Check detail.getErrorCause() for:
    // - INVALID_SUBSCRIPTION
    // - PRODUCT_NOT_ALLOWED
    // - FEATURE_NOT_ALLOWED
}

// Convenience method
boolean allowed = subscriptionVerifier.isFeatureAllowed(feature, subscription);
```

### Subscription Lifecycle

```java
SubscriptionRepository subscriptionRepo = JDBCSubscriptionRepository.create(
    dataSource, "dbo", "subscription", "subscription_plan");

// Create subscription
subscriptionRepo.create(subscription);

// Lifecycle methods
subscriptionRepo.activate(subscriptionId);
subscriptionRepo.cancel(subscriptionId);
subscriptionRepo.terminate(subscriptionId);
```

---

## Remote Server

### Architecture

Remote mode is available in two server-side forms:

- `spring-boot-starter-remoteserver` embeds the remote REST endpoints, API-key security, and JDBC-backed usage and subscription repositories into your own Spring Boot application.
- `remoteserver` packages that starter as the standalone Pmitz server. This is the Docker deployment target documented in [DOCKER.md](DOCKER.md).

### Standalone Deployment

The standalone `remoteserver` module provides a Spring Boot REST API:

```bash
docker run -e SPRING_PROFILES_ACTIVE=postgresql \
           -e PMITZ_API_KEY=your-api-key \
           -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db \
           -e SPRING_DATASOURCE_USERNAME=user \
           -e SPRING_DATASOURCE_PASSWORD=pass \
           terpomo/pmitz-remoteserver:0.9.0
```

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/products` | Add product configuration |
| DELETE | `/products/{productId}` | Remove product |
| GET | `/users/{userId}/usage/{productId}/{featureId}` | Get remaining units |
| POST | `/users/{userId}/usage/{productId}/{featureId}` | Record usage |
| POST | `/users/{userId}/limits-check/{productId}/{featureId}` | Check whether usage would remain within limits |
| GET | `/users/{userId}/subscription-check/{productId}/{featureId}` | Check subscription entitlement |
| GET | `/directory-groups/{groupId}/usage/...` | Group usage queries |
| POST | `/directory-groups/{groupId}/usage/...` | Record group usage |
| POST | `/subscriptions` | Create a subscription |
| GET | `/subscriptions/{subscriptionId}` | Load a subscription |
| PATCH | `/subscriptions/{subscriptionId}/status` | Update subscription status |

### Authentication

All requests require the `X-Api-Key` header matching the configured `PMITZ_API_KEY`. API-key security is the
currently supported remote-server security mode.

### Remote Server Properties

Default repository objects are stored in the `dbo` schema with these table names:

- `usage`
- `user_limit`
- `subscription`
- `subscription_plan`

Override them with Spring configuration properties if needed:

```properties
pmitz.remoteserver.repository.rdb.schema-name=dbo
pmitz.remoteserver.repository.rdb.user-usage-table-name=usage
pmitz.remoteserver.repository.rdb.user-limit-table-name=user_limit
pmitz.remoteserver.repository.rdb.subscription-table-name=subscription
pmitz.remoteserver.repository.rdb.subscription-plan-table-name=subscription_plan
```

---

## Remote Client

### Using LimitVerifierRemoteClient

```java
// PMITZ_API_KEY or -Dpmitz.api.key must be set before creating the client.
LimitVerifierRemoteClient remoteVerifier = new LimitVerifierRemoteClient("http://localhost:8080");

// Upload product definition
remoteVerifier.uploadProduct(productJsonInputStream);

// Use same API as local LimitVerifier
try {
    remoteVerifier.recordFeatureUsage(feature, user, units);
} catch (RemoteCallException e) {
    // Handle network/server errors
}

// Get remaining units
Map<String, Long> remaining = remoteVerifier.getLimitsRemainingUnits(feature, user);
```

### Low-Level Client

```java
System.setProperty("pmitz.api.key", "your-api-key");

PmitzClient client = new PmitzHttpClient(
    "http://localhost:8080",
    new PmitzApiKeyAuthenticationProvider());

FeatureUsageInfo info = client.verifyLimits(
    new FeatureRef("Library", "Reserving books"),
    new IndividualUser("user123"),
    Map.of("Maximum books reserved", 1L));
```

---

## Database Setup

### Supported Databases

- PostgreSQL (recommended for production)
- MySQL
- SQL Server
- H2 (for testing/development)

### Required Tables

**Usage tracking (`dbo.usage`):**
```sql
CREATE TABLE dbo.usage (
    -- Schema varies by database, see /resources/scripts/repos/sql/
);
```

**User limit overrides (`dbo.user_limit`):**
```sql
CREATE TABLE dbo.user_limit (
    -- Schema varies by database
);
```

**Subscriptions (`dbo.subscription`, `dbo.subscription_plan`):**
```sql
CREATE TABLE dbo.subscription (...);
CREATE TABLE dbo.subscription_plan (...);
```

SQL scripts for each database are available in the module resources under `/scripts/repos/sql/`.

---

## Setting User-Specific Limits

Override default limits for specific users:

```java
UserLimitRepository userLimitRepo = UserLimitRepository.builder()
    .jdbcRepository(dataSource, "dbo", "user_limit");

// Give premium user higher limit
CountLimit premiumLimit = new CountLimit("Maximum books reserved", 20);
userLimitRepo.updateLimitRule(feature, premiumLimit, premiumUser);
```

---

## Exception Handling

| Exception | When Thrown |
|-----------|-------------|
| `LimitExceededException` | Usage recording exceeds limit |
| `FeatureNotAllowedException` | User not entitled to feature |
| `FeatureNotFoundException` | Feature doesn't exist |
| `ConfigurationException` | Configuration problem |
| `RepositoryException` | Database access error |
| `RemoteCallException` | Network/remote server error |
| `AuthenticationException` | API authentication failure |

---

## Complete Example

```java
public class LibrarySystem {

    private final LimitVerifier limitVerifier;
    private final SubscriptionVerifier subscriptionVerifier;
    private final ProductRepository productRepo;

    public LibrarySystem(DataSource dataSource) {
        // Load product configuration
        this.productRepo = new InMemoryProductRepository();
        productRepo.load(getClass().getResourceAsStream("/library-product.json"));

        // Build limit verifier
        this.limitVerifier = LimitVerifierBuilder.of(productRepo)
            .withDefaultLimitRuleResolver()
            .withJdbcUsageRepository(dataSource, "library", "usage")
            .build();

        // Build subscription verifier
        this.subscriptionVerifier = SubscriptionVerifierBuilder
            .withJdbcSubscriptionRepository(dataSource, "library", "subscription", "subscription_plan")
            .withDefaultSubscriptionFeatureManager(productRepo)
            .build();
    }

    public void reserveBook(String userId, String bookId) {
        IndividualUser user = new IndividualUser(userId);
        Feature reservingBooks = productRepo.getFeature("Library", "Reserving books");

        // Check subscription entitlement
        Subscription userSub = getUserSubscription(userId);
        if (!subscriptionVerifier.isFeatureAllowed(reservingBooks, userSub)) {
            throw new RuntimeException("Feature not allowed for your subscription");
        }

        // Record usage (throws if limit exceeded)
        limitVerifier.recordFeatureUsage(reservingBooks, user,
            Map.of("Maximum books reserved", 1L));

        // Proceed with reservation...
    }

    public void returnBook(String userId, String bookId) {
        IndividualUser user = new IndividualUser(userId);
        Feature reservingBooks = productRepo.getFeature("Library", "Reserving books");

        // Reduce usage count
        limitVerifier.reduceFeatureUsage(reservingBooks, user,
            Map.of("Maximum books reserved", 1L));

        // Proceed with return...
    }

    public int getRemainingReservations(String userId) {
        IndividualUser user = new IndividualUser(userId);
        Feature reservingBooks = productRepo.getFeature("Library", "Reserving books");

        Map<String, Long> remaining = limitVerifier.getLimitsRemainingUnits(reservingBooks, user);
        return remaining.get("Maximum books reserved").intValue();
    }
}
```

---

## Builder Reference

### LimitVerifierBuilder

```java
LimitVerifierBuilder.of(productRepository)
    // Limit resolution (pick one):
    .withDefaultLimitRuleResolver()           // Use default limits from product
    .withUserLimitRepository(userLimitRepo)   // Support user-specific overrides
    .withCustomLimitRuleResolver(resolver)    // Custom resolver

    // Usage storage (pick one):
    .withJdbcUsageRepository(dataSource, schema, table)
    .withCustomUsageRepository(usageRepo)

    // Optional:
    .withUserLimitVerificationStrategy(strategy)  // Custom verification logic

    .build();
```

### SubscriptionVerifierBuilder

```java
SubscriptionVerifierBuilder
    // Subscription repository (pick one):
    .withSubscriptionRepository(subscriptionRepo)
    .withJdbcSubscriptionRepository(ds, schema, subTable, planTable)

    // Feature manager (pick one):
    .withDefaultSubscriptionFeatureManager(productRepo)
    .withSubscriptionFeatureManager(customManager)

    .build();
```

---

## Additional Resources

- [README.md](README.md) - Project overview and quick start
- [DOCKER.md](DOCKER.md) - Docker deployment instructions
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines
- [CODE_STYLE.md](CODE_STYLE.md) - Code style guidelines
- [Examples module](examples/) - Sample applications with working code

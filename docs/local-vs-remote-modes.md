# Local vs Remote Modes

This is the architecture deep-dive behind the [Local mode vs. Remote mode](../USERGUIDE.md#local-mode-vs-remote-mode) section of the [User Guide](../USERGUIDE.md) — start there for the concepts (the subscription/limit two-gate check, the data model) this page assumes. This page shows what each mode looks like internally.

## Overview

| Aspect | Local Mode | Remote Mode |
|--------|-----------|-------------|
| **Deployment** | Embedded in your application | Centralized `remoteserver`, standalone or starter-embedded |
| **Database access** | Direct JDBC connection | Server manages the database |
| **Network** | None (in-process) | HTTP/HTTPS |
| **Best for** | A single application or monolith | Multiple applications sharing usage and entitlement data |

## Local Mode

Verification runs in-process, reading and writing the database directly.

```mermaid
flowchart TB
    subgraph Application["Your Application"]
        App[Application Code]
        LV[LimitVerifier]
        SV[SubscriptionVerifier]
        PR[ProductRepository]
    end

    subgraph Database["Database"]
        UD[("Usage & limit tables")]
        SD[("Subscription tables")]
    end

    App --> LV
    App --> SV
    LV --> PR
    SV --> PR
    LV -->|JDBC| UD
    SV -->|JDBC| SD
```

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
    alt Within limits
        LV->>UR: incrementUsage(feature, user)
        UR->>DB: UPDATE usage
        LV-->>App: success
    else Limit exceeded
        LV-->>App: LimitExceededException
    end
```

Setup code: [Quick Start](../USERGUIDE.md#quick-start), [Limit Verification](../USERGUIDE.md#limit-verification), [Subscription Verification](../USERGUIDE.md#subscription-verification).

## Remote Mode

Verification is delegated over HTTP/HTTPS to a centralized Pmitz server (`remoteserver`, or an app embedding `spring-boot-starter-remoteserver`).

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
        UD[("Usage tables")]
        SD[("Subscription tables")]
    end

    App --> RC
    RC -->|HTTP/HTTPS| API
    API --> FUT
    FUT --> LV
    FUT --> SV
    LV -->|JDBC| UD
    SV -->|JDBC| SD
```

```mermaid
sequenceDiagram
    participant App as Application
    participant RC as RemoteClient
    participant API as Pmitz Server API
    participant LV as LimitVerifier
    participant DB as Database

    App->>RC: recordFeatureUsage(feature, user, limits)
    RC->>API: POST /{userGroupingType}/{id}/usage/{productId}/{featureId}
    Note over RC,API: X-Api-Key header for auth
    API->>LV: recordFeatureUsage(feature, user, limits)
    LV->>DB: Check and update usage
    alt Within limits
        DB-->>LV: success
        LV-->>API: success
        API-->>RC: HTTP 200 OK
        RC-->>App: success
    else Limit exceeded
        LV-->>API: LimitExceededException
        API-->>RC: HTTP 422 + error details
        RC-->>App: LimitExceededException
    end
```

Endpoints, authentication, server configuration, and client code: [Remote Server](../USERGUIDE.md#remote-server), [Remote Client](../USERGUIDE.md#remote-client).

## Choosing Between Modes

Use the **Best for** row in the [Overview](#overview) table above as a quick check. **Rule of thumb:** start Local; move to Remote only once more than one process needs to agree on the same usage counters or subscription state.

Both modes implement the same `LimitVerifier` / `SubscriptionVerifier` contracts and the same database schema ([Database Setup](../USERGUIDE.md#database-setup)), so switching later is a construction-time decision, not a rewrite.

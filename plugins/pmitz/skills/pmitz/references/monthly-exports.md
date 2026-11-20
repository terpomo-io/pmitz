# Monthly report exports

**Known limitation in 0.10.0.** Calendar-period quotas (hour, day, week, month, year) are not enforced with JDBC usage storage: every recording is accepted and the stored count stays at one. Confirmed on H2, PostgreSQL, MySQL and SQL Server. [Issue #46](https://github.com/terpomo-io/pmitz/issues/46) Pmitz sandboxes already run with the fix, so the quota is enforced there.

Free and Pro both include exports. One completed export costs one unit. The workspace subscription shares a single allowance across its members. Free gets 10 and Pro gets 1,000 per calendar month.

This is a catalog file for `InMemoryProductRepository.load(InputStream)`, which accepts an array of products.

```json
[
  {
    "productId": "reports",
    "features": [{
      "featureId": "export",
      "limits": [{
        "type": "CalendarPeriodRateLimit",
        "id": "monthly-exports",
        "quota": 10,
        "periodicity": "MONTH"
      }]
    }],
    "plans": [
      {"planId": "free", "includedFeatures": ["export"]},
      {
        "planId": "pro",
        "includedFeatures": ["export"],
        "limitsOverride": [{
          "type": "CalendarPeriodRateLimit",
          "id": "monthly-exports",
          "quota": 1000,
          "periodicity": "MONTH"
        }]
      }
    ]
  }
]
```

In an embedded Java handler, assume `subscriptions` is the configured `SubscriptionRepository`, `tracker` is the configured `FeatureUsageTracker`, and the app has resolved `workspaceSubscriptionId` from the signed-in workspace. That subscription needs a stored mapping from `reports` to `free` or `pro`.

```java
var workspace = subscriptions.find(workspaceSubscriptionId).orElseThrow();
var feature = new FeatureRef("reports", "export");
var units = Map.of("monthly-exports", 1L);
tracker.recordFeatureUsage(feature, workspace, units);
```

The call records one unit. Handle `FeatureNotAllowedException` as an access denial and `LimitExceededException` as an exhausted allowance. A Free workspace with nine exports can record one more. Its next request is refused. A Pro workspace with ten can still record its eleventh. This snippet covers the Pmitz call. Connect it to the app's export success, failure and retry handling so that one completed export counts once.

For complete initialization, see [embedded Java](embedded-java.md). For the HTTP equivalent and its response semantics, see [REST](rest.md).

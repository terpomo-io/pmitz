# Product catalog

The REST creation body is one product object. Embedded Java's `InMemoryProductRepository.load(InputStream)` expects an array of product objects. Keep the catalog in the application's source or configuration, including all plan overrides.

| Object | Fields the developer supplies |
|---|---|
| Product | `productId`, `features` (array), `plans` (array) |
| Feature | `featureId`, `limits` (array, empty for access-only features) |
| Plan | `planId`, optional `description`, `includedFeatures` (array of feature ID strings), optional `limitsOverride` (array of complete limit objects) |
| CountLimit | `type: "CountLimit"`, `id`, `count` (integer), optional `unit` label |
| CalendarPeriodRateLimit | `type: "CalendarPeriodRateLimit"`, `id`, `quota` (integer per period), `periodicity`, optional `unit` label |

These are the two registered JSON limit types. A count has no automatic reset. A calendar period is HOUR, DAY, WEEK, MONTH or YEAR. Weeks start Monday. Boundaries use the clock and time zone of the Pmitz process; choose its zone deliberately. The unit label describes the quantity; the application supplies the units counted by each action.

Example REST product with both types:
```json
{"productId":"reports","features":[{"featureId":"export","limits":[
  {"type":"CountLimit","id":"export-credits","count":500,"unit":"credits"},
  {"type":"CalendarPeriodRateLimit","id":"monthly-exports","quota":10,"periodicity":"MONTH"}
]}],"plans":[{"planId":"free","includedFeatures":["export"]},
{"planId":"pro","description":"Pro reports","includedFeatures":["export"],"limitsOverride":[
  {"type":"CalendarPeriodRateLimit","id":"monthly-exports","quota":1000,"periodicity":"MONTH"}
]}]}
```

Each export under this catalog supplies both unit keys. Use distinct limit IDs across features within a product; a plan override is matched by limit ID. Resolution prefers a subject-specific override when configured, then the plan override, then the feature default. Including a limit override does not grant access to an excluded feature.

[JSON mix-ins and subtype registrations](https://github.com/terpomo-io/pmitz/tree/v0.10.0/core/src/main/java/io/terpomo/pmitz/core/repository/product/inmemory)

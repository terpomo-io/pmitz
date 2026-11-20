# Embedded Java wiring

For an embedded application use Java 17+, a JDBC DataSource, and dependencies from Maven Central. Set `PMITZ_JAVA_VERSION` to the published version you intend to integrate. Declare the modules you import on the compile classpath:

```groovy
repositories { mavenCentral() }
dependencies {
    def version = providers.environmentVariable('PMITZ_JAVA_VERSION').get()
    ['core', 'subscriptions', 'limits', 'all'].each { name ->
        implementation "io.terpomo.pmitz:pmitz-${name}:${version}"
    }
}
```

For a complete local run, follow the [embedded export example](exports-integration.md#java-application-with-pmitz-embedded). For Maven, declare the same four artifact IDs under group `io.terpomo.pmitz` at the same version.

For the disposable H2 development example, add `implementation "com.h2database:h2:2.4.240"` if the app constructs an `org.h2.jdbcx.JdbcDataSource`. Initialize the database once using the matching dialect's [schema scripts](https://github.com/terpomo-io/pmitz/tree/v0.10.0/resources/scripts/repos/sql) or [standalone migrations](https://github.com/terpomo-io/pmitz/tree/v0.10.0/remoteserver/src/main/resources/db/migration). The example uses schema dbo and tables usage, subscription and subscription_plan. Embedded wiring assumes these exist.

```java
import java.io.InputStream;
import java.util.Map;
import javax.sql.DataSource;
import io.terpomo.pmitz.all.usage.tracker.FeatureUsageTracker;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.core.subscriptions.*;
import io.terpomo.pmitz.limits.LimitVerifierBuilder;
import io.terpomo.pmitz.subscriptions.SubscriptionVerifierBuilder;
import io.terpomo.pmitz.subscriptions.jdbc.JDBCSubscriptionRepository;

record Reports(SubscriptionRepository subscriptions, FeatureUsageTracker tracker) {
    static Reports connect(DataSource db, InputStream catalogArray) {
        var products = new InMemoryProductRepository();
        products.load(catalogArray);
        var subscriptions = new JDBCSubscriptionRepository(db, "dbo", "subscription", "subscription_plan");
        var entitlement = SubscriptionVerifierBuilder.withSubscriptionRepository(subscriptions)
            .withDefaultSubscriptionFeatureManager(products).build();
        var limits = LimitVerifierBuilder.of(products).withDefaultLimitRuleResolver()
            .withJdbcUsageRepository(db, "dbo", "usage").build();
        return new Reports(subscriptions, FeatureUsageTracker.Builder.build(limits, entitlement));
    }

    void provisionWorkspace(String id, String plan) {
        var workspace = new Subscription(id);
        workspace.setStatus(SubscriptionStatus.ACTIVE);
        workspace.setPlans(Map.of("reports", plan));
        subscriptions.create(workspace);
    }

    void recordExport(String authenticatedWorkspaceId) {
        var workspace = subscriptions.find(authenticatedWorkspaceId).orElseThrow();
        tracker.recordFeatureUsage(new FeatureRef("reports", "export"), workspace,
            Map.of("monthly-exports", 1L));
    }
}
```

Pass a catalog with a single monthly-exports rule, as in [monthly exports](monthly-exports.md). Construct and reuse Reports at application startup and close the caller-owned catalog stream after loading it. Provision a subscription when the workspace gets its plan. The export handler can call recordExport and handle FeatureNotAllowedException or LimitExceededException before delivering the export. Reconcile any failure between recording usage and completing the business action according to the app's transaction or retry model.

For occupied seats, configure a CountLimit and call the tracker only when membership changes. The calls below assume an entitled workspace, a feature named `seat`, and a limit with ID `occupied-seats`:

```java
var feature = new FeatureRef("teams", "seat");
var oneSeat = Map.of("occupied-seats", 1L);
// Adding a new member:
tracker.recordFeatureUsage(feature, workspace, oneSeat);
// Removing a seated member, in the separate removal handler:
tracker.reduceFeatureUsage(feature, workspace, oneSeat);
```

Both methods take positive units. Handle duplicate adds and absent removals before updating Pmitz. A refused add should leave membership unchanged. The exception classes are `io.terpomo.pmitz.core.exception.LimitExceededException` and `io.terpomo.pmitz.core.exception.FeatureNotAllowedException`.

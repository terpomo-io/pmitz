import io.terpomo.pmitz.all.usage.tracker.FeatureUsageTracker;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.core.subscriptions.*;
import io.terpomo.pmitz.limits.LimitVerifierBuilder;
import io.terpomo.pmitz.subscriptions.SubscriptionVerifierBuilder;
import io.terpomo.pmitz.subscriptions.jdbc.JDBCSubscriptionRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class EmbeddedExports {
    static String exportCsv(FeatureUsageTracker tracker, Subscription workspace) {
        tracker.recordFeatureUsage(new FeatureRef("reports", "export"), workspace,
                Map.of("export-credits", 1L));
        return "name,total\nAda,42";
    }

    static InputStream resource(String name) {
        return Objects.requireNonNull(EmbeddedExports.class.getResourceAsStream("/" + name), name);
    }

    public static void main(String[] args) throws Exception {
        var db = new JdbcDataSource();
        db.setURL("jdbc:h2:mem:reports;DB_CLOSE_DELAY=-1");
        try (var connection = db.getConnection()) {
            for (String sql : new String[]{"V1_0__usageAndUserLimit.sql", "V1_1__subscriptions.sql"}) {
                try (var reader = new InputStreamReader(resource(sql), StandardCharsets.UTF_8)) {
                    RunScript.execute(connection, reader);
                }
            }
        }
        var products = new InMemoryProductRepository();
        try (var object = resource("product.json")) {
            // load() expects a JSON array; REST provisioning takes one object.
            var array = "[" + new String(object.readAllBytes(), StandardCharsets.UTF_8) + "]";
            products.load(new ByteArrayInputStream(array.getBytes(StandardCharsets.UTF_8)));
        }
        var subscriptions = new JDBCSubscriptionRepository(db, "dbo", "subscription", "subscription_plan");
        var entitlement = SubscriptionVerifierBuilder.withSubscriptionRepository(subscriptions)
                .withDefaultSubscriptionFeatureManager(products).build();
        var limits = LimitVerifierBuilder.of(products).withDefaultLimitRuleResolver()
                .withJdbcUsageRepository(db, "dbo", "usage").build();
        var tracker = FeatureUsageTracker.Builder.build(limits, entitlement);
        var workspace = new Subscription("demo-workspace");
        workspace.setStatus(SubscriptionStatus.ACTIVE);
        workspace.setPlans(Map.of("reports", "basic"));
        subscriptions.create(workspace);
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                System.out.println(exportCsv(tracker, subscriptions.find(workspace.getId()).orElseThrow()));
            } catch (LimitExceededException ex) {
                System.out.println("Quota exhausted");
            }
        }
    }
}

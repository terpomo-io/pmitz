import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.remote.client.PmitzClient;
import io.terpomo.pmitz.remote.client.http.PmitzApiKeyAuthenticationProvider;
import io.terpomo.pmitz.remote.client.http.PmitzHttpClient;
import java.util.Map;

public class RemoteExports {
    // Supply a workspace ID resolved from the authenticated session in a web app.
    static String exportCsv(PmitzClient client, String workspaceId) {
        var workspace = new Subscription(workspaceId);
        var feature = new FeatureRef("reports", "export");
        var units = Map.of("export-credits", 1L);
        switch (client.verifyLimits(feature, workspace, units).featureStatus()) {
            case NOT_ALLOWED: return "Plan excludes exports";
            case LIMIT_EXCEEDED: return "Quota exhausted";
            case AVAILABLE: break;
        }
        try {
            client.recordOrReduce(feature, workspace, units, false);
        } catch (LimitExceededException ex) {
            return "Quota exhausted";
        }
        return "name,total\nAda,42";
    }

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Supply a workspace subscription ID");
        var client = new PmitzHttpClient(System.getenv("PMITZ_BASE_URL"),
                new PmitzApiKeyAuthenticationProvider());
        System.out.println(exportCsv(client, args[0]));
    }
}

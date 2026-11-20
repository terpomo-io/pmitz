# Add a quota to report exports

Use this example when a developer wants working application code. The shared [product catalog](../assets/exports/product.json) gives each Basic subscription two export credits. Viewer subscriptions have no export feature. This is a count limit; the credits last until usage is reduced. Choose the path that matches the developer’s runtime.

Copy the [exports example directory](../assets/exports) into a development workspace. The commands below run from that copy. Java examples require Java 17+ and Gradle (or the developer’s existing Gradle wrapper). Set `PMITZ_JAVA_VERSION` to the published Maven version the app will use. Maven Central currently publishes the Java libraries at 0.9.0; the Docker server image is 0.10.0.

## Java application with Pmitz embedded

Run `gradle :embedded:run`. The example opens a temporary in-process H2 database, applies the supplied schema, loads the catalog, provisions a workspace on Basic, and calls the export action three times. Expect two CSVs and then “Quota exhausted.” Each invocation starts a fresh demo database.

In the developer's app, use its configured DataSource and migrations. Build the subscription verifier and usage tracker once at startup. Provision the subscription when the workspace is assigned a plan. In the authenticated export handler, obtain the workspace subscription and call the tracker before delivering the CSV. The example explicitly includes each module it imports on the compile classpath. [Embedded code](../assets/exports/embedded/src/main/java/EmbeddedExports.java) · [Gradle dependencies](../assets/exports/build.gradle) · [Embedded wiring](embedded-java.md)

## Provision the REST server for Java or any other backend

Start the server with [Docker Compose](docker-start.md). For this example, use a fresh development catalog and load the base URL and API key into the shell as described there. From the exports example directory, run once:

```bash
api=(curl -fsS -H "X-Api-Key: $PMITZ_API_KEY" -H 'Content-Type: application/json')
"${api[@]}" "$PMITZ_BASE_URL/products" --data-binary @product.json
for plan in basic viewer; do
  "${api[@]}" "$PMITZ_BASE_URL/subscriptions" \
    -d "{\"subscriptionId\":\"${plan}-workspace\",\"status\":\"ACTIVE\",\"plans\":{\"reports\":\"${plan}\"}}"
done
```

Use the same server for the application calls below. The command-line argument stands in for the workspace ID the real backend derives from an authenticated session.

## Java application calling the server

Use the `pmitz-core` and `pmitz-remoteclient` artifacts at the selected Maven version. Build a reusable PmitzHttpClient with the server URL and API-key provider. The example passes a subscription to verifyLimits, reads the decision, then calls recordOrReduce once before returning the CSV. It handles a refused write separately from an unavailable server. Run:

```bash
gradle -q :remote:run --args='basic-workspace'
```

It prints a CSV on success, “Quota exhausted” at capacity, or “Plan excludes exports” for the Viewer workspace. [Java client code](../assets/exports/remote/src/main/java/RemoteExports.java)

## Non-Java application calling the server

Use the backend’s HTTP client. The [Node.js example](../assets/exports/exports.mjs) uses built-in fetch and runs with Node 22. Its exportCsv function takes a trusted workspace ID, checks the feature and quota, records one unit and returns the application response. The same API contract applies to Python, Go and other HTTP clients.

```bash
node exports.mjs basic-workspace
```

The example maps a plan denial to application HTTP 403 and a quota refusal to 429. Unexpected service failures raise an error so the handler can withhold the export. Keep the Pmitz API key on the backend. [HTTP routes and payloads](rest.md)

## Check the result through the application

Starting with a fresh Basic subscription, run the Java remote command once and the Node command once. Both should produce CSV. Run either again: it should refuse. For independent tests of each backend, provision a different subscription for each. Run either command with `viewer-workspace` to see the plan denial. Read the count from the shared server:

```bash
"${api[@]}" "$PMITZ_BASE_URL/subscriptions/basic-workspace/usage/reports/export"
```

Expect zero remaining export credits. These examples exercise sequential requests. Decide how the real handler will handle concurrent requests, a failed business action after usage is recorded, and retries before enabling the rule for real customers.

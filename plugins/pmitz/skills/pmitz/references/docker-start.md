# Start Pmitz locally with Docker

Use Docker Compose v2. Copy the skill's [compose.yaml](../assets/compose.yaml) into a new directory for the developer's local Pmitz instance. Run the commands below in that directory. The template runs the official 0.10.0 server image with PostgreSQL, persists database rows in a named volume, and binds the HTTP port to the local machine.

Create the local `.env` once. Keep the file private and reuse it when restarting the instance.

```bash
(umask 077; set -C; cat > .env <<ENV
PMITZ_API_KEY=$(openssl rand -hex 32)
POSTGRES_PASSWORD=$(openssl rand -hex 32)
ENV
)
docker compose up -d --wait --wait-timeout 120
```

Load the same settings for calls from this shell. Configure the application backend with the resulting base URL and API key. To use a different local port, add `PMITZ_PORT=8081` to `.env` before starting Compose. To run another published server version, add `PMITZ_IMAGE_TAG` with that tag.

```bash
set -a
. ./.env
set +a
export PMITZ_BASE_URL="http://127.0.0.1:${PMITZ_PORT:-8080}"
curl -fsS "$PMITZ_BASE_URL/actuator/health"
```

## Verify the first authenticated action

On a fresh local instance, register a demo product with one seat, assign a workspace to its plan, then request two seats one at a time. These commands use Bash and curl.

```bash
api=(curl -fsS -H "X-Api-Key: $PMITZ_API_KEY" -H 'Content-Type: application/json')
"${api[@]}" "$PMITZ_BASE_URL/products" -d '{
  "productId":"demo","features":[{"featureId":"seat","limits":[
    {"type":"CountLimit","id":"seats","count":1}
  ]}],"plans":[{"planId":"starter","includedFeatures":["seat"]}]
}'
"${api[@]}" "$PMITZ_BASE_URL/subscriptions" -d '{
  "subscriptionId":"demo-workspace","status":"ACTIVE","plans":{"demo":"starter"}
}'
usage="$PMITZ_BASE_URL/subscriptions/demo-workspace/usage/demo/seat"
for request in 1 2; do
  curl -sS -o /dev/null -w "Request $request: HTTP %{http_code}\n" \
    -H "X-Api-Key: $PMITZ_API_KEY" -H 'Content-Type: application/json' \
    "$usage" -d '{"units":{"seats":1},"reduceUnits":false}'
done
"${api[@]}" "$usage"
```

Expect HTTP 200, then HTTP 422, then zero remaining seats. This checks the server, credential, catalog, subscription and usage database. To verify the developer's integration, use a fresh workspace and repeat the boundary through the application's own action. See [REST integration](rest.md) for the request flow.

To stop the instance, run `docker compose down`. The named volume retains subscriptions and usage. Upload the saved product catalog when starting the server again; product definitions live in memory. See [catalog lifecycle](catalog-lifecycle.md).

# Pmitz hosted MCP

The service behind `https://pmitz-mcp.web.app`. It gives an AI assistant a Pmitz sandbox, a temporary Pmitz server with no account, so someone can develop and test plans, feature access and usage limits before installing anything.

## What it does

- **`POST /mcp`** is an MCP server over Streamable HTTP, with no sign-in. `pmitz_create_sandbox` creates a sandbox and returns its key. The other Pmitz tools are the ones of the local MCP server in [`plugins/pmitz/server/pmitz-mcp.mjs`](../plugins/pmitz/server/pmitz-mcp.mjs), with the sandbox key added, plus `pmitz_sandbox_status` and `pmitz_export_plans`.
- **`/sandbox/<id>/...`** is the Pmitz REST API of one sandbox, for the developer's application. The sandbox key goes in the `X-Api-Key` header.
- A sandbox is deleted 72 hours after it is created. It is for development and testing, not for production.

## How it works

One real Pmitz server runs next to the gateway. Each sandbox is a private slice of it: the gateway prefixes the product, subscription and subject IDs of a sandbox with its ID, and removes the prefix from every answer, so sandboxes cannot see each other.

Pmitz keeps its catalog in memory, so the gateway stores every product definition of a sandbox and sends it again after the Pmitz process restarts. That stored copy is also what `pmitz_export_plans` returns.

There are no accounts yet. Product definitions, subscriptions and usage are kept in Pmitz's own form, so a sandbox can be moved by replaying them through the Pmitz API.

Limits, in `server.mjs`: 10 products and 500 subscriptions per sandbox, 120 requests a minute per sandbox, 64 KB per request, and a cap on new sandboxes per hour.

## Run and test it locally

```bash
cd hosted-mcp
cp .env.example .env          # fill in two random values
docker compose up -d --wait   # Pmitz and PostgreSQL
npm install
set -a; . ./.env; set +a
export DATABASE_URL="postgres://pmitz:${POSTGRES_PASSWORD}@127.0.0.1:15432/pmitz"
PORT=18090 PUBLIC_URL=http://127.0.0.1:18090 PMITZ_INTERNAL_URL=http://127.0.0.1:18080 \
  OPENAI_APPS_CHALLENGE=test-token SWEEP_SECONDS=1 node server.mjs &
GATEWAY_URL=http://127.0.0.1:18090 RESTART_PMITZ="docker restart pmitz-hosted-pmitz-1 && sleep 25" node test.mjs
```

The test covers the MCP surface, the REST surface, isolation between sandboxes, a restart of Pmitz, the limits against abuse, expiry and clean-up. Against the deployed service, `LIVE=1 GATEWAY_URL=https://pmitz-mcp.web.app node test.mjs` runs the parts that need no database access.

## Deployment

It runs in the Google Cloud project `terpomo-pmitz-mcp` of the terpomosoft.com organization:

- Cloud Run service `pmitz-mcp` in `us-central1`, defined in [`service.yaml`](service.yaml): the gateway, the Pmitz server image and the Cloud SQL proxy in one instance, scaled between zero and one.
- Cloud SQL for PostgreSQL instance `pmitz-instant`, used by both Pmitz and the gateway.
- Secrets `pmitz-internal-key`, `pmitz-db-password` and `pmitz-database-url` in Secret Manager.
- Firebase Hosting site `pmitz-mcp`, which forwards every request to the Cloud Run service and provides the `pmitz-mcp.web.app` address.

To release a change, build the image from the repository root and replace the service:

```bash
gcloud builds submit --config hosted-mcp/cloudbuild.yaml .
gcloud run services replace hosted-mcp/service.yaml --region us-central1
```

`OPENAI_APPS_CHALLENGE` holds the token OpenAI asks for when it verifies the domain; the service answers it at `/.well-known/openai-apps-challenge`.

One copy of the service is kept running (`autoscaling.knative.dev/minScale: "1"` in `service.yaml`), because a start from zero takes about 20 seconds. A new revision still takes that long to start.

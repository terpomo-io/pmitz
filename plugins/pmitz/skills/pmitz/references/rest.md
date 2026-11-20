# Integrating a backend over HTTP

Use the Pmitz server URL from server-side configuration. Send `X-Api-Key: <PMITZ_API_KEY>` on API calls and `Content-Type: application/json` on JSON writes. Health is available at `GET /actuator/health`. URL-encode each dynamic path segment. Resolve the Pmitz subscription ID from the application's authenticated account or workspace.

## Provisioning

| Method and path | Request | Success and conflict |
|---|---|---|
| POST /products | One product object; see [catalog](catalog.md) | 200; 409 if ID exists |
| DELETE /products/{productId} | Empty | 200; 404 if missing |
| POST /subscriptions | See example below | 200; 409 if ID exists |
| GET /subscriptions/{subscriptionId} | Empty | 200 with subscription; 404 if missing |
| PATCH /subscriptions/{subscriptionId}/status | `{"status":"SUSPENDED"}` | 200; 404 if missing |

The subscription creation body uses `plans` to map product IDs to plan IDs. Supply an explicit status; optional `expirationDate` is an ISO date-time with an offset. A sample development session (using Bash):

```bash
: "${PMITZ_BASE_URL:?}" "${PMITZ_API_KEY:?}"
api=(curl --silent --show-error --fail-with-body -H "X-Api-Key: $PMITZ_API_KEY" -H "Content-Type: application/json")
"${api[@]}" -X POST "$PMITZ_BASE_URL/products" --data-binary @product.json
"${api[@]}" -X POST "$PMITZ_BASE_URL/subscriptions" \
  -d '{"subscriptionId":"workspace-1","status":"ACTIVE","plans":{"reports":"free"}}'
```

Provision when the product or subscription is set up. A 409 establishes only that the ID exists. For a product there is no GET, list or update route. See [catalog lifecycle](catalog-lifecycle.md).

## During an application action

Below, `S` represents `/{groupType}/{groupId}` and `F` represents `/{productId}/{featureId}`. Use `subscriptions` for plan-based access; `users` and `directory-groups` support usage tracking by those subjects.

| Method and route | Body | Result |
|---|---|---|
| GET S/subscription-check/F | Empty | JSON with `featureAllowed` and an `errorCause` on denial |
| POST S/limits-check/F | Map of proposed units, e.g. `{"monthly-exports":1}` | JSON with `featureStatus` and `remainingUsageUnits` **after the proposed action**. A preview consumes nothing. |
| GET S/usage/F | Empty | Current status and remaining units. In 0.10.0 this path passes the original subject to the limit verifier; an ID-only subscription can show the feature default instead of its plan override. |
| POST S/usage/F | `{"units":{"monthly-exports":1},"reduceUnits":false}` | 200 on record; 422 for a quota refusal; 500 in 0.10.0 if the subscription lacks the feature |
| POST S/usage/F | Same body with `"reduceUnits":true` | 200 on reduction; changes the applicable counter |

For a subscription action, call `limits-check` first and branch on the JSON response. For example, in Bash with jq and the curl options defined above:

```bash
s="$PMITZ_BASE_URL/subscriptions/workspace-1"
preview=$("${api[@]}" "$s/limits-check/reports/export" -d '{"monthly-exports":1}') || exit 1
case "$(jq -er '.featureStatus' <<<"$preview")" in
  AVAILABLE)
    "${api[@]}" "$s/usage/reports/export" -d '{"units":{"monthly-exports":1},"reduceUnits":false}' || exit 1
    # Deliver the export only after the write succeeds.
    ;;
  NOT_ALLOWED) echo 'This subscription cannot export.' >&2; exit 1 ;;
  LIMIT_EXCEEDED) echo 'Export allowance exhausted.' >&2; exit 1 ;;
  *) echo 'Unexpected Pmitz response.' >&2; exit 1 ;;
esac
```

If the plan excludes the feature, the preview returns HTTP 200 with `NOT_ALLOWED`. A direct usage write for that same subscription returns **HTTP 500** in 0.10.0. Handle the denial at preview time. A 500 from a later write can have other causes, so withhold the protected result and handle it as a failed write. Send a positive integer for each applicable limit ID. The API has no idempotency key; an automatic retry after an ambiguous write can consume a second unit.

**A JDBC limit is not a strict cap under simultaneous requests.** With four of five units used, two simultaneous requests for the last unit can both return HTTP 200. Six units are then accepted while the usage endpoint reports zero remaining out of five: one increment is lost. The verifier reads usage, computes the new count, then writes it back. This was reproduced on H2 with 0.10.0; PostgreSQL uses the same JDBC code. A preview reserves no capacity. An application that requires a hard cap must serialize competing actions for the same quota across its writers, and check that the accepted actions agree with stored usage.

For a plan override, verify through the application's write path. As a second observation, preview **one more unit**: after the final allowed export, the preview should say LIMIT_EXCEEDED and project −1. That −1 is hypothetical. A negative value from a current-usage read after lowering a limit describes actual existing usage. For access-only features (no limits), check entitlement before doing the action; usage writes require a nonempty units map.

[Controller sources for 0.10.0](https://github.com/terpomo-io/pmitz/tree/v0.10.0/spring-boot-starter-remoteserver/src/main/java/io/terpomo/pmitz/remote/server/controller)

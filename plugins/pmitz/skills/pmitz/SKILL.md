---
name: pmitz
description: Implements subscription plans, feature access and usage limits in an application with Pmitz. Use when adding or changing plan entitlements, quotas, seat limits or usage tracking, in embedded Java or over the Pmitz REST API, or when someone needs a Pmitz server to develop against without installing one.
---

# Pmitz

Choose the integration from the developer’s deployment:

| Application | Start here |
|---|---|
| Java, Pmitz running in the same process | Use [embedded Java](references/embedded-java.md) for dependencies, JDBC setup, subscriptions and enforcement. |
| Java, Pmitz running as a separate service | Start the [Docker server](references/docker-start.md) or create a sandbox, then integrate the [Java remote client](references/exports-integration.md#java-application-calling-the-server). |
| Node.js, Python, Go or another backend | Start the [Docker server](references/docker-start.md) or create a sandbox, then use the [HTTP contract](references/rest.md). A [runnable Node.js integration](references/exports-integration.md#non-java-application-calling-the-server) shows the application action. |

Apart from the sandbox, these paths run local tools: Docker with Compose v2 for the server, Java 17+ with Gradle for the Java examples, Node.js 22 for the Node example, and curl. Check that the ones the chosen path needs are installed before starting, and tell the developer what is missing. Where no local shell is available, work in a sandbox.

The [runnable export walkthrough](references/exports-integration.md) implements the same rule across all three paths. Use a developer’s existing Pmitz server when available. Refer to the [catalog format](references/catalog.md) when modeling rules and [catalog lifecycle](references/catalog-lifecycle.md) when changing them.

Start with the product rule the developer wants to enforce. Identify the action, who may perform it and whose usage counts. For “Free workspaces can export ten reports a month,” the action is exporting one report, the workspace subscription owns the counter, and the period is a calendar month. Ten people using one workspace share ten exports. If “ten per month” could mean per person, per workspace, or per billing cycle, settle that distinction before generating the rule.

## Which Pmitz server to work on

A Pmitz server holds the plans, the subscriptions and the usage. There are two ways to have one, and the tools differ.

**A sandbox, in Terpomo's cloud.** The `pmitz_create_sandbox` tool creates a real Pmitz server in a few seconds, with nothing to install. Use it when someone asks for a sandbox, or needs a server to develop against and has no Docker or no local shell. A sandbox is temporary: it is deleted 72 hours after it is created, it is not for production, and personal or confidential information does not belong in it. Create one sandbox per conversation, show the developer its key, and pass that key as `sandbox` to the other tools. When you hand it over, tell the developer when it expires and that it is not for production. The sandbox also has a REST address that follows the same [HTTP contract](references/rest.md): an application calls it with the sandbox key in the `X-Api-Key` header. Before the sandbox expires, call `pmitz_export_plans` and save the product definitions in the application's repository; they load unchanged into any other Pmitz server.

**The developer's own server.** Use it when the developer already runs Pmitz, or is building for production. Start one with the [Docker server](references/docker-start.md) instructions. When the local Pmitz MCP tools are available (`pmitz_connection_status` and the other `pmitz_` tools that take no `sandbox`), use them instead of hand-written HTTP calls: check the connection first, then read entitlements, limits and subscriptions. The tools that change the server act on real data, so confirm with the developer before creating or removing anything, and prefer a development server. Without them, use the REST calls in the references.

On either server, verify an integration by triggering the action through the application, and use the tools to read the result.

## Implement a rule

1. Identify where the requested action will be enforced. When its handler exists, trace the call to the code that performs it. Find how the app identifies the customer and stores their plan. Inspect the build files and service configuration to see whether Pmitz is already wired in. For a first integration, add the client or dependency, configure its connection or repositories, and connect the app’s customer and plan identifiers to the Pmitz subscription. Preserve existing product, plan, feature and limit IDs so current subscriptions and usage records continue to match.
2. Decide **who has the feature** and **how much they can use** separately. Put the feature in each entitled plan. Define its default limit on the feature and reuse the same limit ID in a plan override. Use a count for occupied resources such as seats and a calendar-period limit for consumption such as exports per month. For a renewal-date reset or rolling window, establish how that period will be implemented before promising it.
3. Choose the integration that fits the developer’s runtime and deployment. Java can embed Pmitz. Any application's backend can call the Pmitz server over HTTP. Connect the backend to the server URL and send the configured API key in the `X-Api-Key` header. If the developer already runs Pmitz, use that server. Identify the subscription from the authenticated application context. When the rule depends on a customer's plan, pass a subscription. The default subscription verifier approves ordinary user and directory-group subjects without looking up a plan.
4. Connect enforcement to the action that consumes the units. A preview reports whether the requested units fit. Recording updates usage and may reject the request. Supply units keyed by limit ID. Explain where a failed action or retried request could change the count, and use the application's existing mechanism for handling it. A seat removal frees capacity. Downloading an export again only consumes another unit if the product rules say it does.
5. Apply the catalog and code changes in the application. Build it and exercise its normal entry point with the user’s rule. For ten exports, start with a fresh subscription, run the first ten exports through the app and confirm the eleventh is refused, and check the stored count. Pmitz’s JDBC usage counter does not guarantee a strict cap under concurrent requests. If the product needs that guarantee, serialize competing actions for the same quota and verify both application outcomes and the stored count. Report the files changed and what actually ran. If working from snippets alone, give the patch and identify what still needs verification in the app. Use the installed release's public API. Use the [count-limit integration](references/exports-integration.md) or [calendar-month example](references/monthly-exports.md) according to the rule.

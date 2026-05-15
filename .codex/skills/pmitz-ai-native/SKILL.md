---
name: pmitz-ai-native
description: Work on Pmitz AI-native features, MCP server tools, subscription entitlements, usage limits, local/remote backends, and first-user validation in this repository.
---

# Pmitz AI-Native

Use this skill when changing Pmitz MCP tooling, agent-facing docs, product JSON flows,
subscription verification, usage limits, or local/remote mode behavior.

## Repository Map

- `core`: domain models, product repository interfaces, product JSON loading.
- `limits`: limit verification and JDBC usage tracking.
- `subscriptions`: subscription repository and entitlement verification.
- `remoteclient`: HTTP client for the Pmitz remote server.
- `remoteserver`: standalone Spring Boot remote server.
- `spring-boot-starter-remoteserver`: embeddable remote server starter.
- `mcpserver`: MCP STDIO server exposing Pmitz operations as AI tools.
- `examples`: first-user samples and product JSON.

## MCP Server Rules

- Treat MCP as a developer/operator AI automation interface, not as the primary
  runtime integration path for applications.
- For application runtime guidance, direct developers to the Pmitz Java APIs,
  `remoteclient`, or the remote HTTP API.
- Keep MCP tool names stable once documented.
- Route tool behavior through `PmitzBackend`; do not duplicate remote/local logic inside tool handlers.
- Remote mode should delegate to `remoteclient`.
- Local mode should compose existing `core`, `limits`, and `subscriptions` APIs.
- Tool responses should include structured content and concise JSON text content.
- Return tool-level errors with `isError=true`; reserve process failures for invalid startup configuration.

## Validation

Run the narrowest useful check first:

```bash
./gradlew :mcpserver:compileJava
./gradlew :mcpserver:run --args='--list-tools'
```

For broader changes, run:

```bash
./gradlew :mcpserver:test
./gradlew build
```

Keep user-facing setup and positioning guidance in `docs/ai-native-mcp.md`.

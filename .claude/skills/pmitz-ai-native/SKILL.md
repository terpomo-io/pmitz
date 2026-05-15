---
name: pmitz-ai-native
description: Use when developing or validating Pmitz MCP tools, AI-native workflows, subscription entitlement checks, usage limit checks, local and remote Pmitz backends, or first-user setup docs.
---

# Pmitz AI-Native

Pmitz is a Java 17+ Gradle multi-module library for subscription management,
feature entitlement verification, and usage limit tracking.

## Working Context

- `core` contains product, feature, plan, subject, and subscription domain models.
- `limits` contains local usage limit verification.
- `subscriptions` contains subscription entitlement verification and JDBC persistence.
- `remoteclient` calls the remote Pmitz REST server.
- `remoteserver` packages the Spring Boot remote server.
- `mcpserver` exposes Pmitz operations to MCP clients over STDIO.

## MCP Development Rules

- Treat MCP as a developer/operator AI automation interface, not as the primary
  runtime integration path for applications.
- For application runtime guidance, direct developers to the Pmitz Java APIs,
  `remoteclient`, or the remote HTTP API.
- Keep the MCP tool contract stable and documented.
- Add backend-specific behavior behind `PmitzBackend`.
- Prefer existing Pmitz APIs over new parallel implementations.
- Remote mode should call `remoteclient`.
- Local mode should compose product repository, limit verifier, and subscription verifier.
- Tool failures should be returned as MCP tool errors with structured error content.

## Useful Commands

```bash
./gradlew :mcpserver:compileJava
./gradlew :mcpserver:test
./gradlew :mcpserver:run --args='--list-tools'
./gradlew build
```

Keep user-facing setup and positioning guidance in `docs/ai-native-mcp.md`.

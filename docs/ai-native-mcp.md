# Pmitz AI-Native MCP Server

The `mcpserver` module exposes Pmitz subscription and usage-limit operations as
Model Context Protocol tools for AI clients.

This is not the normal runtime integration path for an application. Production
applications should call Pmitz through the Java library, the remote client, or
the remote HTTP API. The MCP server is for developer, testing, support, and
operations workflows where an AI assistant needs to inspect, configure, or
exercise Pmitz.

Typical runtime path:

```text
your application -> Pmitz Java API or Pmitz remote server
```

Typical MCP path:

```text
developer/operator -> AI assistant -> Pmitz MCP server -> Pmitz
```

## Run

List available tools:

```bash
./gradlew :mcpserver:run --args='--list-tools'
```

Start the STDIO MCP server:

```bash
./gradlew :mcpserver:run
```

## Recommended Sample Environment

For a realistic first run, use the sibling `pmitz-samples` repository. It starts
the Pmitz remote server, a Java BFF, and a React public-library UI that all use
the same product definition:

```bash
cd ../pmitz-samples
./start-sample-public-library start
```

The sample stores its generated API key in `.local/public-library.env` and runs
the Pmitz server on port `8090` by default. Point the MCP server at that same
sample server:

```bash
cd ../pmitz
export PMITZ_MCP_MODE=remote
export PMITZ_REMOTE_URL=http://localhost:8090
export PMITZ_API_KEY="$(grep PMITZ_API_KEY ../pmitz-samples/.local/public-library.env | cut -d= -f2)"
./gradlew :mcpserver:run
```

The sample product uses:

- `productId`: `public-library`
- `featureId`: `reserve`
- `limitId`: `maxborrowed`

## Modes

Remote mode delegates to a running Pmitz remote server:

```bash
export PMITZ_MCP_MODE=remote
export PMITZ_REMOTE_URL=http://localhost:8080
export PMITZ_API_KEY=your-api-key
./gradlew :mcpserver:run
```

Local mode composes the in-process product repository, limit verifier, and
subscription verifier with JDBC storage:

```bash
export PMITZ_MCP_MODE=local
export PMITZ_PRODUCT_FILE=examples/src/main/resources/product-library.json
export PMITZ_JDBC_URL=jdbc:h2:mem:pmitz
export PMITZ_JDBC_USERNAME=sa
export PMITZ_JDBC_PASSWORD=
export PMITZ_DB_SCHEMA=PUBLIC
./gradlew :mcpserver:run
```

Local mode expects the configured usage and subscription tables to exist. Use
the repository SQL scripts as the source for schema setup.

`PMITZ_PRODUCT_FILE` may point to either a single product JSON object or a JSON
array of products. The local backend normalizes single-product files before
loading them into the in-memory product repository.

## Tools

- `pmitz_upload_product`
- `pmitz_remove_product`
- `pmitz_get_remaining_limits`
- `pmitz_check_limits`
- `pmitz_record_usage`
- `pmitz_reduce_usage`
- `pmitz_verify_entitlement`
- `pmitz_create_subscription`
- `pmitz_find_subscription`
- `pmitz_update_subscription_status`

The repository includes a local E2E test that executes all of these tool
handlers against a fresh H2 schema:

```bash
./gradlew :mcpserver:test --tests io.terpomo.pmitz.mcpserver.tools.PmitzMcpLocalE2ETests
```

## Subject Types

Tool calls that target a user grouping use:

- `user`
- `directory-group`
- `subscription`

## Example Client Configuration

For MCP clients that launch command-based STDIO servers, use:

```json
{
  "mcpServers": {
    "pmitz": {
      "command": "./gradlew",
      "args": [":mcpserver:run"],
      "env": {
        "PMITZ_MCP_MODE": "remote",
        "PMITZ_REMOTE_URL": "http://localhost:8080",
        "PMITZ_API_KEY": "your-api-key"
      }
    }
  }
}
```

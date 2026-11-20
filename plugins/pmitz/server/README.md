# Pmitz MCP server (local)

A local MCP server that lets a coding assistant read and change your own [Pmitz](https://github.com/terpomo-io/pmitz) server through its REST API. It needs Node.js 18 or newer and has no dependencies.

```bash
codex mcp add pmitz-local -- npx -y @terpomosoft/pmitz-mcp@0.1.0
claude mcp add pmitz-local -- npx -y @terpomosoft/pmitz-mcp@0.1.0
```

Set these in the environment the assistant starts from:

| Variable | Meaning |
|---|---|
| `PMITZ_BASE_URL` | Base URL of your Pmitz server, for example `http://127.0.0.1:8080` |
| `PMITZ_API_KEY` | API key of that server. It is sent only to that URL, in the `X-Api-Key` header. |
| `PMITZ_MCP_READ_ONLY` | Optional. Set to `true` to offer only the tools that read. |

The server offers eleven tools: it checks entitlements and limits, reads subscriptions, creates and removes products and subscriptions, and records or reduces usage. A Pmitz API key has full authority over its server, so connect a development server, or use read-only mode.

The tools and settings are described in the [Pmitz plugin README](https://github.com/terpomo-io/pmitz/tree/main/plugins/pmitz#your-own-server-the-local-mcp-server). The plugin bundles this server for Claude Code and Codex, together with a skill and a hosted MCP server that creates a Pmitz sandbox for trying the product.

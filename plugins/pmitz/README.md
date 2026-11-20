# Pmitz

Use this plugin in Claude Code or Codex to add [Pmitz](https://terpomosoft.com/pmitz/) to an application: subscription plans that decide which features a customer can use and how much of each. It covers embedded Java and the Pmitz REST API.

It includes the Pmitz skill, a Docker Compose template, runnable application examples and two MCP servers: `pmitz-sandbox`, which creates a Pmitz sandbox in Terpomo's cloud, and `pmitz-local`, which works on your own Pmitz server.

## Install from a local marketplace

Extract the Pmitz marketplace archive to a directory you can keep on disk. Run these commands from the application where you want to use the plugin, substituting the absolute path to the extracted directory containing `.claude-plugin`, `.agents` and `plugins`.

**Claude Code**

```bash
claude plugin marketplace add /absolute/path/to/pmitz-marketplace
claude plugin install pmitz@terpomo-pmitz
claude plugin details pmitz@terpomo-pmitz
```

**Codex**

```bash
codex --enable plugins plugin marketplace add /absolute/path/to/pmitz-marketplace
codex --enable plugins plugin add pmitz@terpomo-pmitz
codex --enable plugins plugin list --marketplace terpomo-pmitz
```

Start a new session after installing. If plugins are disabled in your Codex profile, launch that session with `codex --enable plugins`.

Try “Create a Pmitz sandbox with a Free plan of 3 exports and a Pro plan of 100, and show me what a Free customer can do.” Or, in an application: “Use Pmitz to give each workspace five seats. Integrate that rule into my app and verify that removing a member frees a seat.” For a server of your own, the skill includes [Docker startup instructions](skills/pmitz/references/docker-start.md).

## A sandbox: the hosted MCP

The `pmitz-sandbox` MCP server runs at `https://pmitz-mcp.web.app/mcp`. Its `pmitz_create_sandbox` tool creates a Pmitz sandbox, a real Pmitz server, in a few seconds, with no account and nothing to install, and returns a sandbox key. The other tools are the ones of the local MCP server, with that key added, plus `pmitz_sandbox_status` and `pmitz_export_plans`.

- **It is temporary.** A sandbox is deleted 72 hours after it is created. It is for development and testing, not for production.
- **Your application can call it.** Each sandbox has a REST address, `https://pmitz-mcp.web.app/sandbox/<id>`, that follows the Pmitz REST API. Send the sandbox key in the `X-Api-Key` header.
- **You keep your plans.** `pmitz_export_plans` returns the product definitions exactly as they were created, ready to load into your own Pmitz server. Subscriptions and recorded usage are not exported.
- **What you put in a sandbox is stored by Terpomo Software** until the sandbox expires. Do not put personal or confidential information in it.

For production, run Pmitz on your own server from the Docker image.

## Your own server: the local MCP server

The plugin includes a small MCP server, `pmitz-local` (`server/pmitz-mcp.mjs`), that the assistant starts on your machine. It lets the assistant look at and change one Pmitz server that you choose. It needs Node.js 18 or newer and has no other dependencies. The skill works without it. The same MCP server is published on npm as `@terpomosoft/pmitz-mcp`.

| Tool | What it does |
|---|---|
| `pmitz_connection_status` | Shows the configured server, whether it answers, and whether writes are enabled |
| `pmitz_verify_entitlement` | Checks whether a subscription, user or group may use a feature |
| `pmitz_check_limits` | Previews whether requested units fit, without recording anything |
| `pmitz_get_remaining_limits` | Reads the remaining units of a feature's limits |
| `pmitz_find_subscription` | Reads a subscription's status and plans |
| `pmitz_create_product`, `pmitz_remove_product` | Creates or removes a product definition |
| `pmitz_create_subscription`, `pmitz_update_subscription_status` | Creates a subscription or changes its status |
| `pmitz_record_usage`, `pmitz_reduce_usage` | Records or subtracts usage |

The first five tools only read. The others change the server. Claude Code and Codex ask you before running a tool that changes something.

Three settings configure it:

| Setting | Claude Code option | Codex environment variable |
|---|---|---|
| Pmitz server URL | Pmitz server URL | `PMITZ_BASE_URL` |
| API key of that server | Pmitz API key | `PMITZ_API_KEY` |
| Read-only mode (optional) | Read-only mode | `PMITZ_MCP_READ_ONLY=true` |

In Claude Code, set them when you enable the plugin or later with `/plugin configure pmitz`. The API key is stored in your system's credential store. In Codex, export the variables in the shell that starts Codex.

Turn on read-only mode to offer only the five tools that read. A Pmitz API key has full authority over its server, so connect the assistant to a development server, or use read-only mode. The assistant cannot change these settings, the destination or the key through a tool call.

## What the plugin needs, runs and sends

The plugin contains one skill made of instructions, reference documents, example code and a Docker Compose file, and the two MCP servers described above. It has no hooks. It sends nothing to Terpomo Software unless the assistant uses a sandbox: then what it puts in the sandbox goes to Terpomo's hosted service.

When you ask the assistant to integrate Pmitz, the skill may have it do the following on your machine, with the tools you already have installed:

- Edit your application's source code and configuration to add the Pmitz checks.
- Run Docker Compose to start a local Pmitz server and PostgreSQL. Docker downloads the `terpomo/pmitz-remoteserver` and `postgres` images from Docker Hub.
- Run Gradle for the Java examples, which downloads the Pmitz libraries from Maven Central, or Node.js 22 for the Node example.
- Call the Pmitz server over HTTP with `curl` or your application's HTTP client. These calls send the server's API key in the `X-Api-Key` header to the Pmitz server address you configured, which is `127.0.0.1` in the bundled setup. The key is generated locally into a `.env` file you keep; it is read from the `PMITZ_API_KEY` environment variable and is not sent anywhere else.

The local MCP server runs as a Node.js process on your machine while the plugin is enabled. It makes HTTP requests only to the Pmitz server URL you configured, and sends the API key you configured only to that URL, in the `X-Api-Key` header. It does not log the key or return it in tool results.

Requirements depend on the path you choose: nothing for a sandbox, Docker with Compose v2 for a local server, Java 17 or newer with Gradle for the Java examples, Node.js 22 for the Node example, and Node.js 18 or newer for the local MCP server.

## Support and privacy

Questions and problems: [terpomosoft.com/contact](https://terpomosoft.com/contact/) or the [Pmitz issue tracker](https://github.com/terpomo-io/pmitz/issues). Privacy policy: [terpomosoft.com/privacy](https://terpomosoft.com/privacy/). Terms of service: [terpomosoft.com/terms](https://terpomosoft.com/terms/).

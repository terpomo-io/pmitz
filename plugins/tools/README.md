# Plugin tools

Used to build and test the Pmitz plugin. They are not part of the plugin itself.

- `build-packages.py` builds the plugin archives into `build/ai-plugins/`, including the one uploaded to OpenAI's directory, which leaves out the local MCP server. Run `python3 plugins/tools/build-packages.py`.
- `local-mcp-test.mjs` tests the local MCP server end to end against a running Pmitz server. Start a disposable one, then run `PMITZ_BASE_URL=http://127.0.0.1:8080 PMITZ_API_KEY=... node plugins/tools/local-mcp-test.mjs`.

The hosted sandbox service has its own test in `hosted-mcp/test.mjs`.

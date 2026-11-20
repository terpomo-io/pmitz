#!/usr/bin/env python3
"""Builds the three Pmitz plugin archives into build/ai-plugins/ at the repository root.

  pmitz-plugin-<version>.zip       the plugin folder as it is in the repository
  pmitz-marketplace-<version>.zip  the plugin plus both marketplace files, for a local install
  pmitz-openai-<version>.zip       upload for OpenAI's directory: the skill and the hosted MCP. OpenAI's
                                   directory refuses a command (stdio) MCP server, so the local MCP server
                                   and the text describing it are left out.

Run from anywhere: python3 plugins/tools/build-packages.py [--demo-url URL]

--demo-url (or PMITZ_DEMO_RECORDING_URL) is the video walkthrough OpenAI asks for at review. It is added to
the OpenAI archive only and is not kept in the repository. Without it the archive carries no link, and OpenAI
keeps the one saved with the plugin.
"""
import argparse
import json
import os
import pathlib
import re
import zipfile

parser = argparse.ArgumentParser(description='Build the Pmitz plugin archives.')
parser.add_argument('--demo-url', default=os.environ.get('PMITZ_DEMO_RECORDING_URL'))
args = parser.parse_args()

repo = pathlib.Path(__file__).resolve().parents[2]
plugin = repo / 'plugins' / 'pmitz'
out = repo / 'build' / 'ai-plugins'
out.mkdir(parents=True, exist_ok=True)

version = json.loads((plugin / '.claude-plugin' / 'plugin.json').read_text())['version']
files = sorted(p for p in plugin.rglob('*') if p.is_file())


def rel(path):
    return path.relative_to(plugin).as_posix()


def build(name, entries):
    target = out / name
    target.unlink(missing_ok=True)
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as archive:
        for arcname, data in entries:
            archive.writestr(arcname, data)
    print(f'{name}: {len(entries)} files')


def replace_once(text, old, new):
    assert text.count(old) == 1, f'expected exactly one occurrence of: {old[:60]}'
    return text.replace(old, new)


full = [(rel(p), p.read_bytes()) for p in files]
build(f'pmitz-plugin-{version}.zip', full)

marketplace = [
    ('pmitz-marketplace/.agents/plugins/marketplace.json', (repo / '.agents/plugins/marketplace.json').read_bytes()),
    ('pmitz-marketplace/.claude-plugin/marketplace.json', (repo / '.claude-plugin/marketplace.json').read_bytes()),
]
build(f'pmitz-marketplace-{version}.zip', marketplace + [('pmitz-marketplace/plugins/pmitz/' + a, d) for a, d in full])

manifest = json.loads((plugin / '.codex-plugin' / 'plugin.json').read_text())
# The hosted MCP works without local execution, so the directory package is not desktop-only.
manifest.pop('requires_local_executor', None)
if args.demo_url:
    manifest['extensions']['com.openai']['review']['demo_recording_url'] = args.demo_url

# Only the hosted MCP: OpenAI's directory refuses a command (stdio) MCP server.
mcp = json.loads((plugin / '.mcp.json').read_text())
mcp = {'mcpServers': {'pmitz-sandbox': {'url': mcp['mcpServers']['pmitz-sandbox']['url']}}}

readme = (plugin / 'README.md').read_text()
readme = replace_once(readme, 'and two MCP servers: `pmitz-sandbox`, which creates a Pmitz sandbox in Terpomo\'s cloud, and `pmitz-local`, which works on your own Pmitz server.',
                      'and the `pmitz-sandbox` MCP server, which creates a Pmitz sandbox in Terpomo\'s cloud.')
readme, count = re.subn(r'## Your own server: the local MCP server\n.*?(?=## What the plugin needs)', '', readme, flags=re.S)
assert count == 1
readme = replace_once(readme, 'The other tools are the ones of the local MCP server, with that key added, plus `pmitz_sandbox_status` and `pmitz_export_plans`.',
                      'The other tools check access and limits, manage products and subscriptions, record usage, and export the plans.')
readme = replace_once(readme, 'and the two MCP servers described above.', 'and the MCP server described above.')
readme, count = re.subn(r'The local MCP server runs as a Node\.js process.*?\n\n', '', readme, flags=re.S)
assert count == 1
readme = replace_once(readme, 'Node.js 22 for the Node example, and Node.js 18 or newer for the local MCP server.',
                      'and Node.js 22 for the Node example.')
assert 'connector' not in readme.lower(), [l for l in readme.splitlines() if 'connector' in l.lower()]

skill = (plugin / 'skills' / 'pmitz' / 'SKILL.md').read_text()
skill, count = re.subn(
    r' When the local Pmitz MCP tools are available.*?Without them, use the REST calls in the references\.',
    ' Work on it with the REST calls in the references. It holds real data, so confirm with the developer before creating or removing anything, and prefer a development server.',
    skill, flags=re.S)
assert count == 1
assert 'connector' not in skill.lower() and 'pmitz_connection_status' not in skill

openai = []
for arcname, data in full:
    if arcname.startswith(('.claude-plugin/', 'server/')):
        continue
    if arcname == '.codex-plugin/plugin.json':
        data = (json.dumps(manifest, indent=2) + '\n').encode()
    if arcname == '.mcp.json':
        data = (json.dumps(mcp, indent=2) + '\n').encode()
    if arcname == 'README.md':
        data = readme.encode()
    if arcname == 'skills/pmitz/SKILL.md':
        data = skill.encode()
    openai.append((arcname, data))
build(f'pmitz-openai-{version}.zip', openai)

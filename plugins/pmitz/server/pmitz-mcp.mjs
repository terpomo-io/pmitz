#!/usr/bin/env node
// Pmitz local MCP server.
//
// A local MCP server (stdio, JSON-RPC 2.0) that lets a coding assistant inspect and change a
// Pmitz server through its REST API. It has no dependencies and needs Node.js 18 or newer.
//
// Configuration comes from environment variables only. Tool arguments can never change the
// destination, supply a credential, or leave read-only mode.
//
//   PMITZ_BASE_URL       Base URL of the Pmitz server, for example http://127.0.0.1:8080
//   PMITZ_API_KEY        API key of that server. Sent only in the X-Api-Key header to PMITZ_BASE_URL.
//   PMITZ_MCP_READ_ONLY  Optional. Set to "true" to offer only the tools that read.

import { realpathSync } from 'node:fs';
import { createInterface } from 'node:readline';
import { fileURLToPath } from 'node:url';

const SERVER_NAME = 'pmitz';
const SERVER_VERSION = '0.1.0';
const SUPPORTED_PROTOCOL_VERSIONS = ['2025-06-18', '2025-03-26', '2024-11-05'];
const REQUEST_TIMEOUT_MS = 15000;
const GROUP_TYPES = ['subscriptions', 'users', 'directory-groups'];
const SUBSCRIPTION_STATUSES = ['TRIAL', 'TRIAL_ENDED', 'PENDING', 'ACTIVE', 'SUSPENDED', 'EXPIRED',
	'PENDING_CANCELLATION', 'CANCELLED', 'TERMINATED', 'ENDED'];

// ---------------------------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------------------------

function readSetting(name) {
	const value = (process.env[name] ?? '').trim();
	// A host that did not fill in a placeholder passes it through literally; treat that as unset.
	return value.startsWith('${') ? '' : value;
}

function normalizeBaseUrl(value) {
	if (!value) {
		return null;
	}
	let url;
	try {
		url = new URL(value);
	}
	catch {
		return null;
	}
	if ((url.protocol !== 'http:' && url.protocol !== 'https:') || url.username || url.password
			|| url.search || url.hash) {
		return null;
	}
	const path = url.pathname.replace(/\/+$/, '');
	return `${url.protocol}//${url.host}${path}`;
}

function loadConfig() {
	const rawBaseUrl = readSetting('PMITZ_BASE_URL');
	const baseUrl = normalizeBaseUrl(rawBaseUrl);
	const apiKey = readSetting('PMITZ_API_KEY');
	const readOnly = ['1', 'true', 'yes', 'on'].includes(readSetting('PMITZ_MCP_READ_ONLY').toLowerCase());

	const problems = [];
	if (!rawBaseUrl) {
		problems.push('PMITZ_BASE_URL is not set.');
	}
	else if (!baseUrl) {
		problems.push('PMITZ_BASE_URL must be an http or https URL without credentials, query or fragment.');
	}
	if (!apiKey) {
		problems.push('PMITZ_API_KEY is not set.');
	}

	return {
		baseUrl,
		apiKey,
		problems,
		readOnly,
	};
}

// ---------------------------------------------------------------------------------------------
// Tool definitions
// ---------------------------------------------------------------------------------------------

const subjectProperties = {
	groupType: {
		type: 'string',
		enum: GROUP_TYPES,
		description: 'Kind of subject. Use "subscriptions" when access depends on a plan.',
	},
	groupId: { type: 'string', description: 'Identifier of the subscription, user or directory group.' },
	productId: { type: 'string', description: 'Product ID from the catalog.' },
	featureId: { type: 'string', description: 'Feature ID inside that product.' },
};
const subjectRequired = ['groupId', 'productId', 'featureId'];
const unitsProperty = {
	type: 'object',
	description: 'Units per limit ID, for example {"monthly-exports": 1}. Each value is a positive integer.',
	additionalProperties: { type: 'integer', minimum: 1 },
};

const TOOLS = [
	{
		name: 'pmitz_connection_status',
		write: false,
		description: 'Shows which Pmitz server these tools are configured for, whether it is reachable, and '
			+ 'whether it is in read-only mode. It never shows the API key.',
		inputSchema: { type: 'object', properties: {}, additionalProperties: false },
	},
	{
		name: 'pmitz_verify_entitlement',
		write: false,
		description: 'Checks whether a subject may use a feature. Returns featureAllowed and, on denial, the cause '
			+ '(INVALID_SUBSCRIPTION, PRODUCT_NOT_ALLOWED or FEATURE_NOT_ALLOWED).',
		inputSchema: { type: 'object', properties: subjectProperties, required: subjectRequired, additionalProperties: false },
	},
	{
		name: 'pmitz_check_limits',
		write: false,
		description: 'Previews whether the requested units fit. Returns featureStatus (AVAILABLE, LIMIT_EXCEEDED or '
			+ 'NOT_ALLOWED) and the units that would remain after the action. Nothing is reserved or recorded.',
		inputSchema: {
			type: 'object',
			properties: { ...subjectProperties, units: unitsProperty },
			required: [...subjectRequired, 'units'],
			additionalProperties: false,
		},
	},
	{
		name: 'pmitz_get_remaining_limits',
		write: false,
		description: 'Reads the remaining units of each limit of a feature for a subject. For a subscription the '
			+ 'limits of its plan are applied. A negative value means usage is already above the limit.',
		inputSchema: { type: 'object', properties: subjectProperties, required: subjectRequired, additionalProperties: false },
	},
	{
		name: 'pmitz_find_subscription',
		write: false,
		description: 'Reads a subscription: its status, expiration date and plan per product.',
		inputSchema: {
			type: 'object',
			properties: { subscriptionId: { type: 'string', description: 'Subscription ID.' } },
			required: ['subscriptionId'],
			additionalProperties: false,
		},
	},
	{
		name: 'pmitz_create_product',
		write: true,
		destructive: false,
		description: 'Creates a product with its features, limits and plans. Fails with a conflict if the product ID '
			+ 'already exists; there is no update, so changing a product means removing it and creating it again.',
		inputSchema: {
			type: 'object',
			properties: {
				product: {
					type: 'object',
					description: 'One product object with productId, features and plans, in the Pmitz catalog format.',
				},
			},
			required: ['product'],
			additionalProperties: false,
		},
	},
	{
		name: 'pmitz_remove_product',
		write: true,
		destructive: true,
		description: 'Removes a product definition. Every check for that product is refused until it is created '
			+ 'again. Stored usage and subscriptions are kept.',
		inputSchema: {
			type: 'object',
			properties: { productId: { type: 'string', description: 'Product ID to remove.' } },
			required: ['productId'],
			additionalProperties: false,
		},
	},
	{
		name: 'pmitz_create_subscription',
		write: true,
		destructive: false,
		description: 'Creates a subscription and assigns a plan per product. Fails with a conflict if the '
			+ 'subscription ID already exists.',
		inputSchema: {
			type: 'object',
			properties: {
				subscriptionId: { type: 'string', description: 'New subscription ID.' },
				status: { type: 'string', enum: SUBSCRIPTION_STATUSES, description: 'Initial status, usually ACTIVE.' },
				plans: {
					type: 'object',
					description: 'Plan ID per product ID, for example {"reports": "free"}.',
					additionalProperties: { type: 'string' },
				},
				expirationDate: {
					type: 'string',
					description: 'Optional ISO date-time with an offset, for example 2027-01-31T00:00:00Z.',
				},
			},
			required: ['subscriptionId', 'status', 'plans'],
			additionalProperties: false,
		},
	},
	{
		name: 'pmitz_update_subscription_status',
		write: true,
		destructive: true,
		description: 'Changes the status of a subscription. A status that is not valid, such as SUSPENDED or '
			+ 'CANCELLED, makes every feature check for that subscription fail.',
		inputSchema: {
			type: 'object',
			properties: {
				subscriptionId: { type: 'string', description: 'Subscription ID.' },
				status: { type: 'string', enum: SUBSCRIPTION_STATUSES, description: 'New status.' },
			},
			required: ['subscriptionId', 'status'],
			additionalProperties: false,
		},
	},
	{
		name: 'pmitz_record_usage',
		write: true,
		destructive: false,
		description: 'Records consumed units for a subject. Refused when a limit would be exceeded. To test an '
			+ 'application integration, trigger the action through the application instead of calling this tool.',
		inputSchema: {
			type: 'object',
			properties: { ...subjectProperties, units: unitsProperty },
			required: [...subjectRequired, 'units'],
			additionalProperties: false,
		},
	},
	{
		name: 'pmitz_reduce_usage',
		write: true,
		destructive: true,
		description: 'Subtracts units from the recorded usage of a subject, for example after a seat is freed. '
			+ 'It subtracts; it does not restore an earlier value.',
		inputSchema: {
			type: 'object',
			properties: { ...subjectProperties, units: unitsProperty },
			required: [...subjectRequired, 'units'],
			additionalProperties: false,
		},
	},
];

function toolListing(tool) {
	return {
		name: tool.name,
		description: tool.description,
		inputSchema: tool.inputSchema,
		annotations: {
			readOnlyHint: !tool.write,
			destructiveHint: Boolean(tool.write && tool.destructive),
			idempotentHint: !tool.write,
			openWorldHint: false,
		},
	};
}

// ---------------------------------------------------------------------------------------------
// Argument checks
// ---------------------------------------------------------------------------------------------

class ToolError extends Error {}

function requireId(args, name) {
	const value = args[name];
	if (typeof value !== 'string' || value.trim() === '') {
		throw new ToolError(`"${name}" must be a non-empty string.`);
	}
	return value;
}

function requireEnum(args, name, allowed, fallback) {
	const value = args[name] ?? fallback;
	if (!allowed.includes(value)) {
		throw new ToolError(`"${name}" must be one of: ${allowed.join(', ')}.`);
	}
	return value;
}

function requireUnits(args) {
	const units = args.units;
	if (units === null || typeof units !== 'object' || Array.isArray(units) || Object.keys(units).length === 0) {
		throw new ToolError('"units" must be an object with at least one limit ID.');
	}
	for (const [limitId, value] of Object.entries(units)) {
		if (!Number.isInteger(value) || value < 1) {
			throw new ToolError(`"units.${limitId}" must be a positive integer.`);
		}
	}
	return units;
}

function subjectPath(args, operation) {
	const groupType = requireEnum(args, 'groupType', GROUP_TYPES, 'subscriptions');
	const segments = [groupType, requireId(args, 'groupId'), operation, requireId(args, 'productId'),
		requireId(args, 'featureId')];
	return '/' + segments.map(encodeURIComponent).join('/');
}

// ---------------------------------------------------------------------------------------------
// REST calls
// ---------------------------------------------------------------------------------------------

// Sends one request to the configured Pmitz server and returns { status, body }.
function createRestTransport(config) {
	return async function callPmitz(method, path, body) {
		const controller = new AbortController();
		const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
		try {
			const headers = { 'X-Api-Key': config.apiKey };
			if (body !== undefined) {
				headers['Content-Type'] = 'application/json';
			}
			const response = await fetch(config.baseUrl + path, {
				method,
				headers,
				body: (body === undefined) ? undefined : JSON.stringify(body),
				signal: controller.signal,
				redirect: 'error',
			});
			const text = await response.text();
			let parsed = null;
			if (text) {
				try {
					parsed = JSON.parse(text);
				}
				catch {
					parsed = text.slice(0, 2000);
				}
			}
			return { status: response.status, body: parsed };
		}
		catch (error) {
			const reason = (error?.name === 'AbortError')
				? `no answer within ${REQUEST_TIMEOUT_MS / 1000} seconds`
				: (error?.cause?.code ?? error?.message ?? 'request failed');
			throw new ToolError(`Could not reach the Pmitz server at ${config.baseUrl}: ${reason}.`);
		}
		finally {
			clearTimeout(timer);
		}
	};
}

// ---------------------------------------------------------------------------------------------
// Toolset
// ---------------------------------------------------------------------------------------------

// Builds the tools for one Pmitz server. The context supplies how to reach it:
//   server            label shown in results, normally the base URL
//   callPmitz         async (method, path, body) => ({ status, body })
//   connectionStatus  async () => object, the answer of pmitz_connection_status
//   readOnly          true to offer only the tools that read
//   problems          configuration problems; when not empty every other tool is refused
//   omit              tool names to leave out
export function createToolset(context) {
	const omitted = new Set(context.omit ?? []);
	const problems = context.problems ?? [];
	const callPmitz = context.callPmitz;

	function outcome(response, meanings) {
		const meaning = meanings[response.status]
			?? ((response.status === 401 || response.status === 403)
				? 'The Pmitz server rejected the API key.'
				: `Unexpected HTTP ${response.status} from the Pmitz server.`);
		return {
			ok: response.status >= 200 && response.status < 300,
			server: context.server,
			httpStatus: response.status,
			meaning,
			result: response.body,
		};
	}

	const HANDLERS = {
		async pmitz_connection_status() {
			return context.connectionStatus();
		},

		async pmitz_verify_entitlement(args) {
			const response = await callPmitz('GET', subjectPath(args, 'subscription-check'));
			return outcome(response, { 200: 'Entitlement decision returned. Read featureAllowed.' });
		},

		async pmitz_check_limits(args) {
			const path = subjectPath(args, 'limits-check');
			const response = await callPmitz('POST', path, requireUnits(args));
			return outcome(response, { 200: 'Preview returned. Read featureStatus; nothing was recorded.' });
		},

		async pmitz_get_remaining_limits(args) {
			const response = await callPmitz('GET', subjectPath(args, 'usage'));
			const limitIds = Object.keys(response.body?.remainingUsageUnits ?? {});
			const groupType = args.groupType ?? 'subscriptions';
			if (response.status !== 200 || groupType !== 'subscriptions' || limitIds.length === 0) {
				return outcome(response, { 200: 'Current status and remaining units returned.' });
			}
			// In Pmitz 0.10.0 the usage endpoint ignores the subscription's plan and answers with the
			// feature's default limits. The preview endpoint applies the plan, so the current values are
			// derived from a preview of one unit per limit, which records nothing.
			const oneUnitEach = Object.fromEntries(limitIds.map((limitId) => [limitId, 1]));
			const preview = await callPmitz('POST', subjectPath(args, 'limits-check'), oneUnitEach);
			const afterOneUnit = preview.body?.remainingUsageUnits;
			if (preview.status !== 200 || !afterOneUnit || limitIds.some((id) => !Number.isFinite(afterOneUnit[id]))) {
				return outcome(preview, { 200: 'The Pmitz server did not return the remaining units.' });
			}
			const remaining = Object.fromEntries(limitIds.map((limitId) => [limitId, afterOneUnit[limitId] + 1]));
			const values = Object.values(remaining);
			response.body = {
				featureStatus: values.some((value) => value < 0) ? 'LIMIT_EXCEEDED' : 'AVAILABLE',
				remainingUsageUnits: remaining,
				limitReached: values.some((value) => value <= 0),
			};
			return outcome(response, { 200: 'Current remaining units, with the limits of the subscription\'s plan applied.' });
		},

		async pmitz_find_subscription(args) {
			const path = '/subscriptions/' + encodeURIComponent(requireId(args, 'subscriptionId'));
			const response = await callPmitz('GET', path);
			return outcome(response, { 200: 'Subscription found.', 404: 'No subscription has this ID.' });
		},

		async pmitz_create_product(args) {
			const product = args.product;
			if (product === null || typeof product !== 'object' || Array.isArray(product)) {
				throw new ToolError('"product" must be one product object, not an array.');
			}
			requireId(product, 'productId');
			const response = await callPmitz('POST', '/products', product);
			return outcome(response, {
				200: 'Product created.',
				409: 'A product with this ID already exists. It was left unchanged.',
			});
		},

		async pmitz_remove_product(args) {
			const path = '/products/' + encodeURIComponent(requireId(args, 'productId'));
			const response = await callPmitz('DELETE', path);
			return outcome(response, { 200: 'Product removed.', 404: 'No product has this ID.' });
		},

		async pmitz_create_subscription(args) {
			const plans = args.plans;
			if (plans === null || typeof plans !== 'object' || Array.isArray(plans)
					|| Object.values(plans).some((planId) => typeof planId !== 'string' || planId === '')) {
				throw new ToolError('"plans" must be an object that maps product IDs to plan IDs.');
			}
			const subscription = {
				subscriptionId: requireId(args, 'subscriptionId'),
				status: requireEnum(args, 'status', SUBSCRIPTION_STATUSES),
				plans,
			};
			if (args.expirationDate !== undefined) {
				subscription.expirationDate = requireId(args, 'expirationDate');
			}
			const response = await callPmitz('POST', '/subscriptions', subscription);
			return outcome(response, {
				200: 'Subscription created.',
				409: 'A subscription with this ID already exists. It was left unchanged.',
			});
		},

		async pmitz_update_subscription_status(args) {
			const path = '/subscriptions/' + encodeURIComponent(requireId(args, 'subscriptionId')) + '/status';
			const body = { status: requireEnum(args, 'status', SUBSCRIPTION_STATUSES) };
			const response = await callPmitz('PATCH', path, body);
			return outcome(response, { 200: 'Status updated.', 404: 'No subscription has this ID.' });
		},

		async pmitz_record_usage(args) {
			const body = { units: requireUnits(args), reduceUnits: false };
			const response = await callPmitz('POST', subjectPath(args, 'usage'), body);
			return outcome(response, {
				200: 'Usage recorded.',
				422: 'Refused: a limit would be exceeded. Nothing was recorded.',
				500: 'The server failed. In Pmitz 0.10.0 this is also the answer when the subject is not entitled '
					+ 'to the feature; check with pmitz_verify_entitlement.',
			});
		},

		async pmitz_reduce_usage(args) {
			const body = { units: requireUnits(args), reduceUnits: true };
			const response = await callPmitz('POST', subjectPath(args, 'usage'), body);
			return outcome(response, {
				200: 'Usage reduced.',
				500: 'The server failed. In Pmitz 0.10.0 this is also the answer when the subject is not entitled '
					+ 'to the feature; check with pmitz_verify_entitlement.',
			});
		},
	};

	// In read-only mode the write tools are hidden, and refused again when a call is dispatched.
	function listTools() {
		return TOOLS.filter((tool) => !omitted.has(tool.name) && (!tool.write || !context.readOnly)).map(toolListing);
	}

	async function callTool(name, args) {
		const tool = TOOLS.find((candidate) => candidate.name === name && !omitted.has(candidate.name));
		if (!tool) {
			throw new ToolError(`Unknown tool: ${name}.`);
		}
		if (tool.write && context.readOnly) {
			throw new ToolError(context.readOnlyMessage ?? 'This MCP server is in read-only mode. A person has to turn '
				+ 'that off by unsetting PMITZ_MCP_READ_ONLY and restarting it.');
		}
		if (name !== 'pmitz_connection_status' && problems.length > 0) {
			throw new ToolError('This MCP server is not configured: ' + problems.join(' '));
		}
		if (args === null || typeof args !== 'object' || Array.isArray(args)) {
			throw new ToolError('Tool arguments must be an object.');
		}
		return HANDLERS[name](args);
	}

	return { listTools, callTool };
}

// The toolset for the server named in the environment: what the stdio MCP server offers.
function createLocalToolset(config) {
	return createToolset({
		server: config.baseUrl,
		callPmitz: createRestTransport(config),
		readOnly: config.readOnly,
		problems: config.problems,
		async connectionStatus() {
			const status = {
				server: config.baseUrl,
				apiKeyConfigured: Boolean(config.apiKey),
				readOnly: config.readOnly,
				configurationProblems: config.problems,
			};
			if (config.readOnly) {
				status.readOnlyNote = 'Read-only mode is on (PMITZ_MCP_READ_ONLY). Only the tools that read are offered.';
			}
			if (config.problems.length === 0) {
				try {
					const response = await fetch(config.baseUrl + '/actuator/health', {
						signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
						redirect: 'error',
					});
					status.reachable = response.ok;
					status.healthHttpStatus = response.status;
				}
				catch (error) {
					status.reachable = false;
					status.reachError = error?.cause?.code ?? error?.message ?? 'request failed';
				}
			}
			return status;
		},
	});
}

export { ToolError, SERVER_VERSION, SUBSCRIPTION_STATUSES, GROUP_TYPES };

// ---------------------------------------------------------------------------------------------
// MCP over stdio (only when this file is run as a program)
// ---------------------------------------------------------------------------------------------

function send(message) {
	process.stdout.write(JSON.stringify(message) + '\n');
}

function reply(id, result) {
	send({ jsonrpc: '2.0', id, result });
}

function replyError(id, code, message) {
	send({ jsonrpc: '2.0', id, error: { code, message } });
}

function toolResult(value, isError) {
	const text = (typeof value === 'string') ? value : JSON.stringify(value, null, 2);
	return { content: [{ type: 'text', text }], isError };
}

async function handle(message) {
	const { id, method, params } = message;
	const isRequest = id !== undefined && id !== null;

	switch (method) {
		case 'initialize': {
			const requested = params?.protocolVersion;
			const protocolVersion = SUPPORTED_PROTOCOL_VERSIONS.includes(requested)
				? requested
				: SUPPORTED_PROTOCOL_VERSIONS[0];
			reply(id, {
				protocolVersion,
				capabilities: { tools: {} },
				serverInfo: { name: SERVER_NAME, version: SERVER_VERSION },
				instructions: 'Tools for one Pmitz server chosen in the settings of this MCP server. Call '
					+ 'pmitz_connection_status first to see which server it is. The tools that change the server '
					+ 'act on real data: confirm with the user before using them.',
			});
			return;
		}
		case 'ping':
			reply(id, {});
			return;
		case 'tools/list':
			reply(id, { tools: toolset.listTools() });
			return;
		case 'tools/call': {
			try {
				const result = await toolset.callTool(params?.name, params?.arguments ?? {});
				reply(id, toolResult(result, result?.ok === false));
			}
			catch (error) {
				if (error instanceof ToolError) {
					reply(id, toolResult(error.message, true));
				}
				else {
					process.stderr.write(`pmitz-mcp: unexpected error in ${params?.name}: ${error?.message}\n`);
					reply(id, toolResult('This MCP server hit an unexpected error.', true));
				}
			}
			return;
		}
		default:
			if (isRequest) {
				replyError(id, -32601, `Method not found: ${method}`);
			}
	}
}

let toolset;

function startStdio() {
	toolset = createLocalToolset(loadConfig());
	const pending = new Set();
	const lines = createInterface({ input: process.stdin, crlfDelay: Infinity });

	lines.on('line', (line) => {
		if (line.trim() === '') {
			return;
		}
		let message;
		try {
			message = JSON.parse(line);
		}
		catch {
			replyError(null, -32700, 'Parse error');
			return;
		}
		if (message === null || typeof message !== 'object' || typeof message.method !== 'string') {
			// Responses to requests this server never sends, or malformed input: nothing to do.
			return;
		}
		const task = handle(message).finally(() => pending.delete(task));
		pending.add(task);
	});

	lines.on('close', async () => {
		await Promise.allSettled([...pending]);
		process.exit(0);
	});
}

function runAsProgram() {
	try {
		return realpathSync(process.argv[1]) === fileURLToPath(import.meta.url);
	}
	catch {
		return false;
	}
}

if (runAsProgram()) {
	startStdio();
}

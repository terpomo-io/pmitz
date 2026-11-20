// Pmitz hosted MCP server.
//
// Gives an assistant a Pmitz sandbox, a temporary Pmitz server ("sandbox") with no account. Every sandbox
// is a private slice of one real Pmitz server that runs next to this process: the gateway prefixes the
// product, subscription and subject IDs of each sandbox, so sandboxes cannot see each other.
//
// Three ways in, all on the same data:
//   POST /mcp          MCP over Streamable HTTP, stateless, no sign-in. The Pmitz tools are the ones of
//                      the local MCP server (plugins/pmitz/server/pmitz-mcp.mjs), with a sandbox key.
//   /sandbox/<id>/...        The Pmitz REST API of one sandbox, for the developer's application.
//                      Authenticate with the sandbox key in the X-Api-Key header.
//   GET /              A short page that says what this is.
//
// Configuration (environment):
//   PMITZ_INTERNAL_URL   Base URL of the Pmitz server behind the gateway, for example http://127.0.0.1:8080
//   PMITZ_INTERNAL_KEY   API key of that server. Never leaves this process.
//   DATABASE_URL         PostgreSQL connection string, the same database the Pmitz server uses
//   PUBLIC_URL           Public base URL of this service, for example https://pmitz-mcp.web.app
//   OPENAI_APPS_CHALLENGE  Optional token served at /.well-known/openai-apps-challenge
//   PORT                 Port to listen on (default 8090)
//   SWEEP_SECONDS        Minimum time between two clean-ups of expired sandboxes (default 600)

import { createHash, randomBytes, timingSafeEqual } from 'node:crypto';
import { createServer } from 'node:http';
import pg from 'pg';

import { createToolset, ToolError, SERVER_VERSION, GROUP_TYPES } from '../plugins/pmitz/server/pmitz-mcp.mjs';

const settings = {
	pmitzUrl: (process.env.PMITZ_INTERNAL_URL ?? 'http://127.0.0.1:8080').replace(/\/+$/, ''),
	pmitzKey: process.env.PMITZ_INTERNAL_KEY ?? '',
	databaseUrl: process.env.DATABASE_URL ?? '',
	publicUrl: (process.env.PUBLIC_URL ?? 'http://127.0.0.1:8090').replace(/\/+$/, ''),
	challenge: (process.env.OPENAI_APPS_CHALLENGE ?? '').trim(),
	port: Number(process.env.PORT ?? 8090),
	sweepSeconds: Number(process.env.SWEEP_SECONDS ?? 600),
};

// Limits. A sandbox is for development and testing, so they are deliberately small.
const LIMITS = {
	lifetimeHours: 72,
	maxBodyBytes: 64 * 1024,
	maxIdLength: 120,
	maxProductsPerSandbox: 10,
	maxSubscriptionsPerSandbox: 500,
	maxActiveSandboxs: 5000,
	requestsPerSandboxPerMinute: 120,
	createsPerAddressPerHour: 30,
	createsPerHour: 300,
	requestsPerAddressPerMinute: 600,
};

const SUPPORTED_PROTOCOL_VERSIONS = ['2025-11-25', '2025-06-18', '2025-03-26', '2024-11-05'];
const REQUEST_TIMEOUT_MS = 15000;
const OPERATIONS = ['usage', 'limits-check', 'subscription-check'];

const pool = new pg.Pool({ connectionString: settings.databaseUrl, max: 5, idleTimeoutMillis: 30000 });

// ---------------------------------------------------------------------------------------------
// Errors
// ---------------------------------------------------------------------------------------------

// A refusal the caller can act on. `status` is the HTTP status the REST surface answers with.
class Refusal extends Error {
	constructor(status, message) {
		super(message);
		this.status = status;
	}
}

// ---------------------------------------------------------------------------------------------
// Rate limits (per process; the service runs as one instance)
// ---------------------------------------------------------------------------------------------

const windows = new Map();

function allow(bucket, limit, windowMs) {
	const now = Date.now();
	const entry = windows.get(bucket);
	if (!entry || now - entry.start >= windowMs) {
		windows.set(bucket, { start: now, count: 1 });
		return true;
	}
	entry.count += 1;
	return entry.count <= limit;
}

setInterval(() => {
	const cutoff = Date.now() - 3600 * 1000;
	for (const [bucket, entry] of windows) {
		if (entry.start < cutoff) {
			windows.delete(bucket);
		}
	}
}, 10 * 60 * 1000).unref();

// ---------------------------------------------------------------------------------------------
// Storage
// ---------------------------------------------------------------------------------------------

async function prepareStorage() {
	await pool.query(`
		CREATE SCHEMA IF NOT EXISTS gateway;
		CREATE TABLE IF NOT EXISTS gateway.sandbox (
			id TEXT PRIMARY KEY,
			key_hash TEXT NOT NULL,
			created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
			expires_at TIMESTAMPTZ NOT NULL
		);
		CREATE TABLE IF NOT EXISTS gateway.product (
			sandbox_id TEXT NOT NULL REFERENCES gateway.sandbox(id) ON DELETE CASCADE,
			product_id TEXT NOT NULL,
			definition JSON NOT NULL,
			PRIMARY KEY (sandbox_id, product_id)
		);
		CREATE TABLE IF NOT EXISTS gateway.subscription (
			sandbox_id TEXT NOT NULL REFERENCES gateway.sandbox(id) ON DELETE CASCADE,
			subscription_id TEXT NOT NULL,
			PRIMARY KEY (sandbox_id, subscription_id)
		);
	`);
}

function sha256(value) {
	return createHash('sha256').update(value).digest('hex');
}

function randomToken(length, alphabet) {
	const bytes = randomBytes(length * 2);
	let token = '';
	for (let index = 0; token.length < length; index += 1) {
		const value = bytes[index % bytes.length];
		if (value < Math.floor(256 / alphabet.length) * alphabet.length) {
			token += alphabet[value % alphabet.length];
		}
		if (index > length * 40) {
			throw new Error('could not generate a token');
		}
	}
	return token;
}

const ID_ALPHABET = 'abcdefghijkmnpqrstuvwxyz23456789';
const MIXED_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789';

function prefixOf(sandboxId) {
	return `${sandboxId}--`;
}

async function createSandbox(address) {
	if (!allow(`create:${address}`, LIMITS.createsPerAddressPerHour, 3600 * 1000)
			|| !allow('create:all', LIMITS.createsPerHour, 3600 * 1000)) {
		throw new Refusal(429, 'Too many sandboxes were created recently. Try again in an hour.');
	}
	const active = await pool.query('SELECT count(*)::int AS n FROM gateway.sandbox WHERE expires_at > now()');
	if (active.rows[0].n >= LIMITS.maxActiveSandboxs) {
		throw new Refusal(503, 'The service is at capacity. Try again later.');
	}
	const id = randomToken(12, ID_ALPHABET);
	const secret = randomToken(32, MIXED_ALPHABET);
	const result = await pool.query(
		`INSERT INTO gateway.sandbox (id, key_hash, expires_at)
		 VALUES ($1, $2, now() + make_interval(hours => $3)) RETURNING expires_at`,
		[id, sha256(secret), LIMITS.lifetimeHours]);
	return { id, key: `pmz_${id}_${secret}`, expiresAt: result.rows[0].expires_at };
}

// Accepts the full key (pmz_<id>_<secret>) and returns the sandbox, or refuses.
async function authenticate(key, expectedId) {
	const match = /^pmz_([a-z0-9]{12})_([A-Za-z0-9]{32})$/.exec(typeof key === 'string' ? key.trim() : '');
	if (!match || (expectedId && match[1] !== expectedId)) {
		throw new Refusal(401, 'The sandbox key is missing or not valid. It looks like pmz_<id>_<secret> and is '
			+ 'returned once by pmitz_create_sandbox.');
	}
	const [, id, secret] = match;
	const result = await pool.query('SELECT key_hash, expires_at, created_at FROM gateway.sandbox WHERE id = $1', [id]);
	const row = result.rows[0];
	const given = Buffer.from(sha256(secret));
	if (!row || !timingSafeEqual(given, Buffer.from(row.key_hash))) {
		throw new Refusal(401, 'The sandbox key is not valid.');
	}
	if (row.expires_at.getTime() <= Date.now()) {
		throw new Refusal(410, 'This sandbox has expired and its data was deleted. Create a new one with '
			+ 'pmitz_create_sandbox.');
	}
	if (!allow(`sandbox:${id}`, LIMITS.requestsPerSandboxPerMinute, 60 * 1000)) {
		throw new Refusal(429, 'Too many requests for this sandbox. Wait a minute.');
	}
	return { id, prefix: prefixOf(id), expiresAt: row.expires_at, createdAt: row.created_at };
}

async function deleteSandboxData(sandboxId) {
	const prefix = prefixOf(sandboxId);
	const products = await pool.query('SELECT product_id FROM gateway.product WHERE sandbox_id = $1', [sandboxId]);
	for (const row of products.rows) {
		await callInternal('DELETE', '/products/' + encodeURIComponent(prefix + row.product_id)).catch(() => {});
	}
	await pool.query('DELETE FROM dbo.usage WHERE product_id LIKE $1', [prefix + '%']);
	await pool.query('DELETE FROM dbo.subscription WHERE subscription_id LIKE $1', [prefix + '%']);
	await pool.query('DELETE FROM gateway.sandbox WHERE id = $1', [sandboxId]);
	loaded.delete(sandboxId);
}

let lastSweep = 0;

// Deletes expired sandboxes. Runs in the background, at most every SWEEP_SECONDS (ten minutes).
function sweepExpired() {
	if (Date.now() - lastSweep < settings.sweepSeconds * 1000) {
		return;
	}
	lastSweep = Date.now();
	pool.query('SELECT id FROM gateway.sandbox WHERE expires_at <= now() LIMIT 200')
		.then(async (result) => {
			for (const row of result.rows) {
				await deleteSandboxData(row.id);
			}
		})
		.catch((error) => console.error(`sweep failed: ${error.message}`));
}

// ---------------------------------------------------------------------------------------------
// The Pmitz server behind the gateway
// ---------------------------------------------------------------------------------------------

async function callInternal(method, path, body) {
	const headers = { 'X-Api-Key': settings.pmitzKey };
	if (body !== undefined) {
		headers['Content-Type'] = 'application/json';
	}
	let response;
	try {
		response = await fetch(settings.pmitzUrl + path, {
			method,
			headers,
			body: (body === undefined) ? undefined : JSON.stringify(body),
			signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
			redirect: 'error',
		});
	}
	catch {
		throw new Refusal(503, 'The Pmitz server behind this service did not answer. Try again in a moment.');
	}
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

// The Pmitz catalog lives in the memory of the Pmitz process. This remembers which sandboxes had
// their stored products sent to the process that is running now.
const loaded = new Set();

// A Pmitz process that restarted has an empty catalog and answers as if no product ever existed.
// A marker product tells the two cases apart: when it can be created again, the process is new.
const MARKER_PRODUCT = { productId: 'gateway--marker', features: [], plans: [] };
let markerCheckedAt = 0;

async function forgetCatalogIfPmitzRestarted() {
	if (Date.now() - markerCheckedAt < 5000) {
		return;
	}
	const response = await callInternal('POST', '/products', MARKER_PRODUCT);
	if (response.status === 200) {
		loaded.clear();
	}
	else if (response.status !== 409) {
		throw new Refusal(503, 'The Pmitz server behind this service is not ready. Try again in a moment.');
	}
	markerCheckedAt = Date.now();
}

async function ensureCatalogLoaded(sandbox) {
	await forgetCatalogIfPmitzRestarted();
	if (loaded.has(sandbox.id)) {
		return;
	}
	const products = await pool.query('SELECT product_id, definition FROM gateway.product WHERE sandbox_id = $1',
		[sandbox.id]);
	for (const row of products.rows) {
		const definition = { ...row.definition, productId: sandbox.prefix + row.product_id };
		const response = await callInternal('POST', '/products', definition);
		if (response.status !== 200 && response.status !== 409) {
			throw new Refusal(503, 'The stored plans of this sandbox could not be loaded. Try again in a moment.');
		}
	}
	loaded.add(sandbox.id);
}

function checkId(value, name) {
	if (typeof value !== 'string' || value.trim() === '' || value.length > LIMITS.maxIdLength) {
		throw new Refusal(400, `"${name}" must be a non-empty string of at most ${LIMITS.maxIdLength} characters.`);
	}
	return value;
}

// Removes the sandbox prefix from everything the Pmitz server answers.
function withoutPrefix(sandbox, body) {
	if (body === null || body === undefined) {
		return body;
	}
	if (typeof body === 'string') {
		return body.split(sandbox.prefix).join('');
	}
	return JSON.parse(JSON.stringify(body).split(sandbox.prefix).join(''));
}

// Runs one Pmitz REST request inside a sandbox. `path` is a Pmitz route such as /products or
// /subscriptions/acme/usage/reports/export. Returns { status, body } with sandbox IDs restored.
async function forward(sandbox, method, path, body) {
	const segments = path.split('?')[0].split('/').filter((segment) => segment !== '')
		.map((segment) => {
			try {
				return decodeURIComponent(segment);
			}
			catch {
				throw new Refusal(400, 'The path is not valid.');
			}
		});
	const encode = (parts) => '/' + parts.map(encodeURIComponent).join('/');
	const scoped = (id, name) => sandbox.prefix + checkId(id, name);
	let target;
	let payload = body;
	let after = async () => {};

	if (segments.length === 1 && segments[0] === 'products' && method === 'POST') {
		if (body === null || typeof body !== 'object' || Array.isArray(body)) {
			throw new Refusal(400, 'The body must be one product object.');
		}
		const productId = checkId(body.productId, 'productId');
		const count = await pool.query('SELECT count(*)::int AS n FROM gateway.product WHERE sandbox_id = $1',
			[sandbox.id]);
		if (count.rows[0].n >= LIMITS.maxProductsPerSandbox) {
			throw new Refusal(422, `A sandbox holds at most ${LIMITS.maxProductsPerSandbox} products.`);
		}
		target = '/products';
		payload = { ...body, productId: sandbox.prefix + productId };
		after = async (response) => {
			if (response.status === 200) {
				await pool.query(
					`INSERT INTO gateway.product (sandbox_id, product_id, definition) VALUES ($1, $2, $3)
					 ON CONFLICT (sandbox_id, product_id) DO UPDATE SET definition = EXCLUDED.definition`,
					[sandbox.id, productId, JSON.stringify(body)]);
			}
		};
	}
	else if (segments.length === 2 && segments[0] === 'products' && method === 'DELETE') {
		const productId = checkId(segments[1], 'productId');
		target = encode(['products', sandbox.prefix + productId]);
		after = async (response) => {
			if (response.status === 200) {
				await pool.query('DELETE FROM gateway.product WHERE sandbox_id = $1 AND product_id = $2',
					[sandbox.id, productId]);
			}
		};
	}
	else if (segments.length === 1 && segments[0] === 'subscriptions' && method === 'POST') {
		if (body === null || typeof body !== 'object' || Array.isArray(body)) {
			throw new Refusal(400, 'The body must be one subscription object.');
		}
		const subscriptionId = checkId(body.subscriptionId, 'subscriptionId');
		const plans = body.plans;
		if (plans === null || typeof plans !== 'object' || Array.isArray(plans)) {
			throw new Refusal(400, '"plans" must be an object that maps product IDs to plan IDs.');
		}
		const count = await pool.query('SELECT count(*)::int AS n FROM gateway.subscription WHERE sandbox_id = $1',
			[sandbox.id]);
		if (count.rows[0].n >= LIMITS.maxSubscriptionsPerSandbox) {
			throw new Refusal(422, `A sandbox holds at most ${LIMITS.maxSubscriptionsPerSandbox} subscriptions.`);
		}
		target = '/subscriptions';
		payload = {
			...body,
			subscriptionId: sandbox.prefix + subscriptionId,
			plans: Object.fromEntries(Object.entries(plans)
				.map(([productId, planId]) => [sandbox.prefix + checkId(productId, 'plans key'), planId])),
		};
		after = async (response) => {
			if (response.status === 200) {
				await pool.query(
					'INSERT INTO gateway.subscription (sandbox_id, subscription_id) VALUES ($1, $2) ON CONFLICT DO NOTHING',
					[sandbox.id, subscriptionId]);
			}
		};
	}
	else if (segments.length === 2 && segments[0] === 'subscriptions' && method === 'GET') {
		target = encode(['subscriptions', scoped(segments[1], 'subscriptionId')]);
	}
	else if (segments.length === 3 && segments[0] === 'subscriptions' && segments[2] === 'status' && method === 'PATCH') {
		target = encode(['subscriptions', scoped(segments[1], 'subscriptionId'), 'status']);
	}
	else if (segments.length === 5 && GROUP_TYPES.includes(segments[0]) && OPERATIONS.includes(segments[2])
			&& (method === 'GET' || method === 'POST')) {
		target = encode([segments[0], scoped(segments[1], 'groupId'), segments[2], scoped(segments[3], 'productId'),
			checkId(segments[4], 'featureId')]);
	}
	else {
		throw new Refusal(404, 'This is not a route of the Pmitz REST API.');
	}

	await ensureCatalogLoaded(sandbox);
	const response = await callInternal(method, target, payload);
	await after(response);
	return { status: response.status, body: withoutPrefix(sandbox, response.body) };
}

// ---------------------------------------------------------------------------------------------
// MCP tools
// ---------------------------------------------------------------------------------------------

const INSTANCE_PROPERTY = {
	type: 'string',
	description: 'The sandbox key returned by pmitz_create_sandbox, for example pmz_ab12cd34ef56_….',
};

const NOTICE = `A sandbox is a temporary Pmitz server for development and testing. It is deleted `
	+ `${LIMITS.lifetimeHours} hours after it is created and is not for production. Do not put personal or `
	+ 'confidential information in it.';

// Told to the user when a sandbox is handed over, not repeated in the tool descriptions.
const FOR_PRODUCTION = 'For production, run Pmitz on your own server from the Docker image.';

function restUrl(sandboxId) {
	return `${settings.publicUrl}/sandbox/${sandboxId}`;
}

function toolsetFor(sandbox) {
	return createToolset({
		server: restUrl(sandbox.id),
		omit: ['pmitz_connection_status'],
		async callPmitz(method, path, body) {
			try {
				return await forward(sandbox, method, path, body);
			}
			catch (error) {
				if (error instanceof Refusal) {
					throw new ToolError(error.message);
				}
				throw error;
			}
		},
	});
}

const OWN_TOOLS = [
	{
		name: 'pmitz_create_sandbox',
		description: 'Creates a new Pmitz sandbox and returns its sandbox key, its REST address and when it '
			+ 'expires. ' + NOTICE + ' Create one only when the user asks for a Pmitz sandbox or needs a server to develop '
			+ 'against, and reuse the key for the rest of the conversation. Show the key to the user: it is the only '
			+ 'way back to the sandbox.',
		inputSchema: { type: 'object', properties: {}, additionalProperties: false },
		annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false, openWorldHint: false },
	},
	{
		name: 'pmitz_sandbox_status',
		description: 'Shows when a sandbox expires, which products it holds, how many subscriptions it has, and '
			+ 'the REST address an application can call.',
		inputSchema: {
			type: 'object',
			properties: { sandbox: INSTANCE_PROPERTY },
			required: ['sandbox'],
			additionalProperties: false,
		},
		annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: false },
	},
	{
		name: 'pmitz_export_plans',
		description: 'Returns the product definitions of a sandbox (features, limits and plans) exactly as they '
			+ 'were created, so they can be kept in source control and loaded into another Pmitz server. '
			+ 'Subscriptions and recorded usage are not exported.',
		inputSchema: {
			type: 'object',
			properties: { sandbox: INSTANCE_PROPERTY },
			required: ['sandbox'],
			additionalProperties: false,
		},
		annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: false },
	},
];

// The Pmitz tools of the local MCP server, each with the sandbox key added.
function sharedTools() {
	const listing = createToolset({ omit: ['pmitz_connection_status'] }).listTools();
	return listing.map((tool) => ({
		...tool,
		description: `${tool.description} Acts on one Pmitz sandbox.`,
		inputSchema: {
			...tool.inputSchema,
			properties: { sandbox: INSTANCE_PROPERTY, ...tool.inputSchema.properties },
			required: ['sandbox', ...(tool.inputSchema.required ?? [])],
		},
	}));
}

const ALL_TOOLS = [...OWN_TOOLS, ...sharedTools()];

function describeSandbox(sandbox) {
	return {
		restBaseUrl: restUrl(sandbox.id),
		restAuthentication: 'Send the sandbox key in the X-Api-Key header.',
		expiresAt: sandbox.expiresAt.toISOString(),
		hoursLeft: Math.max(0, Math.round((sandbox.expiresAt.getTime() - Date.now()) / 36000) / 100),
		notice: NOTICE,
	};
}

async function callTool(name, args, address) {
	if (args === null || typeof args !== 'object' || Array.isArray(args)) {
		throw new ToolError('Tool arguments must be an object.');
	}
	if (!ALL_TOOLS.some((tool) => tool.name === name)) {
		throw new ToolError(`Unknown tool: ${name}.`);
	}
	try {
		if (name === 'pmitz_create_sandbox') {
			const created = await createSandbox(address);
			return {
				ok: true,
				tellTheUser: 'In your answer to the user, always include the sandbox key and when the sandbox expires, '
					+ 'even if they did not ask. The key is shown only now and is the only way back to the sandbox.',
				sandbox: created.key,
				...describeSandbox({ id: created.id, expiresAt: created.expiresAt }),
				forProduction: FOR_PRODUCTION,
				next: 'Create a product with pmitz_create_product, then a subscription with pmitz_create_subscription.',
			};
		}
		const sandbox = await authenticate(args.sandbox);
		if (name === 'pmitz_sandbox_status') {
			const products = await pool.query(
				'SELECT product_id FROM gateway.product WHERE sandbox_id = $1 ORDER BY product_id', [sandbox.id]);
			const subscriptions = await pool.query(
				'SELECT count(*)::int AS n FROM gateway.subscription WHERE sandbox_id = $1', [sandbox.id]);
			return {
				ok: true,
				...describeSandbox(sandbox),
				products: products.rows.map((row) => row.product_id),
				subscriptions: subscriptions.rows[0].n,
			};
		}
		if (name === 'pmitz_export_plans') {
			const products = await pool.query(
				'SELECT definition FROM gateway.product WHERE sandbox_id = $1 ORDER BY product_id', [sandbox.id]);
			return {
				ok: true,
				products: products.rows.map((row) => row.definition),
				howToLoad: 'Send each product object to POST /products of the target Pmitz server, or pass it to the '
					+ 'pmitz_create_product tool of the local Pmitz MCP server. Embedded Java loads the same objects as '
					+ 'one JSON array with InMemoryProductRepository.load.',
			};
		}
		const { sandbox: _key, ...rest } = args;
		return await toolsetFor(sandbox).callTool(name, rest);
	}
	catch (error) {
		if (error instanceof Refusal) {
			throw new ToolError(error.message);
		}
		throw error;
	}
}

// ---------------------------------------------------------------------------------------------
// HTTP
// ---------------------------------------------------------------------------------------------

function clientAddress(request) {
	const forwarded = String(request.headers['x-forwarded-for'] ?? '').split(',')[0].trim();
	return forwarded || request.socket.remoteAddress || 'unknown';
}

function readBody(request) {
	return new Promise((resolve, reject) => {
		const chunks = [];
		let size = 0;
		request.on('data', (chunk) => {
			size += chunk.length;
			if (size > LIMITS.maxBodyBytes) {
				reject(new Refusal(413, `The request body is larger than ${LIMITS.maxBodyBytes / 1024} KB.`));
				request.destroy();
				return;
			}
			chunks.push(chunk);
		});
		request.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
		request.on('error', reject);
	});
}

function sendJson(response, status, value, headers = {}) {
	const text = JSON.stringify(value);
	response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', ...headers });
	response.end(text);
}

function sendText(response, status, text, type = 'text/plain; charset=utf-8') {
	response.writeHead(status, { 'Content-Type': type, 'Cache-Control': 'no-store' });
	response.end(text);
}

const CORS = {
	'Access-Control-Allow-Origin': '*',
	'Access-Control-Allow-Methods': 'POST, OPTIONS',
	'Access-Control-Allow-Headers': 'Content-Type, Accept, Mcp-Protocol-Version, Mcp-Session-Id',
	'Access-Control-Max-Age': '86400',
};

function toolResult(value, isError) {
	const text = (typeof value === 'string') ? value : JSON.stringify(value, null, 2);
	return { content: [{ type: 'text', text }], isError };
}

async function handleRpc(message, address) {
	const { id, method, params } = message;
	const isRequest = id !== undefined && id !== null;
	const result = (value) => ({ jsonrpc: '2.0', id, result: value });

	switch (method) {
		case 'initialize': {
			const requested = params?.protocolVersion;
			return result({
				protocolVersion: SUPPORTED_PROTOCOL_VERSIONS.includes(requested) ? requested : SUPPORTED_PROTOCOL_VERSIONS[0],
				capabilities: { tools: {} },
				serverInfo: { name: 'pmitz-sandbox', title: 'Pmitz sandbox', version: SERVER_VERSION },
				instructions: 'Gives the user a Pmitz sandbox to develop and test subscription plans, feature access and '
					+ 'usage limits, with no account. Call pmitz_create_sandbox once, show the user the sandbox key, '
					+ 'and pass that key to every other tool. ' + NOTICE,
			});
		}
		case 'ping':
			return result({});
		case 'tools/list':
			return result({ tools: ALL_TOOLS });
		case 'tools/call': {
			try {
				const value = await callTool(params?.name, params?.arguments ?? {}, address);
				return result(toolResult(value, value?.ok === false));
			}
			catch (error) {
				if (error instanceof ToolError) {
					return result(toolResult(error.message, true));
				}
				console.error(`unexpected error in ${params?.name}: ${error?.message}`);
				return result(toolResult('The service hit an unexpected error.', true));
			}
		}
		default:
			return isRequest ? { jsonrpc: '2.0', id, error: { code: -32601, message: `Method not found: ${method}` } } : null;
	}
}

async function handleMcp(request, response, address) {
	if (request.method === 'OPTIONS') {
		response.writeHead(204, CORS);
		response.end();
		return;
	}
	if (request.method !== 'POST') {
		sendJson(response, 405, { error: 'Send MCP messages with POST.' }, { Allow: 'POST, OPTIONS', ...CORS });
		return;
	}
	let payload;
	try {
		payload = JSON.parse(await readBody(request));
	}
	catch (error) {
		if (error instanceof Refusal) {
			sendJson(response, error.status, { jsonrpc: '2.0', id: null, error: { code: -32600, message: error.message } }, CORS);
			return;
		}
		sendJson(response, 400, { jsonrpc: '2.0', id: null, error: { code: -32700, message: 'Parse error' } }, CORS);
		return;
	}
	const messages = Array.isArray(payload) ? payload : [payload];
	const replies = [];
	for (const message of messages) {
		if (message === null || typeof message !== 'object' || typeof message.method !== 'string') {
			continue;
		}
		const reply = await handleRpc(message, address);
		if (reply) {
			replies.push(reply);
		}
	}
	if (replies.length === 0) {
		response.writeHead(202, CORS);
		response.end();
		return;
	}
	sendJson(response, 200, Array.isArray(payload) ? replies : replies[0], CORS);
}

async function handleRest(request, response, sandboxId, rest) {
	const sandbox = await authenticate(request.headers['x-api-key'], sandboxId);
	let body;
	if (request.method === 'POST' || request.method === 'PATCH') {
		const text = await readBody(request);
		if (text.trim() !== '') {
			try {
				body = JSON.parse(text);
			}
			catch {
				throw new Refusal(400, 'The body is not valid JSON.');
			}
		}
	}
	const result = await forward(sandbox, request.method, rest, body);
	if (result.body === null || result.body === undefined) {
		response.writeHead(result.status, { 'Cache-Control': 'no-store' });
		response.end();
		return;
	}
	if (typeof result.body === 'string') {
		sendText(response, result.status, result.body);
		return;
	}
	sendJson(response, result.status, result.body);
}

const HOME_PAGE = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Pmitz sandbox</title>
<style>body{font:16px/1.5 system-ui,sans-serif;max-width:42rem;margin:3rem auto;padding:0 1rem;color:#1b1b1f}code{background:#f0f0f3;padding:.1rem .3rem;border-radius:.2rem}</style>
</head><body>
<h1>Pmitz sandbox</h1>
<p>This is the hosted MCP server of <a href="https://terpomosoft.com/pmitz/">Pmitz</a>, the subscription-entitlement and usage-limit product from Terpomo Software.</p>
<p>It gives an AI assistant a Pmitz sandbox to develop and test plans, feature access and usage limits, with no account. A sandbox is temporary: it is deleted ${LIMITS.lifetimeHours} hours after it is created and is not for production. Do not put personal or confidential information in it.</p>
<p>MCP endpoint: <code>${settings.publicUrl}/mcp</code></p>
<p>For production, run Pmitz yourself from the <a href="https://hub.docker.com/r/terpomo/pmitz-remoteserver">Docker image</a>, or <a href="https://terpomosoft.com/contact/">contact Terpomo Software</a> about Pmitz SaaS.</p>
<p><a href="https://terpomosoft.com/terms/">Terms of service</a> · <a href="https://terpomosoft.com/privacy/">Privacy policy</a> · <a href="https://github.com/terpomo-io/pmitz">Source</a></p>
</body></html>`;

const server = createServer(async (request, response) => {
	const address = clientAddress(request);
	const url = new URL(request.url ?? '/', 'http://localhost');
	try {
		if (url.pathname === '/health') {
			sendText(response, 200, 'ok');
			return;
		}
		if (!allow(`address:${address}`, LIMITS.requestsPerAddressPerMinute, 60 * 1000)) {
			throw new Refusal(429, 'Too many requests. Wait a minute.');
		}
		sweepExpired();
		if (url.pathname === '/mcp') {
			await handleMcp(request, response, address);
			return;
		}
		if (url.pathname === '/.well-known/openai-apps-challenge') {
			if (!settings.challenge) {
				throw new Refusal(404, 'Not found.');
			}
			sendText(response, 200, settings.challenge);
			return;
		}
		const rest = /^\/sandbox\/([a-z0-9]{12})(\/.*)$/.exec(url.pathname);
		if (rest) {
			await handleRest(request, response, rest[1], rest[2]);
			return;
		}
		if (url.pathname === '/' && request.method === 'GET') {
			sendText(response, 200, HOME_PAGE, 'text/html; charset=utf-8');
			return;
		}
		throw new Refusal(404, 'Not found.');
	}
	catch (error) {
		if (response.headersSent) {
			response.end();
			return;
		}
		if (error instanceof Refusal) {
			sendJson(response, error.status, { error: error.message });
			return;
		}
		console.error(`unexpected error for ${request.method} ${url.pathname}: ${error?.message}`);
		sendJson(response, 500, { error: 'The service hit an unexpected error.' });
	}
});

if (!settings.pmitzKey || !settings.databaseUrl) {
	console.error('PMITZ_INTERNAL_KEY and DATABASE_URL must be set.');
	process.exit(1);
}

await prepareStorage();
server.listen(settings.port, () => {
	console.log(`Pmitz hosted MCP listening on ${settings.port}, public address ${settings.publicUrl}`);
});

process.on('SIGTERM', () => {
	server.close(() => pool.end().finally(() => process.exit(0)));
});

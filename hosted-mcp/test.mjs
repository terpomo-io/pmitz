// End-to-end test of the hosted MCP against a real Pmitz server.
//
//   1. cp .env.example .env and fill it in, then: docker compose up -d --wait
//   2. start the gateway with SWEEP_SECONDS=1 (see README.md)
//   3. node test.mjs
//
// Environment: GATEWAY_URL, DATABASE_URL, PMITZ_INTERNAL_KEY, and RESTART_PMITZ (a shell command that
// restarts the Pmitz container, used to prove that plans survive a restart).
//
// Against the deployed service, run `LIVE=1 GATEWAY_URL=https://pmitz-mcp.web.app node test.mjs`. That
// skips what needs the database or would use up the service's limits: restart, abuse limits and expiry.

import { execSync } from 'node:child_process';
import pg from 'pg';

const BASE = (process.env.GATEWAY_URL ?? 'http://127.0.0.1:18090').replace(/\/+$/, '');
const INTERNAL_KEY = process.env.PMITZ_INTERNAL_KEY ?? '';
const LIVE = process.env.LIVE === '1';
const db = LIVE ? null : new pg.Client({ connectionString: process.env.DATABASE_URL });
await db?.connect();

let passed = 0;
let failed = 0;
const seen = [];

function check(name, condition, detail) {
	if (condition) {
		passed += 1;
		console.log(`  pass  ${name}`);
	}
	else {
		failed += 1;
		console.log(`  FAIL  ${name}${detail ? ' :: ' + String(detail).slice(0, 300) : ''}`);
	}
}

let nextId = 1;

async function rpc(method, params, options = {}) {
	const response = await fetch(BASE + '/mcp', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json', Accept: 'application/json, text/event-stream' },
		body: JSON.stringify({ jsonrpc: '2.0', id: nextId++, method, params }),
	});
	const text = await response.text();
	seen.push(text);
	return options.raw ? { status: response.status, text } : JSON.parse(text);
}

async function call(name, args = {}) {
	const reply = await rpc('tools/call', { name, arguments: args });
	const text = reply.result?.content?.[0]?.text ?? '';
	let data = null;
	try {
		data = JSON.parse(text);
	}
	catch {
		// plain-text error
	}
	return { isError: reply.result?.isError === true, text, data };
}

async function rest(sandboxId, key, method, path, body) {
	const response = await fetch(`${BASE}/sandbox/${sandboxId}${path}`, {
		method,
		headers: { 'X-Api-Key': key, ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
		body: body === undefined ? undefined : JSON.stringify(body),
	});
	const text = await response.text();
	seen.push(text);
	let data = null;
	try {
		data = JSON.parse(text);
	}
	catch {
		// empty or text body
	}
	return { status: response.status, text, data };
}

const idOf = (key) => key.split('_')[1];

const product = {
	productId: 'reports',
	features: [
		{ featureId: 'export', limits: [{ type: 'CountLimit', id: 'exports', count: 2, unit: 'exports' }] },
		{ featureId: 'audit-log', limits: [] },
	],
	plans: [
		{ planId: 'free', includedFeatures: ['export'] },
		{
			planId: 'pro',
			description: 'Pro',
			includedFeatures: ['export', 'audit-log'],
			limitsOverride: [{ type: 'CountLimit', id: 'exports', count: 7, unit: 'exports' }],
		},
	],
};

console.log('1. Service');
{
	const home = await fetch(BASE + '/');
	const homeText = await home.text();
	check('home page explains the service', home.status === 200 && homeText.includes('temporary') && homeText.includes('<title>Pmitz sandbox</title>'));
	check('health answers', (await fetch(BASE + '/health')).status === 200);
	const challenge = await fetch(BASE + '/.well-known/openai-apps-challenge');
	check('domain challenge is plain text or absent', LIVE
		? [200, 404].includes(challenge.status)
		: challenge.status === 200 && (await challenge.text()) === 'test-token');
	check('GET /mcp is refused', (await fetch(BASE + '/mcp')).status === 405);
	const bad = await fetch(BASE + '/mcp', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{nope' });
	check('malformed JSON is a parse error', bad.status === 400 && (await bad.text()).includes('-32700'));
	const note = await fetch(BASE + '/mcp', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' }),
	});
	check('a notification gets 202 and no body', note.status === 202 && (await note.text()) === '');
	const init = await rpc('initialize', { protocolVersion: '2025-06-18', capabilities: {}, clientInfo: { name: 'test', version: '1' } });
	check('initialize names the server', init.result?.serverInfo?.name === 'pmitz-sandbox' && init.result?.protocolVersion === '2025-06-18');
	check('instructions say a sandbox is temporary', /72 hours/.test(init.result?.instructions ?? ''));
	const tools = (await rpc('tools/list')).result.tools;
	check('13 tools are listed', tools.length === 13, tools.map((tool) => tool.name).join(','));
	check('no connection-status tool', !tools.some((tool) => tool.name === 'pmitz_connection_status'));
	check('every tool but create asks for the sandbox key',
		tools.filter((tool) => tool.name !== 'pmitz_create_sandbox').every((tool) => tool.inputSchema.required.includes('sandbox')));
	check('read tools are marked read-only',
		tools.filter((tool) => ['pmitz_sandbox_status', 'pmitz_export_plans', 'pmitz_check_limits', 'pmitz_verify_entitlement',
			'pmitz_get_remaining_limits', 'pmitz_find_subscription'].includes(tool.name)).every((tool) => tool.annotations.readOnlyHint === true));
	check('remove and status changes are marked destructive',
		tools.filter((tool) => ['pmitz_remove_product', 'pmitz_update_subscription_status', 'pmitz_reduce_usage'].includes(tool.name))
			.every((tool) => tool.annotations.destructiveHint === true));
	check('unknown method is an error', (await rpc('nope/nope')).error?.code === -32601);
}

console.log('2. A sandbox, through MCP');
const created = await call('pmitz_create_sandbox');
const KEY = created.data?.sandbox ?? '';
const ID = idOf(KEY);
{
	check('create returns a key, an address and an expiry', /^pmz_[a-z0-9]{12}_[A-Za-z0-9]{32}$/.test(KEY)
		&& created.data.restBaseUrl.endsWith(`/sandbox/${ID}`) && created.data.hoursLeft === 72, created.text);
	check('create tells the assistant to show the key and the expiry', /sandbox key/.test(created.data.tellTheUser) && /expires/.test(created.data.tellTheUser));
	check('create says what to do for production and shows no unused fields', /Docker image/.test(created.data.forProduction) && created.data.claimCode === undefined);
	const status = await call('pmitz_sandbox_status', { sandbox: KEY });
	check('a new sandbox is empty', status.data?.products?.length === 0 && status.data?.subscriptions === 0, status.text);
	check('a tool without a key is refused', (await call('pmitz_sandbox_status', {})).isError);
	const unknown = await call('pmitz_no_such_tool', {});
	check('an unknown tool is named as unknown', unknown.isError && /Unknown tool/.test(unknown.text), unknown.text);
	check('a wrong key is refused', (await call('pmitz_sandbox_status', { sandbox: KEY.slice(0, -1) + (KEY.endsWith('a') ? 'b' : 'a') })).isError);

	check('product is created', (await call('pmitz_create_product', { sandbox: KEY, product })).data?.httpStatus === 200);
	check('same product again is a conflict', (await call('pmitz_create_product', { sandbox: KEY, product })).data?.httpStatus === 409);
	check('subscription on pro is created',
		(await call('pmitz_create_subscription', { sandbox: KEY, subscriptionId: 'acme', status: 'ACTIVE', plans: { reports: 'pro' } })).data?.httpStatus === 200);
	check('subscription on free is created',
		(await call('pmitz_create_subscription', { sandbox: KEY, subscriptionId: 'small', status: 'ACTIVE', plans: { reports: 'free' } })).data?.httpStatus === 200);
	const found = await call('pmitz_find_subscription', { sandbox: KEY, subscriptionId: 'acme' });
	check('subscription reads back with its own IDs', found.data?.result?.subscriptionId === 'acme'
		&& found.data?.result?.plansByProduct?.reports === 'pro', found.text);
	const subject = { sandbox: KEY, groupId: 'acme', productId: 'reports', featureId: 'export' };
	check('pro may export', (await call('pmitz_verify_entitlement', subject)).data?.result?.featureAllowed === true);
	check('free may not use the audit log',
		(await call('pmitz_verify_entitlement', { ...subject, groupId: 'small', featureId: 'audit-log' })).data?.result?.featureAllowed === false);
	check('pro starts with its plan limit of 7', (await call('pmitz_get_remaining_limits', subject)).data?.result?.remainingUsageUnits?.exports === 7);
	check('free starts with the default of 2',
		(await call('pmitz_get_remaining_limits', { ...subject, groupId: 'small' })).data?.result?.remainingUsageUnits?.exports === 2);
	check('recording 2 units on free is accepted',
		(await call('pmitz_record_usage', { ...subject, groupId: 'small', units: { exports: 2 } })).data?.httpStatus === 200);
	const third = await call('pmitz_record_usage', { ...subject, groupId: 'small', units: { exports: 1 } });
	check('a third unit on free is refused', third.data?.httpStatus === 422 && third.isError, third.text);
	check('preview agrees', (await call('pmitz_check_limits', { ...subject, groupId: 'small', units: { exports: 1 } })).data?.result?.featureStatus === 'LIMIT_EXCEEDED');
	check('reducing frees a unit', (await call('pmitz_reduce_usage', { ...subject, groupId: 'small', units: { exports: 1 } })).data?.httpStatus === 200
		&& (await call('pmitz_get_remaining_limits', { ...subject, groupId: 'small' })).data?.result?.remainingUsageUnits?.exports === 1);
	check('suspending blocks the subscription',
		(await call('pmitz_update_subscription_status', { sandbox: KEY, subscriptionId: 'small', status: 'SUSPENDED' })).data?.httpStatus === 200
		&& (await call('pmitz_verify_entitlement', { ...subject, groupId: 'small' })).data?.result?.featureAllowed === false);
	const exported = await call('pmitz_export_plans', { sandbox: KEY });
	check('export returns the product exactly as created', JSON.stringify(exported.data?.products) === JSON.stringify([product]), exported.text);
	const status2 = await call('pmitz_sandbox_status', { sandbox: KEY });
	check('status lists the product and counts the subscriptions', status2.data?.products?.[0] === 'reports' && status2.data?.subscriptions === 2);
	check('results name the sandbox address, not the internal server',
		(await call('pmitz_verify_entitlement', subject)).data?.server?.endsWith(`/sandbox/${ID}`));
	const monthly = {
		productId: 'monthly',
		features: [{ featureId: 'export', limits: [{ type: 'CalendarPeriodRateLimit', id: 'monthly-exports', quota: 2, periodicity: 'MONTH' }] }],
		plans: [{ planId: 'basic', includedFeatures: ['export'] }],
	};
	await call('pmitz_create_product', { sandbox: KEY, product: monthly });
	await call('pmitz_create_subscription', { sandbox: KEY, subscriptionId: 'monthly-customer', status: 'ACTIVE', plans: { monthly: 'basic' } });
	const perMonth = { sandbox: KEY, groupId: 'monthly-customer', productId: 'monthly', featureId: 'export', units: { 'monthly-exports': 1 } };
	const statuses = [];
	for (let i = 0; i < 3; i++) {
		statuses.push((await call('pmitz_record_usage', perMonth)).data?.httpStatus);
	}
	check('a monthly quota of 2 accepts two and refuses the third', statuses.join() === '200,200,422', statuses.join());
	check('and reports nothing remaining',
		(await call('pmitz_get_remaining_limits', perMonth)).data?.result?.remainingUsageUnits?.['monthly-exports'] === 0);
}

console.log('3. The same sandbox, through its REST address');
{
	const usage = await rest(ID, KEY, 'GET', '/subscriptions/acme/usage/reports/export');
	check('REST reads usage', usage.status === 200 && usage.data?.featureStatus === 'AVAILABLE', usage.text);
	const record = await rest(ID, KEY, 'POST', '/subscriptions/acme/usage/reports/export', { units: { exports: 3 }, reduceUnits: false });
	check('REST records usage', record.status === 200, record.text);
	check('MCP sees what REST recorded',
		(await call('pmitz_get_remaining_limits', { sandbox: KEY, groupId: 'acme', productId: 'reports', featureId: 'export' })).data?.result?.remainingUsageUnits?.exports === 4);
	const newSub = await rest(ID, KEY, 'POST', '/subscriptions', { subscriptionId: 'via-rest', status: 'ACTIVE', plans: { reports: 'free' } });
	check('REST creates a subscription', newSub.status === 200, newSub.text);
	check('REST reads it back', (await rest(ID, KEY, 'GET', '/subscriptions/via-rest')).data?.subscriptionId === 'via-rest');
	check('REST changes its status', (await rest(ID, KEY, 'PATCH', '/subscriptions/via-rest/status', { status: 'CANCELLED' })).status === 200);
	check('REST checks entitlement', (await rest(ID, KEY, 'GET', '/subscriptions/acme/subscription-check/reports/export')).data?.featureAllowed === true);
	check('REST previews limits', (await rest(ID, KEY, 'POST', '/subscriptions/acme/limits-check/reports/export', { exports: 1 })).data?.remainingUsageUnits?.exports === 3);
	check('users are subjects too', (await rest(ID, KEY, 'POST', '/users/u1/usage/reports/export', { units: { exports: 1 }, reduceUnits: false })).status === 200);
	check('REST without a key is refused', (await rest(ID, '', 'GET', '/subscriptions/acme')).status === 401);
	check('an unknown route is 404', (await rest(ID, KEY, 'GET', '/actuator/health')).status === 404);
	check('a product route with a wrong method is 404', (await rest(ID, KEY, 'GET', '/products')).status === 404);
}

console.log('4. Sandboxes cannot see each other');
const other = await call('pmitz_create_sandbox');
const KEY2 = other.data.sandbox;
const ID2 = idOf(KEY2);
{
	check('second sandbox has its own ID', ID2 !== ID);
	check('it does not see the first one\'s subscription', (await call('pmitz_find_subscription', { sandbox: KEY2, subscriptionId: 'acme' })).data?.httpStatus === 404);
	check('it does not see the first one\'s product',
		(await call('pmitz_export_plans', { sandbox: KEY2 })).data?.products?.length === 0);
	const sameIds = { ...product, features: [{ featureId: 'export', limits: [{ type: 'CountLimit', id: 'exports', count: 50 }] }], plans: [{ planId: 'free', includedFeatures: ['export'] }] };
	check('it can use the same product ID', (await call('pmitz_create_product', { sandbox: KEY2, product: sameIds })).data?.httpStatus === 200);
	check('and the same subscription ID',
		(await call('pmitz_create_subscription', { sandbox: KEY2, subscriptionId: 'acme', status: 'ACTIVE', plans: { reports: 'free' } })).data?.httpStatus === 200);
	check('with its own limit of 50',
		(await call('pmitz_get_remaining_limits', { sandbox: KEY2, groupId: 'acme', productId: 'reports', featureId: 'export' })).data?.result?.remainingUsageUnits?.exports === 50);
	check('the first sandbox still has 4 left',
		(await call('pmitz_get_remaining_limits', { sandbox: KEY, groupId: 'acme', productId: 'reports', featureId: 'export' })).data?.result?.remainingUsageUnits?.exports === 4);
	check('one sandbox\'s key does not open another\'s REST address', (await rest(ID, KEY2, 'GET', '/subscriptions/acme')).status === 401);
	check('removing a product in one sandbox leaves the other',
		(await call('pmitz_remove_product', { sandbox: KEY2, productId: 'reports' })).data?.httpStatus === 200
		&& (await call('pmitz_verify_entitlement', { sandbox: KEY, groupId: 'acme', productId: 'reports', featureId: 'export' })).data?.result?.featureAllowed === true);
}

console.log('5. Plans survive a restart of the Pmitz server');
if (process.env.RESTART_PMITZ && !LIVE) {
	execSync(process.env.RESTART_PMITZ, { stdio: 'ignore' });
	await new Promise((resolve) => setTimeout(resolve, 6000));
	const after = await call('pmitz_verify_entitlement', { sandbox: KEY, groupId: 'acme', productId: 'reports', featureId: 'export' });
	check('entitlement still answers from the stored plans', after.data?.result?.featureAllowed === true, after.text);
	check('usage is still there',
		(await call('pmitz_get_remaining_limits', { sandbox: KEY, groupId: 'acme', productId: 'reports', featureId: 'export' })).data?.result?.remainingUsageUnits?.exports === 4);
}
else {
	console.log('  skipped');
}

console.log('6. Limits against abuse');
if (LIVE) {
	console.log('  skipped');
}
else {
	const big = await fetch(BASE + '/mcp', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', id: 1, method: 'ping', params: { pad: 'x'.repeat(70000) } }) }).catch(() => null);
	check('a body over 64 KB is refused', big === null || big.status === 413);
	check('an over-long ID is refused', (await call('pmitz_find_subscription', { sandbox: KEY, subscriptionId: 'x'.repeat(121) })).isError);
	const third = (await call('pmitz_create_sandbox')).data.sandbox;
	let lastProduct = null;
	for (let index = 1; index <= 11; index += 1) {
		lastProduct = await call('pmitz_create_product', { sandbox: third, product: { productId: `p${index}`, features: [], plans: [] } });
	}
	check('the 11th product of a sandbox is refused', lastProduct.isError && /at most 10 products/.test(lastProduct.text), lastProduct.text);
	let refused = 0;
	const burst = await Promise.all(Array.from({ length: 140 }, () => call('pmitz_sandbox_status', { sandbox: third })));
	refused = burst.filter((answer) => answer.isError && /Too many requests/.test(answer.text)).length;
	check('more than 120 requests a minute on one sandbox are refused', refused >= 9, `refused ${refused}`);
	let createRefused = null;
	for (let index = 0; index < 32 && !createRefused; index += 1) {
		const answer = await call('pmitz_create_sandbox');
		if (answer.isError) {
			createRefused = answer;
		}
	}
	check('creating sandboxes is rate limited', createRefused !== null && /Too many sandboxes/.test(createRefused.text));
}

console.log('7. Expiry');
if (LIVE) {
	console.log('  skipped');
}
else {
	await db.query("UPDATE gateway.sandbox SET expires_at = now() - interval '1 minute' WHERE id = $1", [ID]);
	const gone = await call('pmitz_sandbox_status', { sandbox: KEY });
	check('an expired sandbox is refused and says so', gone.isError && /expired/.test(gone.text), gone.text);
	check('its REST address answers 410', (await rest(ID, KEY, 'GET', '/subscriptions/acme')).status === 410);
	await new Promise((resolve) => setTimeout(resolve, 1500));
	await fetch(BASE + '/');
	await new Promise((resolve) => setTimeout(resolve, 2500));
	const prefix = `${ID}--%`;
	const left = await db.query(
		`SELECT (SELECT count(*) FROM gateway.sandbox WHERE id = $1)::int AS sandboxes,
			(SELECT count(*) FROM gateway.product WHERE sandbox_id = $1)::int AS products,
			(SELECT count(*) FROM dbo.subscription WHERE subscription_id LIKE $2)::int AS subscriptions,
			(SELECT count(*) FROM dbo.usage WHERE product_id LIKE $2)::int AS usage`, [ID, prefix]);
	check('its data is deleted', Object.values(left.rows[0]).every((count) => count === 0), JSON.stringify(left.rows[0]));
	check('other sandboxes are untouched', (await call('pmitz_find_subscription', { sandbox: KEY2, subscriptionId: 'acme' })).data?.httpStatus === 200);
}

console.log('8. Nothing internal leaks');
{
	const everything = seen.join('\n');
	check('the internal Pmitz key never appears', LIVE || (INTERNAL_KEY !== '' && !everything.includes(INTERNAL_KEY)));
	check('sandbox prefixes never appear', !everything.includes(`${ID}--`) && !everything.includes(`${ID2}--`));
	check('the internal address never appears', !/127\.0\.0\.1:18080|localhost:8080/.test(everything));
}

await db?.end();
console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed === 0 ? 0 : 1);

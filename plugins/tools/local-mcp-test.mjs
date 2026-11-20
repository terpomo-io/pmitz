// End-to-end test of the local Pmitz MCP server (plugins/pmitz/server/pmitz-mcp.mjs) against a running Pmitz server.
//
// Usage:
//   PMITZ_BASE_URL=http://127.0.0.1:8080 PMITZ_API_KEY=... node plugins/tools/local-mcp-test.mjs
//
// The server must be a disposable development instance: the test creates and removes a product
// and creates subscriptions with a random suffix.

import { spawn } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { fileURLToPath } from 'node:url';

const serverFile = process.argv[2] ?? fileURLToPath(new URL('../pmitz/server/pmitz-mcp.mjs', import.meta.url));
const BASE = process.env.PMITZ_BASE_URL;
const KEY = process.env.PMITZ_API_KEY;
if (!serverFile || !BASE || !KEY) {
	console.error('usage: PMITZ_BASE_URL=... PMITZ_API_KEY=... node local-mcp-test.mjs [pmitz-mcp.mjs]');
	process.exit(2);
}

const suffix = randomBytes(3).toString('hex');
const PRODUCT = `mcp-test-${suffix}`;
const SUB = `mcp-sub-${suffix}`;
let passed = 0;
let failed = 0;
const everythingReceived = [];

function check(name, condition, detail = '') {
	if (condition) {
		passed += 1;
		console.log(`  pass  ${name}`);
	}
	else {
		failed += 1;
		console.log(`  FAIL  ${name} ${detail}`);
	}
}

class Client {
	constructor(env) {
		this.child = spawn(process.execPath, [serverFile], {
			env: { PATH: process.env.PATH, ...env },
			stdio: ['pipe', 'pipe', 'pipe'],
		});
		this.nextId = 1;
		this.waiting = new Map();
		this.buffer = '';
		this.stderr = '';
		this.child.stderr.on('data', (chunk) => { this.stderr += chunk; });
		this.child.stdout.on('data', (chunk) => {
			everythingReceived.push(String(chunk));
			this.buffer += chunk;
			let index;
			while ((index = this.buffer.indexOf('\n')) >= 0) {
				const line = this.buffer.slice(0, index);
				this.buffer = this.buffer.slice(index + 1);
				if (!line.trim()) {
					continue;
				}
				const message = JSON.parse(line);
				const resolve = this.waiting.get(message.id);
				if (resolve) {
					this.waiting.delete(message.id);
					resolve(message);
				}
				else {
					this.unmatched = message;
				}
			}
		});
	}

	request(method, params) {
		const id = this.nextId++;
		return new Promise((resolve, reject) => {
			const timer = setTimeout(() => reject(new Error(`timeout waiting for ${method}`)), 20000);
			this.waiting.set(id, (message) => { clearTimeout(timer); resolve(message); });
			this.child.stdin.write(JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n');
		});
	}

	raw(line) {
		this.child.stdin.write(line + '\n');
	}

	async start() {
		const init = await this.request('initialize', {
			protocolVersion: '2025-06-18', capabilities: {}, clientInfo: { name: 'local-mcp-test', version: '1' },
		});
		this.child.stdin.write(JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' }) + '\n');
		return init;
	}

	async tools() {
		return (await this.request('tools/list', {})).result.tools;
	}

	async call(name, args = {}) {
		const message = await this.request('tools/call', { name, arguments: args });
		const text = message.result?.content?.[0]?.text ?? '';
		let data = text;
		try { data = JSON.parse(text); } catch { /* plain text error */ }
		return { isError: Boolean(message.result?.isError), data, text, raw: message };
	}

	stop() {
		return new Promise((resolve) => {
			this.child.on('exit', resolve);
			this.child.stdin.end();
		});
	}
}

const subject = { groupType: 'subscriptions', groupId: SUB, productId: PRODUCT, featureId: 'seat' };
const product = {
	productId: PRODUCT,
	features: [{ featureId: 'seat', limits: [{ type: 'CountLimit', id: 'seats', count: 2 }] }],
	plans: [{ planId: 'starter', includedFeatures: ['seat'] }, { planId: 'viewer', includedFeatures: [] }],
};

console.log('1. Not configured');
{
	const client = new Client({});
	const init = await client.start();
	check('initialize answers with server info', init.result?.serverInfo?.name === 'pmitz');
	check('protocol version echoed', init.result?.protocolVersion === '2025-06-18');
	const tools = await client.tools();
	check('all 11 tools are listed', tools.length === 11, `(${tools.length})`);
	const status = await client.call('pmitz_connection_status');
	check('status reports missing settings', status.data.configurationProblems?.length === 2);
	const read = await client.call('pmitz_find_subscription', { subscriptionId: 'x' });
	check('read tool refuses when not configured', read.isError && /not configured/.test(read.text));
	await client.stop();
}

console.log('2. Default: all tools');
{
	const client = new Client({ PMITZ_BASE_URL: BASE + '/', PMITZ_API_KEY: KEY });
	await client.start();
	const tools = await client.tools();
	check('all 11 tools are listed', tools.length === 11, `(${tools.length})`);
	check('write tools are not marked read-only', tools.filter((t) => !t.annotations.readOnlyHint).length === 6);
	check('destructive tools are marked', ['pmitz_remove_product', 'pmitz_update_subscription_status', 'pmitz_reduce_usage']
		.every((name) => tools.find((t) => t.name === name).annotations.destructiveHint === true));
	const status = await client.call('pmitz_connection_status');
	check('status: reachable, not read-only', status.data.reachable === true && status.data.readOnly === false);

	const created = await client.call('pmitz_create_product', { product });
	check('create product', !created.isError && created.data.httpStatus === 200, created.text.slice(0, 120));
	const duplicate = await client.call('pmitz_create_product', { product });
	check('duplicate product reports a conflict', duplicate.isError && duplicate.data.httpStatus === 409);
	const sub = await client.call('pmitz_create_subscription', { subscriptionId: SUB, status: 'ACTIVE', plans: { [PRODUCT]: 'starter' } });
	check('create subscription', !sub.isError && sub.data.httpStatus === 200, sub.text.slice(0, 120));
	const viewer = await client.call('pmitz_create_subscription', { subscriptionId: SUB + '-viewer', status: 'ACTIVE', plans: { [PRODUCT]: 'viewer' } });
	check('create second subscription on a plan without the feature', !viewer.isError);

	const found = await client.call('pmitz_find_subscription', { subscriptionId: SUB });
	check('find subscription', found.data.result?.subscriptionId === SUB && found.data.result?.status === 'ACTIVE');
	const missing = await client.call('pmitz_find_subscription', { subscriptionId: 'does-not-exist-' + suffix });
	check('missing subscription reports 404', missing.isError && missing.data.httpStatus === 404);
	const entitled = await client.call('pmitz_verify_entitlement', subject);
	check('entitlement allowed on the starter plan', entitled.data.result?.featureAllowed === true, entitled.text.slice(0, 160));
	const denied = await client.call('pmitz_verify_entitlement', { ...subject, groupId: SUB + '-viewer' });
	check('entitlement denied on the viewer plan', denied.data.result?.featureAllowed === false, denied.text.slice(0, 160));

	const preview = await client.call('pmitz_check_limits', { ...subject, units: { seats: 1 } });
	check('preview: available, 1 would remain', preview.data.result?.featureStatus === 'AVAILABLE' && preview.data.result?.remainingUsageUnits?.seats === 1);
	const first = await client.call('pmitz_record_usage', { ...subject, units: { seats: 1 } });
	const second = await client.call('pmitz_record_usage', { ...subject, units: { seats: 1 } });
	check('two units recorded', first.data.httpStatus === 200 && second.data.httpStatus === 200);
	const third = await client.call('pmitz_record_usage', { ...subject, units: { seats: 1 } });
	check('third unit refused with 422', third.isError && third.data.httpStatus === 422);
	const remaining = await client.call('pmitz_get_remaining_limits', subject);
	check('remaining is 0', remaining.data.result?.remainingUsageUnits?.seats === 0, remaining.text.slice(0, 160));
	const reduced = await client.call('pmitz_reduce_usage', { ...subject, units: { seats: 1 } });
	const afterReduce = await client.call('pmitz_get_remaining_limits', subject);
	check('reduce frees one unit', reduced.data.httpStatus === 200 && afterReduce.data.result?.remainingUsageUnits?.seats === 1);
	const notEntitledWrite = await client.call('pmitz_record_usage', { ...subject, groupId: SUB + '-viewer', units: { seats: 1 } });
	check('write for a subject without the feature explains the 500', notEntitledWrite.isError && /not entitled/.test(notEntitledWrite.data.meaning ?? ''));

	const badUnits = await client.call('pmitz_record_usage', { ...subject, units: { seats: -1 } });
	check('negative units rejected before any call', badUnits.isError && /positive integer/.test(badUnits.text));
	const badGroup = await client.call('pmitz_verify_entitlement', { ...subject, groupType: 'admins' });
	check('unknown group type rejected', badGroup.isError && /must be one of/.test(badGroup.text));
	const slashId = await client.call('pmitz_find_subscription', { subscriptionId: '../products/' + PRODUCT });
	check('an ID with slashes cannot change the route', slashId.isError && slashId.data.httpStatus !== 200);
	const unknown = await client.call('pmitz_delete_everything', {});
	check('unknown tool refused', unknown.isError && /Unknown tool/.test(unknown.text));

	const suspended = await client.call('pmitz_update_subscription_status', { subscriptionId: SUB, status: 'SUSPENDED' });
	const afterSuspend = await client.call('pmitz_verify_entitlement', subject);
	check('suspending a subscription removes entitlement', suspended.data.httpStatus === 200 && afterSuspend.data.result?.featureAllowed === false);
	await client.call('pmitz_update_subscription_status', { subscriptionId: SUB, status: 'ACTIVE' });

	const ping = await client.request('ping', {});
	check('ping answered', ping.result && Object.keys(ping.result).length === 0);
	const unknownMethod = await client.request('resources/list', {});
	check('unknown method returns -32601', unknownMethod.error?.code === -32601);
	client.raw('{not json');
	await new Promise((resolve) => setTimeout(resolve, 300));
	check('malformed input returns a parse error', client.unmatched?.error?.code === -32700);
	await client.stop();
}

console.log('2b. Plan override');
{
	const client = new Client({ PMITZ_BASE_URL: BASE, PMITZ_API_KEY: KEY });
	await client.start();
	const overrideProduct = {
		productId: PRODUCT + '-plans',
		features: [{ featureId: 'export', limits: [{ type: 'CountLimit', id: 'exports', count: 2 }] }],
		plans: [
			{ planId: 'free', includedFeatures: ['export'] },
			{ planId: 'pro', includedFeatures: ['export'], limitsOverride: [{ type: 'CountLimit', id: 'exports', count: 7 }] },
		],
	};
	await client.call('pmitz_create_product', { product: overrideProduct });
	await client.call('pmitz_create_subscription', { subscriptionId: SUB + '-pro', status: 'ACTIVE', plans: { [overrideProduct.productId]: 'pro' } });
	await client.call('pmitz_create_subscription', { subscriptionId: SUB + '-free', status: 'ACTIVE', plans: { [overrideProduct.productId]: 'free' } });
	const pro = { groupType: 'subscriptions', groupId: SUB + '-pro', productId: overrideProduct.productId, featureId: 'export' };
	const free = { ...pro, groupId: SUB + '-free' };
	const proStart = await client.call('pmitz_get_remaining_limits', pro);
	check('pro plan shows its override of 7', proStart.data.result?.remainingUsageUnits?.exports === 7, proStart.text.slice(0, 200));
	const freeStart = await client.call('pmitz_get_remaining_limits', free);
	check('free plan shows the default of 2', freeStart.data.result?.remainingUsageUnits?.exports === 2);
	await client.call('pmitz_record_usage', { ...pro, units: { exports: 1 } });
	const proAfter = await client.call('pmitz_get_remaining_limits', pro);
	const proPreview = await client.call('pmitz_check_limits', { ...pro, units: { exports: 1 } });
	check('pro plan shows 6 after one export', proAfter.data.result?.remainingUsageUnits?.exports === 6);
	check('remaining and preview agree', proPreview.data.result?.remainingUsageUnits?.exports === 5);
	check('reading the allowance recorded nothing', (await client.call('pmitz_get_remaining_limits', pro)).data.result?.remainingUsageUnits?.exports === 6);
	await client.call('pmitz_record_usage', { ...free, units: { exports: 2 } });
	const freeFull = await client.call('pmitz_get_remaining_limits', free);
	check('a used-up allowance shows 0 and limitReached', freeFull.data.result?.remainingUsageUnits?.exports === 0 && freeFull.data.result?.limitReached === true && freeFull.data.result?.featureStatus === 'AVAILABLE');
	await client.call('pmitz_remove_product', { productId: overrideProduct.productId });
	await client.stop();
}

console.log('3. Read-only mode');
{
	const client = new Client({ PMITZ_BASE_URL: BASE, PMITZ_API_KEY: KEY, PMITZ_MCP_READ_ONLY: 'true' });
	await client.start();
	const tools = await client.tools();
	check('only the 5 read tools are listed', tools.length === 5 && tools.every((t) => t.annotations.readOnlyHint === true), `(${tools.length})`);
	const status = await client.call('pmitz_connection_status');
	check('status: reachable, read-only', status.data.reachable === true && status.data.readOnly === true);
	const remaining = await client.call('pmitz_get_remaining_limits', subject);
	check('reads work', !remaining.isError && remaining.data.result?.remainingUsageUnits?.seats === 1);
	const write = await client.call('pmitz_record_usage', { ...subject, units: { seats: 1 } });
	check('a hidden write tool is refused when called directly', write.isError && /read-only mode/.test(write.text));
	const after = await client.call('pmitz_get_remaining_limits', subject);
	check('the refused write changed nothing', after.data.result?.remainingUsageUnits?.seats === 1);
	const remove = await client.call('pmitz_remove_product', { productId: PRODUCT });
	check('remove product refused in read-only mode', remove.isError && /read-only mode/.test(remove.text));
	await client.stop();
}

console.log('4. Read-only setting variants');
{
	for (const value of ['1', 'TRUE', 'on']) {
		const client = new Client({ PMITZ_BASE_URL: BASE, PMITZ_API_KEY: KEY, PMITZ_MCP_READ_ONLY: value });
		await client.start();
		check(`"${value}" turns read-only mode on`, (await client.tools()).length === 5);
		await client.stop();
	}
	for (const value of ['', 'false', '${user_config.pmitz_read_only}']) {
		const client = new Client({ PMITZ_BASE_URL: BASE, PMITZ_API_KEY: KEY, PMITZ_MCP_READ_ONLY: value });
		await client.start();
		check(`"${value}" leaves all tools on`, (await client.tools()).length === 11);
		await client.stop();
	}
	const client = new Client({ PMITZ_BASE_URL: BASE, PMITZ_API_KEY: KEY, PMITZ_MCP_READ_ONLY: 'true' });
	await client.start();
	const write = await client.call('pmitz_remove_product', { productId: PRODUCT });
	check('write refused', write.isError && /read-only mode/.test(write.text));
	await client.stop();
}

console.log('5. Wrong API key and unreachable server');
{
	const wrongKey = new Client({ PMITZ_BASE_URL: BASE, PMITZ_API_KEY: 'wrong-key-' + suffix });
	await wrongKey.start();
	const rejected = await wrongKey.call('pmitz_find_subscription', { subscriptionId: SUB });
	check('wrong key reported as rejected', rejected.isError && /rejected the API key/.test(rejected.data.meaning ?? ''), rejected.text.slice(0, 120));
	await wrongKey.stop();

	const unreachable = new Client({ PMITZ_BASE_URL: 'http://127.0.0.1:9', PMITZ_API_KEY: KEY });
	await unreachable.start();
	const down = await unreachable.call('pmitz_find_subscription', { subscriptionId: SUB });
	check('unreachable server reported clearly', down.isError && /Could not reach/.test(down.text));
	await unreachable.stop();

	const badUrl = new Client({ PMITZ_BASE_URL: 'http://user:pass@127.0.0.1:8080', PMITZ_API_KEY: KEY });
	await badUrl.start();
	const refused = await badUrl.call('pmitz_find_subscription', { subscriptionId: SUB });
	check('a URL with credentials is refused', refused.isError && /not configured/.test(refused.text));
	await badUrl.stop();
}

console.log('6. Cleanup and secrets');
{
	const client = new Client({ PMITZ_BASE_URL: BASE, PMITZ_API_KEY: KEY });
	await client.start();
	const removed = await client.call('pmitz_remove_product', { productId: PRODUCT });
	check('remove product', removed.data.httpStatus === 200);
	const gone = await client.call('pmitz_verify_entitlement', subject);
	check('entitlement denied after the product is removed', gone.data.result?.featureAllowed === false);
	await client.stop();
	check('nothing was written to stderr', client.stderr === '', client.stderr.slice(0, 200));
}
check('the API key never appears in any output of the server', !everythingReceived.join('').includes(KEY));

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed === 0 ? 0 : 1);

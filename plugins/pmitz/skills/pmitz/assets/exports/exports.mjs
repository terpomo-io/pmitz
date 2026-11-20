import { pathToFileURL } from 'node:url';

export async function exportCsv(workspaceId) {
  const base = process.env.PMITZ_BASE_URL;
  const key = process.env.PMITZ_API_KEY;
  if (!base || !key) throw new Error('Configure PMITZ_BASE_URL and PMITZ_API_KEY on the backend');
  const subject = base.replace(/\/+$/, '') + '/subscriptions/' + encodeURIComponent(workspaceId);
  const post = (action, body) => fetch(subject + '/' + action + '/reports/export', {
    method: 'POST',
    headers: { 'X-Api-Key': key, 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(5000),
    redirect: 'error',
  });
  const units = { 'export-credits': 1 };
  const preview = await post('limits-check', units);
  if (!preview.ok) throw new Error('Pmitz preview returned HTTP ' + preview.status);
  const { featureStatus } = await preview.json();
  if (featureStatus === 'NOT_ALLOWED') return { status: 403, body: 'Plan excludes exports' };
  if (featureStatus === 'LIMIT_EXCEEDED') return { status: 429, body: 'Quota exhausted' };
  if (featureStatus !== 'AVAILABLE') throw new Error('Unexpected Pmitz decision');
  const record = await post('usage', { units, reduceUnits: false });
  if (record.status === 422) return { status: 429, body: 'Quota exhausted' };
  if (!record.ok) throw new Error('Pmitz record returned HTTP ' + record.status);
  return { status: 200, body: 'name,total\nAda,42' };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  if (!process.argv[2]) throw new Error('Supply a workspace subscription ID');
  const result = await exportCsv(process.argv[2]);
  console.log(result.status, result.body);
}

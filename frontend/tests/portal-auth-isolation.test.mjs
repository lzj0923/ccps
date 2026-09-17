import assert from 'node:assert/strict';
import test from 'node:test';

import { createServer } from 'vite';

test('frontend uses portal-specific session endpoints and never crosses portal redirects', async () => {
  const vite = await createServer({ root: process.cwd(), server: { middlewareMode: true }, appType: 'custom' });
  const calls = [];
  const previousFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    calls.push({ url: String(url), method: options?.method || 'GET' });
    return new Response(JSON.stringify({ id: 1, role: 'ADMIN' }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  };

  try {
    const api = await vite.ssrLoadModule('/src/services/propertyApi.js');
    const routes = await vite.ssrLoadModule('/src/routeInterceptors.js');

    await api.fetchSession('admin');
    await api.fetchSession('owner');
    await api.logout('owner');

    assert.deepEqual(calls.map(call => [call.url, call.method]), [
      ['/api/auth/admin/session', 'GET'],
      ['/api/auth/owner/session', 'GET'],
      ['/api/auth/owner/logout', 'POST']
    ]);
    assert.equal(routes.adminAuthInterceptor({ mode: 'admin' }, { role: 'OWNER' }), '/admin/login');
    assert.equal(routes.ownerAuthInterceptor({ mode: 'owner' }, { role: 'ADMIN' }), '/owner/login');
  } finally {
    globalThis.fetch = previousFetch;
    await vite.close();
  }
});

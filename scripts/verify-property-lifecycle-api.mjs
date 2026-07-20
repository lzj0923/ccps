import assert from 'node:assert/strict';

const apiBase = process.argv[2] || 'http://localhost:8080/api';

const login = async (portal, identifier) => {
  const response = await fetch(`${apiBase}/auth/${portal}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ identifier, password: '123456', rememberMe: false })
  });
  assert.equal(response.status, 200, `${portal}/${identifier} login failed`);
  const cookie = response.headers.get('set-cookie')?.split(';', 1)[0];
  assert.ok(cookie, `${portal}/${identifier} did not receive a session cookie`);
  return cookie;
};

const getJson = async (path, cookie) => {
  const response = await fetch(`${apiBase}${path}`, { headers: { Cookie: cookie } });
  assert.equal(response.status, 200, `${path} returned ${response.status}`);
  return response.json();
};

const adminCookie = await login('admin', 'admin');
const ownerCookie = await login('owner', 'owner3');
const owners = await getJson('/admin/owners', adminCookie);
const properties = owners.flatMap(owner => owner.properties || []);

assert.ok(properties.length > 0, 'admin owners response has no properties');
assert.ok(properties.every(property => ['PRE_HANDOVER', 'OPERATING', 'DISPOSED'].includes(property.assetStage)));
assert.ok(properties.every(property => Array.isArray(property.services)));
assert.ok(properties.filter(property => property.assetStage === 'PRE_HANDOVER').every(property => property.services.length === 0));
assert.ok(properties.filter(property => property.assetStage === 'OPERATING').every(property => property.paymentStatus === 'not_applicable'));

const dashboard = await getJson('/owner/dashboard', ownerCookie);
assert.ok((dashboard.properties || []).every(property => property.assetStage === 'OPERATING'));
assert.ok((dashboard.properties || []).every(property => Array.isArray(property.services)));
assert.ok((dashboard.properties || []).every(property => typeof property.reserveBalance === 'number'));
assert.ok((dashboard.properties || []).every(property => typeof property.monthlyIncome === 'number'));
assert.ok((dashboard.properties || []).every(property => typeof property.monthlyExpense === 'number'));
assert.ok((dashboard.properties || []).every(property => Number.isInteger(property.pendingMaintenanceCount)));
assert.ok((dashboard.properties || []).some(property => property.services.includes('RENTAL') && property.tenantName));
assert.ok(Number.isInteger(dashboard.summary?.pendingMaintenanceCount));

const ownerOnAdmin = await fetch(`${apiBase}/admin/owners`, { headers: { Cookie: ownerCookie } });
const adminOnOwner = await fetch(`${apiBase}/owner/dashboard`, { headers: { Cookie: adminCookie } });
assert.notEqual(ownerOnAdmin.status, 200, 'owner session accessed the admin API');
assert.notEqual(adminOnOwner.status, 200, 'admin session accessed the owner API');

const stageCounts = Object.fromEntries(['PRE_HANDOVER', 'OPERATING', 'DISPOSED'].map(stage => [
  stage,
  properties.filter(property => property.assetStage === stage).length
]));
const operatingSample = dashboard.properties?.find(property => property.services.includes('RENTAL')) || dashboard.properties?.[0];
console.log(JSON.stringify({
  stageCounts,
  owner3PropertyCount: dashboard.properties?.length || 0,
  operatingSample: operatingSample && {
    unitNo: operatingSample.unitNo,
    services: operatingSample.services,
    tenantName: operatingSample.tenantName,
    monthlyRent: operatingSample.monthlyRent,
    currentMonthRentPaid: operatingSample.currentMonthRentPaid,
    reserveBalance: operatingSample.reserveBalance,
    monthlyIncome: operatingSample.monthlyIncome,
    monthlyExpense: operatingSample.monthlyExpense,
    pendingMaintenanceCount: operatingSample.pendingMaintenanceCount
  }
}));
console.log('Property lifecycle API checks passed.');

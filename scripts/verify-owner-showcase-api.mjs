import assert from 'node:assert/strict';

const apiBase = process.argv[2] || 'http://localhost:8080/api';

const loginResponse = await fetch(`${apiBase}/auth/owner/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ identifier: 'owner', password: '123456', rememberMe: false })
});
assert.equal(loginResponse.status, 200, 'owner login failed');
const cookie = loginResponse.headers.get('set-cookie')?.split(';', 1)[0];
assert.ok(cookie, 'owner login did not return a session cookie');

const dashboardResponse = await fetch(`${apiBase}/owner/dashboard`, {
  headers: { Cookie: cookie }
});
assert.equal(dashboardResponse.status, 200, 'owner dashboard request failed');
const dashboard = await dashboardResponse.json();
const properties = dashboard.properties || [];
const preHandover = properties.filter(property => property.assetStage === 'PRE_HANDOVER');
const operating = properties.filter(property => property.assetStage === 'OPERATING');

assert.equal(properties.length, 7, 'owner should have exactly seven showcase properties');
assert.equal(preHandover.length, 4, 'owner should have four pre-handover properties');
assert.equal(operating.length, 3, 'owner should have three operating properties');
assert.ok(preHandover.every(property => property.purchasePrice > 0), 'all pre-handover purchase prices must be positive');
assert.deepEqual(
  new Set(preHandover.map(property => property.paymentStatus)),
  new Set(['paying', 'due_soon', 'overdue', 'paid']),
  'pre-handover records should cover all four payment states'
);
assert.ok(operating.some(property => property.services.includes('RENTAL') && property.services.includes('MANAGEMENT')));
assert.ok(operating.some(property => property.services.includes('RESALE') && property.services.includes('MANAGEMENT')));
assert.ok(operating.some(property => property.monthlyRent > 0 && property.currentMonthRentPaid > 0));
assert.ok(operating.some(property => property.currentMonthRentOutstanding > 0));
assert.ok(operating.some(property => property.reserveBalance < property.reserveMinimumBalance));
assert.ok(operating.some(property => property.reserveBalance > property.reserveMinimumBalance));
assert.ok(operating.some(property => property.monthlyIncome > 0));
assert.ok(operating.some(property => property.monthlyExpense > 0));
assert.ok(operating.some(property => property.pendingMaintenanceCount > 0));
assert.equal(dashboard.summary?.propertyCount, 7);
assert.ok(dashboard.summary?.monthlyRentIncome > 0);
assert.ok(dashboard.summary?.unpaidPropertyAmount > 0);
assert.ok(dashboard.summary?.reserveBalance > 0);
assert.equal(dashboard.summary?.pendingMaintenanceCount, 2);

console.log(JSON.stringify({
  summary: dashboard.summary,
  propertyStages: { preHandover: preHandover.length, operating: operating.length },
  paymentStates: preHandover.map(({ unitNo, paymentStatus }) => ({ unitNo, paymentStatus })),
  operating: operating.map(property => ({
    unitNo: property.unitNo,
    services: property.services,
    tenantName: property.tenantName,
    monthlyRent: property.monthlyRent,
    rentPaid: property.currentMonthRentPaid,
    rentOutstanding: property.currentMonthRentOutstanding,
    reserveBalance: property.reserveBalance,
    monthlyIncome: property.monthlyIncome,
    monthlyExpense: property.monthlyExpense,
    pendingMaintenanceCount: property.pendingMaintenanceCount
  }))
}, null, 2));
console.log('Owner showcase API checks passed.');

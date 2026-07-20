import assert from 'node:assert/strict';

const apiBase = process.argv[2] || 'http://localhost:8080/api';
const login = await fetch(`${apiBase}/auth/owner/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ identifier: 'owner', password: '123456', rememberMe: false })
});
assert.equal(login.status, 200);
const cookie = login.headers.get('set-cookie')?.split(';', 1)[0];
assert.ok(cookie);

const response = await fetch(`${apiBase}/owner/expenses?startDate=2026-07-01&endDate=2026-08-01`, {
  headers: { Cookie: cookie }
});
assert.equal(response.status, 200);
const body = await response.json();
const maintenanceExpense = (body.expenses || []).find(item => item.workOrderId && item.description === 'Touch-up painting before property viewing');
assert.ok(maintenanceExpense, 'maintenance work order with an amount should appear in expenses');
assert.equal(maintenanceExpense.amount, 980);
assert.equal(maintenanceExpense.paymentStatus, 'unpaid');
console.log(JSON.stringify({ expenseCount: body.expenses.length, maintenanceExpense }, null, 2));
console.log('Expense and maintenance API checks passed.');

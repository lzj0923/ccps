import assert from 'node:assert/strict';

const apiBase = process.argv[2] || 'http://localhost:8080/api';
const password = '123456';
const checks = [
  ['admin', 'admin', 200],
  ['admin', 'admin.ops', 200],
  ['admin', 'admin.finance', 200],
  ['owner', 'owner', 200],
  ['owner', 'owner2', 200],
  ['owner', 'owner3', 200],
  ['owner', 'admin', 403],
  ['admin', 'owner', 403]
];

for (const [portal, identifier, expectedStatus] of checks) {
  const response = await fetch(`${apiBase}/auth/${portal}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ identifier, password, rememberMe: false })
  });
  console.log(`${portal}/${identifier}: ${response.status}`);
  assert.equal(response.status, expectedStatus);
}

console.log('All portal account login checks passed.');

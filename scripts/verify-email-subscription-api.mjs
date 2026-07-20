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

const overviewResponse = await fetch(`${apiBase}/owner/notifications`, { headers: { Cookie: cookie } });
assert.equal(overviewResponse.status, 200);
const overview = await overviewResponse.json();
const email = overview.channels.find(channel => channel.key === 'email');
assert.ok(email, 'email channel missing');
assert.equal(email.status, 'unbound');
assert.equal(overview.channels.some(channel => channel.key === 'whatsapp'), false);

const requestCode = await fetch(`${apiBase}/owner/notifications/email/request-code`, {
  method: 'POST',
  headers: { Cookie: cookie, 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'weiming.tan@example.com' })
});
assert.equal(requestCode.status, 503, 'unconfigured SMTP must not pretend that email was sent');

console.log(JSON.stringify({ channels: overview.channels, unconfiguredSmtpStatus: requestCode.status }, null, 2));
console.log('Email subscription API checks passed.');

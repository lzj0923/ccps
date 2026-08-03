import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminRentalMandateWorkspace.vue', import.meta.url), 'utf8');

test('authorization workspace allows the signer email to be entered explicitly', () => {
  assert.match(source, /v-model\.trim="signingForm\.signerEmail"/);
  assert.match(source, /v-model\.trim="signingForm\.signerName"/);
  assert.match(source, /v-model\.trim="signingForm\.signerEmail"[^>]*type="email"/);
  assert.doesNotMatch(source, /startDocumentSigning\(doc\)\{[^}]*window\.prompt/);
  assert.doesNotMatch(source, /@click="authorizationOpen = true"[^>]*>\{\{ \$t\('tenancy\.generateAuthorization'\) \}\}</);
  assert.match(source, /authorization-document-info[\s\S]*signing-recipient-fields[\s\S]*startDocumentSigning\(authorizationDraft\)/);
});

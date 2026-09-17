import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const source = name => readFileSync(new URL('../src/' + name, import.meta.url), 'utf8');
test('payment detail does not render zero-value summary without installments', () => {
  assert.match(source('components/owner-mobile/OwnerMobileApp.vue'), /v-if="paymentInstallments.length" class="owner-stat-hero is-payment"/);
  assert.match(source('components/owner-mobile/OwnerMobileApp.vue'), /paymentInstallments.length \? paymentSummary.purchasePrice : selectedProperty.purchasePrice/);
});
test('both property card types use authenticated archived covers', () => {
  for (const file of ['OwnerMobileApp.vue', 'OwnerPropertyCards.vue']) {
    assert.match(source('components/owner-mobile/' + file), /<OwnerPropertyCover :property="property"/);
  }
});
test('rental service does not imply an active tenant', () => {
  assert.ok(source('components/owner-mobile/OwnerMobileApp.vue').includes("property.tenantName ? 'occupied' : 'noCurrentLease'"));
});
test('owner mobile must not request the administrator-only property inventory', () => {
  assert.ok(!source('components/owner-mobile/OwnerMobileApp.vue').includes('await fetchProperties('));
});

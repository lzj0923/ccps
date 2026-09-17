import assert from 'node:assert/strict';
import test from 'node:test';
import { createServer } from 'vite';

const server = await createServer({ server: { middlewareMode: true, hmr: false, watch: null }, appType: 'custom' });
const load = async name => (await server.ssrLoadModule(`/src/components/${name}.vue`)).default;
const app = await load('owner-mobile/OwnerMobileApp');
const portal = await load('OwnerMobilePortal');
const cards = await load('owner-mobile/OwnerPropertyCards');
const gallery = await load('owner-mobile/OwnerPhotoGallery');
const row = await load('owner-mobile/OwnerTransactionRow');
const funds = await load('AdminFundOperationsPanel');
await server.close();

test('actual mobile and fund components format API dates without exposing ISO strings', () => {
  for (const component of [app, portal, gallery, row, funds]) {
    assert.equal(component.methods.formatDate('2026-09-11'), '11/09/2026');
    assert.equal(component.methods.formatDate(null), '—');
  }
  for (const component of [portal, gallery, cards]) {
    assert.equal(component.methods.formatMonth('2026-09'), '09/2026');
  }
  assert.equal(app.methods.formatDateTime('2026-09-11T18:30:00'), '11/09/2026 18:30');
});

test('mobile month names already follow the active locale', () => {
  assert.equal(app.methods.monthLabel.call({ $i18n: { locale: 'en' }, cashflowYear: 2026 }, 9), 'Sep');
  assert.notEqual(app.methods.monthLabel.call({ $i18n: { locale: 'zh-CN' }, cashflowYear: 2026 }, 9), 'Sep');
});

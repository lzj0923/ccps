import test from 'node:test';
import assert from 'node:assert/strict';
import { nativePreviewEnabled } from '../src/utils/nativePreview.js';
test('development preview survives navigation and refresh, explicit zero disables it', () => {
  const state = new Map(); const storage = { getItem: k => state.get(k), setItem: (k, v) => state.set(k, v) };
  assert.equal(nativePreviewEnabled(true, '?nativePreview=1', storage), true);
  assert.equal(nativePreviewEnabled(true, '', storage), true);
  assert.equal(nativePreviewEnabled(true, '?nativePreview=0', storage), false);
  assert.equal(nativePreviewEnabled(true, '', storage), false);
  assert.equal(nativePreviewEnabled(false, '?nativePreview=1', storage), false);
});

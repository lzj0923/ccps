import test from 'node:test';
import assert from 'node:assert/strict';

import { appendSignaturePoint, nextSignatureRenderKey, signatureSurface, snapshotSignature, startSignatureStroke } from '../src/utils/signatureCanvas.js';

test('keeps a PNG snapshot after the pointer leaves the signature canvas', () => {
  const canvas = { toDataURL: (format) => format === 'image/png' ? 'data:image/png;base64,signature' : '' };

  assert.equal(snapshotSignature(canvas), 'data:image/png;base64,signature');
});

test('adds each drawn point to the visible signature path', () => {
  assert.equal(appendSignaturePoint('M 12 24', { x: 36, y: 48 }), 'M 12 24 L 36 48');
});

test('stretches the visible signature surface to match pointer coordinates', () => {
  assert.equal(signatureSurface.preserveAspectRatio, 'none');
});

test('creates a fresh visible signature surface after writing finishes', () => {
  assert.equal(nextSignatureRenderKey(4), 5);
});

test('keeps earlier strokes when a signer starts another stroke', () => {
  assert.equal(startSignatureStroke('M 12 24 L 36 48', { x: 60, y: 72 }), 'M 12 24 L 36 48 M 60 72');
});

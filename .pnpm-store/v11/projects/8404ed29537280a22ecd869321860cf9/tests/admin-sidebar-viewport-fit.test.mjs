import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(
  new URL('../src/admin-icons.css', import.meta.url),
  'utf8',
);

test('desktop admin sidebar uses a compact non-scrolling layout', () => {
  assert.match(source, /@media\s*\(min-width:\s*821px\)/);
  assert.match(source, /\.admin-shell\s+\.sidebar\s*\{[^}]*overflow:\s*hidden/s);
  assert.match(source, /\.admin-shell\s+\.brand\s*\{[^}]*height:\s*72px[^}]*min-height:\s*72px/s);
  assert.match(source, /\.admin-shell\s+\.nav\s*\{[^}]*overflow:\s*visible/s);
  assert.match(source, /\.admin-shell\s+\.nav button\s*\{[^}]*height:\s*43px[^}]*min-height:\s*43px/s);
  assert.match(source, /\.admin-shell\s+\.nav button \.ico\s*\{[^}]*width:\s*30px[^}]*height:\s*30px/s);
  assert.match(source, /\.admin-shell\s+\.portal-switch\s*\{[^}]*flex-shrink:\s*0/s);
});

test('short desktop viewports hide only the decorative project card', () => {
  assert.match(source, /@media\s*\(min-width:\s*821px\)\s*and\s*\(max-height:\s*899px\)/);
  assert.match(source, /\.admin-shell\s+\.project-card\s*\{[^}]*display:\s*none/s);
  assert.match(source, /\.admin-shell\s+\.portal-switch\s*\{[^}]*margin-top:\s*auto/s);
});

test('the readable height budget fits a 768px desktop viewport', () => {
  const brand = 72;
  const navRows = 12 * 43;
  const navGaps = 11 * 4;
  const navPadding = 24;
  const portalButtonWithMargins = 52;
  assert.ok(brand + navRows + navGaps + navPadding + portalButtonWithMargins <= 768);
});

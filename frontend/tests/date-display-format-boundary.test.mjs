import test from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { extname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { formatDate, formatDateTime, formatMonth } from '../src/utils/dateFormat.js';

const srcRoot = fileURLToPath(new URL('../src', import.meta.url));
const dateField = /\.(?:date|dueDate|billingMonth|reconciliationMonth|leaseStart|leaseEnd|occurredOn|[A-Za-z0-9_]*(?:Date|At))\b/;
const formatter = /\b(?:formatDate|displayDate|formatDateTime|dateTime|formatTime|formatMonth|monthLabel|leaseDateRange|datePlus)\s*\(/;

function vueFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) return vueFiles(path);
    return extname(entry.name) === '.vue' ? [path] : [];
  });
}

test('shared display formatters use DD/MM/YYYY and MM/YYYY', () => {
  assert.equal(formatDate('2026-08-25'), '25/08/2026');
  assert.equal(formatDateTime('2026-08-25T14:30:59'), '25/08/2026 14:30');
  assert.equal(formatMonth('2026-08'), '08/2026');
});

test('Vue templates do not render raw API date fields', () => {
  const violations = [];
  for (const file of vueFiles(srcRoot)) {
    const source = readFileSync(file, 'utf8');
    for (const match of source.matchAll(/\{\{([^{}]+)\}\}/g)) {
      const expression = match[1].trim();
      if (!dateField.test(expression.replace(/(['"])(?:\\.|(?!\1).)*\1/g, '')) || formatter.test(expression) || expression.startsWith('$t(') || /^(?:copy|reportCopy|navigationCopy)\./.test(expression)) continue;
      violations.push(`${file.slice(srcRoot.length + 1)}: ${expression}`);
    }
  }
  assert.deepEqual(violations, []);
});

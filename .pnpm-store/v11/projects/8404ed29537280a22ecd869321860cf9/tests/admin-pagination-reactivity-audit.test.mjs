import test from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const componentsRoot = fileURLToPath(new URL('../src/components/', import.meta.url));

function vueFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = join(directory, entry.name);
    return entry.isDirectory() ? vueFiles(path) : entry.name.endsWith('.vue') ? [path] : [];
  });
}

function paginationStateNames(source) {
  const names = new Set();
  const patterns = [
    /v-model(?:\.number)?="([A-Za-z_$][\w$]*(?:Page|PageSize))"/g,
    /:disabled="([A-Za-z_$][\w$]*Page)\s*[<>=]/g,
    /@click="(?:go\w*Page\()?([A-Za-z_$][\w$]*Page)(?:\s*[+\-=]|\))/g,
  ];
  for (const pattern of patterns) {
    for (const match of source.matchAll(pattern)) names.add(match[1]);
  }
  return [...names];
}

function hasReactiveDeclaration(source, name) {
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const optionsApiState = new RegExp(`\\b${escaped}\\s*:\\s*`).test(
    source.match(/data\(\)\s*\{[\s\S]*?\n\s*\},\n\s*(?:computed|watch|methods)\s*:/)?.[0] || '',
  );
  const compositionApiState = new RegExp(`\\b(?:const|let)\\s+${escaped}\\s*=\\s*(?:ref|reactive|computed)\\s*\\(`).test(source);
  return optionsApiState || compositionApiState;
}

test('all admin pagination controls use declared reactive state', () => {
  const violations = [];
  for (const file of vueFiles(componentsRoot)) {
    const source = readFileSync(file, 'utf8');
    for (const name of paginationStateNames(source)) {
      if (!hasReactiveDeclaration(source, name)) violations.push(`${file}: ${name}`);
    }
  }
  assert.deepEqual(violations, [], `Undeclared pagination state:\n${violations.join('\n')}`);
});

test('admin pages do not contain hard-coded disabled fake pagers', () => {
  const violations = vueFiles(componentsRoot).filter(file => {
    const source = readFileSync(file, 'utf8');
    return /<button\s+disabled[^>]*>&lt;<\/button>[\s\S]{0,160}<button\s+disabled[^>]*>&gt;<\/button>/.test(source);
  });
  assert.deepEqual(violations, [], `Hard-coded pagination controls:\n${violations.join('\n')}`);
});

test('shared admin pager stays visible whenever a list has records', () => {
  const source = readFileSync(join(componentsRoot, 'AdminListPager.vue'), 'utf8');
  assert.match(source, /v-if="total\s*>\s*0"/);
  assert.doesNotMatch(source, /v-if="total\s*>\s*pageSize"/);
});

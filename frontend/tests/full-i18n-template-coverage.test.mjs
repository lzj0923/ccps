import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { readFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { legacyMessages } from '../src/i18n/legacy.generated.js';

const eslintBin = fileURLToPath(new URL('../node_modules/eslint/bin/eslint.js', import.meta.url));
const frontendRoot = fileURLToPath(new URL('../', import.meta.url));
const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const vueFiles = (directory) => readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
  const path = `${directory}/${entry.name}`;
  return entry.isDirectory() ? vueFiles(path) : entry.name.endsWith('.vue') ? [path] : [];
});
const sourceFiles = (directory) => readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
  const path = `${directory}/${entry.name}`;
  return entry.isDirectory() ? sourceFiles(path) : /\.(?:js|vue)$/.test(entry.name) ? [path] : [];
});

test('all Vue templates keep user-facing text behind i18n keys', () => {
  const result = spawnSync(process.execPath, [
    eslintBin,
    'src',
    '--ext',
    '.js,.vue',
  ], {
    cwd: frontendRoot,
    encoding: 'utf8',
  });

  assert.equal(
    result.status,
    0,
    `i18n lint failed:\n${result.stdout || result.stderr}`,
  );
});

test('secondary interfaces and runtime prompts pass the deep i18n audit', () => {
  const result = spawnSync(process.execPath, ['scripts/audit-i18n.mjs'], {
    cwd: frontendRoot,
    encoding: 'utf8',
  });
  assert.equal(result.status, 0, result.stdout || result.stderr);
  assert.match(result.stdout, /[1-9]\d* 个 dialog/);
  assert.match(result.stdout, /[1-9]\d* 个二级界面容器/);
});

test('runtime labels and API errors use the active locale at presentation boundaries', () => {
  const main = read('../src/main.js');
  const dashboard = read('../src/composables/dashboardViewModel.js');
  const api = read('../src/services/propertyApi.js');

  assert.match(main, /globalProperties\.\$lt = translateLegacyText/);
  assert.match(main, /globalProperties\.\$ltf = translateLegacyTemplate/);
  assert.match(main, /globalProperties\.\$regionName = regionDisplayName/);
  assert.match(dashboard, /localizeLegacyTree\(this\.modules\.find/);
  assert.match(api, /translateLegacyText\(API_ERROR_MESSAGES\.get\(candidate\)\)/);

  const unlocalizedBindings = vueFiles(`${frontendRoot}/src`).flatMap((file) => {
    const source = readFileSync(file, 'utf8');
    const labelCalls = [...source.matchAll(/\{\{\s*((?!\$lt\()[\w$?.]+Label\([^{}]*\))\s*\}\}/g)];
    const errors = [...source.matchAll(/\{\{\s*((?!\$lt\()(?:error|[\w$?.]*Error|errorMessage|loadError|proofError))\s*\}\}/g)];
    return [...labelCalls.filter(match => !/^monthLabel\(/.test(match[1]) || !source.includes('new Intl.DateTimeFormat(this.$i18n.locale')), ...errors].map(match => `${file}: ${match[0]}`);
  });
  assert.deepEqual(unlocalizedBindings, []);
});

test('generated legacy catalogues stay complete and free of translation noise', () => {
  const locales = ['zh-CN', 'zh-TW', 'en'];
  const keySets = locales.map(locale => Object.keys(legacyMessages[locale]).sort());
  assert.deepEqual(keySets[1], keySets[0]);
  assert.deepEqual(keySets[2], keySets[0]);

  for (const locale of locales) {
    assert.equal(Object.values(legacyMessages[locale]).some(value => !String(value).trim()), false);
  }
  assert.equal(Object.values(legacyMessages.en).some(value => /[\u3400-\u9fff]/.test(value)), false);
  const sourceCodePattern = /(?:(?:this|window)\.|showToast\s*\(|=>|\b(?:await|const|let|var)\s+[A-Za-z_$])/u;
  for (const locale of locales) {
    assert.equal(
      Object.values(legacyMessages[locale]).some(value => sourceCodePattern.test(String(value))),
      false,
      `${locale} legacy catalogue must not contain captured source code`,
    );
  }
  assert.equal(
    Object.keys(legacyMessages.en).some(key => legacyMessages.en[key].length > Math.max(120, legacyMessages['zh-CN'][key].length * 6)),
    false,
  );

  const i18nSource = read('../src/i18n/index.js');
  const manualLegacyKeys = new Set([...i18nSource.matchAll(/\b(t_[a-f0-9]{12})\s*:/g)].map(match => match[1]));
  const referencedLegacyKeys = new Set(sourceFiles(`${frontendRoot}/src`).flatMap((file) => (
    [...readFileSync(file, 'utf8').matchAll(/legacy\.(t_[a-f0-9]{12})/g)].map(match => match[1])
  )));
  assert.deepEqual(
    [...referencedLegacyKeys].filter(key => !legacyMessages['zh-CN'][key] && !manualLegacyKeys.has(key)),
    [],
    'every referenced legacy key must exist in the generated or curated catalogue',
  );
});

import assert from 'node:assert/strict';
import { createServer } from 'vite';

const server = await createServer({ server: { middlewareMode: true, hmr: false, watch: null }, appType: 'custom' });
const { i18n } = await server.ssrLoadModule('/src/i18n/index.js');
await server.close();

// Preserve structural assertions while allowing real catalogue-backed labels.
// Never invent labels for missing keys or substitute arbitrary customer data.
export function localizedSource(source, locale) {
  const lookup = key => key.split('.').reduce((value, part) => value?.[part], i18n.global.getLocaleMessage(locale));
  return source.replace(/\bthis\.\$t\('([^']+)'\)|\$t\('([^']+)'\)/g, (call, first, second) => {
    const value = lookup(first || second);
    return typeof value === 'string' ? `'${value.replace(/'/g, "\\'")}'` : call;
  }).replace(/\{\{\s*'([^'\n]*)'\s*\}\}/g, '$1')
    .replace(/:([\w-]+)="'([^'\n]*)'"/g, '$1="$2"');
}

export function matchLocalizedSource(source, pattern, message) {
  const variants = [source, ...['zh-CN', 'zh-TW'].map(locale => localizedSource(source, locale))];
  if (variants.some(value => new RegExp(pattern.source, pattern.flags).test(value))) return;
  assert.match(source, pattern, message);
}

import { createHash } from 'node:crypto';
import { existsSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { pathToFileURL } from 'node:url';
import { baseParse, NodeTypes } from '@vue/compiler-dom';
import { pipeline } from '@huggingface/transformers';
import { Converter } from 'opencc-js';

const root = join(process.cwd(), 'src');
const catalogPath = join(root, 'i18n', 'legacy.generated.js');
const toSimplified = Converter({ from: 'tw', to: 'cn' });
const toTraditional = Converter({ from: 'cn', to: 'tw' });
let zhToEn;
let enToZh;
const textKey = (value) => `t_${createHash('sha1').update(value).digest('hex').slice(0, 12)}`;
const hasChinese = (value) => /[\u3400-\u9fff]/.test(value);
const keepSpace = (value, replacement) => `${value.match(/^\s*/)?.[0] || ''}${replacement}${value.match(/\s*$/)?.[0] || ''}`;
const isMeaningful = (value) => value.trim() && /[\p{L}\p{N}]/u.test(value);
const files = (folder) => readdirSync(folder, { withFileTypes: true }).flatMap((entry) => {
  const target = join(folder, entry.name);
  return entry.isDirectory() ? files(target) : entry.name.endsWith('.vue') ? [target] : [];
});

const legacy = existsSync(catalogPath)
  ? (await import(`${pathToFileURL(catalogPath).href}?cache=${Date.now()}`)).legacyMessages
  : { 'zh-CN': {}, 'zh-TW': {}, en: {} };
const values = new Map();
const replacements = new Map();

function addValue(value) {
  const normalized = value.trim();
  if (!isMeaningful(normalized)) return null;
  const key = textKey(normalized);
  values.set(key, normalized);
  return key;
}

function visit(node, templateStart, fileChanges) {
  if (node.type === NodeTypes.TEXT) {
    const key = addValue(node.content);
    if (key) {
      const start = templateStart + node.loc.start.offset;
      const end = templateStart + node.loc.end.offset;
      fileChanges.push({ start, end, value: keepSpace(node.content, `{{ $t('legacy.${key}') }}`) });
    }
  }
  if (node.props) {
    for (const prop of node.props) {
      if (prop.type !== NodeTypes.ATTRIBUTE || !prop.value || !['title', 'placeholder', 'aria-label', 'alt'].includes(prop.name)) continue;
      const key = addValue(prop.value.content);
      if (!key) continue;
      const start = templateStart + prop.loc.start.offset;
      const end = templateStart + prop.loc.end.offset;
      fileChanges.push({ start, end, value: `:${prop.name}="$t('legacy.${key}')"` });
    }
  }
  for (const child of node.children || []) visit(child, templateStart, fileChanges);
  if (node.branches) for (const branch of node.branches) for (const child of branch.children || []) visit(child, templateStart, fileChanges);
}

function localizeExpression(expression) {
  return expression.replace(/(['"])([^'"\\]*(?:\\.[^'"\\]*)*)\1/g, (full, quote, value) => {
    if (!hasChinese(value) || !isMeaningful(value)) return full;
    const key = addValue(value);
    return key ? `$t('legacy.${key}')` : full;
  });
}

function localizeTemplateExpressions(template, templateStart, fileChanges) {
  const patterns = [/\{\{([\s\S]*?)\}\}/g, /(?:v-[\w-]+|:[\w-]+|@[\w.-]+|#[\w-]+)\s*=\s*"([^"]*)"/g];
  for (const pattern of patterns) {
    for (const match of template.matchAll(pattern)) {
      const expression = match[1];
      const localized = localizeExpression(expression);
      if (localized === expression) continue;
      const expressionStart = match.index + match[0].indexOf(expression);
      fileChanges.push({
        start: templateStart + expressionStart,
        end: templateStart + expressionStart + expression.length,
        value: localized,
      });
    }
  }
}

function localizeUnparsedText(template, templateStart, fileChanges) {
  const pattern = /(^|>)([^<>{]*[\u3400-\u9fff][^<>{]*)(?=<)/g;
  for (const match of template.matchAll(pattern)) {
    const text = match[2];
    const key = addValue(text);
    if (!key) continue;
    const start = templateStart + match.index + match[1].length;
    fileChanges.push({ start, end: start + text.length, value: keepSpace(text, `{{ $t('legacy.${key}') }}`) });
  }
}

for (const file of files(root)) {
  if (file === catalogPath) continue;
  const source = readFileSync(file, 'utf8');
  const openTemplate = source.match(/<template(?:\s[^>]*)?>/);
  const templateEnd = source.lastIndexOf('</template>');
  if (!openTemplate || templateEnd < 0) continue;
  const templateStart = openTemplate.index + openTemplate[0].length;
  const template = source.slice(templateStart, templateEnd);
  const ast = baseParse(template, { onError: () => {} });
  const changes = [];
  visit(ast, templateStart, changes);
  localizeTemplateExpressions(template, templateStart, changes);
  localizeUnparsedText(template, templateStart, changes);
  const nonOverlapping = changes
    .sort((a, b) => a.start - b.start || b.end - a.end)
    .filter((change, index, list) => index === 0 || change.start >= list[index - 1].end);
  if (!nonOverlapping.length) continue;
  let next = source;
  for (const change of nonOverlapping.sort((a, b) => b.start - a.start)) next = `${next.slice(0, change.start)}${change.value}${next.slice(change.end)}`;
  replacements.set(file, next);
}

const missing = [...values].filter(([key]) => !legacy.en[key]);
for (const [key, value] of missing) {
  if (hasChinese(value)) {
    legacy['zh-TW'][key] = toTraditional(value);
    legacy['zh-CN'][key] = toSimplified(value);
  } else {
    legacy.en[key] = value;
  }
}

const englishMissing = missing.filter(([, value]) => hasChinese(value));
const chineseMissing = missing.filter(([, value]) => !hasChinese(value));
async function fillBatches(items, target, apply) {
  const translator = target === 'en'
    ? (zhToEn ||= await pipeline('translation', 'Xenova/opus-mt-zh-en'))
    : (enToZh ||= await pipeline('translation', 'Xenova/opus-mt-en-zh'));
  for (let index = 0; index < items.length; index += 10) {
    const batch = items.slice(index, index + 10);
    const translated = await translator(batch.map(([, value]) => value));
    batch.forEach(([key, original], itemIndex) => apply(key, translated[itemIndex]?.translation_text?.trim() || original));
    process.stdout.write(`Translated ${Math.min(index + batch.length, items.length)}/${items.length} to ${target}\n`);
  }
}
await fillBatches(englishMissing, 'en', (key, value) => { legacy.en[key] = value; });
await fillBatches(chineseMissing, 'zh-CN', (key, value) => {
  legacy['zh-CN'][key] = value;
  legacy['zh-TW'][key] = toTraditional(value);
});

for (const [key, value] of values) {
  legacy['zh-TW'][key] ||= toTraditional(value);
  legacy['zh-CN'][key] ||= toSimplified(value);
  legacy.en[key] ||= value;
}

for (const [file, content] of replacements) writeFileSync(file, content, 'utf8');
const serializedLegacy = JSON.stringify(legacy, (_key, value) => {
  if (typeof value !== 'string') return value;
  return value.replaceAll("{'@'}", '@').replaceAll('@', "{'@'}");
}, 2);
writeFileSync(catalogPath, `// Generated from existing UI text. Run npm run localize:templates after adding new static labels.\nexport const legacyMessages = ${serializedLegacy};\n`, 'utf8');
console.log(`Updated ${replacements.size} templates and ${values.size} localized text entries.`);

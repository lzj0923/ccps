import { createHash } from 'node:crypto';
import { existsSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { pathToFileURL } from 'node:url';
import { baseParse, NodeTypes } from '@vue/compiler-dom';
import { pipeline } from '@huggingface/transformers';
import { Converter } from 'opencc-js';

const root = join(process.cwd(), 'src');
const requestedFiles = process.argv.slice(2).map((value) => value.replaceAll('\\', '/'));
const isTargetFile = (file) => {
  if (!requestedFiles.length) return true;
  const normalized = relative(process.cwd(), file).replaceAll('\\', '/');
  return requestedFiles.some((requested) => normalized === requested || normalized.endsWith(`/${requested}`));
};
const catalogPath = join(root, 'i18n', 'legacy.generated.js');
const toSimplified = Converter({ from: 'tw', to: 'cn' });
const toTraditional = Converter({ from: 'cn', to: 'tw' });
let zhToEn;
let enToZh;
const textKey = (value) => `t_${createHash('sha1').update(value).digest('hex').slice(0, 12)}`;
const hasChinese = (value) => /[\u3400-\u9fff]/.test(value);
const englishCorrections = {
  '智慧经营大屏': 'Smart Operations Dashboard',
  '马来西亚各地区房产规模、出租表现与租金概览': 'Overview of property scale, occupancy and rental income across Malaysia',
  '全国经营概览': 'National Operations Overview',
  '全国单位': 'Total Units',
  '套': 'units',
  '整体出租率': 'Overall Occupancy Rate',
  '出租租金': 'Total Rental Income',
  '平均租金': 'Average Rent',
  '智慧大屏加载失败': 'Failed to load the Smart Dashboard',
  '地区总单位': 'Total Units by Region',
  '地区出租率': 'Occupancy Rate by Region',
  '马来西亚地区经营地图': 'Malaysia Regional Operations Map',
  '点击州属区域查看当地经营数据': 'Select a state to view its operating data',
  '数据库实时统计': 'Live Database Statistics',
  '当前选择': 'Current Selection',
  '州属边界': 'State Boundaries',
  '颜色越深，在管单位越多': 'Darker colours indicate more managed units',
  '当前地区': 'Selected Region',
  '总单位': 'Total Units',
  '出租率': 'Occupancy Rate',
  '总出租租金': 'Total Rental Income',
  '平均出租租金': 'Average Rent',
  '地区总出租租金': 'Total Rental Income by Region',
  '地区总平均出租租金': 'Average Rent by Region',
  '指标区 5': 'Metric Panel 5',
  '指标区 6': 'Metric Panel 6',
  '已预留，等待后续确定展示内容': 'Reserved for future metrics',
  '待配置': 'Pending Configuration',
  '管理总览加载失败': 'Failed to load the Management Dashboard',
  '区域经营概览': 'Regional Operations Overview',
  '个区域 · 数据库实时统计': 'regions · live database statistics',
  '区域': 'Region',
  '在管单位': 'Managed Units',
  '出租单位': 'Occupied Units',
  '入住率': 'Occupancy Rate',
  '月租总额': 'Total Monthly Rent',
  '平均月租': 'Average Monthly Rent',
  '业主备用金': 'Owner Reserve Fund',
  '暂无符合条件的区域数据': 'No matching regional data',
  '未设定联络资料': 'No contact details provided',
  '建案 / 单位': 'Project / Unit',
  '建案／单位': 'Project / Unit',
  '整套房产': 'Entire Property',
  '整租选择“整套房产”；合租请选择具体房间。': 'Select Entire Property for a whole-unit lease, or a specific room for shared rental.',
  '每个房间建立独立租约、账单、押金和附件。': 'Each room has its own lease, bills, deposit and documents.',
  '本次续约会追加到原租约，不会新建租约或覆盖历史合同。': 'This renewal adds a new term to the existing lease without replacing its history.',
  '第': 'Term',
  '租客资料载入失败': 'Failed to load tenant and rent data',
  '租客与租金列表': 'Tenants and Rent',
  '目前没有符合条件的租务资料': 'No matching tenant or rent records',
  '本月及过去月份': 'Current and Previous Months',
  '按当月天数折算': 'Prorated by Days in Month',
  '帐单更新规则': 'Billing Update Rules',
  '会补齐租期内缺少的月份，并重算尚未付款且没有收款记录的帐单；已支付或部分支付帐单不追溯修改。': 'Missing lease months will be created and unpaid bills without collection records will be recalculated. Paid and partially paid bills remain unchanged.',
  '原租约会在生效日前一天结束并保留历史；原租客未结清帐单仍归原租约，新租客使用新的租约编号。': 'The original lease ends one day before the transfer date and remains in history. Its outstanding bills stay with the original tenant, while the new tenant receives a new lease number.',
  '系统会寄出一次性签署连结与电邮验证码。': 'The system will send a one-time signing link and email verification code.',
  '每一期单独保存，不会覆盖原合同；可先续约，之后再补传。': 'Each lease term is stored separately and never overwrites the original contract. Renewal documents may be uploaded later.',
  '开始新租房流程': 'Start New Rental Workflow',
  '完成后会自动选中新房产，继续办理交房、委托、租约和入住。': 'The new property will be selected automatically so the handover, mandate, lease and move-in steps can continue.',
  '选择已有建案': 'Select Existing Project',
  '请选择建案': 'Select a Project',
  '建案编码': 'Project Code',
  '请输入建案名称': 'Enter Project Name',
  '笔记录 · RM': 'records · RM'
};
const cleanModelNoise = (value) => String(value || '')
  .replace(/([_!?%.])\1{3,}[\s\S]*$/u, '')
  .replace(/\s+/g, ' ')
  .trim();
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
  // A targeted run is used to migrate remaining Chinese UI copy without
  // rewriting existing English product names or component-local symbols.
  if (requestedFiles.length && !hasChinese(normalized)) return null;
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

for (const file of files(root).filter(isTargetFile)) {
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
  if (!items.length) return;
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

for (const [key, value] of Object.entries(legacy.en)) {
  legacy.en[key] = englishCorrections[legacy['zh-CN'][key]] || cleanModelNoise(value);
}

for (const [file, content] of replacements) writeFileSync(file, content, 'utf8');
const serializedLegacy = JSON.stringify(legacy, (_key, value) => {
  if (typeof value !== 'string') return value;
  return value.replaceAll("{'@'}", '@').replaceAll('@', "{'@'}");
}, 2);
writeFileSync(catalogPath, `// Generated from existing UI text. Run npm run localize:templates after adding new static labels.\nexport const legacyMessages = ${serializedLegacy};\n`, 'utf8');
console.log(`Updated ${replacements.size} templates and ${values.size} localized text entries.`);

const RULES = [
  { test: value => /第\s*一[个個]月/.test(value) && /不[转轉]/.test(value), text: '第一个月不转' },
  { test: value => /最[后後]\s*一[个個]月/.test(value) && /不[转轉]/.test(value), text: '最后一个月不转' },
  { test: value => /(低于|低於|少于|少於)/.test(value) && /RM\s*300/i.test(value) && /不[转轉]/.test(value), text: '低于RM300 不转' },
];

const stripNumber = value => value.replace(/^\s*\d+\s*[）).、:]?\s*/, '').trim();

export function orderReserveRemarks(value) {
  const source = String(value || '').replace(/\r/g, '').trim();
  if (!source) return '';
  const splitNumberedRules = source
    .replace(/\s+(?=\d+\s*[）).、:]\s*)/g, '\n')
    .replace(/\s+(?=\d+\s+(?:第\s*一[个個]月|最[后後]\s*一[个個]月|(?:低于|低於|少于|少於)\s*RM\s*300))/gi, '\n');
  const entries = splitNumberedRules.split(/\n+/).map(stripNumber).filter(Boolean);
  const recognized = [];
  const other = [];
  for (const entry of entries) {
    const ruleIndex = RULES.findIndex(rule => rule.test(entry));
    if (ruleIndex < 0) other.push(entry);
    else if (!recognized.some(item => item.ruleIndex === ruleIndex)) recognized.push({ ruleIndex });
  }
  if (!recognized.length) return entries.join('\n');
  recognized.sort((a, b) => a.ruleIndex - b.ruleIndex);
  const orderedRules = recognized.map((item, index) => `${index + 1}）${RULES[item.ruleIndex].text}`);
  return [...orderedRules, ...other].join('\n');
}

import fs from 'node:fs';

const component = fs.readFileSync('frontend/src/components/AdminOwnersWorkspace.vue', 'utf8');
const i18n = fs.readFileSync('frontend/src/i18n/index.js', 'utf8');

if (component.includes('legacy.t_db4294e5c095')) {
  throw new Error('新增房产步骤标题仍依赖损坏的旧翻译片段');
}

for (const expected of [
  "'properties.createStepChooseOwner'",
  "'properties.createStepDetails'",
  "createStepChooseOwner: ['步骤 1/2：选择业主'",
  "createStepDetails: ['步骤 2/2：建立房产'"
]) {
  if (!(component + i18n).includes(expected)) {
    throw new Error(`缺少步骤标题：${expected}`);
  }
}

console.log('PASS: 新增房产步骤标题使用完整国际化文案');

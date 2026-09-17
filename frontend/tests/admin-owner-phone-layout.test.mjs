import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const source = readFileSync(fileURLToPath(new URL('../src/components/AdminOwnersWorkspace.vue', import.meta.url)), 'utf8');
const processSource = readFileSync(fileURLToPath(new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url)), 'utf8');

test('新建业主的国家区号与手机号输入框保持独立间距且不互相溢出', () => {
  assert.match(source, /\.owner-phone-entry\{[^}]*column-gap:14px/, '两个输入控件之间必须保留明确间距');
  assert.match(
    source,
    /\.owner-phone-entry>select,\.owner-phone-entry>input\{[^}]*box-sizing:border-box[^}]*min-width:0[^}]*width:100%/,
    '两个控件必须被限制在各自网格列内，避免选择框覆盖手机号输入框'
  );
});

test('新租房流程建立业主时国家选择框不得覆盖手机号输入框', () => {
  assert.match(processSource, /\.process-phone-entry\{[^}]*gap:14px/, '流程表单的两个控件之间必须保留明确间距');
  assert.match(
    processSource,
    /\.process-phone-entry>select,\.process-phone-entry>input\{[^}]*box-sizing:border-box[^}]*min-width:0[^}]*width:100%/,
    '流程表单控件必须限制在各自网格列内，覆盖全局选择框最小宽度'
  );
});

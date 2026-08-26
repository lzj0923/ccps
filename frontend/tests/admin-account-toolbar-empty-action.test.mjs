import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const toolbar = read('../src/components/ModuleToolbar.vue')
const dashboardData = read('../src/data/dashboardData.js')

test('管理员账号没有次要操作文案时不渲染空白按钮', () => {
  assert.match(dashboardData, /adminAccounts[\s\S]*?secondaryAction:\s*['"]['"]/)
  assert.match(toolbar, /showSecondaryAction\(\)\s*\{\s*return Boolean\(String\(this\.toolbarSecondaryAction \|\| ''\)\.trim\(\)\)/)
})

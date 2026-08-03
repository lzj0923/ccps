import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8')

test('切換房產詳情功能時會關閉服務商管理子頁面', () => {
  assert.match(
    source,
    /async selectMenu\(item\)\{this\.vendorManagementOpen=false;this\.activeKey=item\.key;/,
  )
})

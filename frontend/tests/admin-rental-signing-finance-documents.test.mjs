import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const workspace = read('../src/components/AdminRentalSigningWorkspace.vue')
const i18n = read('../src/i18n/index.js')

test('附件签约提供第六个发票与收据入口', () => {
  assert.match(workspace, /key: 'financeDocuments'/)
  assert.match(workspace, /financeDocumentTitle/)
  assert.match(workspace, /openFinanceDocumentPanel/)
})
test('发票与收据二级界面支持选择收支与批量生成', () => {
  assert.match(workspace, /v-if="financeDocumentOpen"/)
  assert.match(workspace, /v-model="financeDocumentSelectedIds"/)
  assert.match(workspace, /financeDocumentType === 'invoice'/)
  assert.match(workspace, /downloadAdminFinanceDocuments\(this\.financeDocumentSelectedIds, this\.financeDocumentType\)/)
  assert.match(i18n, /financeDocumentPanelTitle: \['生成发票或收据'/)
})

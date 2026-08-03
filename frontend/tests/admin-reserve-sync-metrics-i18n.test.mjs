import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const reserve = readFileSync(new URL('../src/components/AdminReserveWorkspace.vue', import.meta.url), 'utf8')
const sync = readFileSync(new URL('../src/components/AdminSyncWorkspace.vue', import.meta.url), 'utf8')
const messages = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8')

test('預備金與 SQL 同步統計卡片使用多語言資源', () => {
  assert.match(reserve, /this\.\$t\(`reserve\.\$\{key\}`/)
  assert.match(sync, /this\.\$t\(`sync\.\$\{key\}`/)
  assert.doesNotMatch(reserve, /label: '預備金總餘額'/)
  assert.doesNotMatch(sync, /label:'待匯出交易'/)
  for (const key of ['totalBalance', 'lowBalanceAccounts', 'monthlyTopups', 'pendingTopups']) assert.match(messages, new RegExp(`${key}:`))
  for (const key of ['pendingExports', 'generatedImportFiles', 'failedTransactions', 'syncBatches']) assert.match(messages, new RegExp(`${key}:`))
})

import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const component = readFileSync(new URL('../src/components/AdminTenancyWorkspace.vue', import.meta.url), 'utf8')
const messages = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8')

test('租客與租金統計卡片使用 tenancy 多語言資源', () => {
  assert.match(component, /this\.\$t\(`tenancy\.\$\{key\}`/)
  assert.doesNotMatch(component, /label: '租客總數'/)
  for (const key of ['totalTenants', 'currentDueRent', 'currentPaidRent', 'currentUnpaidAmount', 'totalUnpaidAmount', 'partialPaymentCount', 'overdueRentCount']) {
    assert.match(messages, new RegExp(`${key}:`))
  }
})

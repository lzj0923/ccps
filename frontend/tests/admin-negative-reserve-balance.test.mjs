import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const maintenance = readFileSync(new URL('../src/components/AdminMaintenanceWorkspace.vue', import.meta.url), 'utf8')
const propertyDetail = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8')
const process = readFileSync(new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url), 'utf8')

test('预备金不足时仍可选择预备金扣款并显示负数余额', () => {
  assert.doesNotMatch(maintenance, /reserveShortage > 0[^\n]+disabled/)
  assert.doesNotMatch(maintenance, /settlementMethod === 'reserve' && this\.reserveShortage > 0/)
  assert.match(maintenance, /reserveAfter\(\) \{ return Number\([^)]+\) - this\.requiredReserveDebit; \}/)

  assert.doesNotMatch(propertyDetail, /handlingInfo\.reserveAccountAvailable \|\| handlingReserveShortage > 0/)
  assert.doesNotMatch(propertyDetail, /settlementMethod==='reserve'&&this\.handlingReserveShortage>0/)
  assert.match(propertyDetail, /handlingReserveAfter\(\)\{return Number\([^)]+\)-this\.handlingRequiredReserve\}/)

  assert.doesNotMatch(process, /reserveAccountAvailable \|\| operationsWorkOrderReserveShortage > 0/)
  assert.doesNotMatch(process, /settlementMethod === 'reserve' && this\.operationsWorkOrderReserveShortage > 0/)
  assert.match(process, /legacy\.t_1921382f0110/)
})

test('选择业主另付时不显示预备金不足提示', () => {
  assert.match(maintenance, /handlingForm\.settlementMethod === 'reserve' && reserveShortage > 0/)
  assert.match(propertyDetail, /handlingForm\.settlementMethod === 'reserve' && handlingReserveShortage > 0/)
  assert.match(process, /operationsWorkOrderHandlingForm\.settlementMethod === 'reserve' && operationsWorkOrderReserveShortage > 0/)
})

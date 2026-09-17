import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const toolbar = readFileSync(new URL('../src/components/ModuleToolbar.vue', import.meta.url), 'utf8')
const messages = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8')

test('管理端主操作按鈕使用多語言資源', () => {
  for (const key of ['addOwner', 'addTenant', 'addExpense', 'addReserveTopup', 'reload', 'addReport']) {
    matchLocalizedSource(toolbar, new RegExp(`ui\\.${key}`))
    matchLocalizedSource(messages, new RegExp(`${key}:`))
  }
  for (const key of ['createProject', 'createPaymentPlan']) {
    matchLocalizedSource(toolbar, new RegExp(`building\\.${key}`))
    matchLocalizedSource(messages, new RegExp(`${key}:`))
  }
  for (const key of ['addLease', 'addMaintenance', 'addReserveDebit']) {
    matchLocalizedSource(toolbar, new RegExp(`ui\\.${key}`))
    matchLocalizedSource(messages, new RegExp(`${key}:`))
  }
})

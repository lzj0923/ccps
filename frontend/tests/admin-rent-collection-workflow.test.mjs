import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const workspace = readFileSync(new URL('../src/components/AdminReminderWorkspace.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8')
const adminTheme = readFileSync(new URL('../src/admin-theme.css', import.meta.url), 'utf8')

test('自动提醒页面包含四阶段租金催缴工作台', () => {
  matchLocalizedSource(workspace, /activeTab === 'collections'/)
  matchLocalizedSource(workspace, /第一封提醒函/)
  matchLocalizedSource(workspace, /第二封提醒函/)
  matchLocalizedSource(workspace, /最终提醒函/)
  matchLocalizedSource(workspace, /终止通知/)
  matchLocalizedSource(workspace, /不(?:会|會)自动结束租约或停用门禁/)
})

test('催缴工作台连接查询发送暂停及恢复接口', () => {
  matchLocalizedSource(api, /fetchAdminRentCollectionWorkflow/)
  matchLocalizedSource(api, /sendAdminRentCollectionStage/)
  matchLocalizedSource(api, /holdAdminRentCollection/)
  matchLocalizedSource(api, /resumeAdminRentCollection/)
})

test('催缴工作台使用独立标签按钮而不是带外框的胶囊条', () => {
  matchLocalizedSource(workspace, /<nav class="reminder-tabs" aria-label="提醒工作区视图">/)
  assert.equal((workspace.match(/:aria-pressed="activeTab ===/g) || []).length, 4)
  matchLocalizedSource(adminTheme, /\.admin-shell \.reminder-tabs\s*\{[^}]*border:0!important/s)
  matchLocalizedSource(adminTheme, /\.admin-shell \.reminder-tabs button\.active b\s*\{[^}]*background:var\(--color-admin-accent\)/s)
})

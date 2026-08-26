import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const workspace = readFileSync(new URL('../src/components/AdminReminderWorkspace.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8')

test('自动提醒页面包含四阶段租金催缴工作台', () => {
  assert.match(workspace, /activeTab === 'collections'/)
  assert.match(workspace, /第一封提醒函/)
  assert.match(workspace, /第二封提醒函/)
  assert.match(workspace, /最终提醒函/)
  assert.match(workspace, /终止通知/)
  assert.match(workspace, /不(?:会|會)自动结束租约或停用门禁/)
})

test('催缴工作台连接查询发送暂停及恢复接口', () => {
  assert.match(api, /fetchAdminRentCollectionWorkflow/)
  assert.match(api, /sendAdminRentCollectionStage/)
  assert.match(api, /holdAdminRentCollection/)
  assert.match(api, /resumeAdminRentCollection/)
})

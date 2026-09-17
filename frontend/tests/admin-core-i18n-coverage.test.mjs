import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const i18n = read('../src/i18n/index.js')
const sidebar = read('../src/components/SidebarNav.vue')
const smartDashboard = read('../src/components/AdminSmartDashboardWorkspace.vue')
const managementDashboard = read('../src/components/AdminDashboardWorkspace.vue')
const rentalWorkflow = read('../src/components/AdminPropertyProcessWorkspace.vue')
const tenancy = read('../src/components/AdminTenancyWorkspace.vue')

test('侧边栏租客管理提供简体、繁体和英文模块名称', () => {
  assert.match(i18n, /adminTenantDirectory: \['租客管理', '租客管理', 'Tenant Management'\]/)
  assert.match(sidebar, /modules\.\$\{module\.id\}\.name/)
})

test('智慧大屏与管理总览的动态统计文字使用 i18n', () => {
  assert.match(smartDashboard, /ui\.regionalManagedUnitsHint/)
  assert.match(smartDashboard, /ui\.regionalAverageRentHint/)
  assert.match(smartDashboard, /ui\.unsetRegion/)
  assert.match(managementDashboard, /ui\.managedProperties/)
  assert.match(managementDashboard, /ui\.propertyLevelSummary/)
})

test('租房流程和租客租金的动态状态使用 i18n，同时保留稳定业务值', () => {
  assert.match(rentalWorkflow, /processCenter\.journeyTenantSetup/)
  assert.match(rentalWorkflow, /handoverCategoryOptions/)
  assert.match(rentalWorkflow, /chooseHandoverChecklistCategory\(category\.value\)/)
  assert.match(tenancy, /uploaded: 'contractUploadedPending'/)
  assert.match(tenancy, /overdue: 'rentOverdue'/)
  assert.match(tenancy, /tenancy\.currentUnpaidLoadFailed/)
})

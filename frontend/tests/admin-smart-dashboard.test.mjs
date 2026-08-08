import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const dashboard = readFileSync(new URL('../src/components/AdminSmartDashboardWorkspace.vue', import.meta.url), 'utf8')
const managementOverview = readFileSync(new URL('../src/components/AdminDashboardWorkspace.vue', import.meta.url), 'utf8')
const metricPanel = readFileSync(new URL('../src/components/SmartDashboardMetricPanel.vue', import.meta.url), 'utf8')
const malaysiaMap = readFileSync(new URL('../src/components/MalaysiaLeafletMap.vue', import.meta.url), 'utf8')
const sidebar = readFileSync(new URL('../src/components/SidebarNav.vue', import.meta.url), 'utf8')
const adminPage = readFileSync(new URL('../src/pages/AdminPage.vue', import.meta.url), 'utf8')
const mapData = JSON.parse(readFileSync(new URL('../src/data/malaysiaStates.geojson', import.meta.url), 'utf8'))

test('智慧大屏以马来西亚地图为核心并展示四项地区指标', () => {
  assert.match(dashboard, /马来西亚地区经营地图/)
  assert.match(dashboard, /title="地区总单位"/)
  assert.match(dashboard, /title="地区出租率"/)
  assert.match(dashboard, /title="地区总出租租金"/)
  assert.match(dashboard, /title="地区总平均出租租金"/)
  assert.match(dashboard, /class="malaysia-map"/)
  assert.match(dashboard, /<MalaysiaLeafletMap/)
  assert.match(malaysiaMap, /tile\.openstreetmap\.org/)
  assert.match(malaysiaMap, /L\.geoJSON\(malaysiaStates/)
  assert.match(malaysiaMap, /layer\.on\('click'/)
  assert.equal(mapData.features.length, 16)
  assert.ok(mapData.features.some(feature => feature.properties.shapeName === 'Kuala Lumpur'))
  assert.match(dashboard, /SmartDashboardMetricPanel/)
  assert.match(metricPanel, /class="metric-list"/)
  assert.doesNotMatch(dashboard, /const MetricRanking = \{/)
})

test('地图使用完整马来西亚实景底图而不是拆分示意图', () => {
  assert.match(malaysiaMap, /zoomSnap: \.25/)
  assert.match(malaysiaMap, /setView\(\[4\.15, 109\.45\], 6\)/)
  assert.doesNotMatch(dashboard, /PENINSULAR MALAYSIA|EAST MALAYSIA/)
  assert.doesNotMatch(dashboard, /geoMercator|geoPath/)
})

test('实景地图压暗马来西亚以外区域并限制拖动范围', () => {
  assert.match(malaysiaMap, /const malaysiaFocusMask/)
  assert.match(malaysiaMap, /malaysiaMaskPane/)
  assert.match(malaysiaMap, /fillOpacity: \.68/)
  assert.match(malaysiaMap, /maxBounds: \[\[-1\.2, 96\], \[10, 122\]\]/)
})

test('指标区五和六保留但不虚构业务数据', () => {
  assert.match(dashboard, /指标区 5/)
  assert.match(dashboard, /指标区 6/)
  assert.match(dashboard, /已预留，等待后续确定展示内容/)
})

test('城市数据在页面端可归并到州属并继续使用真实后台接口', () => {
  assert.match(dashboard, /fetchAdminDashboard/)
  assert.match(dashboard, /findStateByArea\(rawName\)/)
  assert.match(dashboard, /row\.totalRent \/ row\.occupiedCount/)
})

test('智慧大屏是独立一级功能且不会替换管理总览', () => {
  assert.match(sidebar, /class="nav-primary-items"/)
  assert.match(sidebar, /adminPrimaryModules/)
  assert.match(adminPage, /AdminSmartDashboardWorkspace v-if="currentId === 'adminSmartDashboard'"/)
  assert.match(adminPage, /AdminDashboardWorkspace v-else-if="currentId === 'adminDashboard'"/)
  assert.match(managementOverview, /管理总览加载失败/)
  assert.match(managementOverview, /区域经营概览/)
  assert.doesNotMatch(managementOverview, /智慧经营大屏/)
})

import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const dashboard = readFileSync(new URL('../src/components/AdminSmartDashboardWorkspace.vue', import.meta.url), 'utf8')
const managementOverview = readFileSync(new URL('../src/components/AdminDashboardWorkspace.vue', import.meta.url), 'utf8')
const metricPanel = readFileSync(new URL('../src/components/SmartDashboardMetricPanel.vue', import.meta.url), 'utf8')
const malaysiaMap = readFileSync(new URL('../src/components/MalaysiaLeafletMap.vue', import.meta.url), 'utf8')
const sidebar = readFileSync(new URL('../src/components/SidebarNav.vue', import.meta.url), 'utf8')
const adminPage = readFileSync(new URL('../src/pages/AdminPage.vue', import.meta.url), 'utf8')
const router = readFileSync(new URL('../src/router.js', import.meta.url), 'utf8')
const dashboardState = readFileSync(new URL('../src/composables/dashboardState.js', import.meta.url), 'utf8')
const dashboardViewModel = readFileSync(new URL('../src/composables/dashboardViewModel.js', import.meta.url), 'utf8')
const adminTheme = readFileSync(new URL('../src/admin-theme.css', import.meta.url), 'utf8')
const mapData = JSON.parse(readFileSync(new URL('../src/data/malaysiaStates.geojson', import.meta.url), 'utf8'))

test('智慧大屏以马来西亚地图为核心并展示四项地区指标', () => {
  assert.match(dashboard, /legacy\.t_650bc4dc5661/)
  assert.match(dashboard, /legacy\.t_37351f04eaca/)
  assert.match(dashboard, /legacy\.t_e5c25b3d0a49/)
  assert.match(dashboard, /legacy\.t_933e29034eea/)
  assert.match(dashboard, /legacy\.t_45d838d32bec/)
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

test('地区大屏保留独立深色背景且不被管理端白色主题覆盖', () => {
  assert.match(dashboard, /\.smart-board\{[^}]*background:radial-gradient/s)
  assert.doesNotMatch(adminTheme, /\.admin-shell\s+\.smart-board\s*\{[^}]*\bbackground\s*:/s)
})

test('地图标题栏使用大屏专用类且不会套用后台通用白色标题栏', () => {
  assert.match(dashboard, /class="map-heading"/)
  assert.match(dashboard, /\.map-heading\{[^}]*background:linear-gradient/s)
  assert.doesNotMatch(dashboard, /class="panel-heading map-heading"/)
})

test('实景地图压暗马来西亚以外区域并限制拖动范围', () => {
  assert.match(malaysiaMap, /const malaysiaFocusMask/)
  assert.match(malaysiaMap, /malaysiaMaskPane/)
  assert.match(malaysiaMap, /fillOpacity: \.68/)
  assert.match(malaysiaMap, /maxBounds: \[\[-1\.2, 96\], \[10, 122\]\]/)
})

test('指标区五和六保留但不虚构业务数据', () => {
  assert.match(dashboard, /legacy\.t_ae94f2617fef/)
  assert.match(dashboard, /legacy\.t_001f3cc6a187/)
  assert.match(dashboard, /legacy\.t_09e45afc86ed/)
})

test('指标区序号固定显示单个 5 和 6，不进入国际化词库', () => {
  assert.match(dashboard, /class="future-number">5<\/span>/)
  assert.match(dashboard, /class="future-number">6<\/span>/)
  assert.doesNotMatch(dashboard, /t_ac3478d69a3c|t_c1dfd96eea8c/)
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
  assert.match(managementOverview, /ui\.todayWorkbench/)
  assert.match(managementOverview, /ui\.operatingSummary/)
  assert.doesNotMatch(managementOverview, /智慧经营大屏/)
})

test('工作台今日需关注按单套房产归并而不是按地区汇总', () => {
  assert.match(managementOverview, /fetchAdminProperties/)
  assert.match(managementOverview, /fetchAdminReserveOverview/)
  assert.match(managementOverview, /itemsByProperty = new Map\(\)/)
  assert.doesNotMatch(managementOverview, /row\.rentalStatus === 'pending_rental'/)
  assert.doesNotMatch(managementOverview, /isVacant|vacancyAttentionLabel|pendingRentalStatus/)
  assert.match(managementOverview, /Number\(row\.currentBalance \|\| 0\) < 0/)
  assert.match(managementOverview, /Number\(row\.overdueDays \|\| 0\) > 0/)
  assert.match(managementOverview, /row\.confirmationStatus === 'pending'/)
  assert.match(managementOverview, /navigate\(`\/admin\/properties\/\$\{item\.unitId\}`\)/)
  assert.doesNotMatch(managementOverview, /negativeReserveRegions|vacancyRegions|vacancyUnits/)
})

test('工作台实现概念图的四区页面结构', () => {
  assert.match(managementOverview, /class="workbench-top-grid"/)
  assert.match(managementOverview, /class="workbench-bottom-grid"/)
  assert.match(managementOverview, /ui\.needsProcessing/)
  assert.match(managementOverview, /ui\.operatingSummary/)
  assert.match(managementOverview, /ui\.rentCollectionOverview/)
  assert.match(managementOverview, /ui\.propertyActivity/)
  assert.match(managementOverview, /fetchAdminRentFinanceReviews/)
  assert.match(managementOverview, /fetchAdminRentCollections/)
  assert.doesNotMatch(managementOverview, /region-overview-panel/)
})

test('管理员登录及访问后台根地址时默认进入智慧大屏', () => {
  assert.match(router, /path: '\/admin', name: 'admin-home', mode: 'admin', moduleId: 'adminSmartDashboard'/)
  assert.match(router, /path: '\/admin\/dashboard', name: 'admin-dashboard', mode: 'admin', moduleId: 'adminDashboard'/)
  assert.match(dashboardState, /systemMode === 'admin' \? 'adminSmartDashboard' : 'myProperties'/)
  assert.match(dashboardViewModel, /const order = \['adminSmartDashboard', 'adminDashboard'\]/)
})

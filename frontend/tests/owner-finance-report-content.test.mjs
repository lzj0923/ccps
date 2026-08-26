import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const dashboard = readFileSync(new URL('../src/components/OwnerFinanceDashboard.vue', import.meta.url), 'utf8');
const api = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');
const ownerDocumentService = readFileSync(new URL('../../backend/src/main/java/com/ccps/backend/service/OwnerDocumentService.java', import.meta.url), 'utf8');

test('交屋报告和修缮报告展示可预览、下载的附件', () => {
  assert.match(dashboard, /fetchOwnerPropertyHandoverReports/);
  assert.match(dashboard, /fetchOwnerPropertyHandoverReportFile/);
  assert.match(dashboard, /fetchOwnerDocuments/);
  assert.match(dashboard, /fetchOwnerDocumentFile/);
  assert.match(dashboard, /v-for="report in handoverReports"/);
  assert.match(dashboard, /v-for="document in selectedMaintenanceDocuments"/);
  assert.match(dashboard, /reportCopy\.preview/);
  assert.match(dashboard, /reportCopy\.download/);
  assert.match(api, /\/owner\/properties\/\$\{ownerUnitId\}\/handover-reports/);
});

test('服务记录展示维修服务历史而不是财务流水', () => {
  const servicePanel = dashboard.match(/<div v-else id="owner-property-panel-services"[\s\S]*?<\/section><\/div>/)?.[0] || '';
  assert.match(servicePanel, /selectedServiceRecords/);
  assert.match(servicePanel, /workOrderNo/);
  assert.doesNotMatch(servicePanel, /selectedPropertyCashflow/);
  assert.match(dashboard, /serviceHistory\.maintenance/);
});

test('摘要展示房产资料，资讯展示通知中心中属于该房产的通知', () => {
  const summaryPanel = dashboard.match(/<section v-if="propertyTab === 'summary'"[\s\S]*?<\/section>/)?.[0] || '';
  const informationPanel = dashboard.match(/<section v-else-if="propertyTab === 'information'"[\s\S]*?<\/section>/)?.[0] || '';
  assert.match(summaryPanel, /selectedProperty\.projectName/);
  assert.match(summaryPanel, /selectedProperty\.unitNo/);
  assert.match(summaryPanel, /selectedProperty\.areaSqm/);
  assert.doesNotMatch(summaryPanel, /selectedPropertyIncome|selectedPropertyExpense/);
  assert.match(informationPanel, /v-for="item in propertyInformation"/);
  assert.match(informationPanel, /item\.title/);
  assert.match(informationPanel, /item\.body/);
  assert.match(informationPanel, /item\.number/);
  assert.match(dashboard, /展示通知中心里属于当前房产的通知/);
  assert.match(dashboard, /fetchOwnerPropertyInformation/);
  assert.match(api, /\/owner\/properties\/\$\{ownerUnitId\}\/information/);
});

test('签署报告文件可从电子签署目录预览和下载', () => {
  assert.match(ownerDocumentService, /electronic-signatures/);
});

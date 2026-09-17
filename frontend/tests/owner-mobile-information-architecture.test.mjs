import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = path => readFile(new URL(path, import.meta.url), 'utf8');

test('业主 App 使用任务导向的五入口信息架构', async () => {
  const [nav, router, page] = await Promise.all([
    read('../src/components/OwnerBottomNav.vue'),
    read('../src/router.js'),
    read('../src/pages/OwnerPage.vue')
  ]);

  for (const label of ['首页', '资产', '租务', '消息', '我的']) {
    assert.match(nav, new RegExp(label));
  }
  assert.match(nav, /owner-bottom-nav__icon/);
  assert.match(nav, /owner-bottom-nav__label/);
  assert.match(nav, /id: 'myProperties'[\s\S]*label: '首页'/);
  const items = [...nav.matchAll(/\{ id: '(\w+)', activeIds:[^\n]+/g)].map(match => match[0]);
  assert.deepEqual(items.map(item => item.match(/id: '(\w+)'/)[1]), [
    'myProperties', 'ownerNotice', 'ownerProjects', 'ownerRentalHub', 'ownerMore'
  ]);
  assert.match(items[2], /primary: true/);
  assert.equal(items.filter(item => item.includes('primary: true')).length, 1);
  assert.match(nav, /:data-state="isActive\(item\) \? 'active' : 'default'"/);
  assert.match(nav, /:aria-label="unreadLabel"/);
  for (const path of ['/owner/projects', '/owner/rental-services', '/owner/more']) {
    assert.match(router, new RegExp(path.replaceAll('/', '\\/')));
  }
  assert.match(page, /OwnerMobileApp/);
});

test('新版移动 App 覆盖资产、租务、收支、租客、合同和房产资料', async () => {
  const portal = await read('../src/components/owner-mobile/OwnerMobileApp.vue');

  // UI copy is now localized; assert the actual section bindings rather than old inline strings.
  for (const section of ['totalAssetValue', 'OwnerPropertyCards', 'paymentInstallments', 'rentalProperties', 'monthlyBars', 'exportCashflow', 'selectedLeaseId', 'activeLease.leaseNo', 'activeLease.contractDocumentId', 'OwnerPhotoGallery', 'propertyDetail.bankAccount', 'propertyDetail.mandates']) assert.ok(portal.includes(section), section);
  assert.match(portal, /OwnerAccountSettings/);
  assert.match(portal, /OwnerProofSubmission/);
  assert.doesNotMatch(portal, /提交报修|新增房产|审批付款/);
});

test('马来西亚业主端不使用海外房产称呼', async () => {
  const [portal, cards] = await Promise.all([
    read('../src/components/owner-mobile/OwnerMobileApp.vue'),
    read('../src/components/owner-mobile/OwnerPropertyCards.vue')
  ]);

  assert.doesNotMatch(`${portal}\n${cards}`, /海外房产/);
  assert.match(portal, /名下房产/);
});

test('业主房产详情接口限定当前登录业主并返回只读扩展资料', async () => {
  const [controller, mapper, response] = await Promise.all([
    read('../../backend/src/main/java/com/ccps/backend/controller/OwnerPropertyDetailController.java'),
    read('../../backend/src/main/java/com/ccps/backend/mapper/OwnerPropertyDetailMapper.java'),
    read('../../backend/src/main/java/com/ccps/backend/dto/OwnerPropertyDetailResponse.java')
  ]);

  assert.match(controller, /\/api\/owner\/properties/);
  assert.match(mapper, /o\.user_id=#\{userId\}/);
  for (const field of ['BankAccount', 'Mandate', 'Lease', 'Photo']) assert.match(response, new RegExp(field));
});

test('业主总览接口提供国家、币种、购买和租约只读字段', async () => {
  const [dto, mapper] = await Promise.all([
    read('../../backend/src/main/java/com/ccps/backend/dto/OwnerDashboardResponse.java'),
    read('../../backend/src/main/java/com/ccps/backend/mapper/OwnerDashboardMapper.java')
  ]);

  for (const field of ['countryCode', 'currency', 'purchaseDate', 'leaseNo', 'leaseStartDate']) {
    assert.match(dto, new RegExp(field));
  }
  for (const column of ['p.country_code', 'pc.currency', 'pc.signed_date', 'l.lease_no', 'l.start_date']) {
    assert.match(mapper, new RegExp(column.replace('.', '\\.')));
  }
});

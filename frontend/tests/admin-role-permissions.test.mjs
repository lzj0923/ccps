import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import {
  canAccessAdminModule,
  canAccessAdminProcessTarget,
  canManageAdminModule,
  defaultAdminModuleId
} from '../src/utils/adminPermissions.js';

const user = role => ({ roles: [role], permissions: [] });
const appPageSource = readFileSync(new URL('../src/pages/AdminPage.vue', import.meta.url), 'utf8');

test('业务管理员只显示指定功能，租客与租金保持只读', () => {
  const business = user('BUSINESS');
  assert.equal(canAccessAdminModule(business, 'adminProperties'), true);
  assert.equal(canAccessAdminModule(business, 'adminTenantDirectory'), true);
  assert.equal(canAccessAdminModule(business, 'adminTenants'), true);
  assert.equal(canManageAdminModule(business, 'adminTenants'), false);
  assert.equal(canAccessAdminModule(business, 'adminFinance'), false);
  assert.equal(canAccessAdminModule(business, 'adminSmartDashboard'), true);
  assert.equal(canAccessAdminModule(business, 'adminDashboard'), true);
  assert.equal(defaultAdminModuleId(business), 'adminSmartDashboard');
});

test('客服管理员只显示租房流程、附件签约、出租委托和预备金', () => {
  const service = user('CUSTOMER_SERVICE');
  for (const moduleId of ['adminProcess', 'adminRentalSigning', 'adminRentalMandates', 'adminReserve']) {
    assert.equal(canAccessAdminModule(service, moduleId), true);
    assert.equal(canManageAdminModule(service, moduleId), true);
  }
  assert.equal(canAccessAdminModule(service, 'adminProperties'), false);
  assert.equal(canAccessAdminModule(service, 'adminAlerts'), false);
  assert.equal(canAccessAdminModule(service, 'adminSmartDashboard'), true);
  assert.equal(canAccessAdminModule(service, 'adminDashboard'), true);
  assert.equal(defaultAdminModuleId(service), 'adminSmartDashboard');
  assert.equal(canAccessAdminProcessTarget(service, 'adminProperties', 'rental-process'), true);
  assert.equal(canAccessAdminProcessTarget(service, 'adminTenantDirectory', 'rental-process'), true);
  assert.equal(canAccessAdminProcessTarget(service, 'adminProperties', 'direct'), false);
});

test('财务管理员可查询全部模块，只能管理财务、运营工具和系统管理', () => {
  const finance = user('FINANCE');
  for (const moduleId of ['adminProperties', 'adminTenants', 'adminFinance', 'adminAlerts', 'adminAccounts']) {
    assert.equal(canAccessAdminModule(finance, moduleId), true);
  }
  assert.equal(canManageAdminModule(finance, 'adminProperties'), false);
  assert.equal(canManageAdminModule(finance, 'adminTenants'), false);
  assert.equal(canManageAdminModule(finance, 'adminFinance'), true);
  assert.equal(canManageAdminModule(finance, 'adminAlerts'), true);
  assert.equal(canManageAdminModule(finance, 'adminAccounts'), true);
});

test('行政管理员和超级管理员拥有全部模块权限', () => {
  for (const role of ['ADMINISTRATION', 'SUPER_ADMIN']) {
    assert.equal(canAccessAdminModule(user(role), 'adminSystemBackup'), true);
    assert.equal(canManageAdminModule(user(role), 'adminProperties'), true);
  }
});

test('流程深链接返回时回到租房流程而不是隐藏模块首页', () => {
  assert.match(appPageSource, /propertyDetailReturnPath/);
  assert.match(appPageSource, /get\('from'\) === 'rental-process'/);
  assert.match(appPageSource, /return '\/admin\/process'/);
});

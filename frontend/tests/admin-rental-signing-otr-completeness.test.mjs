import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');
const i18n = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');
const controller = readFileSync(new URL('../../backend/src/main/java/com/ccps/backend/controller/AdminContractTemplateController.java', import.meta.url), 'utf8');

test('附件签约只保留一张租赁委托书任务卡并复用两页 OTR 生成能力', () => {
  const tasks = source.match(/signingTasks\(\) \{[\s\S]*?\n\s*activeSignerRole\(\)/)?.[0] || '';
  assert.equal((tasks.match(/rentalFiles\.rentalAppointmentTitle/g) || []).length, 1);
  assert.doesNotMatch(tasks, /rentalFiles\.otrTitle/);
  assert.doesNotMatch(tasks, /key: 'rentalAppointment'/);
  matchLocalizedSource(tasks, /key: 'otr'.*rentalFiles\.rentalAppointmentTitle/s);
  matchLocalizedSource(source, /generateAdminContractTemplate\('otr'/);
  matchLocalizedSource(source, /<strong>\{\{ completedSigningSteps \}\} \/ 6<\/strong>/);
  matchLocalizedSource(source, /completedSigningSteps\(\).*this\.otrSigned/s);
  assert.doesNotMatch(source, /completedSigningSteps\(\).*this\.rentalAppointmentSigned/s);
});

test('两页租赁委托书完整校验第二页并支持可选第二业主独立签署', () => {
  for (const field of ['earnestDepositWords', 'commissionWords', 'commissionMonths', 'sstPercent',
    'commissionAmount', 'agencyFeeTotal', 'startDate', 'endDate', 'landlordAddress',
    'witnessAddress']) {
    matchLocalizedSource(source, new RegExp(`OTR_REQUIRED_FIELDS[^;]*${field}`));
  }
  matchLocalizedSource(source, /otrForm\.hasSecondLandlord[\s\S]*SECOND_LANDLORD_REQUIRED_FIELDS/);
  matchLocalizedSource(source, /v-model="otrForm\.hasSecondLandlord"/);
  for (const role of ['tenant', 'tenant_witness', 'owner', 'owner_witness']) {
    matchLocalizedSource(source, new RegExp(`signerRole: '${role}'`));
  }
  matchLocalizedSource(source, /otrForm\.hasSecondLandlord[\s\S]*signerRole: 'second_owner'/);
  matchLocalizedSource(source, /startAdminMandateSignaturePackage\(this\.currentMandate\.id, document\.id/);
});

test('界面及新生成文件不再显示 OTR 名称', () => {
  matchLocalizedSource(i18n, /description: \['集中生成授权书、租赁委托书与租赁合同/);
  matchLocalizedSource(i18n, /rentalAppointmentTitle: \['租赁委托书'/);
  matchLocalizedSource(i18n, /otrTitle: \['租赁委托书'/);
  matchLocalizedSource(i18n, /otr: \['租赁委托书'/);
  matchLocalizedSource(controller, /String documentName = "租赁委托书"/);
  matchLocalizedSource(source, /displayRentalAppointmentFileName\(this\.otrContract/);
  matchLocalizedSource(source, /category === 'otr' \? this\.displayRentalAppointmentFileName/);
  matchLocalizedSource(source, /replace\(\/OTR\\s\*出价函\/gi, '租赁委托书'\)/);
});

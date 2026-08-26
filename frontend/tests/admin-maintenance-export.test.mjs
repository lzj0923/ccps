import assert from 'node:assert/strict';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';

import * as XLSX from 'xlsx';
import { createServer } from 'vite';

test('maintenance export uses the filtered live rows instead of dashboard samples', async () => {
  const outputDirectory = await mkdtemp(join(tmpdir(), 'ccps-maintenance-export-'));
  const previousDirectory = process.cwd();
  const vite = await createServer({ root: previousDirectory, server: { middlewareMode: true }, appType: 'custom' });
  process.chdir(outputDirectory);

  try {
    const { default: dashboardActions } = await vite.ssrLoadModule('/src/composables/dashboardActions.js');
    dashboardActions.methods.exportCsv.call({
      currentId: 'adminMaintenance',
      currentHeaders: ['日期', '房戶/單位', '業主', '操作'],
      filteredRows: [['2025-05-15', 'Pavilion Square / A-28-05', 'STATIC OWNER']],
      adminMaintenanceExportHeaders: ['工單編號', '建案／項目', '單位', '維修項目', '金額（RM）'],
      adminMaintenanceExportRows: [
        ['MWO-20260824170945-903490', '团结小区', '102', '漏水维修', 1],
        ['MWO-20260824170317-A3DE23', 'WhatsApp Overdue Demo Project', 'WA-DEMO-01', '空调维修', 1]
      ],
      adminMaintenanceExportFileName: '维修工单-export.csv',
      showToast() {}
    });

    const bytes = await readFile(join(outputDirectory, '维修工单-export.xlsx'));
    const workbook = XLSX.read(bytes);
    const rows = XLSX.utils.sheet_to_json(workbook.Sheets[workbook.SheetNames[0]], { header: 1 });

    assert.deepEqual(rows[0], ['工單編號', '建案／項目', '單位', '維修項目', '金額（RM）']);
    assert.ok(rows.length > 3);
    assert.equal(rows[1][0], 'MWO-20260824170945-903490');
    assert.equal(typeof rows[1][4], 'number');
    assert.equal(rows[2][2], 'WA-DEMO-01');
    assert.equal(rows.flat().includes('STATIC OWNER'), false);
    for (const label of ['签名栏位：', '行政部门', '行政主管', '财务部门', '财务主管']) {
      assert.equal(rows.flat().includes(label), true);
    }
  } finally {
    process.chdir(previousDirectory);
    await vite.close();
    await rm(outputDirectory, { recursive: true, force: true });
  }
});

test('maintenance workspace maps the active tab filtered rows into export columns', async () => {
  const root = process.cwd();
  const vite = await createServer({ root, server: { middlewareMode: true }, appType: 'custom' });

  try {
    const { default: workspace } = await vite.ssrLoadModule('/src/components/AdminMaintenanceWorkspace.vue');
    const page = {};
    const context = {
      activeTab: 'expense',
      page,
      filteredRows: [{
        unitId: 1, occurredOn: '2026-08-24', projectName: '团结小区', state: 'Selangor', city: 'Shah Alam', unitNo: '102',
        category: 'tax', description: '門牌稅', amount: 1000, reserveDeductedAmount: 1000, paymentStatus: 'paid',
        confirmationStatus: 'confirmed', paymentMethod: 'reserve_account', paymentDate: '2026-08-24', attachmentCount: 1,
        payerName: '税务局', bankName: 'Maybank', paymentAccountNo: '9988', feeAccountKey: 'landTax', transactionNo: 'EXP-20260824-001'
      }],
      categoryLabel: value => `类别:${value}`,
      paymentLabel: value => `付款:${value}`,
      confirmationLabel: value => `财务:${value}`,
      paymentMethodLabel: value => `方式:${value}`,
      maintenanceStatusLabel: value => `工单:${value}`,
      dateTime: value => String(value || '').replace('T', ' '),
      exportPaymentProfiles: {
        1: {
          ownerName: '张业主',
          tenantName: '陈租客',
          profile: { paymentAccountNumbers: { landTax: 'LT-102' } },
          ownerAccounts: [{ itemName: 'Maybank', paymentName: '税务局', accountNo: '9988', branchCode: 'MBBEMYKL', swiftCode: 'MBBEMYKL', bankAddress: 'Kuala Lumpur', transferLimit: 50000, overseasBank: false, overseasTransferFee: 0 }]
        }
      },
      exportPaymentData: workspace.methods.exportPaymentData
    };

    workspace.methods.syncExportRows.call(context);
    assert.equal(page.adminMaintenanceExportFileName, '支出支付报告.xlsx');
    assert.deepEqual(page.adminMaintenanceExportHeaders, ['日期', '行政区', '房产／单位', '业主', '租客', '收支类别', '收支说明', '金额', '支付方式', '付款方', '费用账户号码', '支付账户号码', 'Jompay', 'Ref No.']);
    assert.equal(page.adminMaintenanceExportRows.length, 1);
    assert.equal(page.adminMaintenanceExportRows[0][page.adminMaintenanceExportHeaders.indexOf('房产／单位')], '团结小区 / 102');
    assert.equal(page.adminMaintenanceExportRows[0][page.adminMaintenanceExportHeaders.indexOf('租客')], '陈租客');
    assert.equal(page.adminMaintenanceExportRows[0][page.adminMaintenanceExportHeaders.indexOf('金额')], 1000);
    assert.equal(page.adminMaintenanceExportRows[0][page.adminMaintenanceExportHeaders.indexOf('费用账户号码')], 'LT-102');
    assert.equal(page.adminMaintenanceExportRows[0][page.adminMaintenanceExportHeaders.indexOf('支付账户号码')], '9988');
    assert.equal(page.adminMaintenanceExportRows[0][page.adminMaintenanceExportHeaders.indexOf('Ref No.')], 'EXP-20260824-001');

    context.activeTab = 'maintenance';
    context.filteredRows = [{
      unitId: 1, workOrderNo: 'MWO-20260824170945-903490', projectName: '团结小区', state: 'Selangor', city: 'Shah Alam', unitNo: '102',
      category: 'plumbing', title: '漏水维修', description: '厨房漏水', requestedAt: '2026-08-24T17:09:00',
      completedAt: '2026-08-24T19:43:58', estimatedAmount: 1, amount: 1, status: 'completed', paymentStatus: 'paid',
      confirmationStatus: 'confirmed', reserveDeductedAmount: 0, attachmentCount: 2,
      payerName: '维修商', paymentAccountNo: '9988', feeAccountKey: 'landTax', feeAccountNo: 'LT-102'
    }];
    workspace.methods.syncExportRows.call(context);

    assert.equal(page.adminMaintenanceExportFileName, '维修支付报告.xlsx');
    assert.equal(page.adminMaintenanceExportRows.length, 1);
    assert.equal(page.adminMaintenanceExportRows[0][page.adminMaintenanceExportHeaders.indexOf('收支说明')], '漏水维修：厨房漏水');
    assert.equal(page.adminMaintenanceExportRows[0][page.adminMaintenanceExportHeaders.indexOf('金额')], 1);
    assert.equal(page.adminMaintenanceExportRows[0][page.adminMaintenanceExportHeaders.indexOf('Ref No.')], 'MWO-20260824170945-903490');
    assert.equal(workspace.methods.categoryLabel.call({ $t: key => key }, 'tax'), '税费');
  } finally {
    await vite.close();
  }
});

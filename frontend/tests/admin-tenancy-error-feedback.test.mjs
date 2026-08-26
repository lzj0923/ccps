import assert from 'node:assert/strict';
import test from 'node:test';
import { createServer } from 'vite';

test('修改租约应显示具体异常原因而不是统一操作失败', async () => {
  const vite = await createServer({ server: { middlewareMode: true }, appType: 'custom' });
  try {
    const { localizeApiErrorMessage } = await vite.ssrLoadModule('/src/services/propertyApi.js');
    assert.equal(
      localizeApiErrorMessage('Lease state changed; reload and try again', 409),
      '租约资料已被其他操作修改，请重新载入后再试',
    );
    assert.equal(
      localizeApiErrorMessage('Only active leases can be edited', 409),
      '只有有效租约可以修改，请重新载入确认租约状态',
    );
    const unknown = localizeApiErrorMessage('Unexpected lease validation rule', 422);
    assert.match(unknown, /Unexpected lease validation rule/);
    assert.match(unknown, /HTTP 422/);
    assert.doesNotMatch(unknown, /^操作失敗，請稍後再試/);
  } finally {
    await vite.close();
  }
});

const TENANT_API_ERROR_MESSAGES = new Map([
  ['Tenant identity number already exists', '该证件号已被现有租客使用，请从“选择已有租客”中选择对应租客。'],
  ['Unable to create tenant', '租客资料建立失败，请检查填写内容后重试。'],
]);

export function localizeTenantApiErrorMessage(message) {
  return TENANT_API_ERROR_MESSAGES.get(String(message || '').trim()) || null;
}

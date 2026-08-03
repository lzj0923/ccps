export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const API_ERROR_MESSAGES = new Map([
  ['This contract document is already signed; replace it before starting a new request', '此合同文件已完成簽署，請先更換文件後再發起新的簽署'],
  ['This signing request is already closed', '此簽署邀請已關閉，不能再次操作'],
  ['Contract file is unavailable', '合同文件目前無法取得'],
  ['Signed contract is not available', '已簽署合同目前無法取得'],
  ['Email service is not configured', '電郵服務尚未設定，暫時無法發送簽署邀請'],
  ['Incorrect verification code', '驗證碼不正確'],
  ['Verification code has expired', '驗證碼已過期，請重新取得'],
  ['Consent is required before signing', '簽署前請先勾選同意'],
  ['Signer name does not match the request', '簽署人姓名與邀請資料不一致'],
  ['Only draft or pending mandates can be reviewed', '当前委托不是草稿或待审核状态，请刷新后再试'],
  ['Unable to review rental mandate', '委托审核失败，请确认授权文件已完成签署'],
  ['Current rental mandate does not cover this lease', '当前出租委托未覆盖完整租期，请先调整委托期限或缩短租约'],
  ['Unit already has an overlapping active lease', '该单位已有重叠中的有效租约，请先结束旧租约或调整租期'],
  ['Active tenant not found', '当前租客不存在或未启用，请重新选择租客'],
  ['Operating unit not found', '当前单位尚未进入可出租状态，请先完成房产交接'],
  ['Invalid rent calculation method', '租金计算方式无效，请刷新页面后重试'],
]);

export function localizeApiErrorMessage(message, status = '') {
  const value = String(message || '').trim();
  if (API_ERROR_MESSAGES.has(value)) return API_ERROR_MESSAGES.get(value);
  if (!value || /^API request failed/i.test(value)) return `服務請求失敗${status ? `（HTTP ${status}）` : ''}`;
  if (/^[\x00-\x7F]*[A-Za-z][\x00-\x7F]*$/.test(value)) return '操作失敗，請稍後再試；如問題持續，請聯絡管理員。';
  return value;
}

async function request(path, options = {}) {
  const isFormData = options.body instanceof FormData;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { ...(isFormData ? {} : { 'Content-Type': 'application/json' }), ...(options.headers || {}) },
    credentials: 'include',
    ...options
  });

  if (!response.ok) {
    let message = `API request failed: ${response.status}`;
    try {
      const body = await response.json();
      message = body.message || message;
    } catch {
      // Keep the HTTP status when the server does not return JSON.
    }
    if (response.status === 401 && !path.startsWith('/auth/')) {
      const portal = path.startsWith('/admin/') || path.startsWith('/properties') ? 'admin' : 'owner';
      window.dispatchEvent(new CustomEvent('ccps-auth-expired', { detail: { portal } }));
    }
    throw new Error(localizeApiErrorMessage(message, response.status));
  }

  if (response.status === 204) return null;
  const responseText = await response.text();
  if (!responseText.trim()) return null;
  const contentType = response.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) return responseText;
  try {
    return JSON.parse(responseText);
  } catch {
    throw new Error('服务器返回的数据格式不正确');
  }
}

export function fetchProperties() {
  return request('/properties');
}

export function fetchOwnerDashboard() {
  return request('/owner/dashboard');
}

export function updateOwnerPropertyServices(ownerUnitId, services) {
  return request(`/owner/dashboard/properties/${ownerUnitId}/services`, {
    method: 'PUT',
    body: JSON.stringify({ services })
  });
}

export function fetchAdminOwners() {
  return request('/admin/owners');
}

export function fetchAdminProperties(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  const suffix = query.toString() ? `?${query}` : '';
  return request(`/admin/properties${suffix}`);
}

export function fetchAdminOwnerSummary() {
  return request('/admin/owners/summary');
}

export function fetchAdminBuildingPaymentProgress(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  const suffix = query.toString() ? `?${query}` : '';
  return request(`/admin/buildings/payment-progress${suffix}`);
}

export function createAdminBuildingProject(payload) {
  return request('/admin/buildings/projects', { method: 'POST', body: JSON.stringify(payload) });
}

export function fetchAdminPaymentContracts() {
  return request('/admin/buildings/payment-contracts');
}

export function createAdminPaymentPlan(payload) {
  return request('/admin/buildings/payment-plans', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateAdminPaymentInstallment(installmentId, payload) {
  return request(`/admin/buildings/installments/${installmentId}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function sendAdminPaymentReminder(installmentId) {
  return request(`/admin/buildings/installments/${installmentId}/reminders`, { method: 'POST' });
}

export function fetchAdminReminders() {
  return request('/admin/reminders');
}

export function createAdminReminderRule(payload) {
  return request('/admin/reminders/rules', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateAdminReminderRule(ruleId, payload) {
  return request(`/admin/reminders/rules/${ruleId}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function toggleAdminReminderRule(ruleId, enabled) {
  return request(`/admin/reminders/rules/${ruleId}/enabled?enabled=${enabled}`, { method: 'PUT' });
}

export function deleteAdminReminderRule(ruleId) {
  return request(`/admin/reminders/rules/${ruleId}`, { method: 'DELETE' });
}

export function runAdminReminderRule(ruleId) {
  return request(`/admin/reminders/rules/${ruleId}/run`, { method: 'POST' });
}

export function runAllAdminReminderRules() {
  return request('/admin/reminders/run', { method: 'POST' });
}

export function retryAdminReminderDelivery(deliveryId) {
  return request(`/admin/reminders/deliveries/${deliveryId}/retry`, { method: 'POST' });
}


export function fetchAdminReports(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  return request(`/admin/reports${query.toString() ? `?${query}` : ''}`);
}
export function generateAdminReport(payload) { return request('/admin/reports/runs', { method: 'POST', body: JSON.stringify(payload) }); }
export async function downloadAdminReport(runId) {
  const response = await fetch(`${API_BASE_URL}/admin/reports/runs/${runId}/file`, { credentials: 'include' });
  if (!response.ok) { let message = `API request failed: ${response.status}`; try { message = (await response.json()).message || message; } catch { /* status */ } throw new Error(localizeApiErrorMessage(message, response.status)); }
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function fetchAdminFinanceReviews(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  const suffix = query.toString() ? `?${query}` : '';
  return request(`/admin/finance/reviews${suffix}`);
}

export function fetchAdminFinanceProjects(type = '') {
  return request(`/admin/finance/projects${type ? `?type=${encodeURIComponent(type)}` : ''}`);
}

export function getAdminFinanceDocumentUrl(financeRecordId, documentType) {
  const type = documentType === 'invoice' ? 'invoice' : 'receipt';
  return `${API_BASE_URL}/admin/finance/reviews/${financeRecordId}/document?documentType=${type}`;
}

export async function downloadAdminFinanceDocuments(financeRecordIds, documentType) {
  const type = documentType === 'invoice' ? 'invoice' : 'receipt';
  const response = await fetch(`${API_BASE_URL}/admin/finance/reviews/documents/batch?documentType=${type}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, credentials: 'include',
    body: JSON.stringify(financeRecordIds || [])
  });
  if (!response.ok) throw new Error(`API request failed: ${response.status}`);
  const disposition = response.headers.get('Content-Disposition') || '';
  const match = disposition.match(/filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i);
  return { blob: await response.blob(), filename: decodeURIComponent(match?.[1] || match?.[2] || `${type}-documents.zip`) };
}

export function fetchAdminReserveOverview() {
  return request('/admin/reserve');
}

export function updateAdminReserveSettings(accountId, payload) {
  return request(`/admin/reserve/accounts/${accountId}/settings`, {
    method: 'PUT', body: JSON.stringify(payload)
  });
}

export function createAdminReserveDirectTopup(accountId, payload) {
  return request(`/admin/reserve/accounts/${accountId}/direct-topups`, {
    method: 'POST', body: JSON.stringify(payload)
  });
}

export function createAdminReserveRefund(accountId, payload) {
  return request(`/admin/reserve/accounts/${accountId}/refunds`, {
    method: 'POST', body: JSON.stringify(payload)
  });
}

export function reviewAdminReserveTopup(financeRecordId, approved, note) {
  return request(`/admin/reserve/topups/${financeRecordId}/review`, {
    method: 'POST', body: JSON.stringify({ approved, note })
  });
}

export function batchConfirmAdminReserveTopups(ids, note) {
  return request('/admin/reserve/topups/batch-confirm', {
    method: 'POST', body: JSON.stringify({ ids, note })
  });
}

export async function fetchAdminReserveProof(documentId, download = false) {
  const response = await fetch(`${API_BASE_URL}/admin/reserve/proofs/${documentId}?download=${download}`, { credentials: 'include' });
  if (!response.ok) {
    let message = `API request failed: ${response.status}`;
    try { message = (await response.json()).message || message; } catch { /* Keep status. */ }
    throw new Error(localizeApiErrorMessage(message, response.status));
  }
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function fetchAdminTenancy(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  const suffix = query.toString() ? `?${query}` : '';
  return request(`/admin/tenancy${suffix}`);
}

export function fetchAdminTenancyOptions() { return request('/admin/tenancy/options'); }
export function fetchAdminTenantDirectory(filters = {}) {
  const params = new URLSearchParams(Object.entries(filters).filter(([, value]) => value !== null && value !== undefined && value !== ''));
  return request(`/admin/tenancy/tenants${params.size ? `?${params}` : ''}`);
}
export function fetchAdminTenantDetail(tenantId) { return request(`/admin/tenancy/tenants/${tenantId}`); }
export function fetchAdminLeaseRentInvoices(leaseId) { return request(`/admin/tenancy/leases/${leaseId}/rent-invoices`); }
export function fetchAdminRentalMandates(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && value !== null && value !== '') query.set(key, value); });
  return request(`/admin/rental-mandates${query.toString() ? `?${query}` : ''}`);
}
export function fetchAdminRentalMandateOptions() { return request('/admin/rental-mandates/options'); }
export function createAdminRentalMandate(payload) { return request('/admin/rental-mandates', { method: 'POST', body: JSON.stringify(payload) }); }
export function submitAdminRentalMandate(mandateId) { return request(`/admin/rental-mandates/${mandateId}/submit`, { method: 'POST' }); }
export function reviewAdminRentalMandate(mandateId, payload) { return request(`/admin/rental-mandates/${mandateId}/review`, { method: 'POST', body: JSON.stringify(payload) }); }
export function updateAdminRentalMandateStatus(mandateId, payload) { return request(`/admin/rental-mandates/${mandateId}/status`, { method: 'PUT', body: JSON.stringify(payload) }); }
export function fetchAdminRentalMandateHistory(mandateId) { return request(`/admin/rental-mandates/${mandateId}/history`); }
export function fetchAdminPropertyHandover(mandateId) { return request(`/admin/rental-mandates/${mandateId}/handover`); }
export function saveAdminPropertyHandover(mandateId, payload) { return request(`/admin/rental-mandates/${mandateId}/handover`, { method: 'PUT', body: JSON.stringify(payload) }); }
export function fetchAdminRentalMandateDocuments(mandateId) { return request(`/admin/rental-mandates/${mandateId}/documents`); }
export function uploadAdminRentalMandateDocument(mandateId, relationType, file) { const body = new FormData(); body.append('relationType', relationType); body.append('file', file); return request(`/admin/rental-mandates/${mandateId}/documents`, { method: 'POST', body }); }
export async function downloadAdminRentalMandateDocument(mandateId, documentId) { const response = await fetch(`${API_BASE_URL}/admin/rental-mandates/${mandateId}/documents/${documentId}/file`, { credentials: 'include' }); if (!response.ok) throw new Error(localizeApiErrorMessage('', response.status)); return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' }; }
export function fetchAdminRentFinanceReviews(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && value !== null && value !== '') query.set(key, value); });
  return request(`/admin/tenancy/rent-reviews${query.toString() ? `?${query}` : ''}`);
}
export function fetchAdminRentFinanceProjects() { return request('/admin/tenancy/rent-reviews/projects'); }
export async function fetchAdminRentReceipt(financeRecordId) {
  const response = await fetch(`${API_BASE_URL}/admin/tenancy/rent-payments/${financeRecordId}/receipt`, { credentials: 'include' });
  if (!response.ok) throw new Error(localizeApiErrorMessage('', response.status));
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function getAdminRentReceiptUrl(financeRecordId) {
  return `${API_BASE_URL}/admin/tenancy/rent-payments/${financeRecordId}/receipt`;
}

export function fetchAdminRentCollections(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && value !== null && value !== '') query.set(key, value); });
  return request(`/admin/tenancy/rent-collections${query.toString() ? `?${query}` : ''}`);
}

export function fetchAdminRentCollectionProjects() { return request('/admin/tenancy/rent-collections/projects'); }

export function confirmAdminRentCollection(invoiceId, payload, proof = null) {
  const body = new FormData();
  body.append('payload', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
  if (proof) body.append('proof', proof);
  return request(`/admin/tenancy/rent-invoices/${invoiceId}/confirm`, { method: 'POST', body });
}
export function createAdminTenant(payload) { return request('/admin/tenancy/tenants', { method: 'POST', body: JSON.stringify(payload) }); }
export function updateAdminTenant(tenantId, payload) { return request(`/admin/tenancy/tenants/${tenantId}`, { method: 'PUT', body: JSON.stringify(payload) }); }
export function deleteAdminTenant(tenantId) { return request(`/admin/tenancy/tenants/${tenantId}`, { method: 'DELETE' }); }
export function createAdminLease(payload) { return request('/admin/tenancy/leases', { method: 'POST', body: JSON.stringify(payload) }); }
export function createAdminLeaseFirstInvoice(leaseId, billingMonth) {
  return request(`/admin/tenancy/leases/${leaseId}/rent-invoices`, {
    method: 'POST',
    body: JSON.stringify({ billingMonth })
  });
}
export function updateAdminLease(leaseId, payload) { return request(`/admin/tenancy/leases/${leaseId}`, { method: 'PUT', body: JSON.stringify(payload) }); }
export function closeAdminLease(leaseId, payload) { return request(`/admin/tenancy/leases/${leaseId}/close`, { method: 'POST', body: JSON.stringify(payload) }); }
export function fetchAdminLeasePayments(leaseId) { return request(`/admin/tenancy/leases/${leaseId}/payments`); }
export function updateAdminLeasePayment(leaseId, paymentId, payload) { return request(`/admin/tenancy/leases/${leaseId}/payments/${paymentId}`, { method: 'PUT', body: JSON.stringify(payload) }); }
export function deleteAdminLeasePayment(leaseId, paymentId) { return request(`/admin/tenancy/leases/${leaseId}/payments/${paymentId}`, { method: 'DELETE' }); }
export function transferAdminLease(leaseId, payload) { return request(`/admin/tenancy/leases/${leaseId}/transfer`, { method: 'POST', body: JSON.stringify(payload) }); }
export function uploadAdminLeaseContract(leaseId, file) {
  const body = new FormData(); body.append('file', file);
  return request(`/admin/tenancy/leases/${leaseId}/contract`, { method: 'POST', body });
}
export function startAdminLeaseSignature(leaseId, payload) { return request(`/admin/e-signatures/leases/${leaseId}`, { method: 'POST', body: JSON.stringify(payload) }); }
export function startAdminMandateDocumentSignature(mandateId, documentId, payload) { return request(`/admin/e-signatures/rental-mandates/${mandateId}/documents/${documentId}`, { method: 'POST', body: JSON.stringify(payload) }); }
export function fetchPublicSignature(token) { return request(`/public/signatures/${token}`); }
export function resendPublicSignatureCode(token) { return request(`/public/signatures/${token}/verification-code`, { method: 'POST' }); }
export function signPublicSignature(token, payload) { return request(`/public/signatures/${token}/sign`, { method: 'POST', body: JSON.stringify(payload) }); }
export async function fetchAdminLeaseContract(leaseId, download = false) {
  const response = await fetch(`${API_BASE_URL}/admin/tenancy/leases/${leaseId}/contract?download=${download}`, { credentials: 'include' });
  if (!response.ok) {
    let message = `API request failed: ${response.status}`;
    try { message = (await response.json()).message || message; } catch { /* Keep status. */ }
    throw new Error(localizeApiErrorMessage(message, response.status));
  }
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}
export async function generateAdminContractTemplate(templateType, fields) {
  const response = await fetch(`${API_BASE_URL}/admin/contract-templates/${encodeURIComponent(templateType)}/generate`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, credentials: 'include',
    body: JSON.stringify({ templateType, fields })
  });
  if (!response.ok) {
    let message = `API request failed: ${response.status}`;
    try { message = (await response.json()).message || message; } catch { /* Keep status. */ }
    throw new Error(localizeApiErrorMessage(message, response.status));
  }
  const disposition = response.headers.get('Content-Disposition') || '';
  const match = disposition.match(/filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i);
  const expectedExtension = '.pdf';
  let filename = decodeURIComponent(match?.[1] || match?.[2] || `${templateType}${expectedExtension}`);
  if (!filename.toLowerCase().endsWith(expectedExtension)) {
    filename = `${filename.replace(/\.[^.]+$/, '')}${expectedExtension}`;
  }
  return { blob: await response.blob(), filename };
}
export function sendAdminRentReminder(invoiceId) { return request(`/admin/tenancy/rent-invoices/${invoiceId}/reminders`, { method: 'POST' }); }
export function uploadAdminRentProof(financeRecordId, file) {
  const body = new FormData(); body.append('file', file);
  return request(`/admin/tenancy/rent-payments/${financeRecordId}/proof`, { method: 'POST', body });
}
export async function fetchAdminRentProof(documentId, download = false) {
  const response = await fetch(`${API_BASE_URL}/admin/tenancy/rent-proofs/${documentId}?download=${download}`, { credentials: 'include' });
  if (!response.ok) {
    let message = `API request failed: ${response.status}`;
    try { message = (await response.json()).message || message; } catch { /* Keep status. */ }
    throw new Error(localizeApiErrorMessage(message, response.status));
  }
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function confirmAdminFinanceReview(financeRecordId, note) {
  return request(`/admin/finance/reviews/${financeRecordId}/confirm`, {
    method: 'POST', body: JSON.stringify({ note })
  });
}

export function rejectAdminFinanceReview(financeRecordId, note) {
  return request(`/admin/finance/reviews/${financeRecordId}/reject`, {
    method: 'POST', body: JSON.stringify({ note })
  });
}

export function batchConfirmAdminFinanceReviews(ids, note) {
  return request('/admin/finance/reviews/batch-confirm', {
    method: 'POST', body: JSON.stringify({ ids, note })
  });
}

export async function fetchAdminFinanceProof(documentId, download = false) {
  const response = await fetch(`${API_BASE_URL}/admin/finance/proofs/${documentId}?download=${download}`, {
    credentials: 'include'
  });
  if (!response.ok) {
    let message = `API request failed: ${response.status}`;
    try { message = (await response.json()).message || message; } catch { /* Keep status. */ }
    throw new Error(localizeApiErrorMessage(message, response.status));
  }
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function fetchAdminAccounts() {
  return request('/admin/accounts');
}

export function createAdminAccount(payload) {
  return request('/admin/accounts', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateAdminAccount(id, payload) {
  return request(`/admin/accounts/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function deleteAdminAccount(id) {
  return request(`/admin/accounts/${id}`, { method: 'DELETE' });
}

export function createAdminOwner(payload) {
  return request('/admin/owners', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateAdminOwner(ownerId, payload) {
  return request(`/admin/owners/${ownerId}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function fetchAdminPropertyProjects() {
  return request('/admin/owners/projects');
}

export function createAdminOwnerProperty(ownerId, payload) {
  return request(`/admin/owners/${ownerId}/properties`, { method: 'POST', body: JSON.stringify(payload) });
}

export function fetchAdminOwnerProperty(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}`);
}

export function updateAdminOwnerProperty(ownerId, ownerUnitId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export function fetchAdminPropertyOwnerships(unitId) {
  return request(`/admin/units/${unitId}/ownerships`);
}

export function createAdminPropertyOwnership(unitId, payload) {
  return request(`/admin/units/${unitId}/ownerships`, { method: 'POST', body: JSON.stringify(payload) });
}

export function updateAdminPropertyOwnership(unitId, ownershipId, payload) {
  return request(`/admin/units/${unitId}/ownerships/${ownershipId}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function deleteAdminPropertyOwnership(unitId, ownershipId) {
  return request(`/admin/units/${unitId}/ownerships/${ownershipId}`, { method: 'DELETE' });
}

export function fetchAdminPropertyBasicProfile(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/basic-profile`);
}

export function saveAdminPropertyBasicProfile(ownerId, ownerUnitId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/basic-profile`, {
    method: 'PUT', body: JSON.stringify(payload)
  });
}

export function fetchAdminPropertyWorkspace(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/workspace`);
}

export function fetchAdminPropertyWorkspaceById(unitId) {
  return request(`/admin/properties/${unitId}/workspace`);
}

export function saveAdminPropertyWorkspaceBasic(unitId, payload) {
  return request(`/admin/properties/${unitId}/workspace/basic`, {
    method: 'PUT', body: JSON.stringify(payload)
  });
}

export function fetchAdminPropertyContract(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/contract`);
}

export function saveAdminPropertyContract(ownerId, ownerUnitId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/contract`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function deleteAdminPropertyContract(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/contract`, { method: 'DELETE' });
}

export function fetchAdminPropertyContractRecords(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/contracts`);
}

export function fetchAdminPropertyContractLeaseOptions(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/contracts/lease-options`);
}

function propertyContractRecordBody(payload, file) {
  const body = new FormData();
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') body.append(key, value);
  });
  if (file) body.append('file', file);
  return body;
}

export function createAdminPropertyContractRecord(ownerId, ownerUnitId, payload, file) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/contracts`, {
    method: 'POST', body: propertyContractRecordBody(payload, file)
  });
}

export function updateAdminPropertyContractRecord(ownerId, ownerUnitId, contractId, payload, file) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/contracts/${contractId}`, {
    method: 'PUT', body: propertyContractRecordBody(payload, file)
  });
}

export function deleteAdminPropertyContractRecord(ownerId, ownerUnitId, contractId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/contracts/${contractId}`, { method: 'DELETE' });
}

export async function downloadAdminPropertyContractRecord(ownerId, ownerUnitId, contractId) {
  const response = await fetch(`${API_BASE_URL}/admin/owners/${ownerId}/properties/${ownerUnitId}/contracts/${contractId}/file`, { credentials: 'include' });
  if (!response.ok) throw new Error(`下載合約附件失敗：${response.status}`);
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function fetchAdminPropertyPhotos(ownerId, ownerUnitId, leaseId = null) {
  const query = leaseId == null ? '' : `?leaseId=${encodeURIComponent(leaseId)}`;
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/photos${query}`);
}

function propertyPhotoBody(payload, file) {
  const body = new FormData();
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== null && value !== undefined) body.append(key, value);
  });
  if (file) body.append('file', file);
  return body;
}

export function createAdminPropertyPhoto(ownerId, ownerUnitId, payload, file) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/photos`, {
    method: 'POST', body: propertyPhotoBody(payload, file)
  });
}

export function updateAdminPropertyPhoto(ownerId, ownerUnitId, photoId, payload, file) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/photos/${photoId}`, {
    method: 'PUT', body: propertyPhotoBody(payload, file)
  });
}

export function deleteAdminPropertyPhoto(ownerId, ownerUnitId, photoId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/photos/${photoId}`, { method: 'DELETE' });
}

export function adminPropertyPhotoContentUrl(ownerId, ownerUnitId, photoId) {
  return `${API_BASE_URL}/admin/owners/${ownerId}/properties/${ownerUnitId}/photos/${photoId}/content`;
}

function propertyAttachmentBody(payload, file) {
  const body = new FormData();
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== null && value !== undefined) body.append(key, value);
  });
  if (file) body.append('file', file);
  return body;
}

export function fetchAdminPropertyAttachments(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/attachments`);
}

export function createAdminPropertyAttachment(ownerId, ownerUnitId, payload, file) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/attachments`, {
    method: 'POST', body: propertyAttachmentBody(payload, file)
  });
}

export function updateAdminPropertyAttachment(ownerId, ownerUnitId, attachmentId, payload, file) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/attachments/${attachmentId}`, {
    method: 'PUT', body: propertyAttachmentBody(payload, file)
  });
}

export function deleteAdminPropertyAttachment(ownerId, ownerUnitId, attachmentId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/attachments/${attachmentId}`, { method: 'DELETE' });
}

export async function downloadAdminPropertyAttachment(ownerId, ownerUnitId, attachmentId) {
  const response = await fetch(`${API_BASE_URL}/admin/owners/${ownerId}/properties/${ownerUnitId}/attachments/${attachmentId}/file`, { credentials: 'include' });
  if (!response.ok) throw new Error(`下載相關附件失敗：${response.status}`);
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

function propertyHandoverReportBody(payload, file, photos = [], photoMeta = []) {
  const body = new FormData();
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') body.append(key, value);
  });
  if (file) body.append('file', file);
  photos.forEach(photo => body.append('photos', photo));
  if (photoMeta.length) body.append('photoMeta', JSON.stringify(photoMeta));
  return body;
}

export function fetchAdminPropertyHandoverReports(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/handover-reports`);
}

export function fetchAdminPropertyHandoverChecklist(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/handover-checklist`);
}

export function createAdminPropertyHandoverChecklistItem(ownerId, ownerUnitId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/handover-checklist`, { method: 'POST', body: JSON.stringify(payload) });
}

export function updateAdminPropertyHandoverChecklistItem(ownerId, ownerUnitId, itemId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/handover-checklist/${itemId}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function deleteAdminPropertyHandoverChecklistItem(ownerId, ownerUnitId, itemId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/handover-checklist/${itemId}`, { method: 'DELETE' });
}

export function createAdminPropertyHandoverReport(ownerId, ownerUnitId, payload, file, photos = [], photoMeta = []) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/handover-reports`, {
    method: 'POST', body: propertyHandoverReportBody(payload, file, photos, photoMeta)
  });
}

export function updateAdminPropertyHandoverReport(ownerId, ownerUnitId, reportId, payload, file, photos = [], photoMeta = []) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/handover-reports/${reportId}`, {
    // Multipart form data is not consistently parsed for PUT requests by
    // servlet containers. POST is used for updates here for broad browser /
    // server compatibility; the backend keeps the PUT route for existing clients.
    method: 'POST', body: propertyHandoverReportBody(payload, file, photos, photoMeta)
  });
}

export function deleteAdminPropertyHandoverReport(ownerId, ownerUnitId, reportId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/handover-reports/${reportId}`, { method: 'DELETE' });
}

export async function downloadAdminPropertyHandoverReport(ownerId, ownerUnitId, reportId) {
  const response = await fetch(`${API_BASE_URL}/admin/owners/${ownerId}/properties/${ownerUnitId}/handover-reports/${reportId}/file`, { credentials: 'include' });
  if (!response.ok) throw new Error(`下載交屋報告附件失敗：${response.status}`);
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function fetchAdminPropertyImportantMessages(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/important-messages`);
}

export function createAdminPropertyImportantMessage(ownerId, ownerUnitId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/important-messages`, {
    method: 'POST', body: JSON.stringify(payload)
  });
}

export function updateAdminPropertyImportantMessage(ownerId, ownerUnitId, messageId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/important-messages/${messageId}`, {
    method: 'PUT', body: JSON.stringify(payload)
  });
}

export function deleteAdminPropertyImportantMessage(ownerId, ownerUnitId, messageId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/important-messages/${messageId}`, { method: 'DELETE' });
}

export function fetchAdminPropertyBankAccounts(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/bank-accounts`);
}

export function createAdminPropertyBankAccount(ownerId, ownerUnitId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/bank-accounts`, {
    method: 'POST', body: JSON.stringify(payload)
  });
}

export function updateAdminPropertyBankAccount(ownerId, ownerUnitId, accountId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/bank-accounts/${accountId}`, {
    method: 'PUT', body: JSON.stringify(payload)
  });
}

export function deleteAdminPropertyBankAccount(ownerId, ownerUnitId, accountId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/bank-accounts/${accountId}`, { method: 'DELETE' });
}

function propertyCashflowBody(payload, file) {
  const body = new FormData();
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') body.append(key, value);
  });
  if (file) body.append('file', file);
  return body;
}

export function fetchAdminPropertyCashflows(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/income-expenses`);
}

export function createAdminPropertyCashflow(ownerId, ownerUnitId, payload, file) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/income-expenses`, {
    method: 'POST', body: propertyCashflowBody(payload, file)
  });
}

export function updateAdminPropertyCashflow(ownerId, ownerUnitId, cashflowId, payload, file) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/income-expenses/${cashflowId}`, {
    method: 'PUT', body: propertyCashflowBody(payload, file)
  });
}

export function deleteAdminPropertyCashflow(ownerId, ownerUnitId, cashflowId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/income-expenses/${cashflowId}`, { method: 'DELETE' });
}

export async function downloadAdminPropertyCashflowProof(ownerId, ownerUnitId, cashflowId) {
  const response = await fetch(`${API_BASE_URL}/admin/owners/${ownerId}/properties/${ownerUnitId}/income-expenses/${cashflowId}/proof`, { credentials: 'include' });
  if (!response.ok) throw new Error(`下載收支憑證失敗：${response.status}`);
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function fetchAdminPropertyMaintenance(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/maintenance-records`);
}

export function createAdminPropertyMaintenance(ownerId, ownerUnitId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/maintenance-records`, {
    method: 'POST', body: JSON.stringify(payload)
  });
}

export function updateAdminPropertyMaintenance(ownerId, ownerUnitId, workOrderId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/maintenance-records/${workOrderId}`, {
    method: 'PUT', body: JSON.stringify(payload)
  });
}

export function deleteAdminPropertyMaintenance(ownerId, ownerUnitId, workOrderId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/maintenance-records/${workOrderId}`, {
    method: 'DELETE'
  });
}

export function fetchAdminPropertyRepairReports(ownerId, ownerUnitId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/repair-reports`);
}

export function createAdminPropertyRepairReport(ownerId, ownerUnitId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/repair-reports`, {
    method: 'POST', body: JSON.stringify(payload)
  });
}

export function updateAdminPropertyRepairReport(ownerId, ownerUnitId, workOrderId, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/repair-reports/${workOrderId}`, {
    method: 'PUT', body: JSON.stringify(payload)
  });
}

export function deleteAdminPropertyRepairReport(ownerId, ownerUnitId, workOrderId) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/repair-reports/${workOrderId}`, {
    method: 'DELETE'
  });
}

export function fetchAdminPropertyOperations(ownerId, ownerUnitId, month) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/operations?month=${encodeURIComponent(month)}`);
}

export function createAdminPropertyOperationsCharge(ownerId, ownerUnitId, month, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/operations/charges?month=${encodeURIComponent(month)}`, {
    method: 'POST', body: JSON.stringify(payload)
  });
}

export function createAdminPropertyOperationsWorkOrder(ownerId, ownerUnitId, month, payload) {
  return request(`/admin/owners/${ownerId}/properties/${ownerUnitId}/operations/work-orders?month=${encodeURIComponent(month)}`, {
    method: 'POST', body: JSON.stringify(payload)
  });
}

export function fetchOwnerRentIncome(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  const suffix = query.toString() ? `?${query}` : '';
  return request(`/owner/rent-income${suffix}`);
}

export function confirmOwnerRentReceipt(invoiceId) {
  return request(`/owner/rent-income/${invoiceId}/confirm-receipt`, { method: 'POST' });
}

export function fetchOwnerExpenses(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  const suffix = query.toString() ? `?${query}` : '';
  return request(`/owner/expenses${suffix}`);
}

export function fetchAdminExpenses(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  const suffix = query.toString() ? `?${query}` : '';
  return request(`/admin/expenses${suffix}`);
}

export function fetchAdminMaintenanceOptions() {
  return request('/admin/expenses/options');
}

export function fetchAdminVendors() {
  return request('/admin/vendors');
}

export function createAdminVendor(payload) {
  return request('/admin/vendors', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateAdminVendor(id, payload) {
  return request(`/admin/vendors/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function deactivateAdminVendor(id) {
  return request(`/admin/vendors/${id}`, { method: 'DELETE' });
}

export function createAdminExpense(payload) {
  return request('/admin/expenses/records', { method: 'POST', body: JSON.stringify(payload) });
}

export function createAdminMaintenance(payload) {
  return request('/admin/expenses/maintenance', { method: 'POST', body: JSON.stringify(payload) });
}

export function fetchAdminMaintenanceDetail(workOrderId) {
  return request(`/admin/expenses/maintenance/${workOrderId}`);
}

export function fetchAdminMaintenanceHandling(workOrderId) {
  return request(`/admin/expenses/maintenance/${workOrderId}/handling`);
}

export function uploadAdminMaintenancePhotos(workOrderId, relationType, files) {
  const body = new FormData();
  body.append('relationType', relationType);
  files.forEach(file => body.append('files', file));
  return request(`/admin/expenses/maintenance/${workOrderId}/attachments`, { method: 'POST', body });
}

export async function fetchAdminMaintenanceAttachment(documentId, download = false) {
  const response = await fetch(`${API_BASE_URL}/admin/expenses/attachments/${documentId}?download=${download}`, {
    credentials: 'include'
  });
  if (!response.ok) throw new Error(`維修附件讀取失敗：${response.status}`);
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function completeAdminMaintenance(workOrderId, payload) {
  return request(`/admin/expenses/maintenance/${workOrderId}/complete`, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function fetchMaintenanceDetail(workOrderId) {
  return request(`/owner/expenses/maintenance/${workOrderId}`);
}

export function uploadMaintenanceAttachments(workOrderId, relationType, files) {
  const body = new FormData();
  body.append('relationType', relationType);
  files.forEach(file => body.append('files', file));
  return request(`/owner/expenses/maintenance/${workOrderId}/attachments`, { method: 'POST', body });
}

export async function fetchMaintenanceAttachment(documentId, download = false) {
  const response = await fetch(`${API_BASE_URL}/owner/expenses/attachments/${documentId}?download=${download}`, {
    credentials: 'include'
  });
  if (!response.ok) {
    let message = `API request failed: ${response.status}`;
    try {
      const body = await response.json();
      message = body.message || message;
    } catch {
      // Keep the HTTP status when the server does not return JSON.
    }
    throw new Error(localizeApiErrorMessage(message, response.status));
  }
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function fetchOwnerReserve(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  const suffix = query.toString() ? `?${query}` : '';
  return request(`/owner/reserve${suffix}`);
}

export function submitReserveTopup(payload, files) {
  const body = new FormData();
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== undefined && value !== null) body.append(key, value);
  });
  files.forEach(file => body.append('files', file));
  return request('/owner/reserve/topups', { method: 'POST', body });
}

export async function fetchReserveDocument(documentId, download = false) {
  const response = await fetch(`${API_BASE_URL}/owner/reserve/documents/${documentId}?download=${download}`, {
    credentials: 'include'
  });
  if (!response.ok) {
    let message = `API request failed: ${response.status}`;
    try {
      const body = await response.json();
      message = body.message || message;
    } catch {
      // Keep the HTTP status when the server does not return JSON.
    }
    throw new Error(localizeApiErrorMessage(message, response.status));
  }
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function fetchOwnerNotifications() {
  return request('/owner/notifications');
}

export function requestOwnerEmailVerification(email) {
  return request('/owner/notifications/email/request-code', {
    method: 'POST',
    body: JSON.stringify({ email })
  });
}

export function verifyOwnerEmail(email, code) {
  return request('/owner/notifications/email/verify', {
    method: 'POST',
    body: JSON.stringify({ email, code })
  });
}

export function toggleOwnerEmailSubscription(enabled) {
  return request('/owner/notifications/email/subscription', {
    method: 'PUT',
    body: JSON.stringify({ enabled })
  });
}

export function markOwnerNotificationRead(notificationId) {
  return request(`/owner/notifications/${notificationId}/read`, { method: 'POST' });
}

export function markAllOwnerNotificationsRead() {
  return request('/owner/notifications/read-all', { method: 'POST' });
}

export function fetchOwnerDocuments() {
  return request('/owner/documents');
}

export async function fetchOwnerDocumentFile(documentId, download = false) {
  const response = await fetch(`${API_BASE_URL}/owner/documents/${documentId}/file?download=${download}`, {
    credentials: 'include'
  });
  if (!response.ok) {
    let message = `API request failed: ${response.status}`;
    try {
      const body = await response.json();
      message = body.message || message;
    } catch {
      // Keep the HTTP status when the server does not return JSON.
    }
    throw new Error(localizeApiErrorMessage(message, response.status));
  }
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function fetchPaymentProgress(ownerUnitId) {
  return request(`/owner/payment-progress/${ownerUnitId}`);
}

export function submitPaymentProof(ownerUnitId, payload, files) {
  const body = new FormData();
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== null && value !== undefined) body.append(key, value);
  });
  files.forEach(file => body.append('files', file));
  return request(`/owner/payment-progress/${ownerUnitId}/proofs`, { method: 'POST', body });
}

export function createProperty(payload) {
  return request('/properties', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateProperty(id, payload) {
  return request(`/properties/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function deleteProperty(id) {
  return request(`/properties/${id}`, { method: 'DELETE' });
}

export function login(payload, portal = null) {
  const endpoint = portal === 'admin' || portal === 'owner'
    ? `/auth/${portal}/login`
    : '/auth/login';
  return request(endpoint, { method: 'POST', body: JSON.stringify(payload) });
}

export function fetchSession(portal) {
  return request(`/auth/${portal}/session`);
}

export function fetchAdminAudit(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value);
  });
  const suffix = query.toString() ? `?${query}` : '';
  return request(`/admin/audit${suffix}`);
}

export function fetchAdminAuditOptions() {
  return request('/admin/audit/options');
}

export function logout(portal) {
  return request(`/auth/${portal}/logout`, { method: 'POST' });
}

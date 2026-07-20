const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

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
    throw new Error(message);
  }

  return response.status === 204 ? null : response.json();
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

export function fetchAdminSync() { return request('/admin/sync'); }
export function previewAdminSync(payload) { return request('/admin/sync/preview', { method: 'POST', body: JSON.stringify(payload) }); }
export function createAdminSyncBatch(payload) { return request('/admin/sync/batches', { method: 'POST', body: JSON.stringify(payload) }); }
export function retryAdminSyncBatch(batchId) { return request(`/admin/sync/batches/${batchId}/retry`, { method: 'POST' }); }
export async function downloadAdminSyncBatch(batchId) {
  const response = await fetch(`${API_BASE_URL}/admin/sync/batches/${batchId}/file`, { credentials: 'include' });
  if (!response.ok) { let message = `API request failed: ${response.status}`; try { message = (await response.json()).message || message; } catch { /* status */ } throw new Error(message); }
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
}

export function fetchAdminReports() { return request('/admin/reports'); }
export function generateAdminReport(payload) { return request('/admin/reports/runs', { method: 'POST', body: JSON.stringify(payload) }); }
export async function downloadAdminReport(runId) {
  const response = await fetch(`${API_BASE_URL}/admin/reports/runs/${runId}/file`, { credentials: 'include' });
  if (!response.ok) { let message = `API request failed: ${response.status}`; try { message = (await response.json()).message || message; } catch { /* status */ } throw new Error(message); }
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
    throw new Error(message);
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
export async function downloadAdminRentalMandateDocument(mandateId, documentId) { const response = await fetch(`${API_BASE_URL}/admin/rental-mandates/${mandateId}/documents/${documentId}/file`, { credentials: 'include' }); if (!response.ok) throw new Error(`API request failed: ${response.status}`); return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' }; }
export function fetchAdminRentFinanceReviews(filters = {}) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && value !== null && value !== '') query.set(key, value); });
  return request(`/admin/tenancy/rent-reviews${query.toString() ? `?${query}` : ''}`);
}
export function fetchAdminRentFinanceProjects() { return request('/admin/tenancy/rent-reviews/projects'); }

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
export function createAdminLease(payload) { return request('/admin/tenancy/leases', { method: 'POST', body: JSON.stringify(payload) }); }
export function updateAdminLease(leaseId, payload) { return request(`/admin/tenancy/leases/${leaseId}`, { method: 'PUT', body: JSON.stringify(payload) }); }
export function transferAdminLease(leaseId, payload) { return request(`/admin/tenancy/leases/${leaseId}/transfer`, { method: 'POST', body: JSON.stringify(payload) }); }
export function uploadAdminLeaseContract(leaseId, file) {
  const body = new FormData(); body.append('file', file);
  return request(`/admin/tenancy/leases/${leaseId}/contract`, { method: 'POST', body });
}
export async function fetchAdminLeaseContract(leaseId, download = false) {
  const response = await fetch(`${API_BASE_URL}/admin/tenancy/leases/${leaseId}/contract?download=${download}`, { credentials: 'include' });
  if (!response.ok) {
    let message = `API request failed: ${response.status}`;
    try { message = (await response.json()).message || message; } catch { /* Keep status. */ }
    throw new Error(message);
  }
  return { blob: await response.blob(), contentDisposition: response.headers.get('Content-Disposition') || '' };
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
    throw new Error(message);
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
    throw new Error(message);
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
    throw new Error(message);
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
    throw new Error(message);
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
    throw new Error(message);
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

export function logout(portal) {
  return request(`/auth/${portal}/logout`, { method: 'POST' });
}

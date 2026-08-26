const ARCHIVED_STATUSES = new Set(['archived', 'superseded', 'voided']);
const PENDING_STATUSES = new Set(['pending', 'sent', 'viewed', 'in_progress']);

export function rentalFileLifecycle(file = {}) {
  const status = String(file.status || '').toLowerCase();
  const documentType = String(file.documentType || '').toLowerCase();
  const relationType = String(file.relationType || '').toLowerCase();

  if (file.archived || ARCHIVED_STATUSES.has(status)) return 'archived';
  if (file.signedFile || documentType === 'signed_contract' || relationType.endsWith('_signed')) return 'signed';
  if (file.kind === 'mandate' || file.kind === 'contract') return 'generated';
  if (PENDING_STATUSES.has(status)) return 'pending';
  if (status === 'disabled') return 'disabled';
  return 'stored';
}

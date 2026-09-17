export function ownerDocumentKey(file) {
  return `${file.source || 'document'}:${file.documentId ?? file.id}`;
}

export function matchesOwnerDocumentProperty(file, property) {
  if (!property) return true;
  if (file.ownerUnitId != null) return String(file.ownerUnitId) === String(property.ownerUnitId);
  const unit = String(file.unitNo || '').trim();
  const project = String(file.projectName || '').trim();
  // Owner-level documents are shared across the owner's property views.
  if (!unit && !project) return true;
  return (!unit || unit === String(property.unitNo || '').trim())
    && (!project || project === String(property.projectName || '').trim());
}

export const ownerDocumentCategories = ['all', 'sale', 'lease', 'property_contract', 'proof', 'cashflow', 'handover', 'photo', 'other'];
export function matchesOwnerDocumentCategory(file, category) {
  if (category === 'all') return true;
  if (category === 'proof') return ['proof', 'receipt'].includes(file.category);
  if (category === 'cashflow') return ['cashflow', 'finance', 'maintenance', 'work_order'].includes(file.category);
  return file.category === category;
}

export function ownerDocumentFilePath(id, download = false, source = 'document') {
  if (!['document', 'property_contract'].includes(source)) throw new Error('Unknown document source');
  return `/owner/documents/${encodeURIComponent(id)}/file?download=${download}&source=${source}`;
}

const CURRENT_MANDATE_STATUSES = new Set(['draft', 'pending_review', 'active', 'suspended']);

const asArray = value => (Array.isArray(value) ? value : []);
const hasValue = value => value !== null && value !== undefined && value !== '';
const sameId = (left, right) => hasValue(left) && hasValue(right) && String(left) === String(right);
const statusOf = value => String(value || '').toLowerCase();
const CLOSED_LEASE_STATUSES = new Set(['terminated', 'transferred', 'completed', 'cancelled']);

export const reportBelongsToMandate = (report, mandateOrId, nextMandate = null) => {
  const mandateId = typeof mandateOrId === 'object' ? mandateOrId?.id : mandateOrId;
  if (sameId(report?.mandateId ?? report?.rentalMandateId, mandateId)) return true;
  try {
    const content = typeof report?.contentJson === 'string' ? JSON.parse(report.contentJson) : report?.contentJson;
    if (sameId(content?.mandateId ?? content?.rentalMandateId, mandateId)) return true;
  } catch {
    // Fall through to the timestamp compatibility rule for older payloads.
  }
  const mandateCreatedAt = typeof mandateOrId === 'object' ? mandateOrId?.createdAt : null;
  const reportCreatedAt = report?.createdAt || report?.updatedAt || report?.reportDate;
  const nextMandateCreatedAt = nextMandate?.createdAt || nextMandate?.startDate;
  return Boolean(mandateCreatedAt && reportCreatedAt
    && new Date(reportCreatedAt).getTime() >= new Date(mandateCreatedAt).getTime()
    && (!nextMandateCreatedAt || new Date(reportCreatedAt).getTime() < new Date(nextMandateCreatedAt).getTime()));
};

const belongsTo = (record, key, id) => sameId(record?.[key], id)
  || (sameId(record?.entityId, id) && record?.entityType === `rental_${key}`);

const action = (key, target, reasonKey = null) => ({ key, target, reasonKey });

const stage = (key, status, ownerRole, missingItems, primaryAction, blockingReasonKey = null) => ({
  key,
  status,
  ownerRole,
  missingItems,
  primaryAction,
  blockingReasonKey,
});

export const isAuthorizationSignedDocument = document => {
  const relationType = statusOf(document?.relationType);
  const documentType = statusOf(document?.documentType);
  return ['authorization_signed', 'authorization', 'management_authorization_signed'].includes(relationType)
    || (relationType === 'authorization' && documentType === 'signed_contract');
};

export function selectCurrentRentalMandate({ property = {}, mandates = [], asOf = new Date().toISOString().slice(0, 10) } = {}) {
  return asArray(mandates)
    .filter(mandate => sameId(mandate?.ownerUnitId, property?.ownerUnitId))
    .filter(mandate => CURRENT_MANDATE_STATUSES.has(statusOf(mandate?.status)))
    .filter(mandate => !mandate?.startDate || mandate.startDate <= asOf)
    .filter(mandate => !mandate?.endDate || mandate.endDate >= asOf)
    .sort((left, right) => String(right?.startDate || '').localeCompare(String(left?.startDate || ''))
      || Number(right?.id || 0) - Number(left?.id || 0))[0] || null;
}

const scopedDocuments = (documents, mandateId) => asArray(documents)
  .filter(document => belongsTo(document, 'mandateId', mandateId));

const scopedLeases = (leases, mandateId) => asArray(leases)
  .filter(lease => sameId(lease?.rentalMandateId ?? lease?.mandateId, mandateId));

const scopedInvoices = (invoices, leaseId) => asArray(invoices)
  .filter(invoice => sameId(invoice?.leaseId, leaseId));

const scopedPayments = (payments, leaseId, invoiceIds) => asArray(payments)
  .filter(payment => sameId(payment?.leaseId, leaseId) || invoiceIds.has(String(payment?.invoiceId)));

const reportContent = report => {
  try { return typeof report?.contentJson === 'string' ? JSON.parse(report.contentJson) : (report?.contentJson || {}); }
  catch { return {}; }
};

const reportBelongsToLease = (report, leaseId, type) => {
  const content = reportContent(report);
  return sameId(content?.leaseId, leaseId)
    && statusOf(content?.handoverType || content?.type) === type;
};

export function buildRentalWorkbench({ property = {}, mandates = [], workspace = {}, documents = [], invoices = [], payments = {} } = {}) {
  const propertyOperating = statusOf(property?.assetStage) === 'operating';
  const currentMandate = selectCurrentRentalMandate({ property, mandates });
  const mandateDocuments = currentMandate ? scopedDocuments(documents, currentMandate.id) : [];
  const authorizationSigned = mandateDocuments.some(isAuthorizationSignedDocument);
  const authorizationDocument = mandateDocuments.find(document =>
    ['authorization', 'management_authorization_draft'].includes(statusOf(document?.relationType))) || null;
  const authorizationRequestStatus = statusOf(authorizationDocument?.signatureStatus);
  const authorizationSigning = {
    status: authorizationSigned ? 'signed' : (authorizationRequestStatus || 'not_started'),
    documentId: authorizationDocument?.id || null,
    signerName: authorizationDocument?.signatureSignerName || '',
    signerEmail: authorizationDocument?.signatureSignerEmail || '',
    requestedAt: authorizationDocument?.signatureRequestedAt || null,
    expiresAt: authorizationDocument?.signatureExpiresAt || null,
    signedAt: authorizationDocument?.signatureSignedAt || null,
  };
  const mandateActive = statusOf(currentMandate?.status) === 'active';

  const latestMandate = asArray(mandates)
    .filter(mandate => sameId(mandate?.ownerUnitId, property?.ownerUnitId))
    .sort((left, right) => String(right?.startDate || '').localeCompare(String(left?.startDate || ''))
      || Number(right?.id || 0) - Number(left?.id || 0))[0] || null;
  const mandateLeases = currentMandate ? scopedLeases(workspace?.leases, currentMandate.id) : [];
  const closureMandate = currentMandate || latestMandate;
  const closureLeases = closureMandate ? scopedLeases(workspace?.leases, closureMandate.id) : [];
  const currentLease = currentMandate
    ? mandateLeases
      .filter(lease => statusOf(lease?.status) === 'active')
      .sort((left, right) => String(right?.startDate || '').localeCompare(String(left?.startDate || '')))[0] || null
    : null;
  const latestClosedLease = closureLeases
    .filter(lease => CLOSED_LEASE_STATUSES.has(statusOf(lease?.status)))
    .sort((left, right) => String(right?.endDate || right?.startDate || '').localeCompare(String(left?.endDate || left?.startDate || '')))[0] || null;
  const cycleLease = currentLease || latestClosedLease;
  const currentLeaseId = currentLease?.id ?? currentLease?.leaseId;
  const currentTenant = workspace?.tenant || workspace?.currentTenant || null;
  const tenantReady = Boolean(cycleLease?.tenantId || cycleLease?.tenantName || currentTenant?.id || workspace?.tenantId);
  const leaseReady = Boolean(cycleLease);
  const leasingReady = tenantReady && leaseReady;
  const currentInvoices = currentLease ? scopedInvoices(invoices, currentLeaseId) : [];
  const invoiceIds = new Set(currentInvoices.map(invoice => String(invoice.id ?? invoice.invoiceId)));
  const currentPayments = currentLease ? scopedPayments(payments, currentLeaseId, invoiceIds) : [];
  const firstInvoice = currentInvoices[0];
  const firstReceiptReady = Boolean(firstInvoice && Number(firstInvoice.amountDue || 0) > 0
    && Number(firstInvoice.amountPaid || 0) >= Number(firstInvoice.amountDue || 0)
    && currentPayments.some(payment => statusOf(payment?.confirmationStatus) === 'confirmed'));
  const finalHandoverReportReady = latestClosedLease
    ? asArray(workspace?.handovers).some(report => reportBelongsToLease(report, latestClosedLease.id ?? latestClosedLease.leaseId, 'move_out'))
    : false;
  const leaseClosure = latestClosedLease
    ? {
      leaseId: latestClosedLease.id ?? latestClosedLease.leaseId,
      endDate: latestClosedLease.endDate || null,
      tenantName: latestClosedLease.tenantName || null,
      status: 'completed',
      handoverReportReady: finalHandoverReportReady,
      reason: statusOf(latestClosedLease.status) === 'transferred'
        ? 'transfer'
        : statusOf(latestClosedLease.status) === 'terminated'
          ? 'early_termination'
          : 'normal_expiry',
      missingItems: [],
    }
    : { leaseId: null, endDate: null, tenantName: null, status: currentLease ? 'active' : 'not_started', handoverReportReady: false, reason: null, missingItems: [] };

  const stages = [
    stage(
      'propertySetup',
      propertyOperating ? 'completed' : 'in_progress',
      'admin',
      propertyOperating ? [] : ['property_handover'],
      propertyOperating ? null : action('complete_handover', { type: 'property', tab: 'summary' }),
    ),
    stage(
      'mandateAuthorization',
      !propertyOperating ? 'pending' : !currentMandate ? 'in_progress' : mandateActive ? 'completed' : 'in_progress',
      'business',
      !propertyOperating ? ['property_handover'] : !currentMandate ? ['rental_mandate'] : !mandateActive ? ['mandate_activation'] : [],
      !propertyOperating
        ? action('complete_handover', { type: 'property', tab: 'summary' })
        : !currentMandate
        ? action('create_mandate', { type: 'rentalMandate', action: 'create' })
        : null,
    ),
    stage(
      'leasingSigning',
      !mandateActive ? 'pending' : leasingReady ? 'completed' : 'in_progress',
      'business',
      !mandateActive ? ['mandate_activation'] : !tenantReady ? ['tenant'] : !leaseReady ? ['lease'] : [],
      !mandateActive
        ? null
        : !tenantReady
          ? action('create_tenant', { type: 'tenancy', action: 'tenant-create' })
          : !leaseReady
            ? action('create_lease', { type: 'tenancy', action: 'lease-create' })
            : null,
    ),
    stage(
      'rentalOperations',
      !leaseReady ? 'pending' : latestClosedLease ? 'completed' : currentLease ? 'in_progress' : 'pending',
      'admin',
      !leaseReady ? ['lease'] : [],
      currentLease ? action('open_operations_center', { type: 'property', tab: 'operations' }) : null,
    ),
    stage(
      'leaseClosure',
      latestClosedLease ? 'completed' : 'pending',
      'business',
      latestClosedLease ? [] : !currentLease ? ['lease'] : ['lease_closure'],
      currentLease ? action('close_lease', { type: 'tenancy', action: 'close' }) : null,
    ),
  ];

  return {
    currentMandate,
    authorizationSigning,
    currentLease,
    currentInvoices,
    firstInvoice,
    dailyOperations: {
      firstReceiptReady,
      currentInvoice: firstInvoice,
      leaseClosure,
    },
    leaseClosure,
    historyCount: asArray(mandates).filter(mandate => sameId(mandate?.ownerUnitId, property?.ownerUnitId)
      && !sameId(mandate?.id, currentMandate?.id)).length,
    stages,
    currentTask: stages.find(item => item.status !== 'completed') || null,
    includesDailyOperations: true,
  };
}

const CURRENT_MANDATE_STATUSES = new Set(['draft', 'pending_review', 'active', 'suspended']);

const asArray = value => (Array.isArray(value) ? value : []);
const hasValue = value => value !== null && value !== undefined && value !== '';
const sameId = (left, right) => hasValue(left) && hasValue(right) && String(left) === String(right);
const statusOf = value => String(value || '').toLowerCase();
const CLOSED_LEASE_STATUSES = new Set(['terminated', 'transferred', 'completed', 'cancelled']);

export const reportBelongsToMandate = (report, mandateOrId) => {
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
  return Boolean(mandateCreatedAt && reportCreatedAt
    && new Date(reportCreatedAt).getTime() >= new Date(mandateCreatedAt).getTime());
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

const isSigned = record => statusOf(record?.status) === 'signed'
  || statusOf(record?.status) === 'completed'
  || hasValue(record?.signedDate);

export const isAuthorizationSignedDocument = document => {
  const relationType = statusOf(document?.relationType);
  const documentType = statusOf(document?.documentType);
  return ['signed_contract', 'authorization_signed', 'authorization'].includes(relationType)
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

const scopedContracts = (contracts, leaseId) => asArray(contracts)
  .filter(contract => sameId(contract?.leaseId, leaseId));

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
  const currentMandate = selectCurrentRentalMandate({ property, mandates });
  const mandateDocuments = currentMandate ? scopedDocuments(documents, currentMandate.id) : [];
  const currentHandoverReports = currentMandate
    ? asArray(workspace?.handovers).filter(report => reportBelongsToMandate(report, currentMandate))
    : [];
  const handoverReportReady = !currentMandate || currentHandoverReports.length > 0;

  const authorizationDraft = mandateDocuments.some(document =>
    ['authorization_draft', 'authorization'].includes(document?.relationType));
  const authorizationSigned = mandateDocuments.some(isAuthorizationSignedDocument);
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
  const currentLeaseId = currentLease?.id ?? currentLease?.leaseId;
  const leaseContracts = currentLease ? scopedContracts(workspace?.contracts, currentLeaseId) : [];
  const currentTenant = workspace?.tenant || workspace?.currentTenant || null;
  const tenantReady = Boolean(currentLease?.tenantId || currentLease?.tenantName || currentTenant?.id || workspace?.tenantId);
  const otrReady = leaseContracts.some(contract => contract?.contractType === 'O_LEASE_RESERVATION' && isSigned(contract));
  const leaseReady = Boolean(currentLease);
  const leaseContractReady = leaseContracts.some(contract => contract?.contractType === 'L_LEASE' && isSigned(contract));
  const leasingReady = tenantReady && otrReady && leaseReady && leaseContractReady;
  const currentInvoices = currentLease ? scopedInvoices(invoices, currentLeaseId) : [];
  const invoiceIds = new Set(currentInvoices.map(invoice => String(invoice.id ?? invoice.invoiceId)));
  const currentPayments = currentLease ? scopedPayments(payments, currentLeaseId, invoiceIds) : [];
  const firstInvoice = currentInvoices[0];
  const firstReceiptReady = Boolean(firstInvoice && Number(firstInvoice.amountDue || 0) > 0
    && Number(firstInvoice.amountPaid || 0) >= Number(firstInvoice.amountDue || 0)
    && currentPayments.some(payment => statusOf(payment?.confirmationStatus) === 'confirmed'));
  const handoverCompleted = (workspace?.handover?.status === 'completed'
    && (!currentMandate || sameId(workspace.handover.mandateId, currentMandate.id)))
    || currentHandoverReports.some(item => item?.completed === true || statusOf(item?.status) === 'completed');
  const handoverReady = handoverReportReady && handoverCompleted;
  const finalHandoverReportReady = latestClosedLease
    ? asArray(workspace?.handovers).some(report => reportBelongsToLease(report, latestClosedLease.id ?? latestClosedLease.leaseId, 'move_out'))
    : false;
  const leaseClosure = latestClosedLease
    ? {
      leaseId: latestClosedLease.id ?? latestClosedLease.leaseId,
      endDate: latestClosedLease.endDate || null,
      tenantName: latestClosedLease.tenantName || null,
      status: finalHandoverReportReady ? 'completed' : 'awaiting_handover_report',
      reason: statusOf(latestClosedLease.status) === 'transferred'
        ? 'transfer'
        : statusOf(latestClosedLease.status) === 'terminated'
          ? 'early_termination'
          : 'normal_expiry',
      missingItems: finalHandoverReportReady ? [] : ['lease_end_handover_report'],
    }
    : { leaseId: null, endDate: null, tenantName: null, status: currentLease ? 'active' : 'not_started', reason: null, missingItems: [] };

  const stages = [
    stage(
      'mandateAuthorization',
      !currentMandate ? 'pending'
        : !authorizationDraft ? 'in_progress'
          : !authorizationSigned ? 'blocked'
            : mandateActive ? 'completed' : 'in_progress',
      'business',
      !currentMandate ? ['rental_mandate'] : !authorizationDraft ? ['authorization_draft'] : !authorizationSigned ? ['authorization_signature'] : !mandateActive ? ['mandate_review'] : [],
      !currentMandate
        ? action('create_mandate', { type: 'rentalMandate', action: 'create' })
        : !authorizationDraft
          ? action('generate_authorization', { type: 'rentalMandate', action: 'authorization' })
          : !authorizationSigned
            ? action('view_signing_status', { type: 'rentalMandate', action: 'sign' })
            : action('review_mandate', { type: 'rentalMandate', action: 'review' }),
      currentMandate && authorizationDraft && !authorizationSigned ? 'authorization_signing_required' : null,
    ),
    stage(
      'leasingSigning',
      !mandateActive ? 'pending' : leasingReady ? 'completed' : 'in_progress',
      'business',
      !mandateActive ? ['mandate_activation'] : !tenantReady ? ['tenant'] : !leaseReady ? ['lease'] : !otrReady ? ['otr_offer'] : !leaseContractReady ? ['lease_contract_signature'] : [],
      !mandateActive
        ? action('review_mandate', { type: 'rentalMandate', action: 'review' })
        : !tenantReady
          ? action('create_tenant', { type: 'tenancy', action: 'tenant-create' })
          : !leaseReady
            ? action('create_lease', { type: 'tenancy', action: 'lease-create' })
          : !otrReady
            ? action('generate_otr', { type: 'tenancy', action: 'otr' })
            : action('sign_lease_contract', { type: 'tenancy', action: 'lease-contract' }),
    ),
    stage(
      'moveInCollection',
      !leaseContractReady ? 'pending' : handoverReady ? 'completed' : 'in_progress',
      'business',
      !leaseContractReady ? ['lease_contract_signature'] : !handoverReportReady ? ['handover_report'] : !handoverReady ? ['handover'] : [],
      !leaseContractReady
        ? action('sign_lease_contract', { type: 'tenancy', action: 'lease-contract' })
        : !handoverReady
          ? action('complete_handover', { type: 'property', tab: 'summary' })
          : null,
    ),
  ];

  return {
    currentMandate,
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

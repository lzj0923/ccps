const sameId = (left, right) => left !== null && left !== undefined
  && right !== null && right !== undefined
  && String(left) === String(right);

const belongsToCurrentRental = (file, { mandateId, leaseId }) => {
  if (file.kind === 'mandate' || file.kind === 'handover') return sameId(file.mandateId, mandateId);
  if (['contract', 'leaseContract', 'receipt', 'paymentProof'].includes(file.kind)) {
    return sameId(file.leaseId, leaseId);
  }
  return false;
};

const belongsToStage = (file, stageKey) => {
  if (stageKey === 'preparation') return file.kind === 'handover';
  if (stageKey === 'mandateAuthorization') return file.kind === 'mandate';
  if (stageKey === 'leasingSigning') return ['contract', 'leaseContract'].includes(file.kind);
  if (stageKey === 'moveInCollection') return file.kind === 'handover';
  return false;
};

export function filterGeneratedFilesByStage(files = [], stageKey, currentRental = {}) {
  return (Array.isArray(files) ? files : [])
    .filter(file => belongsToStage(file, stageKey))
    .filter(file => belongsToCurrentRental(file, currentRental));
}

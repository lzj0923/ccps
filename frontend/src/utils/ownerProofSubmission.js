export function validateOwnerProof(form, files, today, maximum = 1000000) {
  if (!form.targetId) return 'target';
  if (!/^\d+(\.\d{1,2})?$/.test(String(form.amount)) || Number(form.amount) <= 0 || Number(form.amount) > maximum) return 'amount';
  if (!/^\d{4}-\d{2}-\d{2}$/.test(form.paymentDate || '') || form.paymentDate > today) return 'date';
  if (!form.bankName?.trim() || !form.reference?.trim() || !form.payerName?.trim()) return 'required';
  if (form.bankName.trim().length + form.reference.trim().length + 3 > 120 || form.payerName.trim().length > 160 || (form.note || '').length > 200) return 'length';
  if (!files.length || files.length > 4 || files.some(file => !['image/jpeg', 'image/png', 'application/pdf'].includes(file.type) || file.size <= 0 || file.size > 10 * 1024 * 1024)) return 'files';
  return '';
}

export function ownerProofPayload(form, kind) {
  const { amount, paymentDate, bankName, reference, payerName, note } = form;
  return { [kind === 'reserve' ? 'reserveAccountId' : 'installmentId']: form.targetId, amount, paymentDate,
    paymentMethod: 'bank_transfer', bankName, reference, payerName, note };
}

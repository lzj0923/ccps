// Ordinary debits use an unsigned amount; adjustments carry their own sign.
export function reserveTransactionPositive(type, amount) {
  if (type === 'adjustment') return Number(amount) >= 0;
  return ['topup', 'transfer_in', 'transfer_reverse_in'].includes(type);
}

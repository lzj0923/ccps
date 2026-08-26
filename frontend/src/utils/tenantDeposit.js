export const TENANT_DEPOSIT_MONTHS = 2.5;

export function calculateTenantDeposit(monthlyRent) {
  const rent = Number(monthlyRent);
  if (!Number.isFinite(rent) || rent <= 0) return 0;
  return Math.round((rent * TENANT_DEPOSIT_MONTHS + Number.EPSILON) * 100) / 100;
}

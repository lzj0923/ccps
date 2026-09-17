export const TENANT_DEPOSIT_MONTHS = 2.5;

export function calculateTenantDeposit(monthlyRent) {
  const rent = Number(monthlyRent);
  if (!Number.isFinite(rent) || rent <= 0) return 0;
  return Math.round((rent * TENANT_DEPOSIT_MONTHS + Number.EPSILON) * 100) / 100;
}

export function splitTenantDeposit(totalDeposit, monthlyRent, explicitUtilityDeposit) {
  const total = Math.max(0, Number(totalDeposit) || 0);
  const rent = Math.max(0, Number(monthlyRent) || 0);
  const requestedUtility = explicitUtilityDeposit == null || explicitUtilityDeposit === ''
    ? rent * 0.5
    : Math.max(0, Number(explicitUtilityDeposit) || 0);
  const utilityDeposit = Math.min(total, requestedUtility);
  return {
    securityDeposit: Math.round((total - utilityDeposit + Number.EPSILON) * 100) / 100,
    utilityDeposit: Math.round((utilityDeposit + Number.EPSILON) * 100) / 100,
  };
}

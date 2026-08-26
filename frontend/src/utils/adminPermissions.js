export const ADMIN_STAFF_ROLES = Object.freeze({
  SUPER_ADMIN: '超级管理员',
  FINANCE: '财务管理员',
  BUSINESS: '业务管理员',
  CUSTOMER_SERVICE: '客服管理员',
  ADMINISTRATION: '行政管理员'
});

const roleModules = {
  FINANCE: new Set(['adminSmartDashboard', 'adminDashboard', 'adminOwners', 'adminProperties', 'adminOffMarketProperties', 'adminData', 'adminDeposits', 'adminTenants', 'adminMaintenance', 'adminFinance', 'adminReserve', 'adminReports']),
  BUSINESS: new Set(['adminSmartDashboard', 'adminDashboard', 'adminProjects', 'adminOwners', 'adminProperties', 'adminOffMarketProperties', 'adminData', 'adminProcess', 'adminRentalSigning', 'adminDeposits', 'adminTenantDirectory', 'adminTenants', 'adminRentalMandates', 'adminMaintenance', 'adminFinance', 'adminReserve', 'adminReports']),
  CUSTOMER_SERVICE: new Set(['adminSmartDashboard', 'adminDashboard', 'adminOwners', 'adminProperties', 'adminOffMarketProperties', 'adminProcess', 'adminRentalSigning', 'adminDeposits', 'adminTenantDirectory', 'adminTenants', 'adminMaintenance', 'adminAlerts']),
  ADMINISTRATION: new Set(['adminSmartDashboard', 'adminDashboard', 'adminProjects', 'adminOwners', 'adminProperties', 'adminOffMarketProperties', 'adminData', 'adminProcess', 'adminRentalSigning', 'adminDeposits', 'adminTenantDirectory', 'adminTenants', 'adminRentalMandates', 'adminMaintenance', 'adminFinance', 'adminReserve', 'adminAlerts', 'adminReports', 'adminAccounts', 'adminAudit', 'adminSystemBackup'])
};

export function adminStaffRole(user) {
  const roles = Array.isArray(user?.roles) ? user.roles : [];
  return roles.map(role => String(role).toUpperCase()).find(role => ADMIN_STAFF_ROLES[role]) || '';
}

export function canAccessAdminModule(user, moduleId) {
  if (moduleId === 'adminSmartDashboard') return true;
  const role = adminStaffRole(user);
  // Existing installations remain usable until the RBAC migration is run.
  if (!role || role === 'SUPER_ADMIN') return true;
  return roleModules[role]?.has(moduleId) === true;
}

export function hasAdminPermission(user, permission) {
  const role = adminStaffRole(user);
  if (!role || role === 'SUPER_ADMIN') return true;
  return Array.isArray(user?.permissions)
    && user.permissions.map(value => String(value).toUpperCase()).includes(permission);
}

export function canManageAdminModule(user, moduleId) {
  if (['adminSmartDashboard', 'adminDashboard'].includes(moduleId)) return false;
  if (['adminAccounts', 'adminAudit', 'adminSystemBackup'].includes(moduleId)) return hasAdminPermission(user, 'SYSTEM_MANAGE');
  if (moduleId === 'adminReports') return hasAdminPermission(user, 'REPORT_MANAGE');
  if (['adminFinance', 'adminReserve', 'adminDeposits'].includes(moduleId)) return hasAdminPermission(user, 'FINANCE_MANAGE');
  if (['adminMaintenance', 'adminAlerts'].includes(moduleId)) {
    return hasAdminPermission(user, 'OPERATIONS_MANAGE') || hasAdminPermission(user, 'BUSINESS_MANAGE');
  }
  return hasAdminPermission(user, 'BUSINESS_MANAGE');
}

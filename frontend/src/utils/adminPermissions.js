export const ADMIN_STAFF_ROLES = Object.freeze({
  SUPER_ADMIN: '超级管理员',
  FINANCE: '财务',
  BUSINESS: '业务',
  CUSTOMER_SERVICE: '客服',
  ADMINISTRATION: '行政'
});

export const ADMIN_MODULE_IDS = Object.freeze([
  'adminSmartDashboard',
  'adminDashboard',
  'adminProjects',
  'adminOwners',
  'adminProperties',
  'adminData',
  'adminProcess',
  'adminRentalSigning',
  'adminDeposits',
  'adminTenantDirectory',
  'adminTenants',
  'adminRentalMandates',
  'adminOffMarketProperties',
  'adminMaintenance',
  'adminFinance',
  'adminReserve',
  'adminAlerts',
  'adminReports',
  'adminAccounts',
  'adminAudit',
  'adminSystemBackup'
]);

const allModules = new Set(ADMIN_MODULE_IDS);
const financeManagedModules = new Set([
  'adminMaintenance',
  'adminFinance',
  'adminReserve',
  'adminAlerts',
  'adminReports',
  'adminAccounts',
  'adminAudit',
  'adminSystemBackup'
]);

const rentalProcessTargetModules = new Set([
  'adminProperties',
  'adminRentalMandates',
  'adminTenantDirectory',
  'adminTenants'
]);

// 2026-09-03 CCPS 确认的岗位菜单及操作范围。
const roleAccess = {
  BUSINESS: {
    visible: new Set(['adminSmartDashboard', 'adminDashboard', 'adminProperties', 'adminProcess', 'adminRentalSigning', 'adminTenantDirectory', 'adminTenants']),
    managed: new Set(['adminProperties', 'adminProcess', 'adminRentalSigning', 'adminTenantDirectory'])
  },
  CUSTOMER_SERVICE: {
    visible: new Set(['adminSmartDashboard', 'adminDashboard', 'adminProcess', 'adminRentalSigning', 'adminRentalMandates', 'adminReserve']),
    managed: new Set(['adminProcess', 'adminRentalSigning', 'adminRentalMandates', 'adminReserve'])
  },
  FINANCE: {
    visible: allModules,
    managed: financeManagedModules
  },
  ADMINISTRATION: {
    visible: allModules,
    managed: allModules
  }
};

const roleDefaultModule = Object.freeze({
  BUSINESS: 'adminSmartDashboard',
  CUSTOMER_SERVICE: 'adminSmartDashboard',
  FINANCE: 'adminSmartDashboard',
  ADMINISTRATION: 'adminSmartDashboard',
  SUPER_ADMIN: 'adminSmartDashboard'
});

export function adminStaffRole(user) {
  const roles = Array.isArray(user?.roles) ? user.roles : [];
  return roles.map(role => String(role).toUpperCase()).find(role => ADMIN_STAFF_ROLES[role]) || '';
}

export function canAccessAdminModule(user, moduleId) {
  const role = adminStaffRole(user);
  // Existing installations remain usable until the RBAC migration is run.
  if (!role || role === 'SUPER_ADMIN') return true;
  return roleAccess[role]?.visible.has(moduleId) === true;
}

export function canAccessAdminProcessTarget(user, moduleId, source) {
  return source === 'rental-process'
    && canManageAdminModule(user, 'adminProcess')
    && rentalProcessTargetModules.has(moduleId);
}

export function defaultAdminModuleId(user) {
  const role = adminStaffRole(user);
  return roleDefaultModule[role] || 'adminSmartDashboard';
}

export function hasAdminPermission(user, permission) {
  const role = adminStaffRole(user);
  if (!role || role === 'SUPER_ADMIN') return true;
  return Array.isArray(user?.permissions)
    && user.permissions.map(value => String(value).toUpperCase()).includes(permission);
}

export function canManageAdminModule(user, moduleId) {
  if (['adminSmartDashboard', 'adminDashboard'].includes(moduleId)) return false;
  const role = adminStaffRole(user);
  if (!role || role === 'SUPER_ADMIN') return true;
  return roleAccess[role]?.managed.has(moduleId) === true;
}

const routeTable = [
  { path: '/login', name: 'login', mode: 'public', moduleId: null },
  { path: '/owner/login', name: 'owner-login', mode: 'public', portal: 'owner', moduleId: null },
  { path: '/admin/login', name: 'admin-login', mode: 'public', portal: 'admin', moduleId: null },
  { path: '/owner', name: 'owner-home', mode: 'owner', moduleId: 'myProperties' },
  { path: '/owner/projects', name: 'owner-projects', mode: 'owner', moduleId: 'ownerProjects' },
  { path: '/owner/properties', name: 'owner-properties', mode: 'owner', moduleId: 'myProperties' },
  { path: '/owner/rental-services', name: 'owner-rental-services', mode: 'owner', moduleId: 'ownerRentalHub' },
  { path: '/owner/more', name: 'owner-more', mode: 'owner', moduleId: 'ownerMore' },
  { path: '/owner/payment', name: 'owner-payment', mode: 'owner', moduleId: 'ownerPayment' },
  { path: '/owner/finance', name: 'owner-finance', mode: 'owner', moduleId: 'ownerFinance' },
  { path: '/owner/rent-income', name: 'owner-rent-income', mode: 'owner', moduleId: 'rentIncome' },
  { path: '/owner/expenses', name: 'owner-expenses', mode: 'owner', moduleId: 'ownerExpenses' },
  { path: '/owner/reserve', name: 'owner-reserve', mode: 'owner', moduleId: 'ownerReserve' },
  { path: '/owner/notifications', name: 'owner-notifications', mode: 'owner', moduleId: 'ownerNotice' },
  { path: '/owner/documents', name: 'owner-documents', mode: 'owner', moduleId: 'ownerDocuments' },
  { path: '/admin', name: 'admin-home', mode: 'admin', moduleId: 'adminSmartDashboard' },
  { path: '/admin/smart-dashboard', name: 'admin-smart-dashboard', mode: 'admin', moduleId: 'adminSmartDashboard' },
  { path: '/admin/dashboard', name: 'admin-dashboard', mode: 'admin', moduleId: 'adminDashboard' },
  { path: '/admin/projects', name: 'admin-projects', mode: 'admin', moduleId: 'adminProjects' },
  { path: '/admin/owners', name: 'admin-owners', mode: 'admin', moduleId: 'adminOwners' },
  { path: '/admin/properties', name: 'admin-properties', mode: 'admin', moduleId: 'adminProperties' },
  { path: '/admin/off-market-rentals', name: 'admin-off-market-rentals', mode: 'admin', moduleId: 'adminOffMarketProperties' },
  { path: '/admin/process', name: 'admin-process', mode: 'admin', moduleId: 'adminProcess' },
  { path: '/admin/rental-signing', name: 'admin-rental-signing', mode: 'admin', moduleId: 'adminRentalSigning' },
  { path: '/admin/deposits', name: 'admin-deposits', mode: 'admin', moduleId: 'adminDeposits' },
  { path: '/admin/contracts', name: 'admin-contracts', mode: 'admin', moduleId: 'adminContracts' },
  { path: '/admin/accounts', name: 'admin-accounts', mode: 'admin', moduleId: 'adminAccounts' },
  { path: '/admin/tenant-directory', name: 'admin-tenant-directory', mode: 'admin', moduleId: 'adminTenantDirectory' },
  { path: '/admin/tenants', name: 'admin-tenants', mode: 'admin', moduleId: 'adminTenants' },
  { path: '/admin/rental-mandates', name: 'admin-rental-mandates', mode: 'admin', moduleId: 'adminRentalMandates' },
  { path: '/admin/maintenance', name: 'admin-maintenance', mode: 'admin', moduleId: 'adminMaintenance' },
  { path: '/admin/finance', name: 'admin-finance', mode: 'admin', moduleId: 'adminFinance' },
  { path: '/admin/buildings', name: 'admin-buildings', mode: 'admin', moduleId: 'adminData' },
  { path: '/admin/reserve', name: 'admin-reserve', mode: 'admin', moduleId: 'adminReserve' },
  { path: '/admin/alerts', name: 'admin-alerts', mode: 'admin', moduleId: 'adminAlerts' },
  { path: '/admin/reports', name: 'admin-reports', mode: 'admin', moduleId: 'adminReports' },
  { path: '/admin/audit', name: 'admin-audit', mode: 'admin', moduleId: 'adminAudit' },
  { path: '/admin/system-backup', name: 'admin-system-backup', mode: 'admin', moduleId: 'adminSystemBackup' }
];

const modulePathMap = Object.fromEntries(routeTable.filter(route => route.moduleId).map(route => [route.moduleId, route.path]));
modulePathMap.adminOwners = '/admin/owners';
modulePathMap.adminRentalMandates = '/admin/rental-mandates';

export function resolveRoute(location = window.location) {
  const pathname = String(location.pathname || '/').replace(/\/$/, '') || '/';
  const exact = routeTable.find(route => route.path === pathname);
  if (exact) return exact;
  const propertyDetail = pathname.match(/^\/admin\/properties\/([^/]+)$/);
  if (propertyDetail) return { path: pathname, name: 'admin-property-detail', mode: 'admin', moduleId: 'adminProperties', propertyId: propertyDetail[1] };
  const offMarketPropertyDetail = pathname.match(/^\/admin\/off-market-rentals\/([^/]+)$/);
  if (offMarketPropertyDetail) return { path: pathname, name: 'admin-off-market-rental-detail', mode: 'admin', moduleId: 'adminOffMarketProperties', propertyId: offMarketPropertyDetail[1] };
  const signature = pathname.match(/^\/sign\/([^/]+)$/);
  if (signature) return { path: pathname, name: 'public-signature', mode: 'public', moduleId: null, signatureToken: signature[1] };

  const queryPage = new URLSearchParams(location.search || '').get('page');
  const legacyRoute = queryPage && routeTable.find(route => route.moduleId === queryPage);
  if (legacyRoute && (!pathname || pathname === '/')) return legacyRoute;
  if (pathname.startsWith('/admin')) return routeTable.find(route => route.name === 'admin-home');
  if (pathname.startsWith('/owner')) return routeTable.find(route => route.name === 'owner-home');
  return routeTable.find(route => route.name === 'owner-home');
}

export function routeForModule(moduleId) {
  return modulePathMap[moduleId] || (String(moduleId).startsWith('admin') ? '/admin' : '/owner');
}

export function navigate(path, { replace = false } = {}) {
  if (window.location.pathname !== path || window.location.search) {
    window.history[replace ? 'replaceState' : 'pushState']({}, '', path);
  }
  window.dispatchEvent(new CustomEvent('app-route-change'));
}

export function routes() { return routeTable.slice(); }

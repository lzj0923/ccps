const routeTable = [
  { path: '/login', name: 'login', mode: 'public', moduleId: null },
  { path: '/owner/login', name: 'owner-login', mode: 'public', portal: 'owner', moduleId: null },
  { path: '/admin/login', name: 'admin-login', mode: 'public', portal: 'admin', moduleId: null },
  { path: '/owner', name: 'owner-home', mode: 'owner', moduleId: 'myProperties' },
  { path: '/owner/properties', name: 'owner-properties', mode: 'owner', moduleId: 'myProperties' },
  { path: '/owner/payment', name: 'owner-payment', mode: 'owner', moduleId: 'ownerPayment' },
  { path: '/owner/rent-income', name: 'owner-rent-income', mode: 'owner', moduleId: 'rentIncome' },
  { path: '/owner/expenses', name: 'owner-expenses', mode: 'owner', moduleId: 'ownerExpenses' },
  { path: '/owner/reserve', name: 'owner-reserve', mode: 'owner', moduleId: 'ownerReserve' },
  { path: '/owner/notifications', name: 'owner-notifications', mode: 'owner', moduleId: 'ownerNotice' },
  { path: '/owner/documents', name: 'owner-documents', mode: 'owner', moduleId: 'ownerDocuments' },
  { path: '/admin', name: 'admin-home', mode: 'admin', moduleId: 'adminOwners' },
  { path: '/admin/owners', name: 'admin-owners', mode: 'admin', moduleId: 'adminOwners' },
  { path: '/admin/properties', name: 'admin-properties', mode: 'admin', moduleId: 'adminProperties' },
  // Account CRUD is embedded in the existing owner/property workspace.
  { path: '/admin/accounts', name: 'admin-accounts', mode: 'admin', moduleId: 'adminOwners' },
  { path: '/admin/tenants', name: 'admin-tenants', mode: 'admin', moduleId: 'adminTenants' },
  { path: '/admin/rental-mandates', name: 'admin-rental-mandates', mode: 'admin', moduleId: 'adminRentalMandates' },
  { path: '/admin/maintenance', name: 'admin-maintenance', mode: 'admin', moduleId: 'adminMaintenance' },
  { path: '/admin/finance', name: 'admin-finance', mode: 'admin', moduleId: 'adminFinance' },
  { path: '/admin/buildings', name: 'admin-buildings', mode: 'admin', moduleId: 'adminData' },
  { path: '/admin/reserve', name: 'admin-reserve', mode: 'admin', moduleId: 'adminReserve' },
  { path: '/admin/alerts', name: 'admin-alerts', mode: 'admin', moduleId: 'adminAlerts' },
  { path: '/admin/sync', name: 'admin-sync', mode: 'admin', moduleId: 'adminSync' },
  { path: '/admin/reports', name: 'admin-reports', mode: 'admin', moduleId: 'adminReports' }
];

const modulePathMap = Object.fromEntries(routeTable.filter(route => route.moduleId).map(route => [route.moduleId, route.path]));
modulePathMap.adminOwners = '/admin/owners';
modulePathMap.adminRentalMandates = '/admin/rental-mandates';

export function resolveRoute(location = window.location) {
  const pathname = String(location.pathname || '/').replace(/\/$/, '') || '/';
  const exact = routeTable.find(route => route.path === pathname);
  if (exact) return exact;

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

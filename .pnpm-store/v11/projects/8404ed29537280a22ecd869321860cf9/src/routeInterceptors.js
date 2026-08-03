export function userMode(user) {
  const role = String(user?.role || user?.userRole || user?.type || '').toLowerCase();
  return role.includes('admin') || role.includes('manager') ? 'admin' : 'owner';
}

export function adminAuthInterceptor(route, user) {
  if (route.mode !== 'admin') return null;
  if (!user) return '/admin/login';
  return userMode(user) === 'admin' ? null : '/admin/login';
}

export function ownerAuthInterceptor(route, user) {
  if (route.mode !== 'owner') return null;
  if (!user) return '/owner/login';
  return userMode(user) === 'owner' ? null : '/owner/login';
}

export function interceptRoute(route, user) {
  return adminAuthInterceptor(route, user) || ownerAuthInterceptor(route, user);
}

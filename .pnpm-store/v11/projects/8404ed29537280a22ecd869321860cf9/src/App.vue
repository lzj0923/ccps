<template>
  <div id="app" v-cloak>
    <SignaturePage v-if="authReady && isSignaturePage" />
    <LoginPage v-else-if="authReady && showLogin" :portal="loginPortal" @login="handleLogin" />
    <OwnerSystem v-else-if="authReady && systemMode === 'owner'" />
    <AdminSystem v-else-if="authReady" />
  </div>
</template>

<script>
import OwnerSystem from './systems/owner/OwnerSystem.vue';
import AdminSystem from './systems/admin/AdminSystem.vue';
import LoginPage from './pages/LoginPage.vue';
import SignaturePage from './pages/SignaturePage.vue';
import dashboardState from './composables/dashboardState';
import dashboardViewModel from './composables/dashboardViewModel';
import dashboardActions from './composables/dashboardActions';
import { fetchSession, logout } from './services/propertyApi';
import { navigate, resolveRoute } from './router';
import { interceptRoute, userMode } from './routeInterceptors';

export default {
  components: { OwnerSystem, AdminSystem, LoginPage, SignaturePage },
  data() {
    const route = resolveRoute();
    return {
      authReady: false,
      currentUsers: { admin: null, owner: null },
      routeIsPublic: route.mode === 'public',
      routePortal: route.portal || null,
      routeMode: route.mode === 'admin' || route.portal === 'admin' ? 'admin' : 'owner'
    };
  },
  mixins: [dashboardState, dashboardViewModel, dashboardActions],
  computed: {
    systemMode() { return this.routeMode; },
    currentUser() { return this.currentUsers[this.systemMode] || null; },
    authenticated() { return Boolean(this.currentUser); },
    isSignaturePage() { return resolveRoute().name === 'public-signature'; },
    showLogin() { return !this.authenticated || this.routeIsPublic; },
    loginPortal() { return this.routePortal; }
  },
  async mounted() {
    window.addEventListener('popstate', this.syncRoute);
    window.addEventListener('app-route-change', this.syncRoute);
    window.addEventListener('ccps-auth-expired', this.handleSessionExpired);
    const [adminSession, ownerSession] = await Promise.allSettled([
      fetchSession('admin'),
      fetchSession('owner')
    ]);
    this.currentUsers = {
      admin: adminSession.status === 'fulfilled' ? adminSession.value : null,
      owner: ownerSession.status === 'fulfilled' ? ownerSession.value : null
    };
    window.localStorage.removeItem('ccps-authenticated');
    window.localStorage.removeItem('ccps-user');
    this.authReady = true;
    this.syncRoute();
  },
  beforeUnmount() {
    window.removeEventListener('popstate', this.syncRoute);
    window.removeEventListener('app-route-change', this.syncRoute);
    window.removeEventListener('ccps-auth-expired', this.handleSessionExpired);
  },
  methods: {
    roleMode(user) {
      return userMode(user);
    },
    syncRoute() {
      const route = resolveRoute();
      this.routeIsPublic = route.mode === 'public';
      this.routePortal = route.portal || null;
      this.routeMode = route.mode === 'admin' || route.portal === 'admin' ? 'admin' : 'owner';
      if (!this.authReady) return;
      const portal = route.mode === 'admin' || route.portal === 'admin' ? 'admin' : 'owner';
      const redirect = interceptRoute(route, this.currentUsers[portal]);
      if (redirect && redirect !== window.location.pathname) {
        navigate(redirect, { replace: true });
        return;
      }
      if (route.moduleId && route.moduleId !== this.currentId) this.currentId = route.moduleId;
    },
    handleLogin(user, portal) {
      const mode = portal === 'admin' || portal === 'owner' ? portal : this.roleMode(user);
      this.currentUsers = { ...this.currentUsers, [mode]: user };
      navigate(`/${mode}`, { replace: true });
    },
    handleSessionExpired(event) {
      const route = resolveRoute();
      const activeMode = route.mode === 'admin' || route.portal === 'admin' ? 'admin' : 'owner';
      const expiredMode = event?.detail?.portal || activeMode;
      this.currentUsers = { ...this.currentUsers, [expiredMode]: null };
      if (expiredMode === activeMode) navigate(`/${expiredMode}/login`, { replace: true });
    },
    async handleLogout() {
      const mode = this.systemMode;
      try {
        await logout(mode);
        this.currentUsers = { ...this.currentUsers, [mode]: null };
        navigate(`/${mode}/login`, { replace: true });
        return true;
      } catch (error) {
        this.showToast(error.message || '退出登入失敗，請稍後再試');
        return false;
      }
    }
  },
  provide() { return { page: this }; }
}
</script>

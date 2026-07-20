<template>
  <main class="login-page" :class="{ 'admin-login-page': isAdminPortal }">
    <section class="login-visual" aria-label="CCPS 物業管理">
      <div class="login-visual-inner">
        <div class="login-brand"><img class="ccps-login-logo" src="/ccps-logo.png" alt="CCPS 家慶企業" /></div>

        <div v-if="isAdminPortal" class="admin-login-intro">
          <span class="admin-eyebrow">CCPS / 管理控制台</span>
          <h1>掌握全局，<br /><em>安心管理。</em></h1>
          <p>安全存取物業組合、財務審核、維修流程與系統營運資料。</p>
          <div class="admin-login-status"><i></i><span>所有系統運作正常</span><b>安全工作階段</b></div>
        </div>
        <div v-else class="login-intro"><span class="eyebrow">SMART PROPERTY OPERATIONS</span><h1>Property operations,<br /><em>made clearer.</em></h1><p>Manage properties, owners, leases and finance in one focused workspace.</p></div>

        <div v-if="isAdminPortal" class="admin-login-footer"><div><strong>09</strong><span>營運模組</span></div><div><strong>24/7</strong><span>稽核可見性</span></div><div><strong>01</strong><span>受保護工作區</span></div></div>
        <div v-else class="login-visual-footer"><div><strong>24/7</strong><span>Traceable data</span></div><div><strong>360°</strong><span>Full workflows</span></div><div><strong>100%</strong><span>Visible operations</span></div></div>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-form-wrap">
        <div v-if="isAdminPortal" class="admin-panel-heading"><span class="admin-panel-mark">A</span><div><span class="login-kicker">管理員存取</span><h2>{{ portalTitle }}</h2></div></div>
        <div class="mobile-login-brand"><img class="ccps-login-logo" src="/ccps-logo.png" alt="CCPS 家慶企業" /></div>
        <template v-if="!isAdminPortal"><span class="login-kicker">WELCOME BACK</span><h2>{{ portalTitle }}</h2></template>
        <p class="login-subtitle">{{ portalSubtitle }}</p>

        <form class="login-form" @submit.prevent="submitLogin">
          <label for="login-username">{{ usernameLabel }}</label>
          <div class="login-input" :class="{ focused: focusedField === 'username' }"><span class="login-input-icon">◎</span><input id="login-username" v-model.trim="username" type="text" autocomplete="username" :placeholder="usernamePlaceholder" @focus="focusedField = 'username'" @blur="focusedField = ''" /></div>
          <label for="login-password">{{ passwordLabel }}</label>
          <div class="login-input" :class="{ focused: focusedField === 'password' }"><span class="login-input-icon">●</span><input id="login-password" v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" :placeholder="passwordPlaceholder" @focus="focusedField = 'password'" @blur="focusedField = ''" /><button type="button" class="password-toggle" @click="showPassword = !showPassword">{{ showPassword ? '隱藏' : '顯示' }}</button></div>
          <div class="login-options"><label class="remember-option"><input v-model="rememberMe" type="checkbox" /> <span>{{ rememberLabel }}</span></label><button type="button" class="forgot-link" @click="notice = forgotNotice">{{ forgotLabel }}</button></div>
          <p v-if="errorMessage" class="login-error" role="alert">{{ errorMessage }}</p>
          <button class="login-submit" type="submit" :disabled="submitting">{{ submitting ? submittingLabel : submitLabel }}<span>→</span></button>
        </form>

        <p class="login-demo">{{ demoLabel }}：<strong>{{ demoUsername }}</strong><span> / </span><strong>123456</strong></p>
        <p v-if="notice" class="login-notice">{{ notice }}</p>
        <p class="login-copyright">© 2026 CCPS Property Management · {{ workspaceLabel }}</p>
      </div>
    </section>
  </main>
</template>

<script>
import '../admin-login.css';
import { login } from '../services/propertyApi';

export default {
  props: { portal: { type: String, default: null } },
  emits: ['login'],
  data() {
    return { username: '', password: '', rememberMe: true, showPassword: false, focusedField: '', submitting: false, errorMessage: '', notice: '' };
  },
  computed: {
    isAdminPortal() { return this.portal === 'admin'; },
    portalTitle() { return this.isAdminPortal ? '管理員登入' : this.portal === 'owner' ? 'Owner sign in' : 'Welcome back'; },
    portalSubtitle() { return this.isAdminPortal ? '登入管理中心以繼續工作' : `Sign in to your ${this.portal === 'owner' ? 'owner' : 'property management'} workspace`; },
    usernameLabel() { return this.isAdminPortal ? '帳號或電子郵件' : 'Username or email'; },
    passwordLabel() { return this.isAdminPortal ? '密碼' : 'Password'; },
    usernamePlaceholder() { return this.isAdminPortal ? '請輸入管理員帳號或電子郵件' : 'Enter username or email'; },
    passwordPlaceholder() { return this.isAdminPortal ? '請輸入登入密碼' : 'Enter password'; },
    rememberLabel() { return this.isAdminPortal ? '記住登入狀態' : 'Remember me'; },
    forgotLabel() { return this.isAdminPortal ? '忘記密碼？' : 'Forgot password?'; },
    forgotNotice() { return this.isAdminPortal ? '請聯絡系統管理員重設密碼。' : 'Please contact your system administrator.'; },
    submitLabel() { return this.isAdminPortal ? '登入管理中心' : 'Sign in'; },
    submittingLabel() { return this.isAdminPortal ? '登入中…' : 'Signing in…'; },
    demoLabel() { return this.isAdminPortal ? '測試帳號' : 'Demo account'; },
    workspaceLabel() { return this.isAdminPortal ? '安全管理工作區' : 'Secure workspace'; },
    demoUsername() { return this.portal === 'owner' ? 'owner' : 'admin'; }
  },
  methods: {
    async submitLogin() {
      this.errorMessage = '';
      this.notice = '';
      if (!this.username || !this.password) { this.errorMessage = this.isAdminPortal ? '請輸入帳號與密碼' : 'Username and password are required'; return; }
      this.submitting = true;
      try {
        const user = await login({ identifier: this.username, password: this.password, rememberMe: this.rememberMe }, this.portal);
        this.$emit('login', user, this.portal);
      } catch (error) {
        this.errorMessage = error.message || (this.isAdminPortal ? '登入失敗，請稍後再試。' : 'Login failed. Please try again.');
      } finally { this.submitting = false; }
    }
  }
};
</script>

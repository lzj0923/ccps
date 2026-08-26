<template>
  <main class="login-page" :class="{ 'admin-login-page': isAdminPortal }">
    <section class="login-visual" :aria-label="$t('legacy.t_9a77c831d013')">
      <div class="login-visual-inner">
        <div class="login-brand"><img class="ccps-login-logo" src="/ccps-logo.png" :alt="$t('legacy.t_107b9d73136a')" /></div>

        <div v-if="isAdminPortal" class="admin-login-intro">
          <span class="admin-eyebrow">{{ $t('login.adminConsole') }}</span>
          <h1>{{ $t('login.adminHeadline') }}<br /><em>{{ $t('login.adminHeadlineEmphasis') }}</em></h1>
          <p>{{ $t('login.adminDescription') }}</p>
          <div class="admin-login-status"><i></i><span>{{ $t('login.allSystemsNormal') }}</span><b>{{ $t('login.secureSession') }}</b></div>
        </div>
        <div v-else class="login-intro"><span class="eyebrow">{{ $t('legacy.t_27995c2c24fd') }}</span><h1>{{ $t('legacy.t_8eb83516d30b') }}<br /><em>{{ $t('legacy.t_84d8e61a5f97') }}</em></h1><p>{{ $t('legacy.t_a7090aee6b4d') }}</p></div>

        <div v-if="isAdminPortal" class="admin-login-footer"><div><strong>{{ $t('legacy.t_4b581cdce628') }}</strong><span>{{ $t('login.operationModules') }}</span></div><div><strong>{{ $t('legacy.t_c41127dfda00') }}</strong><span>{{ $t('login.auditVisibility') }}</span></div><div><strong>{{ $t('legacy.t_ddfe163345d3') }}</strong><span>{{ $t('login.protectedWorkspace') }}</span></div></div>
        <div v-else class="login-visual-footer"><div><strong>{{ $t('legacy.t_c41127dfda00') }}</strong><span>{{ $t('legacy.t_9a60a84ff749') }}</span></div><div><strong>{{ $t('legacy.t_d5d19a2c18ab') }}</strong><span>{{ $t('legacy.t_5a7883b96087') }}</span></div><div><strong>{{ $t('legacy.t_fae31ecec0fc') }}</strong><span>{{ $t('legacy.t_0ee351b65ab8') }}</span></div></div>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-form-wrap">
        <div class="login-language"><LanguageSwitcher /></div>
        <div v-if="isAdminPortal" class="admin-panel-heading"><span class="admin-panel-mark">{{ $t('legacy.t_6dcd4ce23d88') }}</span><div><span class="login-kicker">{{ $t('login.adminAccess') }}</span><h2>{{ portalTitle }}</h2></div></div>
        <div class="mobile-login-brand"><img class="ccps-login-logo" src="/ccps-logo.png" :alt="$t('legacy.t_107b9d73136a')" /></div>
        <template v-if="!isAdminPortal"><span class="login-kicker">{{ $t('login.welcomeBack') }}</span><h2>{{ portalTitle }}</h2></template>
        <p class="login-subtitle">{{ portalSubtitle }}</p>

        <form class="login-form" @click="countryMenuOpen = false" @submit.prevent="submitLogin">
          <label for="login-username">{{ usernameLabel }}</label>
          <div v-if="isOwnerPortal" class="login-input phone-login-input" :class="{ focused: focusedField === 'username' }">
            <div class="phone-country-picker" @click.stop>
              <button type="button" class="phone-country-trigger" :aria-expanded="countryMenuOpen" aria-haspopup="listbox" @click="toggleCountryMenu">
                <span>{{ selectedLoginCountry.label }}</span><b>{{ selectedLoginCountry.dialCode }}</b><i>⌄</i>
              </button>
              <div v-if="countryMenuOpen" class="phone-country-menu" role="listbox" @click.stop>
                <input ref="countrySearch" v-model.trim="countryQuery" type="search" :placeholder="countrySearchPlaceholder" autocomplete="off" @keydown.esc="countryMenuOpen = false" />
                <button v-for="country in filteredLoginCountries" :key="country.code" type="button" class="phone-country-option" :class="{ selected: country.code === loginCountry }" role="option" :aria-selected="country.code === loginCountry" @click="selectLoginCountry(country)">
                  <span>{{ country.label }}</span><b>{{ country.dialCode }}</b>
                </button>
                <span v-if="!filteredLoginCountries.length" class="phone-country-empty">{{ countryEmptyLabel }}</span>
              </div>
            </div>
            <span class="login-input-icon">◎</span><input id="login-username" v-model.trim="username" type="text" autocomplete="username" :placeholder="usernamePlaceholder" @focus="focusedField = 'username'" @blur="focusedField = ''" />
          </div>
          <div v-else class="login-input" :class="{ focused: focusedField === 'username' }"><span class="login-input-icon">◎</span><input id="login-username" v-model.trim="username" type="text" autocomplete="username" :placeholder="usernamePlaceholder" @focus="focusedField = 'username'" @blur="focusedField = ''" /></div>
          <label for="login-password">{{ passwordLabel }}</label>
          <div class="login-input" :class="{ focused: focusedField === 'password' }"><span class="login-input-icon">●</span><input id="login-password" v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" :placeholder="passwordPlaceholder" @focus="focusedField = 'password'" @blur="focusedField = ''" /><button type="button" class="password-toggle" @click="showPassword = !showPassword">{{ showPassword ? $t('login.hide') : $t('login.show') }}</button></div>
          <div class="login-options"><label class="remember-option"><input v-model="rememberMe" type="checkbox" /> <span>{{ rememberLabel }}</span></label><button type="button" class="forgot-link" @click="notice = forgotNotice">{{ forgotLabel }}</button></div>
          <p v-if="errorMessage" class="login-error" role="alert">{{ errorMessage }}</p>
          <button class="login-submit" type="submit" :disabled="submitting">{{ submitting ? submittingLabel : submitLabel }}<span>→</span></button>
        </form>

        <p class="login-demo">{{ demoLabel }}：<strong>{{ demoUsername }}</strong><span> / </span><strong>{{ $t('legacy.t_7c4a8d09ca37') }}</strong></p>
        <p v-if="notice" class="login-notice">{{ notice }}</p>
        <p class="login-copyright">{{ $t('legacy.t_0734a82b1ce9') }} {{ workspaceLabel }}</p>
      </div>
    </section>
  </main>
</template>

<script>
import '../admin-login.css';
import { login } from '../services/propertyApi';
import LanguageSwitcher from '../components/LanguageSwitcher.vue';
import { PHONE_COUNTRIES } from '../utils/tenantPhone';

export default {
  props: { portal: { type: String, default: null } },
  emits: ['login'],
  components: { LanguageSwitcher },
  data() {
    return { username: '', password: '', rememberMe: true, showPassword: false, focusedField: '', submitting: false, errorMessage: '', notice: '', loginCountry: 'MY', countryQuery: '', countryMenuOpen: false, phoneCountries: PHONE_COUNTRIES };
  },
  computed: {
    isAdminPortal() { return this.portal === 'admin'; }, isOwnerPortal() { return this.portal === 'owner'; },
    filteredLoginCountries() { const query = this.countryQuery.toLowerCase(); return this.phoneCountries.filter(country => !query || `${country.label} ${country.code} ${country.dialCode}`.toLowerCase().includes(query)); },
    selectedLoginCountry() { return this.phoneCountries.find(country => country.code === this.loginCountry) || this.phoneCountries[0]; },
    portalTitle() { return this.isAdminPortal ? this.$t('login.adminSignIn') : this.portal === 'owner' ? this.$t('login.ownerSignIn') : this.$t('login.welcomeBack'); },
    portalSubtitle() { return this.isAdminPortal ? this.$t('login.signInToAdmin') : this.$t('login.ownerSubtitle'); },
    usernameLabel() { return this.isOwnerPortal ? this.$t('login.ownerPhoneOrAccount') : this.$t('login.username'); }, passwordLabel() { return this.$t('login.password'); },
    usernamePlaceholder() { return this.isOwnerPortal ? this.$t('login.ownerPhonePlaceholder') : this.$t('login.usernamePlaceholder'); }, passwordPlaceholder() { return this.$t('login.passwordPlaceholder'); },
    countrySearchPlaceholder() { return this.$t('login.searchCountry'); }, countryEmptyLabel() { return this.$t('login.noCountry'); },
    rememberLabel() { return this.$t('login.remember'); }, forgotLabel() { return this.$t('login.forgot'); }, forgotNotice() { return this.$t('login.forgotNotice'); },
    submitLabel() { return this.$t('login.signIn'); }, submittingLabel() { return this.$t('login.signingIn'); },
    demoLabel() { return this.$t('login.demo'); }, workspaceLabel() { return this.$t('login.secureWorkspace'); },
    demoUsername() { return this.portal === 'owner' ? 'owner' : 'admin'; }
  },
  methods: {
    toggleCountryMenu() { this.countryMenuOpen = !this.countryMenuOpen; if (this.countryMenuOpen) this.$nextTick(() => this.$refs.countrySearch?.focus()); },
    selectLoginCountry(country) { this.loginCountry = country.code; this.countryQuery = ''; this.countryMenuOpen = false; },
    loginIdentifier() {
      const raw = String(this.username || '').trim();
      if (!this.isOwnerPortal || !raw || raw.includes('@')) return raw;
      if (raw.startsWith('+')) return `+${raw.slice(1).replace(/\D/g, '')}`;
      if (/^[\d\s().-]+$/.test(raw)) { const national = raw.replace(/\D/g, '').replace(/^0+/, ''); return national ? `${this.selectedLoginCountry.dialCode}${national}` : raw; }
      return raw;
    },
    async submitLogin() {
      this.errorMessage = '';
      this.notice = '';
      if (!this.username || !this.password) { this.errorMessage = this.$t('login.required'); return; }
      this.submitting = true;
      try {
        const user = await login({ identifier: this.loginIdentifier(), password: this.password, rememberMe: this.rememberMe }, this.portal);
        this.$emit('login', user, this.portal);
      } catch (error) {
        this.errorMessage = error.message || this.$t('login.failed');
      } finally { this.submitting = false; }
    }
  }
};
</script>

<template>
  <section class="owner-self-service">
    <h2>{{ $t('ownerApp.account.title') }}</h2>
    <p>{{ $t('ownerApp.account.scope') }}</p>
    <button v-if="!opened" type="button" @click="load">{{ $t('ownerApp.account.edit') }}</button>
    <div v-else>
      <p v-if="loading" aria-live="polite">{{ $t('legacy.t_03a17d236ff2') }}</p>
      <form v-else-if="profile" @submit.prevent="save">
        <fieldset :disabled="busy"><legend>{{ profile.fullName }}</legend>
          <label v-for="key in ['mobilePhone', 'homePhone', 'officePhone']" :key="key" class="owner-app-field"><span>{{ $t('ownerApp.account.' + key) }}</span><input v-model.trim="form[key]" type="tel" maxlength="40"></label>
          <label class="owner-app-field"><span>{{ $t('ownerApp.account.mailingAddress') }}</span><textarea v-model.trim="form.mailingAddress" maxlength="500" rows="3"></textarea></label>
          <div class="owner-filter-actions"><button type="submit">{{ $t('ownerApp.account.save') }}</button><button type="button" @click="close">{{ $t('ownerApp.account.cancel') }}</button></div>
        </fieldset>
      </form>
      <button v-else-if="!loading" type="button" @click="load">{{ $t('ownerApp.retry') }}</button>
      <details v-if="profile" @toggle="clearPasswords">
        <summary>{{ $t('ownerApp.account.password') }}</summary>
        <p>{{ $t('ownerApp.account.passwordHint') }}</p>
        <form @submit.prevent="changePassword"><fieldset :disabled="busy">
          <label class="owner-app-field"><span>{{ $t('ownerApp.account.currentPassword') }}</span><input v-model="password.currentPassword" type="password" autocomplete="current-password" required maxlength="72"></label>
          <label class="owner-app-field"><span>{{ $t('ownerApp.account.newPassword') }}</span><input v-model="password.newPassword" type="password" autocomplete="new-password" required minlength="10" maxlength="72"></label>
          <label class="owner-app-field"><span>{{ $t('ownerApp.account.confirmPassword') }}</span><input v-model="confirmPassword" type="password" autocomplete="new-password" required maxlength="72"></label>
          <button type="submit">{{ $t('ownerApp.account.password') }}</button>
        </fieldset></form>
      </details>
    </div>
    <p v-if="error" role="alert">{{ $lt(error) }}</p><p v-if="message" role="status">{{ message }}</p>
  </section>
</template>
<script>
import { fetchOwnerAccount, updateOwnerAccount, changeOwnerPassword } from '../../services/propertyApi';
export default {
  emits: ['password-changed'],
  data: () => ({ opened: false, loading: false, busy: false, profile: null, form: {}, error: '', message: '', password: { currentPassword: '', newPassword: '' }, confirmPassword: '' }),
  methods: {
    async load() { this.opened = true; this.loading = true; this.error = ''; this.message = ''; try { this.profile = await fetchOwnerAccount(); this.form = { ...this.profile }; } catch (e) { this.error = e.message; } finally { this.loading = false; } },
    close() { if (this.busy) return; this.opened = false; this.profile = null; this.form = {}; this.clearPasswords(); },
    clearPasswords() { this.password = { currentPassword: '', newPassword: '' }; this.confirmPassword = ''; },
    async save() {
      if (this.busy) return; this.busy = true; this.error = ''; this.message = '';
      try { const { mobilePhone, homePhone, officePhone, mailingAddress } = this.form; this.profile = await updateOwnerAccount({ mobilePhone, homePhone, officePhone, mailingAddress }); this.form = { ...this.profile }; this.message = this.$t('ownerApp.account.saved'); }
      catch (e) { this.error = e.message; } finally { this.busy = false; }
    },
    async changePassword() {
      if (this.busy) return; this.error = ''; this.message = '';
      if (this.password.newPassword !== this.confirmPassword) { this.error = this.$t('ownerApp.account.mismatch'); return; }
      this.busy = true;
      try { await changeOwnerPassword(this.password); this.clearPasswords(); this.message = this.$t('ownerApp.account.signInAgain'); this.$emit('password-changed'); }
      catch (e) { this.error = e.message; } finally { this.busy = false; }
    }
  }
};
</script>

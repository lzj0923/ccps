<template>
  <section class="panel admin-accounts-workspace">
    <div class="owners-panel-head">
      <div><h2>{{ $t('legacy.t_d15f7ecd95ea') }}</h2><span>{{ filteredAccounts.length }} / {{ accounts.length }} {{ $t('legacy.t_5b01eaed842d') }}</span></div>
      <span v-if="loading">{{ $t('legacy.t_884a89a6bd46') }}</span>
    </div>

    <div v-if="loadError" class="admin-owner-state error" role="alert">
      <strong>{{ $t('legacy.t_a26e1a9b0b1b') }}</strong><span>{{ loadError }}</span><button type="button" @click="loadAccounts">{{ $t('legacy.t_0c9157b5bfac') }}</button>
    </div>
    <div v-else class="table-wrap">
      <table>
        <thead><tr><th>{{ $t('legacy.t_fd9df0166c0d') }}</th><th>{{ $t('legacy.t_46f3d1c2f818') }}</th><th>{{ $t('legacy.t_e5fb9b389e3e') }}</th><th>{{ $t('legacy.t_2dd7a66ef301') }}</th><th>{{ $t('legacy.t_45293595eae3') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead>
        <tbody>
          <tr v-for="account in pagedAccounts" :key="account.id">
            <td>{{ account.username }}</td>
            <td>{{ account.displayName }}</td>
            <td>{{ account.phone || '—' }}</td>
            <td>{{ account.ownerId ? `業主 #${account.ownerId}` : '—' }}</td>
            <td><span class="tag" :class="account.status === 'active' ? 'green' : 'gray'">{{ account.status === 'active' ? $t('legacy.t_ce6c3dc32674') : $t('legacy.t_d989e55188c9') }}</span></td>
            <td class="account-actions"><button type="button" @click="openEdit(account)">{{ $t('legacy.t_c9c77517fe85') }}</button><button type="button" class="danger" @click="removeAccount(account)">{{ account.status === 'active' ? $t('legacy.t_d989e55188c9') : $t('legacy.t_ca33657147e6') }}</button></td>
          </tr>
          <tr v-if="!loading && !filteredAccounts.length"><td colspan="6" class="admin-owner-empty">{{ $t('legacy.t_ba4ced55e182') }}</td></tr>
        </tbody>
      </table>
    </div>
    <AdminListPager :page="pageNumber" :page-size="pageSize" :total="filteredAccounts.length" @update:page="pageNumber=$event" @update:page-size="pageSize=$event;pageNumber=1" />

    <dialog ref="accountDialog" class="modal admin-account-dialog">
      <form method="dialog" @submit.prevent="saveAccount">
        <div class="modal-head"><h3>{{ editingId ? $t('legacy.t_a1096f6be72b') : $t('legacy.t_7ed89c848748') }}</h3><button class="icon-close" type="button" @click="closeDialog">×</button></div>
        <div class="form-grid">
          <label>{{ $t('legacy.t_fd9df0166c0d') }}<input v-model.trim="form.username" maxlength="80" required :placeholder="$t('legacy.t_ca43b3065fc5')"></label>
          <label>{{ $t('legacy.t_46f3d1c2f818') }}<input v-model.trim="form.displayName" maxlength="120" required></label>
          <label>{{ $t('legacy.t_e5fb9b389e3e') }}<input v-model.trim="form.phone" maxlength="40"></label>
          <label>{{ $t('legacy.t_d2fbfa77a8af') }}<input v-model.trim="form.email" type="email" maxlength="190"></label>
          <input v-model="form.accountType" type="hidden">
          <label>{{ $t('legacy.t_45293595eae3') }}<select v-model="form.status"><option value="active">{{ $t('legacy.t_ce6c3dc32674') }}</option><option value="inactive">{{ $t('legacy.t_d989e55188c9') }}</option></select></label>
          <label class="wide">{{ editingId ? $t('legacy.t_828704ac9559') : $t('legacy.t_6e25cb2224f0') }}<input v-model="form.password" type="password" minlength="6" maxlength="120" :required="!editingId" :placeholder="$t('legacy.t_fae79d5b88ce')"></label>
          <p v-if="formError" class="admin-property-error wide">{{ formError }}</p>
        </div>
        <menu><button type="button" @click="closeDialog">{{ $t('legacy.t_4d0b4688c787') }}</button><button type="submit" class="primary-btn" :disabled="saving">{{ saving ? $t('legacy.t_8488ea2522af') : $t('legacy.t_680a749af241') }}</button></menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
import { createAdminAccount, deleteAdminAccount, fetchAdminAccounts, updateAdminAccount } from '../services/propertyApi';
import AdminListPager from './AdminListPager.vue';

const emptyForm = () => ({ username: '', password: '', displayName: '', email: '', phone: '', accountType: 'OWNER', status: 'active' });

export default {
  components:{AdminListPager},
  mixins: [pageBridge],
  data() {
    return { accounts: [], loading: false, saving: false, loadError: '', formError: '', editingId: null, form: emptyForm(), pageNumber:1, pageSize:10 };
  },
  computed: {
    createRequestNonce() { return this.page.adminAccountCreateNonce; },
    filteredAccounts() {
      const keyword = String(this.page.moduleSearch || '').trim().toLowerCase();
      const status = String(this.page.statusFilter || '');
      return this.accounts.filter(account => account.accountType === 'OWNER').filter(account => {
        const text = `${account.username} ${account.displayName} ${account.phone || ''} ${account.email || ''}`.toLowerCase();
        const matchesKeyword = !keyword || text.includes(keyword);
        const matchesStatus = status.includes('全部') || (status.includes('啟用') && account.status === 'active') || (status.includes('停用') && account.status === 'inactive');
        return matchesKeyword && matchesStatus;
      });
    },pagedAccounts(){const start=(this.pageNumber-1)*this.pageSize;return this.filteredAccounts.slice(start,start+this.pageSize)}
  },
  watch: { createRequestNonce(value, previousValue) { if (value > previousValue) this.openCreate(); },'page.moduleSearch'(){this.pageNumber=1},'page.statusFilter'(){this.pageNumber=1} },
  mounted() { this.loadAccounts(); },
  methods: {
    async loadAccounts() {
      this.loading = true; this.loadError = '';
      try { this.accounts = (await fetchAdminAccounts()).filter(account => account.accountType === 'OWNER'); }
      catch (error) { this.loadError = error.message || 'API request failed'; }
      finally { this.loading = false; }
    },
    openCreate() { this.editingId = null; this.form = emptyForm(); this.formError = ''; this.$refs.accountDialog.showModal(); },
    openEdit(account) {
      this.editingId = account.id;
      this.form = { username: account.username, password: '', displayName: account.displayName, email: account.email || '', phone: account.phone || '', accountType: 'OWNER', status: account.status };
      this.formError = ''; this.$refs.accountDialog.showModal();
    },
    closeDialog() { this.$refs.accountDialog?.close(); },
    async saveAccount() {
      this.saving = true; this.formError = '';
      try {
        const payload = { ...this.form };
        if (this.editingId && !payload.password) delete payload.password;
        if (this.editingId) await updateAdminAccount(this.editingId, payload);
        else await createAdminAccount(payload);
        await this.loadAccounts(); this.closeDialog(); this.page.showToast(this.editingId ? '帳號已更新' : '帳號已建立');
      } catch (error) { this.formError = error.message || '帳號儲存失敗'; }
      finally { this.saving = false; }
    },
    async removeAccount(account) {
      const action = account.status === 'active' ? '停用' : '恢復';
      if (!window.confirm(`確定要${action}帳號「${account.username}」嗎？`)) return;
      try {
        if (account.status === 'active') await deleteAdminAccount(account.id);
        else await updateAdminAccount(account.id, { status: 'active' });
        await this.loadAccounts(); this.page.showToast(`帳號已${action}`);
      }
      catch (error) { this.loadError = error.message || `帳號${action}失敗`; }
    }
  }
};
</script>

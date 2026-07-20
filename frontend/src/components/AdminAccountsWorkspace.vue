<template>
  <section class="panel admin-accounts-workspace">
    <div class="owners-panel-head">
      <div><h2>帳號管理</h2><span>{{ filteredAccounts.length }} / {{ accounts.length }} 個帳號</span></div>
      <span v-if="loading">正在從資料庫載入…</span>
    </div>

    <div v-if="loadError" class="admin-owner-state error" role="alert">
      <strong>帳號資料載入失敗</strong><span>{{ loadError }}</span><button type="button" @click="loadAccounts">重新載入</button>
    </div>
    <div v-else class="table-wrap">
      <table>
        <thead><tr><th>登入帳號</th><th>顯示名稱</th><th>手機號</th><th>帳號類型</th><th>關聯業主</th><th>狀態</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="account in filteredAccounts" :key="account.id">
            <td>{{ account.username }}</td>
            <td>{{ account.displayName }}</td>
            <td>{{ account.phone || '—' }}</td>
            <td><span class="tag" :class="account.accountType === 'ADMIN' ? 'blue' : 'green'">{{ account.accountType === 'ADMIN' ? '管理員' : '業主' }}</span></td>
            <td>{{ account.ownerId ? `業主 #${account.ownerId}` : '—' }}</td>
            <td><span class="tag" :class="account.status === 'active' ? 'green' : 'gray'">{{ account.status === 'active' ? '啟用' : '停用' }}</span></td>
            <td class="account-actions"><button type="button" @click="openEdit(account)">修改</button><button type="button" class="danger" @click="removeAccount(account)">{{ account.status === 'active' ? '停用' : '恢復' }}</button></td>
          </tr>
          <tr v-if="!loading && !filteredAccounts.length"><td colspan="7" class="admin-owner-empty">目前沒有符合條件的帳號</td></tr>
        </tbody>
      </table>
    </div>

    <dialog ref="accountDialog" class="modal admin-account-dialog">
      <form method="dialog" @submit.prevent="saveAccount">
        <div class="modal-head"><h3>{{ editingId ? '修改帳號' : '新增帳號' }}</h3><button class="icon-close" type="button" @click="closeDialog">×</button></div>
        <div class="form-grid">
          <label>登入帳號<input v-model.trim="form.username" maxlength="80" required placeholder="手機號或管理員帳號"></label>
          <label>顯示名稱<input v-model.trim="form.displayName" maxlength="120" required></label>
          <label>手機號<input v-model.trim="form.phone" maxlength="40"></label>
          <label>郵箱<input v-model.trim="form.email" type="email" maxlength="190"></label>
          <label>帳號類型<select v-model="form.accountType"><option value="OWNER">業主</option><option value="ADMIN">管理員</option></select></label>
          <label>狀態<select v-model="form.status"><option value="active">啟用</option><option value="inactive">停用</option></select></label>
          <label class="wide">{{ editingId ? '新密碼（留空表示不修改）' : '密碼' }}<input v-model="form.password" type="password" minlength="6" maxlength="120" :required="!editingId" placeholder="至少 6 位"></label>
          <p v-if="formError" class="admin-property-error wide">{{ formError }}</p>
        </div>
        <menu><button type="button" @click="closeDialog">取消</button><button type="submit" class="primary-btn" :disabled="saving">{{ saving ? '儲存中…' : '確認儲存' }}</button></menu>
      </form>
    </dialog>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
import { createAdminAccount, deleteAdminAccount, fetchAdminAccounts, updateAdminAccount } from '../services/propertyApi';

const emptyForm = () => ({ username: '', password: '', displayName: '', email: '', phone: '', accountType: 'OWNER', status: 'active' });

export default {
  mixins: [pageBridge],
  data() {
    return { accounts: [], loading: false, saving: false, loadError: '', formError: '', editingId: null, form: emptyForm() };
  },
  computed: {
    createRequestNonce() { return this.page.adminAccountCreateNonce; },
    filteredAccounts() {
      const keyword = String(this.page.moduleSearch || '').trim().toLowerCase();
      const status = String(this.page.statusFilter || '');
      return this.accounts.filter(account => {
        const text = `${account.username} ${account.displayName} ${account.phone || ''} ${account.email || ''}`.toLowerCase();
        const matchesKeyword = !keyword || text.includes(keyword);
        const matchesStatus = status.includes('全部') || (status.includes('啟用') && account.status === 'active') || (status.includes('停用') && account.status === 'inactive');
        return matchesKeyword && matchesStatus;
      });
    }
  },
  watch: { createRequestNonce(value, previousValue) { if (value > previousValue) this.openCreate(); } },
  mounted() { this.loadAccounts(); },
  methods: {
    async loadAccounts() {
      this.loading = true; this.loadError = '';
      try { this.accounts = await fetchAdminAccounts(); }
      catch (error) { this.loadError = error.message || 'API request failed'; }
      finally { this.loading = false; }
    },
    openCreate() { this.editingId = null; this.form = emptyForm(); this.formError = ''; this.$refs.accountDialog.showModal(); },
    openEdit(account) {
      this.editingId = account.id;
      this.form = { username: account.username, password: '', displayName: account.displayName, email: account.email || '', phone: account.phone || '', accountType: account.accountType, status: account.status };
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

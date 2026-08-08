<template>
  <section class="backup-workspace">
    <header class="backup-hero">
      <div class="hero-icon"><DatabaseBackup :size="28" :stroke-width="1.9" /></div>
      <div><h1>{{ $t('backup.title') }}</h1><p>{{ $t('backup.subtitle') }}</p></div>
      <button class="refresh-button" type="button" :disabled="loading" @click="loadBackups"><RefreshCw :size="17" />{{ $t('ui.reload') }}</button>
    </header>

    <div v-if="notice" class="notice" :class="notice.type"><CircleCheck v-if="notice.type === 'success'" :size="19" /><TriangleAlert v-else :size="19" /><span>{{ notice.message }}</span></div>

    <div class="action-grid">
      <article class="action-card create-card">
        <div class="card-heading"><span class="card-icon"><Archive :size="23" /></span><div><h2>{{ $t('backup.createTitle') }}</h2><p>{{ $t('backup.createHint') }}</p></div></div>
        <div class="scope-row"><Database :size="18" /><span>MySQL</span><FolderArchive :size="18" /><span>uploads/</span></div>
        <button class="primary-button" type="button" :disabled="creating || restoring" @click="createBackup"><LoaderCircle v-if="creating" class="spin" :size="19" /><Download v-else :size="19" />{{ creating ? $t('backup.creating') : $t('backup.create') }}</button>
      </article>

      <article class="action-card restore-card">
        <div class="card-heading"><span class="card-icon warning"><ShieldAlert :size="23" /></span><div><h2>{{ $t('backup.restoreTitle') }}</h2><p>{{ $t('backup.restoreHint') }}</p></div></div>
        <label class="file-picker"><input type="file" accept=".zip,application/zip" :disabled="restoring" @change="selectFile"><Upload :size="18" /><span>{{ selectedFile ? selectedFile.name : $t('backup.selectFile') }}</span></label>
        <label class="confirmation-field"><span>{{ $t('backup.confirmation') }}</span><input v-model="confirmation" :disabled="restoring" type="text" autocomplete="off" :placeholder="$t('backup.confirmationHint')"></label>
        <button class="danger-button" type="button" :disabled="!canRestore" @click="restoreBackup"><LoaderCircle v-if="restoring" class="spin" :size="19" /><RotateCcw v-else :size="19" />{{ restoring ? $t('backup.restoring') : $t('backup.restore') }}</button>
      </article>
    </div>

    <article class="history-panel">
      <div class="panel-heading"><div><h2>{{ $t('backup.history') }}</h2><p>{{ $t('backup.historyHint') }}</p></div><span class="record-count">{{ backups.length }}</span></div>
      <div v-if="loading" class="empty-state"><LoaderCircle class="spin" :size="22" />{{ $t('backup.loading') }}</div>
      <div v-else-if="!backups.length" class="empty-state"><ArchiveX :size="27" />{{ $t('backup.empty') }}</div>
      <div v-else class="table-wrap">
        <table><thead><tr><th>{{ $t('backup.fileName') }}</th><th>{{ $t('backup.type') }}</th><th>{{ $t('backup.createdAt') }}</th><th>{{ $t('backup.size') }}</th><th>{{ $t('backup.action') }}</th></tr></thead>
          <tbody><tr v-for="backup in backups" :key="backup.fileName"><td><div class="file-name"><FileArchive :size="18" /><strong>{{ backup.fileName }}</strong></div></td><td><span class="type-badge" :class="backup.type">{{ backup.type === 'pre_restore' ? $t('backup.preRestore') : $t('backup.manual') }}</span></td><td>{{ formatDate(backup.createdAt) }}</td><td>{{ formatSize(backup.size) }}</td><td><button class="download-button" type="button" :disabled="downloading === backup.fileName" @click="downloadBackup(backup)"><Download :size="16" />{{ $t('backup.download') }}</button></td></tr></tbody>
        </table>
      </div>
    </article>
  </section>
</template>

<script>
import { Archive, ArchiveX, CircleCheck, Database, DatabaseBackup, Download, FileArchive, FolderArchive, LoaderCircle, RefreshCw, RotateCcw, ShieldAlert, TriangleAlert, Upload } from '@lucide/vue';
import { createAdminSystemBackup, downloadAdminSystemBackup, fetchAdminSystemBackups, restoreAdminSystemBackup } from '../services/propertyApi';

export default {
  components: { Archive, ArchiveX, CircleCheck, Database, DatabaseBackup, Download, FileArchive, FolderArchive, LoaderCircle, RefreshCw, RotateCcw, ShieldAlert, TriangleAlert, Upload },
  data() { return { backups: [], loading: false, creating: false, restoring: false, downloading: '', selectedFile: null, confirmation: '', notice: null }; },
  computed: { canRestore() { return Boolean(this.selectedFile) && this.confirmation === 'RESTORE CCPS' && !this.restoring && !this.creating; } },
  mounted() { this.loadBackups(); },
  methods: {
    async loadBackups() { this.loading = true; try { const data = await fetchAdminSystemBackups(); this.backups = data.backups || []; } catch (error) { this.showNotice('error', `${this.$t('backup.loadFailed')}：${error.message}`); } finally { this.loading = false; } },
    async createBackup() { this.creating = true; this.notice = null; try { const file = await createAdminSystemBackup(); this.saveBlob(file, `ccps-backup-${Date.now()}.zip`); this.showNotice('success', this.$t('backup.createSuccess')); await this.loadBackups(); } catch (error) { this.showNotice('error', `${this.$t('backup.createFailed')}：${error.message}`); } finally { this.creating = false; } },
    async downloadBackup(backup) { this.downloading = backup.fileName; try { this.saveBlob(await downloadAdminSystemBackup(backup.fileName), backup.fileName); } catch (error) { this.showNotice('error', `${this.$t('backup.downloadFailed')}：${error.message}`); } finally { this.downloading = ''; } },
    selectFile(event) { this.selectedFile = event.target.files?.[0] || null; this.notice = null; },
    async restoreBackup() { if (!this.canRestore) return; this.restoring = true; this.notice = null; try { const result = await restoreAdminSystemBackup(this.selectedFile, this.confirmation); this.showNotice('success', this.$t('backup.restoreSuccess', { file: result.safetyBackupFileName })); this.selectedFile = null; this.confirmation = ''; await this.loadBackups(); } catch (error) { this.showNotice('error', `${this.$t('backup.restoreFailed')}：${error.message}`); } finally { this.restoring = false; } },
    saveBlob(result, fallbackName) { const disposition = result.contentDisposition || ''; const utf = disposition.match(/filename\*=UTF-8''([^;]+)/i); const plain = disposition.match(/filename="?([^";]+)"?/i); let name = fallbackName; try { name = decodeURIComponent(utf?.[1] || plain?.[1] || fallbackName); } catch { name = fallbackName; } const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = name; document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url); },
    formatDate(value) { if (!value) return '—'; return new Intl.DateTimeFormat(this.$i18n.locale, { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value)); },
    formatSize(value) { const bytes = Number(value || 0); if (bytes < 1024) return `${bytes} B`; if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`; if (bytes < 1024 ** 3) return `${(bytes / 1024 ** 2).toFixed(1)} MB`; return `${(bytes / 1024 ** 3).toFixed(2)} GB`; },
    showNotice(type, message) { this.notice = { type, message }; }
  }
};
</script>

<style scoped>
.backup-workspace{display:grid;gap:20px;margin:0 28px 32px;color:#082f57}.backup-hero{display:flex;align-items:center;gap:16px;padding:22px 24px;border:1px solid #d4e1ec;border-radius:12px;background:linear-gradient(135deg,#fff 0%,#f0faf9 100%);box-shadow:0 8px 24px rgba(12,55,86,.07)}.hero-icon{display:grid;place-items:center;width:54px;height:54px;border-radius:14px;background:#087f85;color:#fff}.backup-hero h1,.action-card h2,.history-panel h2{margin:0}.backup-hero h1{font-size:24px}.backup-hero p,.card-heading p,.panel-heading p{margin:5px 0 0;color:#61758c;line-height:1.6}.refresh-button{margin-left:auto}.refresh-button,.download-button{display:inline-flex;align-items:center;justify-content:center;gap:7px;min-height:38px;padding:0 14px;border:1px solid #c7d6e4;border-radius:8px;background:#fff;color:#174d79;font-weight:700}.notice{display:flex;align-items:flex-start;gap:10px;padding:13px 16px;border-radius:9px;font-weight:600}.notice.success{border:1px solid #a7dfc3;background:#edf9f2;color:#087344}.notice.error{border:1px solid #efb6b6;background:#fff2f2;color:#ae2020}.action-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.action-card,.history-panel{border:1px solid #d5e0eb;border-radius:12px;background:#fff;box-shadow:0 7px 20px rgba(17,56,86,.06)}.action-card{display:flex;flex-direction:column;gap:17px;min-height:300px;padding:24px}.action-card.create-card{border-top:4px solid #07969c}.action-card.restore-card{border-top:4px solid #d69000}.card-heading{display:flex;align-items:flex-start;gap:13px}.card-heading h2,.history-panel h2{font-size:19px}.card-icon{display:grid;flex:0 0 auto;place-items:center;width:43px;height:43px;border-radius:11px;background:#e8f6f5;color:#087f85}.card-icon.warning{background:#fff4d8;color:#b46f00}.scope-row{display:flex;align-items:center;gap:8px;padding:13px 15px;border-radius:8px;background:#f4f8fb;color:#315b7e;font-weight:700}.scope-row span+svg{margin-left:14px}.primary-button,.danger-button{display:flex;align-items:center;justify-content:center;gap:9px;min-height:46px;margin-top:auto;border:0;border-radius:8px;color:#fff;font-size:15px;font-weight:800}.primary-button{background:#078f95}.danger-button{background:#c35c18}.primary-button:disabled,.danger-button:disabled,.refresh-button:disabled,.download-button:disabled{cursor:not-allowed;opacity:.52}.file-picker{display:flex;align-items:center;gap:9px;min-height:44px;padding:0 13px;border:1px dashed #aebfd0;border-radius:8px;background:#f8fafc;color:#335c7d;font-weight:600;cursor:pointer;overflow:hidden}.file-picker span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.file-picker input{position:absolute;width:1px;height:1px;opacity:0}.confirmation-field{display:grid;gap:7px;color:#435d76;font-size:13px;font-weight:700}.confirmation-field input{height:42px;padding:0 12px;border:1px solid #c9d7e4;border-radius:8px;color:#0a3157;font:inherit;font-weight:600}.confirmation-field input:focus{outline:2px solid rgba(7,150,156,.2);border-color:#07969c}.history-panel{overflow:hidden}.panel-heading{display:flex;align-items:center;justify-content:space-between;padding:19px 22px;border-bottom:1px solid #dce5ee}.record-count{display:grid;place-items:center;min-width:31px;height:31px;padding:0 8px;border-radius:999px;background:#eaf4fb;color:#0b588a;font-weight:800}.table-wrap{overflow:auto}.history-panel table{width:100%;border-collapse:collapse;font-size:14px}.history-panel th,.history-panel td{padding:14px 18px;border-bottom:1px solid #e3eaf1;text-align:left;white-space:nowrap}.history-panel th{background:#f6f9fc;color:#486179;font-size:13px}.history-panel tbody tr:hover{background:#f8fbfd}.file-name{display:flex;align-items:center;gap:9px;color:#164d79}.type-badge{display:inline-flex;padding:5px 9px;border-radius:999px;background:#e7f6f1;color:#08724d;font-size:12px;font-weight:800}.type-badge.pre_restore{background:#fff2d8;color:#9a6100}.empty-state{display:flex;align-items:center;justify-content:center;gap:10px;min-height:150px;color:#72869a}.spin{animation:spin .9s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:900px){.action-grid{grid-template-columns:1fr}.backup-workspace{margin-inline:14px}.backup-hero{align-items:flex-start;flex-wrap:wrap}.refresh-button{margin-left:0}}
</style>

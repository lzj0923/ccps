<template>
  <section class="document-center-page">
    <div v-if="errorMessage" class="document-api-message">{{ $lt(errorMessage) }}</div>
    <div class="document-summary-grid">
      <article v-for="card in summaryCards" :key="card.label" class="document-summary-card">
        <span class="document-summary-icon" v-html="icons[card.icon]"></span>
        <div><small>{{ $lt(card.label) }}</small><strong>{{ card.value }}</strong><p>{{ $lt(card.note) }}</p></div>
        <button :aria-label="$lt(card.actionLabel)" @click="applySummary(card.action)" v-html="icons[card.trailing]"></button>
      </article>
    </div>

    <div class="document-workspace">
      <div class="document-main-column">
        <form class="document-filter-card" @submit.prevent="applyFilters">
          <label><span>{{ $t('legacy.t_54b5dbbe0688') }}</span><select v-model="propertyFilter"><option value="all">{{ $t('legacy.t_9d65de4d5c85') }}</option><option v-for="project in projects" :key="project" :value="project">{{ project }}</option></select></label>
          <label><span>{{ $t('legacy.t_3987335856f9') }}</span><select v-model="typeFilter"><option value="all">{{ $t('legacy.t_e60f5834f6e8') }}</option><option v-for="tab in categories.slice(1)" :key="tab.key" :value="tab.key">{{ $lt(documentCategoryLabel(tab)) }}</option></select></label>
          <label class="document-date-filter"><span>{{ $t('legacy.t_1e972d484677') }}</span><div><i v-html="icons.calendar"></i><input v-model="startDate" type="date" :aria-label="$t('legacy.t_7fd7a227e9ce')"><b>－</b><input v-model="endDate" type="date" :aria-label="$t('legacy.t_27eefa5237a0')"></div></label>
          <label><span>{{ $t('legacy.t_45293595eae3') }}</span><select v-model="documentStatusFilter"><option value="all">{{ $t('legacy.t_026ed0343be6') }}</option><option value="active">{{ $t('ui.documentStatusActive') }}</option><option value="confirmed">{{ $t('ui.documentStatusConfirmed') }}</option><option value="pending_signature">{{ $t('ui.documentStatusPendingSignature') }}</option><option value="processing">{{ $t('ui.documentStatusProcessing') }}</option><option value="expiring">{{ $t('ui.documentStatusExpiring') }}</option><option value="expired">{{ $t('ui.documentStatusExpired') }}</option></select></label>
          <label class="document-search"><span>{{ $t('legacy.t_f2b534e3821f') }}</span><div><input v-model="keyword" :placeholder="$t('legacy.t_837373934d01')"><i v-html="icons.search"></i></div></label>
          <button class="document-query-button" type="submit">{{ $t('legacy.t_505ba2176546') }}</button><button class="document-reset-button" type="button" @click="resetFilters">{{ $t('legacy.t_3d81345303ab') }}</button>
        </form>

        <section class="document-table-card">
          <nav class="document-category-tabs" :aria-label="$t('legacy.t_59b17669a7c9')"><button v-for="tab in categories" :key="tab.key" :class="{ active: category === tab.key }" @click="changeCategory(tab.key)">{{ $lt(documentCategoryLabel(tab)) }} <b>({{ tab.count }})</b></button></nav>
          <div class="document-table-scroll">
            <table class="document-table">
              <thead><tr><th><input type="checkbox" :aria-label="$t('legacy.t_06f07e92177d')" @change="toggleAll"></th><th>{{ $t('legacy.t_1eed2b677db6') }}</th><th>{{ $t('legacy.t_a3595dcda1dd') }}</th><th>{{ $t('legacy.t_3987335856f9') }}</th><th>{{ $t('legacy.t_67a655bef981') }}</th><th>{{ $t('legacy.t_45293595eae3') }}</th><th>{{ $t('legacy.t_fd20702c73d1') }}</th><th>{{ $t('legacy.t_f3ea6d345e2a') }}</th></tr></thead>
              <tbody>
                <tr v-for="file in pagedFiles" :key="file.id" :class="{ selected: selectedId === file.id }" @click="selectFile(file)">
<td><input v-model="selectedIds" :value="file.id" type="checkbox" :aria-label="$t('ui.selectItemAria', { name: file.name })" @click.stop></td>
                  <td><div class="document-file-name"><span :class="file.icon" v-html="icons[file.icon]"></span><div><strong>{{ file.name }}</strong><small>{{ file.number }}</small></div></div></td>
                  <td><div class="document-property-cell"><strong>{{ file.project || $t('legacy.t_f40d930ffc26') }}</strong><small>{{ file.location || '—' }}{{ file.unit ? ` · ${file.unit}` : '' }}</small></div></td>
                  <td><span class="document-type-badge" :class="file.tone">{{ $lt(documentTypeLabel(file)) }}</span></td>
<td><time>{{ displayDate(file.uploadDate) }}</time><small>{{ file.uploadTime }}</small></td>
                  <td><span class="document-status" :class="statusClass(file.statusKey)">{{ $lt(documentStatusLabel(file.statusKey)) }}</span></td>
                  <td>{{ file.size }}</td>
                  <td><div class="document-row-actions"><button :aria-label="$t('ui.previewFileAria', { name: file.name })" :disabled="!file.downloadable" @click.stop="previewFile(file)" v-html="icons.eye"></button><button :aria-label="$t('ui.downloadFileAria', { name: file.name })" :disabled="!file.downloadable" @click.stop="downloadFile(file)" v-html="icons.download"></button><button :aria-label="$t('ui.moreFileAria', { name: file.name })" @click.stop="showToast($t('legacy.t_d16501aeae6d'))">•••</button></div></td>
                </tr>
                <tr v-if="loading"><td colspan="8" class="document-empty">{{ $t('legacy.t_d692c6c50883') }}</td></tr>
                <tr v-else-if="!pagedFiles.length"><td colspan="8" class="document-empty">{{ $t('legacy.t_52eae0803847') }}</td></tr>
              </tbody>
            </table>
          </div>
          <footer class="document-table-footer"><span>{{ $t('legacy.t_3b6ef811b85a') }} {{ filteredFiles.length }} {{ $t('legacy.t_c845e49258c0') }}</span><div class="document-pagination"><button :disabled="currentPage === 1" @click="currentPage--">‹</button><button v-for="page in visiblePages" :key="page" :class="{ active: currentPage === page }" @click="currentPage = page">{{ page }}</button><button :disabled="currentPage === totalPages" @click="currentPage++">›</button></div><select v-model.number="pageSize" :aria-label="$t('legacy.t_4fab5ca1141c')"><option :value="10">{{ $t('legacy.t_fc6da0e815a1') }}</option><option :value="20">{{ $t('legacy.t_93d673672fa5') }}</option></select></footer>
        </section>
      </div>

      <aside v-if="selectedFile" class="document-detail-card">
        <header><h2>{{ $t('legacy.t_d9c4991c0a4a') }}</h2><button :aria-label="$t('legacy.t_48b47156015e')" @click="selectedId = files[0]?.id">×</button></header>
        <div class="document-detail-file"><span :class="selectedFile.icon" v-html="icons[selectedFile.icon]"></span><div><strong>{{ selectedFile.name }}</strong><small>{{ selectedFile.number }}</small></div><b :class="statusClass(selectedFile.statusKey)">{{ $lt(documentStatusLabel(selectedFile.statusKey)) }}</b></div>
        <div class="document-detail-actions"><button :disabled="!selectedFile.downloadable" @click="previewFile(selectedFile)"><i v-html="icons.eye"></i>{{ $t('legacy.t_adcecad4c817') }}</button><button :disabled="!selectedFile.downloadable" @click="downloadFile(selectedFile)"><i v-html="icons.download"></i>{{ $t('legacy.t_33e3f60d0c5b') }}</button><button @click="showToast($t('legacy.t_d16501aeae6d'))">{{ $t('legacy.t_4682f736016b') }}</button></div>
<section class="document-detail-section"><h3>{{ $t('legacy.t_34f3833a3799') }}</h3><dl class="document-info-grid"><div><dt>{{ $t('legacy.t_3987335856f9') }}</dt><dd>{{ $lt(documentTypeLabel(selectedFile)) }}</dd></div><div><dt>{{ $t('legacy.t_0afd9082e001') }}</dt><dd>{{ selectedFile.uploaderName || $t('legacy.t_641d7d9592a5') }}</dd></div><div><dt>{{ $t('legacy.t_67a655bef981') }}</dt><dd>{{ displayDate(selectedFile.uploadDate) }} {{ selectedFile.uploadTime }}</dd></div><div><dt>{{ $t('legacy.t_80920cd66728') }}</dt><dd>{{ displayDate(selectedFile.updatedDate) }} {{ selectedFile.updatedTime }}</dd></div><div><dt>{{ $t('legacy.t_2ad8b0b96fec') }}</dt><dd>{{ selectedFile.size }}</dd></div><div><dt>{{ $t('legacy.t_989d1affa089') }}</dt><dd>{{ $t('legacy.t_a851fa355624') }}</dd></div></dl></section>
        <section class="document-detail-section"><h3>{{ $t('legacy.t_a3595dcda1dd') }}</h3><div class="document-detail-property"><span></span><div><strong>{{ selectedFile.project || $t('legacy.t_f40d930ffc26') }}</strong><small>{{ selectedFile.location || '—' }}</small><em>{{ $t('legacy.t_5d877f3bb7e4') }}{{ selectedFile.unit || '—' }}</em></div><button @click="showToast($t('legacy.t_ddcb5820e5d4'))">{{ $t('legacy.t_2902c92d0900') }}</button></div></section>
        <section class="document-detail-section document-expiry"><h3>{{ $t('legacy.t_ce425cc5276d') }}</h3><div><i v-html="icons.calendar"></i><p>{{ $t('legacy.t_baecbeaeb2fa') }}<strong>{{ selectedFile.expiry }}</strong><small>{{ $t('legacy.t_a937aeb950d3') }}</small></p></div></section>
<section class="document-detail-section document-versions"><h3>{{ $t('legacy.t_21b050ee6bd9') }}</h3><article><strong>{{ $t('legacy.t_a851fa355624') }}</strong><time>{{ displayDate(selectedFile.updatedDate) }} {{ selectedFile.updatedTime }}</time><span>{{ selectedFile.uploaderName || $t('legacy.t_641d7d9592a5') }}</span><b>{{ $t('legacy.t_2f26214ec855') }}</b></article></section>
        <footer><button @click="showToast($t('legacy.t_44fd3916b47a'))"><i v-html="icons.refresh"></i>{{ $t('legacy.t_9bba4d889b09') }}</button><button @click="showToast($t('legacy.t_ff8517d2502e'))"><i v-html="icons.headset"></i>{{ $t('legacy.t_b1121621e50e') }}</button></footer>
      </aside>
    </div>
  </section>
</template>

<script>
import { formatDate } from '../utils/dateFormat';
import pageBridge from '../pageBridge';
import { ownerDocumentKey } from '../utils/ownerDocuments';
import { fetchOwnerDocumentFile, fetchOwnerDocuments } from '../services/propertyApi';

const documentCategoryKeys = {
  all: 'ui.documentCategoryAll', sale: 'ui.documentCategorySale', lease: 'ui.documentCategoryLease',
  proof: 'ui.documentCategoryProof', receipt: 'ui.documentCategoryReceipt', maintenance: 'ui.documentCategoryMaintenance',
  finance: 'ui.documentCategoryFinance', other: 'ui.documentCategoryOther'
};

const documentTypeKeys = {
  '預備金憑證': 'ui.documentTypeReserveProof', '预备金凭证': 'ui.documentTypeReserveProof',
  '收支附件': 'ui.documentTypeCashflowAttachment', '其他附件': 'ui.documentTypePropertyAttachment',
  '授權委託書': 'ui.documentTypeAuthorization', '授权委托书': 'ui.documentTypeAuthorization',
  '代管文件': 'ui.documentTypeMandate', '交屋文件': 'ui.documentTypeHandover',
  '簽署文件': 'ui.documentTypeSigned', '签署文件': 'ui.documentTypeSigned'
};

const documentStatusAliases = {
  active: 'active', '已生效': 'active',
  approved: 'confirmed', confirmed: 'confirmed', '已確認': 'confirmed', '已确认': 'confirmed',
  pending_signature: 'pending_signature', '待簽署': 'pending_signature', '待签署': 'pending_signature',
  pending_review: 'processing', processing: 'processing', '處理中': 'processing', '处理中': 'processing',
  expiring: 'expiring', '即將到期': 'expiring', '即将到期': 'expiring',
  expired: 'expired', '已過期': 'expired', '已过期': 'expired'
};

const documentStatusKeys = {
  active: 'ui.documentStatusActive', confirmed: 'ui.documentStatusConfirmed',
  pending_signature: 'ui.documentStatusPendingSignature', processing: 'ui.documentStatusProcessing',
  expiring: 'ui.documentStatusExpiring', expired: 'ui.documentStatusExpired'
};

export default {
  mixins: [pageBridge],
  data() {
    return {
      loading: true, errorMessage: '', files: [], selectedId: null, selectedIds: [], category: 'all', propertyFilter: 'all', typeFilter: 'all', documentStatusFilter: 'all', keyword: '', startDate: '', endDate: '', currentPage: 1, pageSize: 10, summary: { totalCount: 0, pendingSignatureCount: 0, monthNewCount: 0, expiringCount: 0 }, categories: [],
      icons: {
        document: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 2h9l4 4v16H6z"/><path d="M15 2v5h5M9 12h7M9 16h5"/></svg>', folder: '<svg viewBox="0 0 24 24" fill="none"><path d="M3 7h7l2 2h9v11H3z"/></svg>', signature: '<svg viewBox="0 0 24 24" fill="none"><path d="m4 17 4-1 10-10-3-3L5 13l-1 4Z"/><path d="M12 18h8"/></svg>', arrow: '<svg viewBox="0 0 24 24" fill="none"><path d="m9 5 7 7-7 7"/></svg>', cloud: '<svg viewBox="0 0 24 24" fill="none"><path d="M7 18H5a4 4 0 0 1 0-8h1a6 6 0 0 1 11-2 5 5 0 0 1 0 10h-2"/><path d="M12 11v10m0-10-3 3m3-3 3 3"/></svg>', trend: '<svg viewBox="0 0 24 24" fill="none"><path d="m4 17 5-5 4 3 7-8"/><path d="M16 7h4v4"/></svg>', clock: '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9"/><path d="M12 7v6l4 2"/></svg>', calendar: '<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 10h18"/></svg>', search: '<svg viewBox="0 0 24 24" fill="none"><circle cx="11" cy="11" r="7"/><path d="m16 16 5 5"/></svg>', pdf: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 2h9l4 4v16H6z"/><path d="M15 2v5h5M8 16h8M8 12h6"/></svg>', image: '<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><path d="m4 18 5-5 4 3 3-3 5 5"/></svg>', eye: '<svg viewBox="0 0 24 24" fill="none"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg>', download: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3v12m0 0-4-4m4 4 4-4M4 19v2h16v-2"/></svg>', refresh: '<svg viewBox="0 0 24 24" fill="none"><path d="M20 7V3l-2 2a8 8 0 1 0 2 9"/></svg>', headset: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 14v-2a8 8 0 0 1 16 0v2M4 14h3v6H4zM17 14h3v6h-3zM17 20c-1 2-3 2-5 2"/></svg>'
      }
    };
  },
  computed: {
    projects() { return [...new Set(this.files.map(file => file.project).filter(Boolean))]; },
    summaryCards() { return [{ label: this.$t('ui.documentTotal'), value: this.summary.totalCount, note: this.$t('ui.documentAll'), icon: 'document', trailing: 'folder', action: 'all', actionLabel: this.$t('ui.documentViewAll') }, { label: this.$t('ui.documentPendingSignature'), value: this.summary.pendingSignatureCount, note: this.$t('ui.documentNeedsSignature'), icon: 'signature', trailing: 'arrow', action: 'pending', actionLabel: this.$t('ui.documentViewPending') }, { label: this.$t('ui.documentMonthNew'), value: this.summary.monthNewCount, note: this.$t('ui.documentUploadedThisMonth'), icon: 'cloud', trailing: 'trend', action: 'month', actionLabel: this.$t('ui.documentViewMonth') }, { label: this.$t('ui.documentExpiring'), value: this.summary.expiringCount, note: this.$t('ui.documentWithin30Days'), icon: 'clock', trailing: 'calendar', action: 'expiry', actionLabel: this.$t('ui.documentViewExpiring') }]; },
    filteredFiles() { const key = this.keyword.trim().toLowerCase(); return this.files.filter(file => { const createdDate = (file.createdAt || '').slice(0, 10); return (this.category === 'all' || file.category === this.category) && (this.typeFilter === 'all' || file.category === this.typeFilter) && (this.propertyFilter === 'all' || file.project === this.propertyFilter) && (this.documentStatusFilter === 'all' || file.statusKey === this.documentStatusFilter) && (!this.startDate || !createdDate || createdDate >= this.startDate) && (!this.endDate || !createdDate || createdDate <= this.endDate) && (!key || `${file.name} ${file.number} ${file.project || ''} ${file.unit || ''}`.toLowerCase().includes(key)); }); },
    totalPages() { return Math.max(1, Math.ceil(this.filteredFiles.length / this.pageSize)); },
    visiblePages() { return Array.from({ length: Math.min(5, this.totalPages) }, (_, index) => index + 1); },
    pagedFiles() { return this.filteredFiles.slice((this.currentPage - 1) * this.pageSize, this.currentPage * this.pageSize); },
    selectedFile() { return this.files.find(file => file.id === this.selectedId) || this.files[0] || null; }
  },
  watch: { propertyFilter() { this.currentPage = 1; }, typeFilter() { this.currentPage = 1; }, documentStatusFilter() { this.currentPage = 1; }, keyword() { this.currentPage = 1; }, startDate() { this.currentPage = 1; }, endDate() { this.currentPage = 1; }, pageSize() { this.currentPage = 1; } },
  mounted() { this.loadDocuments(); },
  methods: {
    displayDate(value) { return formatDate(value); },
    async loadDocuments() { this.loading = true; this.errorMessage = ''; try { const data = await fetchOwnerDocuments(); this.summary = data.summary || this.summary; this.categories = data.categories || []; this.files = (data.documents || []).map(this.normalizeFile); if (this.files.length && !this.selectedId) { this.selectedId = this.files[0].id; this.selectedIds = [this.selectedId]; } } catch (error) { this.errorMessage = error.message || '文件資料讀取失敗'; } finally { this.loading = false; } },
    normalizeFile(file) { const icon = (file.mimeType || '').includes('image') ? 'image' : 'pdf'; const created = file.createdAt || ''; const updated = file.updatedAt || created; return { ...file, documentId: file.id, id: ownerDocumentKey(file), statusKey: documentStatusAliases[file.status] || 'processing', project: file.projectName, location: file.city, unit: file.unitNo, icon, tone: file.category === 'sale' ? 'gold' : file.category === 'lease' ? 'blue' : file.category === 'proof' ? 'green' : file.category === 'maintenance' ? 'purple' : 'gray', uploadDate: created.slice(0, 10) || '—', uploadTime: created.slice(11, 16), updatedDate: updated.slice(0, 10) || '—', updatedTime: updated.slice(11, 16), size: this.formatSize(file.fileSize), expiry: file.expiresAt || '—' }; },
    formatSize(bytes) { if (!bytes) return '—'; if (bytes < 1024) return `${bytes} B`; if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`; return `${(bytes / 1024 / 1024).toFixed(2)} MB`; },
    async previewFile(file) { try { const result = await fetchOwnerDocumentFile(file.documentId ?? file.id, false, file.source || 'document'); const url = URL.createObjectURL(result.blob); window.open(url, '_blank', 'noopener'); setTimeout(() => URL.revokeObjectURL(url), 60000); } catch (error) { this.showToast(error.message || '文件預覽失敗'); } },
    async downloadFile(file) { try { const result = await fetchOwnerDocumentFile(file.documentId ?? file.id, true, file.source || 'document'); const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = file.name; link.click(); URL.revokeObjectURL(url); } catch (error) { this.showToast(error.message || '文件下載失敗'); } },
    applyFilters() { this.currentPage = 1; },
    resetFilters() { this.propertyFilter = 'all'; this.typeFilter = 'all'; this.documentStatusFilter = 'all'; this.keyword = ''; this.startDate = ''; this.endDate = ''; this.currentPage = 1; },
    changeCategory(value) { this.category = value; this.typeFilter = 'all'; this.currentPage = 1; },
    selectFile(file) { this.selectedId = file.id; if (!this.selectedIds.includes(file.id)) this.selectedIds = [file.id]; },
    toggleAll(event) { this.selectedIds = event.target.checked ? this.pagedFiles.map(file => file.id) : []; },
    documentCategoryLabel(tab) { const key = typeof tab === 'string' ? tab : tab?.key; return documentCategoryKeys[key] ? this.$t(documentCategoryKeys[key]) : this.$lt(tab?.label || key || ''); },
    documentTypeLabel(file) { return documentTypeKeys[file?.typeLabel] ? this.$t(documentTypeKeys[file.typeLabel]) : this.documentCategoryLabel(file?.category); },
    documentStatusLabel(statusKey) { return documentStatusKeys[statusKey] ? this.$t(documentStatusKeys[statusKey]) : this.$t('ui.documentStatusProcessing'); },
    statusClass(statusKey) { return { active: 'valid', confirmed: 'confirmed', pending_signature: 'processing', processing: 'processing', expiring: 'expiring', expired: 'expired' }[statusKey] || 'valid'; },
    applySummary(action) { this.propertyFilter = 'all'; this.typeFilter = 'all'; this.category = 'all'; this.keyword = ''; if (action === 'pending') this.documentStatusFilter = 'pending_signature'; else if (action === 'expiry') this.documentStatusFilter = 'expiring'; else if (action === 'month') { const now = new Date(); this.startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`; this.endDate = ''; this.documentStatusFilter = 'all'; } else this.documentStatusFilter = 'all'; this.currentPage = 1; }
  }
};
</script>

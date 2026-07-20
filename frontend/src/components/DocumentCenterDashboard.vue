<template>
  <section class="document-center-page">
    <div v-if="errorMessage" class="document-api-message">{{ errorMessage }}</div>
    <div class="document-summary-grid">
      <article v-for="card in summaryCards" :key="card.label" class="document-summary-card">
        <span class="document-summary-icon" v-html="icons[card.icon]"></span>
        <div><small>{{ card.label }}</small><strong>{{ card.value }}</strong><p>{{ card.note }}</p></div>
        <button :aria-label="card.actionLabel" @click="applySummary(card.action)" v-html="icons[card.trailing]"></button>
      </article>
    </div>

    <div class="document-workspace">
      <div class="document-main-column">
        <form class="document-filter-card" @submit.prevent="applyFilters">
          <label><span>房產篩選</span><select v-model="propertyFilter"><option>全部房產</option><option v-for="project in projects" :key="project">{{ project }}</option></select></label>
          <label><span>文件類型</span><select v-model="typeFilter"><option value="all">全部類型</option><option v-for="tab in categories.slice(1)" :key="tab.key" :value="tab.key">{{ tab.label }}</option></select></label>
          <label class="document-date-filter"><span>日期範圍</span><div><i v-html="icons.calendar"></i><input v-model="startDate" type="date" aria-label="開始日期"><b>－</b><input v-model="endDate" type="date" aria-label="結束日期"></div></label>
          <label><span>狀態</span><select v-model="statusFilter"><option>全部狀態</option><option>已生效</option><option>已確認</option><option>待簽署</option><option>處理中</option><option>即將到期</option><option>已過期</option></select></label>
          <label class="document-search"><span>關鍵詞搜索</span><div><input v-model="keyword" placeholder="搜索文件名稱、編號、備注"><i v-html="icons.search"></i></div></label>
          <button class="document-query-button" type="submit">查詢</button><button class="document-reset-button" type="button" @click="resetFilters">重置</button>
        </form>

        <section class="document-table-card">
          <nav class="document-category-tabs" aria-label="文件分類"><button v-for="tab in categories" :key="tab.key" :class="{ active: category === tab.key }" @click="changeCategory(tab.key)">{{ tab.label }} <b>({{ tab.count }})</b></button></nav>
          <div class="document-table-scroll">
            <table class="document-table">
              <thead><tr><th><input type="checkbox" aria-label="全選文件" @change="toggleAll"></th><th>文件名稱</th><th>所屬房產</th><th>文件類型</th><th>上傳日期</th><th>狀態</th><th>大小</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="file in pagedFiles" :key="file.id" :class="{ selected: selectedId === file.id }" @click="selectFile(file)">
                  <td><input v-model="selectedIds" :value="file.id" type="checkbox" :aria-label="`選擇 ${file.name}`" @click.stop></td>
                  <td><div class="document-file-name"><span :class="file.icon" v-html="icons[file.icon]"></span><div><strong>{{ file.name }}</strong><small>{{ file.number }}</small></div></div></td>
                  <td><div class="document-property-cell"><strong>{{ file.project || '未指定房產' }}</strong><small>{{ file.location || '—' }}{{ file.unit ? ` · ${file.unit}` : '' }}</small></div></td>
                  <td><span class="document-type-badge" :class="file.tone">{{ file.typeLabel }}</span></td>
                  <td><time>{{ file.uploadDate }}</time><small>{{ file.uploadTime }}</small></td>
                  <td><span class="document-status" :class="statusClass(file.status)">{{ file.status }}</span></td>
                  <td>{{ file.size }}</td>
                  <td><div class="document-row-actions"><button :aria-label="`預覽 ${file.name}`" :disabled="!file.downloadable" @click.stop="previewFile(file)" v-html="icons.eye"></button><button :aria-label="`下載 ${file.name}`" :disabled="!file.downloadable" @click.stop="downloadFile(file)" v-html="icons.download"></button><button :aria-label="`更多 ${file.name}`" @click.stop="showToast('更多文件操作已開啟')">•••</button></div></td>
                </tr>
                <tr v-if="loading"><td colspan="8" class="document-empty">正在讀取文件…</td></tr>
                <tr v-else-if="!pagedFiles.length"><td colspan="8" class="document-empty">沒有符合條件的文件</td></tr>
              </tbody>
            </table>
          </div>
          <footer class="document-table-footer"><span>共 {{ filteredFiles.length }} 條記錄</span><div class="document-pagination"><button :disabled="currentPage === 1" @click="currentPage--">‹</button><button v-for="page in visiblePages" :key="page" :class="{ active: currentPage === page }" @click="currentPage = page">{{ page }}</button><button :disabled="currentPage === totalPages" @click="currentPage++">›</button></div><select v-model.number="pageSize" aria-label="每頁文件數"><option :value="10">10 條/頁</option><option :value="20">20 條/頁</option></select></footer>
        </section>
      </div>

      <aside v-if="selectedFile" class="document-detail-card">
        <header><h2>選中文件</h2><button aria-label="關閉詳情" @click="selectedId = files[0]?.id">×</button></header>
        <div class="document-detail-file"><span :class="selectedFile.icon" v-html="icons[selectedFile.icon]"></span><div><strong>{{ selectedFile.name }}</strong><small>{{ selectedFile.number }}</small></div><b :class="statusClass(selectedFile.status)">{{ selectedFile.status }}</b></div>
        <div class="document-detail-actions"><button :disabled="!selectedFile.downloadable" @click="previewFile(selectedFile)"><i v-html="icons.eye"></i>在線預覽</button><button :disabled="!selectedFile.downloadable" @click="downloadFile(selectedFile)"><i v-html="icons.download"></i>下載文件</button><button @click="showToast('更多文件操作已開啟')">••• 更多操作</button></div>
        <section class="document-detail-section"><h3>文件信息</h3><dl class="document-info-grid"><div><dt>文件類型</dt><dd>{{ selectedFile.typeLabel }}</dd></div><div><dt>創建者</dt><dd>{{ selectedFile.uploaderName || 'CCPS 管理員' }}</dd></div><div><dt>上傳日期</dt><dd>{{ selectedFile.uploadDate }} {{ selectedFile.uploadTime }}</dd></div><div><dt>最近更新</dt><dd>{{ selectedFile.updatedDate }} {{ selectedFile.updatedTime }}</dd></div><div><dt>文件大小</dt><dd>{{ selectedFile.size }}</dd></div><div><dt>版本</dt><dd>v1.0</dd></div></dl></section>
        <section class="document-detail-section"><h3>所屬房產</h3><div class="document-detail-property"><span></span><div><strong>{{ selectedFile.project || '未指定房產' }}</strong><small>{{ selectedFile.location || '—' }}</small><em>單位：{{ selectedFile.unit || '—' }}</em></div><button @click="showToast('已開啟房產詳情')">查看房產詳情 →</button></div></section>
        <section class="document-detail-section document-expiry"><h3>到期提醒</h3><div><i v-html="icons.calendar"></i><p>合約到期日：<strong>{{ selectedFile.expiry }}</strong><small>到期前 30 天將通過通知中心提醒您</small></p></div></section>
        <section class="document-detail-section document-versions"><h3>版本歷史</h3><article><strong>v1.0</strong><time>{{ selectedFile.updatedDate }} {{ selectedFile.updatedTime }}</time><span>{{ selectedFile.uploaderName || 'CCPS 管理員' }}</span><b>當前版本</b></article></section>
        <footer><button @click="showToast('更新申請已提交')"><i v-html="icons.refresh"></i>申請更新</button><button @click="showToast('正在聯繫管理員')"><i v-html="icons.headset"></i>聯繫管理員</button></footer>
      </aside>
    </div>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
import { fetchOwnerDocumentFile, fetchOwnerDocuments } from '../services/propertyApi';

export default {
  mixins: [pageBridge],
  data() {
    return {
      loading: true, errorMessage: '', files: [], selectedId: null, selectedIds: [], category: 'all', propertyFilter: '全部房產', typeFilter: 'all', statusFilter: '全部狀態', keyword: '', startDate: '', endDate: '', currentPage: 1, pageSize: 10, summary: { totalCount: 0, pendingSignatureCount: 0, monthNewCount: 0, expiringCount: 0 }, categories: [],
      icons: {
        document: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 2h9l4 4v16H6z"/><path d="M15 2v5h5M9 12h7M9 16h5"/></svg>', folder: '<svg viewBox="0 0 24 24" fill="none"><path d="M3 7h7l2 2h9v11H3z"/></svg>', signature: '<svg viewBox="0 0 24 24" fill="none"><path d="m4 17 4-1 10-10-3-3L5 13l-1 4Z"/><path d="M12 18h8"/></svg>', arrow: '<svg viewBox="0 0 24 24" fill="none"><path d="m9 5 7 7-7 7"/></svg>', cloud: '<svg viewBox="0 0 24 24" fill="none"><path d="M7 18H5a4 4 0 0 1 0-8h1a6 6 0 0 1 11-2 5 5 0 0 1 0 10h-2"/><path d="M12 11v10m0-10-3 3m3-3 3 3"/></svg>', trend: '<svg viewBox="0 0 24 24" fill="none"><path d="m4 17 5-5 4 3 7-8"/><path d="M16 7h4v4"/></svg>', clock: '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9"/><path d="M12 7v6l4 2"/></svg>', calendar: '<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 10h18"/></svg>', search: '<svg viewBox="0 0 24 24" fill="none"><circle cx="11" cy="11" r="7"/><path d="m16 16 5 5"/></svg>', pdf: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 2h9l4 4v16H6z"/><path d="M15 2v5h5M8 16h8M8 12h6"/></svg>', image: '<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><path d="m4 18 5-5 4 3 3-3 5 5"/></svg>', eye: '<svg viewBox="0 0 24 24" fill="none"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg>', download: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3v12m0 0-4-4m4 4 4-4M4 19v2h16v-2"/></svg>', refresh: '<svg viewBox="0 0 24 24" fill="none"><path d="M20 7V3l-2 2a8 8 0 1 0 2 9"/></svg>', headset: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 14v-2a8 8 0 0 1 16 0v2M4 14h3v6H4zM17 14h3v6h-3zM17 20c-1 2-3 2-5 2"/></svg>'
      }
    };
  },
  computed: {
    projects() { return [...new Set(this.files.map(file => file.project).filter(Boolean))]; },
    summaryCards() { return [{ label: '文件總數', value: this.summary.totalCount, note: '全部文件', icon: 'document', trailing: 'folder', action: 'all', actionLabel: '查看全部文件' }, { label: '待簽署文件', value: this.summary.pendingSignatureCount, note: '需要您簽署', icon: 'signature', trailing: 'arrow', action: 'pending', actionLabel: '查看待簽署文件' }, { label: '本月新增文件', value: this.summary.monthNewCount, note: '本月上傳', icon: 'cloud', trailing: 'trend', action: 'month', actionLabel: '查看本月新增文件' }, { label: '即將到期文件', value: this.summary.expiringCount, note: '30天內到期', icon: 'clock', trailing: 'calendar', action: 'expiry', actionLabel: '查看即將到期文件' }]; },
    filteredFiles() { const key = this.keyword.trim().toLowerCase(); return this.files.filter(file => { const createdDate = (file.createdAt || '').slice(0, 10); return (this.category === 'all' || file.category === this.category) && (this.typeFilter === 'all' || file.category === this.typeFilter) && (this.propertyFilter === '全部房產' || file.project === this.propertyFilter) && (this.statusFilter === '全部狀態' || file.status === this.statusFilter) && (!this.startDate || !createdDate || createdDate >= this.startDate) && (!this.endDate || !createdDate || createdDate <= this.endDate) && (!key || `${file.name} ${file.number} ${file.project || ''} ${file.unit || ''}`.toLowerCase().includes(key)); }); },
    totalPages() { return Math.max(1, Math.ceil(this.filteredFiles.length / this.pageSize)); },
    visiblePages() { return Array.from({ length: Math.min(5, this.totalPages) }, (_, index) => index + 1); },
    pagedFiles() { return this.filteredFiles.slice((this.currentPage - 1) * this.pageSize, this.currentPage * this.pageSize); },
    selectedFile() { return this.files.find(file => file.id === this.selectedId) || this.files[0] || null; }
  },
  watch: { propertyFilter() { this.currentPage = 1; }, typeFilter() { this.currentPage = 1; }, statusFilter() { this.currentPage = 1; }, keyword() { this.currentPage = 1; }, startDate() { this.currentPage = 1; }, endDate() { this.currentPage = 1; }, pageSize() { this.currentPage = 1; } },
  mounted() { this.loadDocuments(); },
  methods: {
    async loadDocuments() { this.loading = true; this.errorMessage = ''; try { const data = await fetchOwnerDocuments(); this.summary = data.summary || this.summary; this.categories = data.categories || []; this.files = (data.documents || []).map(this.normalizeFile); if (this.files.length && !this.selectedId) { this.selectedId = this.files[0].id; this.selectedIds = [this.selectedId]; } } catch (error) { this.errorMessage = error.message || '文件資料讀取失敗'; } finally { this.loading = false; } },
    normalizeFile(file) { const icon = (file.mimeType || '').includes('image') ? 'image' : 'pdf'; const created = file.createdAt || ''; const updated = file.updatedAt || created; return { ...file, project: file.projectName, location: file.city, unit: file.unitNo, icon, tone: file.category === 'sale' ? 'gold' : file.category === 'lease' ? 'blue' : file.category === 'proof' ? 'green' : file.category === 'maintenance' ? 'purple' : 'gray', uploadDate: created.slice(0, 10) || '—', uploadTime: created.slice(11, 16), updatedDate: updated.slice(0, 10) || '—', updatedTime: updated.slice(11, 16), size: this.formatSize(file.fileSize), expiry: file.expiresAt || '—' }; },
    formatSize(bytes) { if (!bytes) return '—'; if (bytes < 1024) return `${bytes} B`; if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`; return `${(bytes / 1024 / 1024).toFixed(2)} MB`; },
    async previewFile(file) { try { const result = await fetchOwnerDocumentFile(file.id, false); const url = URL.createObjectURL(result.blob); window.open(url, '_blank', 'noopener'); setTimeout(() => URL.revokeObjectURL(url), 60000); } catch (error) { this.showToast(error.message || '文件預覽失敗'); } },
    async downloadFile(file) { try { const result = await fetchOwnerDocumentFile(file.id, true); const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = file.name; link.click(); URL.revokeObjectURL(url); } catch (error) { this.showToast(error.message || '文件下載失敗'); } },
    applyFilters() { this.currentPage = 1; },
    resetFilters() { this.propertyFilter = '全部房產'; this.typeFilter = 'all'; this.statusFilter = '全部狀態'; this.keyword = ''; this.startDate = ''; this.endDate = ''; this.currentPage = 1; },
    changeCategory(value) { this.category = value; this.typeFilter = 'all'; this.currentPage = 1; },
    selectFile(file) { this.selectedId = file.id; if (!this.selectedIds.includes(file.id)) this.selectedIds = [file.id]; },
    toggleAll(event) { this.selectedIds = event.target.checked ? this.pagedFiles.map(file => file.id) : []; },
    statusClass(status) { return { 已生效: 'valid', 已確認: 'confirmed', 待簽署: 'processing', 處理中: 'processing', 即將到期: 'expiring', 已過期: 'expired' }[status] || 'valid'; },
    applySummary(action) { this.propertyFilter = '全部房產'; this.typeFilter = 'all'; this.category = 'all'; this.keyword = ''; if (action === 'pending') this.statusFilter = '待簽署'; else if (action === 'expiry') this.statusFilter = '即將到期'; else if (action === 'month') { const now = new Date(); this.startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`; this.endDate = ''; this.statusFilter = '全部狀態'; } else this.statusFilter = '全部狀態'; this.currentPage = 1; }
  }
};
</script>

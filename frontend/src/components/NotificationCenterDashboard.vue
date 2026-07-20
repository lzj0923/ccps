<template>
  <section class="notification-center-page">
    <div v-if="errorMessage" class="notification-api-message">{{ errorMessage }}</div>
    <div class="notification-summary-grid">
      <article v-for="card in summaryCards" :key="card.label" class="notification-summary-card">
        <div class="notification-summary-icon" v-html="icons[card.icon]"></div>
        <div><span>{{ card.label }}</span><strong>{{ card.value }}</strong><button @click="applySummary(card.action)">{{ card.note }} <b>›</b></button></div>
      </article>
    </div>

    <div class="notification-workspace">
      <section class="notification-list-card">
        <div class="notification-category-tabs">
          <button v-for="tab in categories" :key="tab.key" :class="{ active: category === tab.key }" @click="changeCategory(tab.key)">{{ tab.label }} <b>{{ tab.count }}</b></button>
        </div>
        <form class="notification-filters" @submit.prevent>
          <label class="notification-search"><span v-html="icons.search"></span><input v-model="keyword" placeholder="搜索通知標題、內容或房產"></label>
          <select v-model="readFilter" aria-label="通知狀態"><option>全部狀態</option><option>未讀</option><option>已讀</option></select>
          <select v-model="priorityFilter" aria-label="通知優先級"><option>全部優先級</option><option>高</option><option>中</option><option>低</option></select>
          <label class="notification-date"><span v-html="icons.calendar"></span><input v-model="startDate" type="date" aria-label="開始日期"><i>至</i><input v-model="endDate" type="date" aria-label="結束日期"></label>
          <button type="button" @click="resetFilters">重置</button>
        </form>
        <div class="notification-list">
          <div v-if="loading" class="notification-empty">正在讀取通知…</div>
          <article v-for="notice in pagedNotifications" v-else :key="notice.id" :class="{ selected: selectedId === notice.id, unread: !notice.read }" @click="selectNotice(notice)">
            <i class="notification-unread-dot"></i>
            <span class="notification-kind-icon" :class="notice.tone" v-html="icons[notice.icon]"></span>
            <div class="notification-list-copy"><h3>{{ notice.title }} <b v-if="notice.badge">{{ notice.badge }}</b></h3><p>{{ notice.body }}</p></div>
            <div class="notification-property"><strong>{{ notice.project || '所有房產' }}</strong><small>{{ notice.unit || '—' }}</small></div>
            <div class="notification-meta"><time>{{ formatTime(notice.createdAt) }}</time><small><span v-html="icons[notice.read ? 'clock' : 'unread']"></span>{{ notice.read ? '已讀' : '未讀' }}</small></div>
            <span class="notification-priority" :class="priorityClass(notice.priority)">{{ priorityLabel(notice.priority) }}</span><b class="notification-row-arrow">›</b>
          </article>
          <div v-if="!loading && !pagedNotifications.length" class="notification-empty">沒有符合條件的通知</div>
        </div>
        <footer class="notification-list-footer"><span>共 {{ filteredNotifications.length }} 條通知</span><div><button :disabled="currentPage === 1" @click="currentPage--">‹</button><button v-for="number in visiblePageNumbers" :key="number" :class="{ active: currentPage === number }" @click="currentPage = number">{{ number }}</button><button :disabled="currentPage === totalPages" @click="currentPage++">›</button></div></footer>
      </section>

      <section v-if="selectedNotice" class="notification-detail-card">
        <header><h2>{{ selectedNotice.title }}</h2><span :class="priorityClass(selectedNotice.priority)">{{ selectedNotice.badge || `${priorityLabel(selectedNotice.priority)}優先級` }}</span></header>
        <div class="notification-detail-meta"><span v-html="icons.calendar"></span>{{ formatTime(selectedNotice.createdAt) }}<i></i>通知編號：{{ selectedNotice.number }}<b :class="{ read: selectedNotice.read }">{{ selectedNotice.read ? '已讀' : '未讀' }}</b></div>
        <div class="notification-message"><p>{{ selectedNotice.detail || selectedNotice.title }}</p><p>{{ selectedNotice.message || selectedNotice.body }}</p><p>如您已完成相關事項，請忽略此通知。</p></div>
        <section class="notification-property-section"><h3>相關房產信息</h3><div class="notification-property-card"><div class="notification-property-photo"></div><div><strong>{{ selectedNotice.project || '所有房產' }}</strong><span>{{ selectedNotice.unit || '—' }}</span><small>{{ selectedNotice.city || '—' }}</small></div><dl><div><dt>關聯金額</dt><dd>{{ selectedNotice.amount ? `RM ${selectedNotice.amount}` : '—' }}</dd></div><div><dt>關聯日期</dt><dd>{{ selectedNotice.dueDate || '—' }}</dd></div></dl></div></section>
        <section class="notification-quick-actions"><h3>快速操作</h3><div><button v-for="action in quickActions" :key="action.key" type="button" @click="handleQuickAction(action)"><span v-html="icons[action.icon]"></span>{{ action.label }}</button></div></section>
        <section class="notification-related-records"><h3>相關記錄</h3><dl><div><dt>通知編號</dt><dd>{{ selectedNotice.number }}</dd></div><div><dt>通知類型</dt><dd>{{ categoryLabel(selectedNotice.category) }}</dd></div><div><dt>目前狀態</dt><dd>{{ selectedNotice.read ? '已讀' : '未讀' }}</dd></div></dl><button @click="markSelectedRead">標記為已讀 <span>→</span></button></section>
      </section>

      <aside class="notification-side-column">
        <section class="notification-side-card notification-tasks"><header><h2>待處理事項</h2><button @click="readFilter = '未讀'">查看全部 ({{ tasks.length }})</button></header><article v-for="task in tasks" :key="task.type + task.title"><i :class="priorityClass(task.priority)" v-html="icons[taskIcon(task.type)]"></i><div><strong>{{ task.title }}</strong><small>{{ task.detail }}</small><span>{{ priorityLabel(task.priority) }}優先級</span></div><b>{{ task.count }}</b></article><p v-if="!tasks.length" class="notification-side-empty">暫無待處理事項</p></section>
        <section class="notification-side-card notification-shortcuts"><header><h2>快捷入口</h2></header><button @click="changeCategory('all')"><span v-html="icons.message"></span><div><strong>全部通知</strong><small>查看所有通知記錄</small></div><b>{{ summary.totalCount }}</b></button><button @click="markAllRead"><span v-html="icons.check"></span><div><strong>標記為已讀</strong><small>一鍵標記全部為已讀</small></div></button><button @click="showToast('通知設定已開啟')"><span v-html="icons.settings"></span><div><strong>通知設置</strong><small>管理通知偏好與提醒方式</small></div></button><button @click="showToast('幫助中心已開啟')"><span v-html="icons.help"></span><div><strong>幫助中心</strong><small>常見問題與操作指引</small></div></button></section>
        <section class="notification-side-card notification-subscriptions"><header><h2>通知訂閱設置</h2></header><h3>接收方式</h3><div><button v-for="channel in channels" :key="channel.key" :class="{ active: channel.status === 'enabled', pending: channel.status === 'pending' }" @click="handleChannel(channel)"><span v-html="icons[channel.key === 'email' ? 'mail' : channel.key] || icons.bell"></span><strong>{{ channel.label }}</strong><i>{{ channel.status === 'enabled' ? '✓' : channel.status === 'pending' ? '…' : '+' }}</i></button><button type="button" class="notification-channel-disabled" disabled aria-label="WhatsApp 通知尚未開放"><span v-html="icons.whatsapp"></span><strong>WhatsApp</strong><i>＋</i></button></div><button class="notification-preferences" @click="openEmailSubscription">管理郵件通知 <span>→</span></button></section>
      </aside>
    </div>

    <div v-if="emailDialogOpen" class="property-detail-overlay" @click.self="closeEmailDialog">
      <section class="property-detail-dialog email-subscription-dialog" role="dialog" aria-modal="true" aria-label="郵件通知綁定">
        <header><div><span>通知訂閱</span><h2>郵件通知設定</h2></div><button :disabled="emailBusy" @click="closeEmailDialog">×</button></header>
        <div class="email-subscription-body">
          <template v-if="emailStep === 'bind'">
            <p>輸入接收通知的郵箱，我們會發送一組 6 位數驗證碼。</p>
            <label><span>郵件地址</span><input v-model.trim="emailAddress" type="email" placeholder="name@example.com" :disabled="emailBusy"></label>
            <button class="email-primary-action" :disabled="emailBusy || !validEmail" @click="requestEmailCode">{{ emailBusy ? '發送中…' : '發送驗證碼' }}</button>
          </template>
          <template v-else-if="emailStep === 'verify'">
            <p>驗證碼已發送至 <strong>{{ emailAddress }}</strong>，10 分鐘內有效。</p>
            <label><span>6 位數驗證碼</span><input v-model.trim="emailCode" inputmode="numeric" maxlength="6" placeholder="000000" :disabled="emailBusy"></label>
            <button class="email-primary-action" :disabled="emailBusy || !/^\d{6}$/.test(emailCode)" @click="verifyEmailCode">{{ emailBusy ? '驗證中…' : '驗證並啟用' }}</button>
            <button class="email-link-action" :disabled="emailBusy" @click="requestEmailCode">重新發送驗證碼</button>
          </template>
          <template v-else>
            <div class="email-bound-state"><span>✓</span><div><strong>郵件已綁定</strong><small>{{ emailAddress }}</small></div></div>
            <label class="email-subscription-toggle"><div><strong>接收郵件通知</strong><small>房款、租金、預備金及維修提醒</small></div><input v-model="emailEnabled" type="checkbox" :disabled="emailBusy" @change="toggleEmail"></label>
            <button class="email-link-action" :disabled="emailBusy" @click="emailStep = 'bind'">更換郵件地址</button>
          </template>
          <p v-if="emailError" class="email-subscription-error">{{ emailError }}</p>
        </div>
      </section>
    </div>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
import { fetchOwnerNotifications, markAllOwnerNotificationsRead, markOwnerNotificationRead, requestOwnerEmailVerification, toggleOwnerEmailSubscription, verifyOwnerEmail } from '../services/propertyApi';

export default {
  mixins: [pageBridge],
  data() {
    return {
      loading: true, errorMessage: '', notifications: [], selectedId: null, category: 'all', keyword: '', readFilter: '全部狀態', priorityFilter: '全部優先級', startDate: '', endDate: '', currentPage: 1, pageSize: 10,
      summary: { unreadCount: 0, pendingCount: 0, monthSystemCount: 0, importantCount: 0, totalCount: 0 }, categories: [], tasks: [], channels: [],
      emailDialogOpen: false, emailStep: 'bind', emailAddress: '', emailCode: '', emailEnabled: false, emailBusy: false, emailError: '',
      icons: {
        bell: '<svg viewBox="0 0 24 24" fill="none"><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9ZM10 21h4"/></svg>', clipboard: '<svg viewBox="0 0 24 24" fill="none"><rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4V2h6v2M8 9h8M8 13h6"/></svg>', announcement: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 14V9l13-5v15L4 14ZM17 9h3M18 4l2-2M18 19l2 2M6 14l2 6h4l-2-5"/></svg>', shield: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3 4 6v6c0 5 3.4 8 8 10 4.6-2 8-5 8-10V6l-8-3Z"/><path d="M12 8v5M12 17h.1"/></svg>', search: '<svg viewBox="0 0 24 24" fill="none"><circle cx="11" cy="11" r="7"/><path d="m16 16 5 5"/></svg>', calendar: '<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 10h18"/></svg>', payment: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 2h8l4 4v16H6V2Z"/><path d="M14 2v5h5M9 12h6M9 16h4"/></svg>', home: '<svg viewBox="0 0 24 24" fill="none"><path d="m3 11 9-8 9 8v10H5V10"/><path d="M9 21v-7h6v7"/></svg>', wrench: '<svg viewBox="0 0 24 24" fill="none"><path d="M14 6a5 5 0 0 0-6.5 6.5L3 17l4 4 4.5-4.5A5 5 0 0 0 18 10l-3 3-4-4 3-3Z"/></svg>', file: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 2h8l4 4v16H6V2Z"/><path d="M14 2v5h5M9 13h6M9 17h5"/></svg>', clock: '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>', unread: '<svg viewBox="0 0 24 24" fill="none"><path d="M3 6h18v12H3V6Z"/><path d="m3 7 9 7 9-7"/></svg>', detail: '<svg viewBox="0 0 24 24" fill="none"><rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 8h6M9 12h6M9 16h4"/></svg>', upload: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 16V4m0 0-5 5m5-5 5 5M4 18v3h16v-3"/></svg>', headset: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 14v-2a8 8 0 0 1 16 0v2M4 14h3v6H4zM17 14h3v6h-3zM17 20c-1 2-3 2-5 2"/></svg>', message: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 4h16v13H9l-5 4V4Z"/><path d="M8 9h8M8 13h5"/></svg>', check: '<svg viewBox="0 0 24 24" fill="none"><path d="m4 12 5 5L20 6"/></svg>', settings: '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3"/><path d="M19 12a7 7 0 0 0-.1-1l2-2-2-3-3 1a7 7 0 0 0-2-1l-1-3H9L8 6a7 7 0 0 0-2 1L3 6 1 9l2 2a7 7 0 0 0 0 2l-2 2 2 3 3-1a7 7 0 0 0 2 1l1 3h4l1-3a7 7 0 0 0 2-1l3 1 2-3-2-2a7 7 0 0 0 .1-1Z"/></svg>', help: '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9"/><path d="M9.5 9a2.5 2.5 0 1 1 3 2.5c-.5.2-.5.8-.5 1.5M12 17h.1"/></svg>', mail: '<svg viewBox="0 0 24 24" fill="none"><path d="M3 5h18v14H3V5Z"/><path d="m3 6 9 7 9-7"/></svg>', whatsapp: '<svg viewBox="0 0 24 24" fill="none"><path d="M20 11a8 8 0 0 1-11.8 7L4 20l1.7-4A8 8 0 1 1 20 11Z"/><path d="M9 8c1 4 3 6 7 7"/></svg>'
      }
    };
  },
  computed: {
    summaryCards() { return [{ icon: 'bell', label: '未讀通知', value: this.summary.unreadCount, note: '查看全部', action: 'unread' }, { icon: 'clipboard', label: '待處理通知', value: this.summary.pendingCount, note: '查看待處理', action: 'pending' }, { icon: 'announcement', label: '本月系統通知', value: this.summary.monthSystemCount, note: '本月發送', action: 'system' }, { icon: 'shield', label: '重要提醒', value: this.summary.importantCount, note: '需盡快處理', action: 'high' }]; },
    filteredNotifications() { const key = this.keyword.trim().toLowerCase(); return this.notifications.filter(n => (this.category === 'all' || n.category === this.category) && (this.readFilter === '全部狀態' || (this.readFilter === '已讀') === n.read) && (this.priorityFilter === '全部優先級' || this.priorityLabel(n.priority) === this.priorityFilter) && (!this.startDate || String(n.createdAt).slice(0, 10) >= this.startDate) && (!this.endDate || String(n.createdAt).slice(0, 10) <= this.endDate) && (!key || `${n.title} ${n.body} ${n.project || ''} ${n.unit || ''}`.toLowerCase().includes(key))); },
    totalPages() { return Math.max(1, Math.ceil(this.filteredNotifications.length / this.pageSize)); },
    pagedNotifications() { return this.filteredNotifications.slice((this.currentPage - 1) * this.pageSize, this.currentPage * this.pageSize); },
    visiblePageNumbers() { return Array.from({ length: Math.min(5, this.totalPages) }, (_, i) => i + 1); },
    selectedNotice() { return this.notifications.find(n => n.id === this.selectedId) || null; },
    validEmail() { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.emailAddress); },
    quickActions() {
      const actions = {
        payment: [
          { key: 'payment-progress', icon: 'payment', label: '查看房款進度', message: '已開啟房款進度' },
          { key: 'payment-proof', icon: 'upload', label: '上傳付款憑證', message: '請到房款進度頁面上傳付款憑證' },
          { key: 'contact-admin', icon: 'headset', label: '聯繫管理員', message: '正在聯繫管理員' }
        ],
        rent: [
          { key: 'rent-income', icon: 'home', label: '查看租金收入', message: '已開啟租金收入' },
          { key: 'rent-confirm', icon: 'check', label: '確認租金到賬', message: '請到租金收入頁面確認到賬' },
          { key: 'contact-admin', icon: 'headset', label: '聯繫管理員', message: '正在聯繫管理員' }
        ],
        reserve: [
          { key: 'reserve-detail', icon: 'shield', label: '查看預備金', message: '已開啟預備金詳情' },
          { key: 'reserve-topup', icon: 'upload', label: '補充預備金', message: '請到預備金頁面提交補充申請' },
          { key: 'contact-admin', icon: 'headset', label: '聯繫管理員', message: '正在聯繫管理員' }
        ],
        maintenance: [
          { key: 'maintenance-detail', icon: 'wrench', label: '查看維修詳情', message: '已開啟維修詳情' },
          { key: 'contact-admin', icon: 'headset', label: '聯繫管理員', message: '正在聯繫管理員' }
        ],
        document: [
          { key: 'document-detail', icon: 'file', label: '查看文件資料', message: '已開啟文件資料' },
          { key: 'document-upload', icon: 'upload', label: '上傳文件', message: '請到文件資料頁面上傳文件' },
          { key: 'contact-admin', icon: 'headset', label: '聯繫管理員', message: '正在聯繫管理員' }
        ],
        system: [
          { key: 'notice-detail', icon: 'detail', label: '查看通知詳情', message: '已開啟通知詳情' },
          { key: 'contact-admin', icon: 'headset', label: '聯繫管理員', message: '正在聯繫管理員' }
        ]
      };
      return actions[this.selectedNotice?.category] || actions.system;
    }
  },
  watch: { keyword() { this.currentPage = 1; }, readFilter() { this.currentPage = 1; }, priorityFilter() { this.currentPage = 1; }, startDate() { this.currentPage = 1; }, endDate() { this.currentPage = 1; } },
  mounted() { this.loadNotifications(); },
  methods: {
    async loadNotifications() { this.loading = true; this.errorMessage = ''; try { const data = await fetchOwnerNotifications(); this.summary = data.summary || this.summary; this.categories = data.categories || []; this.tasks = data.tasks || []; this.channels = data.channels || []; this.notifications = (data.notifications || []).map(this.normalizeNotice); if (this.notifications.length && !this.selectedId) this.selectedId = this.notifications[0].id; } catch (error) { this.errorMessage = error.message || '通知數據讀取失敗'; } finally { this.loading = false; } },
    normalizeNotice(notice) { const priority = notice.priority || 'normal'; const map = { payment: 'payment', rent: 'home', reserve: 'shield', maintenance: 'wrench', document: 'file', system: 'announcement' }; return { ...notice, project: notice.projectName, unit: notice.unitNo, read: notice.status === 'read', icon: map[notice.category] || 'announcement', tone: priority === 'high' ? 'red' : (notice.category === 'reserve' ? 'green' : 'blue'), badge: priority === 'high' ? '重要' : '' }; },
    changeCategory(value) { this.category = value; this.currentPage = 1; },
    async selectNotice(notice) { this.selectedId = notice.id; if (!notice.read) { notice.read = true; notice.status = 'read'; this.summary.unreadCount = Math.max(0, this.summary.unreadCount - 1); try { await markOwnerNotificationRead(notice.id); } catch { notice.read = false; notice.status = 'unread'; } } },
    priorityClass(value) { return { high: 'high', normal: 'medium', low: 'low', 高: 'high', 中: 'medium', 低: 'low' }[value] || 'low'; },
    priorityLabel(value) { return { high: '高', normal: '中', low: '低', 高: '高', 中: '中', 低: '低' }[value] || '低'; },
    categoryLabel(value) { return (this.categories.find(item => item.key === value) || {}).label || value; },
    taskIcon(type) { return { payment: 'payment', rent: 'home', reserve: 'shield', maintenance: 'wrench', document: 'file', system: 'announcement' }[type] || 'bell'; },
    formatTime(value) { if (!value) return '—'; const date = new Date(value); return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }); },
    resetFilters() { this.keyword = ''; this.readFilter = '全部狀態'; this.priorityFilter = '全部優先級'; this.startDate = ''; this.endDate = ''; this.currentPage = 1; },
    handleQuickAction(action) { this.showToast(action.message); },
    applySummary(action) {
      if (action === 'unread' || action === 'pending') {
        this.category = 'all';
        this.readFilter = '未讀';
        this.priorityFilter = '全部優先級';
      } else if (action === 'system') {
        this.category = 'system';
        this.readFilter = '全部狀態';
        this.priorityFilter = '全部優先級';
      } else if (action === 'high') {
        this.category = 'all';
        this.readFilter = '全部狀態';
        this.priorityFilter = '高';
      }
      this.currentPage = 1;
    },
    async markAllRead() { try { await markAllOwnerNotificationsRead(); this.notifications.forEach(n => { n.read = true; n.status = 'read'; }); this.summary.unreadCount = 0; this.tasks = []; this.showToast('全部通知已標記為已讀'); } catch (error) { this.showToast(error.message || '操作失敗'); } },
    async markSelectedRead() { if (this.selectedNotice && !this.selectedNotice.read) await this.selectNotice(this.selectedNotice); },
    handleChannel(channel) { if (channel.key === 'email') this.openEmailSubscription(); else this.showToast('站內通知已啟用'); },
    openEmailSubscription() { const channel = this.channels.find(item => item.key === 'email'); this.emailAddress = channel?.destination || ''; this.emailEnabled = channel?.status === 'enabled'; this.emailStep = channel?.verified ? 'manage' : channel?.status === 'pending' ? 'verify' : 'bind'; this.emailCode = ''; this.emailError = ''; this.emailDialogOpen = true; },
    closeEmailDialog() { if (!this.emailBusy) { this.emailDialogOpen = false; this.emailError = ''; this.emailCode = ''; } },
    applyEmailChannel(channel) { const index = this.channels.findIndex(item => item.key === 'email'); if (index >= 0) this.channels.splice(index, 1, channel); else this.channels.push(channel); },
    async requestEmailCode() { if (!this.validEmail || this.emailBusy) return; this.emailBusy = true; this.emailError = ''; try { const channel = await requestOwnerEmailVerification(this.emailAddress); this.applyEmailChannel(channel); this.emailStep = 'verify'; this.showToast('驗證碼已發送，請檢查郵箱'); } catch (error) { this.emailError = error.message || '驗證碼發送失敗'; } finally { this.emailBusy = false; } },
    async verifyEmailCode() { if (!/^\d{6}$/.test(this.emailCode) || this.emailBusy) return; this.emailBusy = true; this.emailError = ''; try { const channel = await verifyOwnerEmail(this.emailAddress, this.emailCode); this.applyEmailChannel(channel); this.emailEnabled = true; this.emailStep = 'manage'; this.emailCode = ''; this.showToast('郵件通知已成功啟用'); } catch (error) { this.emailError = error.message || '郵件驗證失敗'; } finally { this.emailBusy = false; } },
    async toggleEmail() { if (this.emailBusy) return; const target = this.emailEnabled; this.emailBusy = true; this.emailError = ''; try { const channel = await toggleOwnerEmailSubscription(target); this.applyEmailChannel(channel); this.showToast(target ? '郵件通知已開啟' : '郵件通知已暫停'); } catch (error) { this.emailEnabled = !target; this.emailError = error.message || '郵件訂閱更新失敗'; } finally { this.emailBusy = false; } }
  }
};
</script>

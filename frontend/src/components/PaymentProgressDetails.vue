<template>
  <main class="payment-progress-page">
    <section class="payment-property-card">
      <div class="payment-property-photo"><span>{{ $t('legacy.t_c6a41f9e64a4') }}</span></div>
      <div class="payment-property-info">
        <div class="payment-property-title">
          <label class="payment-property-selector" :aria-label="$t('legacy.t_54d7385f1f3b')">
            <select v-model="selectedPropertyKey" :disabled="!availableProperties.length">
              <option v-if="!availableProperties.length" value="">{{ $t('legacy.t_a87d773427c0') }}</option>
              <option v-for="item in availableProperties" :key="item.key" :value="item.key">
                {{ item.projectName }} · {{ item.unitNo || $t('legacy.t_394b00946c93') }}
              </option>
            </select>
            <ChevronDown />
          </label>
        </div>
        <div class="payment-owner-grid">
          <div><UserRound /><span>{{ $t('legacy.t_9372e35aa0f9') }}<strong>{{ property.owner }}</strong></span></div>
          <div><Phone /><span>{{ $t('legacy.t_e02f6e5760fd') }}<strong>{{ property.phone }}</strong></span></div>
          <div><CalendarDays /><span>{{ $t('legacy.t_1e13248dbf58') }}<strong>{{ property.signedDate }}</strong></span></div>
          <div><BadgeCheck /><span>{{ $t('legacy.t_886c40d24b98') }}<strong :class="paymentStatusClass">{{ paymentStatusLabel }}</strong></span></div>
        </div>
      </div>
    </section>

    <section class="payment-stat-grid">
      <article v-for="stat in stats" :key="stat.label">
        <component :is="stat.icon" />
        <div><span>{{ stat.label }}</span><strong :class="stat.tone">{{ stat.value }}</strong><small v-if="stat.note" :class="stat.noteTone">{{ stat.note }}</small></div>
      </article>
    </section>

    <section v-if="paymentLoading" class="payment-detail-state">{{ $t('legacy.t_a13de92a7917') }}</section>
    <section v-else-if="paymentError" class="payment-detail-state error" role="alert">
      <strong>{{ $t('legacy.t_4f052bf0a9d7') }}</strong><span>{{ paymentError }}</span><button @click="loadPaymentDetails">{{ $t('legacy.t_0a12f2ebe04f') }}</button>
    </section>
    <section v-else-if="!selectedPropertyKey" class="payment-detail-state">{{ $t('legacy.t_9a5a1ee361c2') }}</section>

    <section v-else class="payment-workspace">
      <div class="payment-main-card">
        <div class="payment-section-title">{{ $t('legacy.t_7b21acdd297f') }}</div>
        <template v-if="installments.length">
          <div class="milestone-track">
            <div v-for="(stage, index) in stages" :key="stage.id" class="milestone" :class="stage.state">
              <div class="milestone-line" v-if="index < stages.length - 1"></div>
              <span class="milestone-dot"><Check v-if="stage.state === 'done'" /><Building2 v-else-if="stage.state === 'current'" /><LockKeyhole v-else /></span>
              <strong>{{ stage.label }}</strong>
              <small>{{ stage.note }}</small>
            </div>
          </div>

          <div class="installment-heading">{{ $t('legacy.t_5da80e2162d7') }}</div>
          <div class="installment-table-wrap">
            <table class="installment-table">
              <thead><tr><th>{{ $t('legacy.t_4ca39faad0bd') }}</th><th>{{ $t('legacy.t_a298bdb58afb') }}</th><th>{{ $t('legacy.t_57cc0b38b602') }}</th><th>{{ $t('legacy.t_b0a5b93d3722') }}</th><th>{{ $t('legacy.t_5eda4c259280') }}</th><th>{{ $t('legacy.t_058f511c98cf') }}</th><th>{{ $t('legacy.t_62e951a692ff') }}</th><th>{{ $t('legacy.t_cad9a7bf3c95') }}</th><th>{{ $t('legacy.t_a2a569f69fdb') }}</th></tr></thead>
              <tbody>
                <tr v-for="row in installments" :key="row.id" :class="{ current: row.state === 'current' }">
                  <td><i :class="row.state">{{ row.no }}</i>{{ row.name }}<small v-if="row.note">{{ row.note }}</small></td>
                  <td>{{ row.due }}</td><td>{{ row.amount }}</td><td :class="{ green: row.paidValue > 0 }">{{ row.paid }}</td><td :class="{ gold: row.unpaidValue > 0 }">{{ row.unpaid }}</td>
                  <td>{{ displayDate(row.paymentDate) }}</td><td><span class="table-status" :class="row.state">{{ row.status }}</span></td><td><button v-if="row.receipt" @click="viewReceipt(row)">{{ $t('legacy.t_f7acefd2d4cd') }}</button><span v-else>—</span></td><td class="finance-confirmation-cell"><span v-if="row.confirmed" class="confirmed"><BadgeCheck />{{ $t('legacy.t_d9fea67ad2be') }}</span><span v-else>{{ row.confirmation }}</span><small v-if="row.rejectionReason" class="rejection-reason">{{ $t('legacy.t_0f93c2bb0a58') }}{{ row.rejectionReason }}</small></td>
                </tr>
              </tbody>
              <tfoot><tr><td>{{ $t('legacy.t_92bcbf71cb7b') }}</td><td></td><td>{{ money(scheduledAmount) }}</td><td class="green">{{ money(paid) }}</td><td class="gold">{{ money(remaining) }}</td><td colspan="4">{{ paidInstallments }} / {{ totalInstallments }} {{ $t('legacy.t_fc73601f2012') }}</td></tr></tfoot>
            </table>
          </div>
        </template>
        <div v-else class="payment-plan-empty"><FileText /><strong>{{ $t('legacy.t_ccd4489c1dcf') }}</strong><span>{{ $t('legacy.t_005d4fca04f6') }}</span></div>
      </div>

      <aside class="payment-summary-card">
        <h3>{{ $t('legacy.t_7dafb78adf68') }}</h3>
        <div class="payment-summary-top">
          <div class="payment-ring" :style="{ '--progress': `${progress * 3.6}deg` }"><div><strong>{{ progress.toFixed(2) }}%</strong><span>{{ $t('legacy.t_7c307758b5d5') }}</span></div></div>
          <div class="summary-amounts"><span>{{ $t('legacy.t_b0a5b93d3722') }}<strong class="green">{{ money(paid) }}</strong></span><span>{{ $t('legacy.t_5eda4c259280') }}<strong class="gold">{{ money(remaining) }}</strong></span></div>
        </div>
        <dl>
          <div><dt><CalendarDays />{{ $t('legacy.t_7e43746be503') }}</dt><dd>{{ latestPaymentDate }}</dd></div>
          <div><dt><WalletCards />{{ $t('legacy.t_c6b9a8cfdb21') }}</dt><dd>{{ latestPaymentMethod }}<small v-if="latestPaymentMethod !== '—'">{{ paymentMethodDescription }}</small></dd></div>
          <div><dt><BadgeCheck />{{ $t('legacy.t_9beff15b09ff') }}</dt><dd class="finance-confirmation-summary" :class="{ green: latestPaymentConfirmed }"><span><BadgeCheck v-if="latestPaymentConfirmed" />{{ latestConfirmationLabel }}</span><small v-if="latestRejectionReason" class="rejection-reason">{{ $t('legacy.t_d388e6fe0905') }}{{ latestRejectionReason }}</small></dd></div>
        </dl>
        <button class="receipt-button" :disabled="!latestReceiptAvailable" @click="viewLatestReceipt"><ReceiptText />{{ $t('legacy.t_bb4bc00cff43') }}</button>
        <button class="upload-button" :disabled="!installments.length" @click="uploadProof"><Upload />{{ $t('legacy.t_f202b281d042') }}</button>
        <p><ShieldCheck />{{ $t('legacy.t_4624cfd3d5a6') }}</p>
      </aside>
    </section>
  </main>
</template>

<script>
import {
  BadgeCheck, Building2, CalendarDays, ChartPie, Check, ChevronDown, Coins, FileText,
  Home, Hourglass, LockKeyhole, Phone, ReceiptText, ShieldCheck, Upload,
  UserRound, WalletCards
} from '@lucide/vue';
import { fetchPaymentProgress } from '../services/propertyApi';
import { formatDate } from '../utils/dateFormat';

const paymentStatusLabels = {
  paying: '正常缴费中', paid: '已缴清', due_soon: '即将到期', overdue: '已逾期', not_configured: '尚未设置'
};

const installmentStatusLabels = {
  paid: '已完成', current: '当前应缴', overdue: '已逾期', pending: '未到期'
};

const confirmationLabels = {
  confirmed: '已确认', pending: '待确认', rejected: '已退回'
};

export default {
  inject: ['page'],
  emits: ['upload'],
  props: { initialPropertyKey: { type: String, default: '' } },
  components: { BadgeCheck, Building2, CalendarDays, Check, ChevronDown, LockKeyhole, Phone, ReceiptText, ShieldCheck, Upload, UserRound, WalletCards },
  data() {
    return {
      selectedPropertyKey: '',
      paymentDetails: null,
      paymentLoading: false,
      paymentError: '',
      requestSerial: 0
    };
  },
  computed: {
    availableProperties() {
      return (this.page.ownerDashboard?.properties || [])
        .filter(item => item.assetStage === 'PRE_HANDOVER')
        .map((item, index) => ({
          ...item,
          key: String(item.ownerUnitId ?? item.id ?? `${item.projectName}-${item.unitNo}-${index}`)
        }));
    },
    sourceProperty() {
      return this.availableProperties.find(item => item.key === this.selectedPropertyKey) || {};
    },
    property() {
      const detail = this.paymentDetails?.property || {};
      return {
        owner: detail.ownerName || '—',
        phone: detail.phone || '—',
        signedDate: detail.signedDate || '—'
      };
    },
    summary() { return this.paymentDetails?.summary || {}; },
    total() { return Number(this.summary.purchasePrice ?? this.sourceProperty.purchasePrice ?? 0); },
    scheduledAmount() { return Number(this.summary.scheduledAmount ?? 0); },
    paid() { return Number(this.summary.paidAmount ?? this.sourceProperty.paidAmount ?? 0); },
    remaining() { return Math.max(0, Number(this.summary.remainingAmount ?? this.sourceProperty.remainingAmount ?? (this.total - this.paid))); },
    paidInstallments() { return Number(this.summary.paidInstallmentCount ?? this.sourceProperty.paidInstallmentCount ?? 0); },
    totalInstallments() { return Number(this.summary.totalInstallmentCount ?? this.sourceProperty.totalInstallmentCount ?? 0); },
    progress() { return this.total ? Math.min(100, this.paid / this.total * 100) : 0; },
    paymentStatus() { return this.paymentDetails?.property?.paymentStatus || this.sourceProperty.paymentStatus || 'not_configured'; },
    paymentStatusLabel() { return paymentStatusLabels[this.paymentStatus] || '尚未设置'; },
    paymentStatusClass() { return ['payment-property-status', this.paymentStatus]; },
    nextDueAmount() { return Number(this.summary.nextDueAmount ?? 0); },
    nextDueDate() { return this.summary.nextDueDate || this.sourceProperty.nextDueDate || '—'; },
    nextDueNote() {
      if (!this.summary.nextDueDate) return '';
      const days = this.daysUntil(this.summary.nextDueDate);
      if (days < 0) return `已逾期 ${Math.abs(days)} 天`;
      if (days === 0) return '今天到期';
      return `剩余 ${days} 天`;
    },
    stats() {
      return [
        { icon: Home, label: '房产总价', value: this.money(this.total) },
        { icon: WalletCards, label: '已缴金额', value: this.money(this.paid), tone: 'green' },
        { icon: FileText, label: '未缴金额', value: this.money(this.remaining), tone: 'gold' },
        { icon: ChartPie, label: '已缴期数', value: `${this.paidInstallments} / ${this.totalInstallments} 期` },
        { icon: Hourglass, label: '剩余期数', value: `${Math.max(0, this.totalInstallments - this.paidInstallments)} 期` },
        { icon: Coins, label: '下一期应缴金额', value: this.money(this.nextDueAmount), tone: 'gold' },
        { icon: CalendarDays, label: '下一期到期日', value: this.nextDueDate, note: this.nextDueNote, noteTone: this.nextDueNote.includes('逾期') ? 'overdue-note' : '' }
      ];
    },
    installments() {
      return (this.paymentDetails?.installments || []).map(row => {
        const state = row.status === 'paid' ? 'done' : row.status === 'current' ? 'current' : row.status === 'overdue' ? 'overdue' : 'locked';
        return {
          id: row.id,
          no: row.installmentNo,
          name: row.milestone || `第 ${row.installmentNo} 期`,
          due: row.dueDate || '—',
          amount: this.amount(row.amountDue),
          paid: this.amount(row.amountPaid),
          unpaid: this.amount(row.unpaidAmount),
          paidValue: Number(row.amountPaid || 0),
          unpaidValue: Number(row.unpaidAmount || 0),
          paymentDate: row.paymentDate || '—',
          status: installmentStatusLabels[row.status] || row.status || '未到期',
          state,
          note: row.status === 'current' || row.status === 'overdue' ? this.dueNote(row.dueDate) : '',
          receipt: Number(row.receiptCount || 0) > 0,
          confirmed: row.confirmationStatus === 'confirmed',
          confirmation: confirmationLabels[row.confirmationStatus] || '—',
          rejectionReason: row.confirmationStatus === 'rejected' ? row.rejectionReason || '' : ''
        };
      });
    },
    stages() {
      return this.installments.map(row => ({ id: row.id, label: row.name, note: row.status, state: row.state }));
    },
    latestPayment() { return this.paymentDetails?.latestPayment || null; },
    latestPaymentDate() { return this.latestPayment?.paymentDate || '—'; },
    latestPaymentMethod() { return this.latestPayment?.paymentMethod || '—'; },
    paymentMethodDescription() {
      return { bank_transfer: '银行转账', fpx: 'FPX', cash: '现金', card: '银行卡' }[String(this.latestPaymentMethod).toLowerCase()] || '';
    },
    latestPaymentConfirmed() { return this.latestPayment?.confirmationStatus === 'confirmed'; },
    latestConfirmationLabel() { return confirmationLabels[this.latestPayment?.confirmationStatus] || '—'; },
    latestRejectionReason() { return this.latestPayment?.confirmationStatus === 'rejected' ? this.latestPayment?.rejectionReason || '' : ''; },
    latestReceiptAvailable() { return Boolean(this.latestPayment?.receiptId); }
  },
  watch: {
    availableProperties: {
      immediate: true,
      handler(properties) {
        if (!properties.some(item => item.key === this.selectedPropertyKey)) {
          this.selectedPropertyKey = properties.some(item => item.key === this.initialPropertyKey)
            ? this.initialPropertyKey
            : properties[0]?.key || '';
        }
      }
    },
    selectedPropertyKey: {
      immediate: true,
      handler(value) {
        if (value) this.loadPaymentDetails();
        else {
          this.paymentDetails = null;
          this.paymentError = '';
        }
      }
    }
  },
  methods: {
    async loadPaymentDetails() {
      const ownerUnitId = Number(this.selectedPropertyKey);
      if (!Number.isFinite(ownerUnitId)) return;
      const requestId = ++this.requestSerial;
      this.paymentLoading = true;
      this.paymentError = '';
      this.paymentDetails = null;
      try {
        const details = await fetchPaymentProgress(ownerUnitId);
        if (requestId === this.requestSerial) this.paymentDetails = details;
      } catch (error) {
        if (requestId === this.requestSerial) this.paymentError = error.message || '无法载入房款进度';
      } finally {
        if (requestId === this.requestSerial) this.paymentLoading = false;
      }
    },
    currencyPrefix() {
      const currency = this.paymentDetails?.property?.currency || 'MYR';
      return currency === 'MYR' ? 'RM' : currency;
    },
    displayDate(value) { return formatDate(value); },
    money(value) { return `${this.currencyPrefix()} ${this.amount(value)}`; },
    amount(value) { return Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    daysUntil(dateText) {
      const target = new Date(`${dateText}T00:00:00`);
      const today = new Date();
      target.setHours(0, 0, 0, 0);
      today.setHours(0, 0, 0, 0);
      return Math.round((target - today) / 86400000);
    },
    dueNote(dateText) {
      if (!dateText) return '';
      const days = this.daysUntil(dateText);
      return days < 0 ? `已逾期 ${Math.abs(days)} 天` : days === 0 ? '今天到期' : `剩余 ${days} 天`;
    },
    viewReceipt(row) { this.page.showToast(`第 ${row.no} 期共有付款凭证，查看功能将在凭证阶段接入`); },
    viewLatestReceipt() { if (this.latestReceiptAvailable) this.page.showToast('查看凭证功能将在凭证阶段接入'); },
    uploadProof() {
      const rawInstallments = this.paymentDetails?.installments || [];
      const installment = rawInstallments.find(item => item.status === 'overdue')
        || rawInstallments.find(item => item.status === 'current')
        || rawInstallments.find(item => Number(item.unpaidAmount || 0) > 0)
        || null;
      this.$emit('upload', {
        ownerUnitId: this.paymentDetails?.property?.ownerUnitId,
        projectName: this.paymentDetails?.property?.projectName || this.sourceProperty.projectName || '—',
        unitNo: this.paymentDetails?.property?.unitNo || this.sourceProperty.unitNo || '—',
        ownerName: this.paymentDetails?.property?.ownerName || '—',
        currency: this.paymentDetails?.property?.currency || 'MYR',
        installmentId: installment?.id || null,
        installmentNo: installment?.installmentNo || null,
        milestone: installment?.milestone || '尚未设置付款计划',
        amount: Number(installment?.unpaidAmount || 0),
        dueDate: installment?.dueDate || null,
        paymentStatus: this.paymentStatus
      });
    }
  }
};
</script>

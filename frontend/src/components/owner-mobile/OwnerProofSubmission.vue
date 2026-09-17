<template>
  <section class="owner-self-service">
    <h2>{{ $t('ownerApp.proof.' + kind) }}</h2>
    <p>{{ $t('ownerApp.proof.hint') }}</p>
    <button v-if="!opened" type="button" @click="open">{{ $t('ownerApp.proof.open') }}</button>
    <p v-else-if="loading" aria-live="polite">{{ $t('legacy.t_03a17d236ff2') }}</p>
    <form v-else-if="!submitted && options.length" @submit.prevent="submit">
      <fieldset :disabled="busy">
        <label class="owner-app-field"><span>{{ $t('ownerApp.proof.target') }}</span><select v-model="form.targetId" required><option value="" disabled>{{ $t('ownerApp.proof.choose') }}</option><option v-for="item in options" :key="item.id" :value="String(item.id)">{{ $lt(item.label) }}</option></select></label>
        <p>{{ $t('ownerApp.currency') }}：{{ currency }}</p>
        <label class="owner-app-field"><span>{{ $t('ownerApp.amount') }}</span><input v-model="form.amount" type="number" inputmode="decimal" min="0.01" :max="maximum" step="0.01" required></label>
        <label class="owner-app-field"><span>{{ $t('ownerApp.proof.paymentDate') }}</span><input v-model="form.paymentDate" type="date" :max="today" required></label>
        <label v-for="key in ['bankName', 'reference', 'payerName']" :key="key" class="owner-app-field"><span>{{ $t('ownerApp.proof.' + key) }}</span><input v-model.trim="form[key]" :maxlength="key === 'payerName' ? 160 : 55" required></label>
        <label class="owner-app-field"><span>{{ $t('ownerApp.proof.note') }}</span><textarea v-model="form.note" maxlength="200" rows="2"></textarea></label>
        <label class="owner-app-field"><span>{{ $t('ownerApp.proof.files') }}</span><input type="file" accept="image/jpeg,image/png,application/pdf" multiple required @change="files = Array.from($event.target.files || [])"></label>
        <p>{{ $t('ownerApp.proof.fileHint') }}</p><ul v-if="files.length"><li v-for="(file, index) in files" :key="index">{{ file.name }}</li></ul>
        <div class="owner-filter-actions"><button type="submit">{{ $t('ownerApp.proof.submit') }}</button><button type="button" @click="close">{{ $t('ownerApp.account.cancel') }}</button></div>
      </fieldset>
    </form>
    <p v-else-if="opened && !submitted && !error">{{ $t('ownerApp.proof.noTarget') }}</p>
    <p v-if="submitted" role="status">{{ $t('ownerApp.proof.submitted') }} {{ submitted }}</p>
    <p v-if="error" role="alert">{{ $lt(error) }}</p>
    <button v-if="opened && error && !options.length" type="button" @click="open">{{ $t('ownerApp.retry') }}</button>
  </section>
</template>
<script>
import { fetchOwnerReserve, submitReserveTopup, submitPaymentProof } from '../../services/propertyApi';
import { validateOwnerProof, ownerProofPayload } from '../../utils/ownerProofSubmission';
function localDate() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; }
export default {
  props: { kind: { type: String, default: 'reserve' }, property: { type: Object, required: true }, installments: { type: Array, default: () => [] }, ownerName: { type: String, default: '' } },
  emits: ['submitted'],
  data: () => ({ opened: false, loading: false, busy: false, error: '', submitted: '', accounts: [], files: [], form: {}, today: localDate() }),
  computed: {
    currency() { return this.property.currency || 'MYR'; },
    options() { return this.kind === 'reserve' ? this.accounts.filter(a => String(a.ownerUnitId) === String(this.property.ownerUnitId)).map(a => ({ ...a, label: `${a.projectName} · ${a.unitNo}` })) : this.installments.filter(i => Number(i.unpaidAmount ?? Number(i.amountDue) - Number(i.amountPaid)) > 0).map(i => ({ ...i, label: `${i.milestone || i.installmentNo} · ${i.dueDate || '—'}` })); },
    maximum() { const item = this.options.find(i => String(i.id) === String(this.form.targetId)); return this.kind === 'reserve' ? 1000000 : Math.max(0, Number(item?.unpaidAmount ?? Number(item?.amountDue) - Number(item?.amountPaid)) || 0); }
  },
  methods: {
    async open() {
      this.opened = true; this.loading = true; this.error = ''; this.submitted = ''; this.files = [];
      this.form = { targetId: '', amount: '', paymentDate: localDate(), bankName: '', reference: '', payerName: this.ownerName, note: '' };
      try { if (this.kind === 'reserve') this.accounts = (await fetchOwnerReserve()).accounts || []; if (this.options.length === 1) this.form.targetId = String(this.options[0].id); }
      catch (e) { this.error = e.message; } finally { this.loading = false; }
    },
    close() { if (this.busy) return; this.opened = false; this.form = {}; this.files = []; this.error = ''; },
    async submit() {
      if (this.busy || this.submitted) return;
      const invalid = validateOwnerProof(this.form, this.files, localDate(), this.maximum);
      if (invalid) { this.error = this.$t('ownerApp.proof.errors.' + invalid); return; }
      this.busy = true; this.error = '';
      try {
        const payload = ownerProofPayload(this.form, this.kind);
        const result = this.kind === 'reserve' ? await submitReserveTopup(payload, this.files) : await submitPaymentProof(this.property.ownerUnitId, payload, this.files);
        this.submitted = result.transactionNo || result.receiptNo || String(result.financeRecordId || result.receiptId);
        this.files = []; this.$emit('submitted');
      } catch (e) { this.error = e.message; } finally { this.busy = false; }
    }
  }
};
</script>

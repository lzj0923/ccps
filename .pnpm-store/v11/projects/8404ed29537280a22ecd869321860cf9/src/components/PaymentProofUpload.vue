<template>
  <main class="proof-upload-page">
    <nav class="proof-breadcrumb" :aria-label="$t('legacy.t_01c6361f68c1')">
      <button @click="$emit('back')"><ArrowLeft />{{ $t('legacy.t_11d024154013') }}</button><span>{{ $t('legacy.t_034f523e2a94') }}</span><ChevronRight /><strong>{{ $t('legacy.t_f202b281d042') }}</strong>
    </nav>

    <section class="proof-property-summary">
      <div class="proof-property-photo"><span>{{ $t('legacy.t_c6a41f9e64a4') }}</span></div>
      <div class="proof-project"><h2>{{ context.projectName }}</h2><p><MapPin />{{ context.unitNo }}</p></div>
      <dl>
        <div><dt>{{ $t('legacy.t_3726e8cddd71') }}</dt><dd>{{ context.projectName }}</dd></div>
        <div><dt>{{ $t('legacy.t_cf4210e06be7') }}</dt><dd>{{ context.unitNo }}</dd></div>
        <div><dt>{{ $t('legacy.t_ad92d9755f76') }}</dt><dd>{{ installmentTitle }}<small>{{ context.milestone }}</small></dd></div>
        <div><dt>{{ $t('legacy.t_57cc0b38b602') }}</dt><dd class="gold">{{ money(context.amount) }}</dd></div>
        <div><dt>{{ $t('legacy.t_11cb5e87977f') }}</dt><dd>{{ context.dueDate || '—' }}</dd></div>
        <div><dt>{{ $t('legacy.t_045859e7926b') }}</dt><dd><span class="proof-status">{{ $t('legacy.t_9441216485cb') }}</span></dd></div>
      </dl>
    </section>

    <section class="proof-upload-layout">
      <form class="proof-form-card" @submit.prevent="submitProof">
        <header><h2>{{ $t('legacy.t_f202b281d042') }}</h2><p><ShieldCheck />{{ $t('legacy.t_ed140cf6490c') }}</p></header>

        <div class="proof-form-grid">
          <label><span>{{ $t('legacy.t_d890d302f7f7') }} <b>*</b></span><div class="proof-money-input"><i>{{ currencyPrefix }}</i><input v-model.trim="form.amount" inputmode="decimal" /></div></label>
          <label><span>{{ $t('legacy.t_058f511c98cf') }} <b>*</b></span><div class="proof-input-icon"><input v-model="form.paymentDate" type="date" /><CalendarDays /></div></label>
          <label><span>{{ $t('legacy.t_c6b9a8cfdb21') }} <b>*</b></span><select v-model="form.paymentMethod"><option value="bank_transfer">{{ $t('legacy.t_c48cae56d799') }}</option><option value="fpx">{{ $t('legacy.t_19bc4fdbb38e') }}</option><option value="card">{{ $t('legacy.t_0be83ef13bec') }}</option><option value="cash">{{ $t('legacy.t_6548450b8d16') }}</option></select></label>
          <label><span>{{ $t('legacy.t_b0628057d28d') }} <b>*</b></span><select v-model="form.bankName"><option value="">{{ $t('legacy.t_daaaa0d41385') }}</option><option>{{ $t('legacy.t_34d716ee1ed7') }}</option><option>{{ $t('legacy.t_33fa893dc4af') }}</option><option>{{ $t('legacy.t_79f8d7b45f3f') }}</option><option>{{ $t('legacy.t_db4ff83ba2f7') }}</option><option>{{ $t('legacy.t_56e57611f697') }}</option><option>{{ $t('legacy.t_86f029c17f29') }}</option></select></label>
          <label><span>{{ $t('legacy.t_e7a506c6af62') }} <b>*</b></span><input v-model.trim="form.reference" :placeholder="$t('legacy.t_06bffbcbdfe8')" /></label>
          <label><span>{{ $t('legacy.t_608ae116f54d') }} <b>*</b></span><input v-model.trim="form.payerName" :placeholder="$t('legacy.t_462768fa6386')" /></label>
          <label class="proof-note"><span>{{ $t('legacy.t_e0361480e3a5') }} <em>{{ $t('legacy.t_dedbe4a2c3c7') }}</em></span><textarea v-model="form.note" maxlength="200" :placeholder="$t('legacy.t_4adfc47a5805')"></textarea><small>{{ form.note.length }}{{ $t('legacy.t_76c4513dfc66') }}</small></label>
        </div>

        <section class="proof-file-section">
          <div class="proof-file-title"><strong>{{ $t('legacy.t_f202b281d042') }} <b>*</b></strong><span>{{ $t('legacy.t_a7f2d190ba95') }}</span></div>
          <input ref="fileInput" class="proof-file-input" type="file" accept="image/jpeg,image/png,application/pdf" multiple @change="selectFiles" />
          <button type="button" class="proof-dropzone" @click="$refs.fileInput.click()" @dragover.prevent @drop.prevent="dropFiles">
            <UploadCloud /><strong>{{ $t('legacy.t_7ba2a542f09c') }}</strong><span>{{ $t('legacy.t_e1ed6779275b') }}</span>
          </button>
          <div v-if="files.length" class="proof-file-grid">
            <article v-for="(file, index) in files" :key="file.key">
              <img v-if="file.previewUrl" :src="file.previewUrl" :alt="file.name" />
              <div v-else class="proof-pdf"><FileText /><b>{{ $t('legacy.t_d613d88cb2d8') }}</b></div>
              <button type="button" :title="$t('legacy.t_8b93574e20c8')" @click="removeFile(index)"><X /></button>
              <strong>{{ file.name }}</strong><span>{{ file.sizeText }}</span>
            </article>
          </div>
        </section>

        <p v-if="formError" class="proof-form-error" role="alert">{{ formError }}</p>
        <div class="proof-form-actions">
          <button type="submit" class="proof-submit" :disabled="submitting || submitted"><Send />{{ submitting ? $t('legacy.t_17e519c5a6bd') : submitted ? $t('legacy.t_612157899b62') : $t('legacy.t_c87e1a999cb1') }}<span>→</span></button>
          <button type="button" class="proof-draft" @click="saveDraft"><Save />{{ $t('legacy.t_4cd30ef91e0b') }}</button>
          <button type="button" class="proof-cancel" @click="$emit('back')">{{ $t('legacy.t_4d0b4688c787') }}</button>
        </div>
      </form>

      <aside class="proof-side-column">
        <section class="proof-review-card">
          <h2>{{ $t('legacy.t_2bcd4ecdd9e1') }}</h2>
          <div class="proof-review-steps">
            <div class="active"><i>{{ $t('legacy.t_356a192b7913') }}</i><strong>{{ $t('legacy.t_612157899b62') }}</strong><span>{{ $t('legacy.t_941cb01a9790') }}</span></div>
            <div><i>{{ $t('legacy.t_da4b9237bacc') }}</i><strong>{{ $t('legacy.t_fe7b041177cc') }}</strong><span>{{ $t('legacy.t_fe58c849a9c4') }}</span></div>
            <div><i>{{ $t('legacy.t_77de68daecd8') }}</i><strong>{{ $t('legacy.t_95106ffd0ba4') }}</strong><span>{{ $t('legacy.t_33246f6a5e5b') }}</span></div>
          </div>
          <p><Info />{{ $t('legacy.t_86058f59261b') }}</p>
        </section>

        <section class="proof-history-card">
          <header><h2>{{ $t('legacy.t_6845ca967bca') }}</h2><button>{{ $t('legacy.t_ed2172fd7894') }}</button></header>
          <article v-if="submitted" class="proof-history-item">
            <i></i><time>{{ submittedAt }}</time><div><strong>{{ $t('legacy.t_4a11f1fba60e') }}</strong><span>{{ $t('legacy.t_6105960978a7') }}{{ form.payerName }}</span><span>{{ $t('legacy.t_cff276c0ab1d') }}</span></div><b>{{ $t('legacy.t_612157899b62') }}</b>
          </article>
          <div v-else class="proof-history-empty"><FileText /><span>{{ $t('legacy.t_3587a657e394') }}</span></div>
        </section>
      </aside>
    </section>
  </main>
</template>

<script>
import {
  ArrowLeft, CalendarDays, ChevronRight, FileText, Info, MapPin, Save,
  Send, ShieldCheck, UploadCloud, X
} from '@lucide/vue';
import { submitPaymentProof } from '../services/propertyApi';

const fileSize = size => size >= 1048576 ? `${(size / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(size / 1024))} KB`;
const todayText = () => {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
};

export default {
  components: { ArrowLeft, CalendarDays, ChevronRight, FileText, Info, MapPin, Save, Send, ShieldCheck, UploadCloud, X },
  inject: ['page'],
  emits: ['back'],
  props: { context: { type: Object, required: true } },
  data() {
    return {
      form: {
        amount: Number(this.context.amount || 0).toFixed(2),
        paymentDate: todayText(),
        paymentMethod: 'bank_transfer',
        bankName: '',
        reference: '',
        payerName: this.context.ownerName === '—' ? '' : this.context.ownerName,
        note: ''
      },
      files: [],
      formError: '',
      submitting: false,
      submitted: false,
      submittedAt: '',
      receiptNo: ''
    };
  },
  computed: {
    installmentTitle() { return this.context.installmentNo ? `第 ${this.context.installmentNo} 期` : '—'; },
    currencyPrefix() { return this.context.currency === 'MYR' ? 'RM' : this.context.currency || 'RM'; }
  },
  beforeUnmount() { this.files.forEach(file => file.previewUrl && URL.revokeObjectURL(file.previewUrl)); },
  methods: {
    money(value) { return `${this.currencyPrefix} ${Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; },
    selectFiles(event) { this.acceptFiles(event.target.files); event.target.value = ''; },
    dropFiles(event) { this.acceptFiles(event.dataTransfer.files); },
    acceptFiles(fileList) {
      this.formError = '';
      const acceptedTypes = ['image/jpeg', 'image/png', 'application/pdf'];
      for (const file of Array.from(fileList)) {
        if (this.files.length >= 4) { this.formError = '最多只能上传 4 个文件'; break; }
        if (!acceptedTypes.includes(file.type)) { this.formError = `${file.name} 的文件格式不支持`; continue; }
        if (file.size > 10 * 1024 * 1024) { this.formError = `${file.name} 超过 10MB`; continue; }
        const duplicate = this.files.some(item => item.name === file.name && item.size === file.size);
        if (duplicate) continue;
        this.files.push({
          key: `${file.name}-${file.size}-${file.lastModified}`,
          raw: file,
          name: file.name,
          size: file.size,
          sizeText: fileSize(file.size),
          previewUrl: file.type.startsWith('image/') ? URL.createObjectURL(file) : ''
        });
      }
    },
    removeFile(index) {
      const [file] = this.files.splice(index, 1);
      if (file?.previewUrl) URL.revokeObjectURL(file.previewUrl);
    },
    validate() {
      if (!this.context.installmentId) return '此房产没有可上传凭证的付款分期';
      if (!Number(this.form.amount) || Number(this.form.amount) <= 0) return '请输入正确的付款金额';
      if (!this.form.paymentDate || !this.form.paymentMethod || !this.form.bankName || !this.form.reference || !this.form.payerName) return '请填写所有必填项目';
      if (!this.files.length) return '请至少上传一份付款凭证';
      return '';
    },
    async submitProof() {
      this.formError = this.validate();
      if (this.formError) return;
      this.submitting = true;
      try {
        const result = await submitPaymentProof(this.context.ownerUnitId, {
          installmentId: this.context.installmentId,
          amount: this.form.amount,
          paymentDate: this.form.paymentDate,
          paymentMethod: this.form.paymentMethod,
          bankName: this.form.bankName,
          reference: this.form.reference,
          payerName: this.form.payerName,
          note: this.form.note
        }, this.files.map(file => file.raw));
        this.submitted = true;
        this.receiptNo = result.receiptNo;
        this.submittedAt = new Date(result.submittedAt).toLocaleString('zh-CN', { hour12: false });
        this.page.showToast(`付款凭证已提交：${result.receiptNo}`);
      } catch (error) {
        this.formError = error.message || '付款凭证提交失败，请稍后再试';
      } finally {
        this.submitting = false;
      }
    },
    saveDraft() { this.page.showToast('草稿已暂存在当前页面'); }
  }
};
</script>

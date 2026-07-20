<template>
  <main class="proof-upload-page">
    <nav class="proof-breadcrumb" aria-label="页面路径">
      <button @click="$emit('back')"><ArrowLeft />返回</button><span>房款进度</span><ChevronRight /><strong>上传付款凭证</strong>
    </nav>

    <section class="proof-property-summary">
      <div class="proof-property-photo"><span>CCPS</span></div>
      <div class="proof-project"><h2>{{ context.projectName }}</h2><p><MapPin />{{ context.unitNo }}</p></div>
      <dl>
        <div><dt>建案名称</dt><dd>{{ context.projectName }}</dd></div>
        <div><dt>单位编号</dt><dd>{{ context.unitNo }}</dd></div>
        <div><dt>当前期数/阶段</dt><dd>{{ installmentTitle }}<small>{{ context.milestone }}</small></dd></div>
        <div><dt>应缴金额</dt><dd class="gold">{{ money(context.amount) }}</dd></div>
        <div><dt>到期日</dt><dd>{{ context.dueDate || '—' }}</dd></div>
        <div><dt>当前状态</dt><dd><span class="proof-status">待上传凭证</span></dd></div>
      </dl>
    </section>

    <section class="proof-upload-layout">
      <form class="proof-form-card" @submit.prevent="submitProof">
        <header><h2>上传付款凭证</h2><p><ShieldCheck />您的资料将被安全加密并仅用于财务审核。</p></header>

        <div class="proof-form-grid">
          <label><span>付款金额 <b>*</b></span><div class="proof-money-input"><i>{{ currencyPrefix }}</i><input v-model.trim="form.amount" inputmode="decimal" /></div></label>
          <label><span>付款日期 <b>*</b></span><div class="proof-input-icon"><input v-model="form.paymentDate" type="date" /><CalendarDays /></div></label>
          <label><span>付款方式 <b>*</b></span><select v-model="form.paymentMethod"><option value="bank_transfer">银行转账 (Bank Transfer)</option><option value="fpx">FPX</option><option value="card">银行卡</option><option value="cash">现金</option></select></label>
          <label><span>银行名称 <b>*</b></span><select v-model="form.bankName"><option value="">请选择银行</option><option>Maybank Berhad</option><option>CIMB Bank</option><option>Public Bank</option><option>RHB Bank</option><option>Hong Leong Bank</option><option>其他银行</option></select></label>
          <label><span>交易参考号 / 转账备注 <b>*</b></span><input v-model.trim="form.reference" placeholder="请输入银行交易参考号" /></label>
          <label><span>付款人姓名 <b>*</b></span><input v-model.trim="form.payerName" placeholder="请输入付款人姓名" /></label>
          <label class="proof-note"><span>备注 <em>（选填）</em></span><textarea v-model="form.note" maxlength="200" placeholder="请输入备注信息..."></textarea><small>{{ form.note.length }}/200</small></label>
        </div>

        <section class="proof-file-section">
          <div class="proof-file-title"><strong>上传付款凭证 <b>*</b></strong><span>支持 JPG、PNG、PDF，单个文件不超过 10MB，最多 4 个文件</span></div>
          <input ref="fileInput" class="proof-file-input" type="file" accept="image/jpeg,image/png,application/pdf" multiple @change="selectFiles" />
          <button type="button" class="proof-dropzone" @click="$refs.fileInput.click()" @dragover.prevent @drop.prevent="dropFiles">
            <UploadCloud /><strong>拖拽文件到此处，或点击上传</strong><span>请确保凭证清晰可见，包含银行盖章或交易成功信息</span>
          </button>
          <div v-if="files.length" class="proof-file-grid">
            <article v-for="(file, index) in files" :key="file.key">
              <img v-if="file.previewUrl" :src="file.previewUrl" :alt="file.name" />
              <div v-else class="proof-pdf"><FileText /><b>PDF</b></div>
              <button type="button" title="移除文件" @click="removeFile(index)"><X /></button>
              <strong>{{ file.name }}</strong><span>{{ file.sizeText }}</span>
            </article>
          </div>
        </section>

        <p v-if="formError" class="proof-form-error" role="alert">{{ formError }}</p>
        <div class="proof-form-actions">
          <button type="submit" class="proof-submit" :disabled="submitting || submitted"><Send />{{ submitting ? '提交中…' : submitted ? '已提交' : '提交凭证' }}<span>→</span></button>
          <button type="button" class="proof-draft" @click="saveDraft"><Save />保存草稿</button>
          <button type="button" class="proof-cancel" @click="$emit('back')">取消</button>
        </div>
      </form>

      <aside class="proof-side-column">
        <section class="proof-review-card">
          <h2>审核流程</h2>
          <div class="proof-review-steps">
            <div class="active"><i>1</i><strong>已提交</strong><span>等待财务接收</span></div>
            <div><i>2</i><strong>待财务确认</strong><span>审核中</span></div>
            <div><i>3</i><strong>已确认 / 退回补件</strong><span>完成</span></div>
          </div>
          <p><Info />提交后，财务团队将在 1–3 个工作日内审核您的凭证。如需补件，我们将通过通知中心与您联系。</p>
        </section>

        <section class="proof-history-card">
          <header><h2>提交记录</h2><button>查看全部</button></header>
          <article v-if="submitted" class="proof-history-item">
            <i></i><time>{{ submittedAt }}</time><div><strong>凭证已提交</strong><span>提交人：{{ form.payerName }}</span><span>提交凭证，等待财务接收</span></div><b>已提交</b>
          </article>
          <div v-else class="proof-history-empty"><FileText /><span>目前没有提交记录</span></div>
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

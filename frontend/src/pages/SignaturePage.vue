<template>
  <main class="signature-page">
    <section class="signature-card">
      <header><span class="brand">{{ $t('legacy.t_c6a41f9e64a4') }}</span><h1>{{ $t('legacy.t_0851436388a2') }}</h1><p>{{ $t('legacy.t_34b5c28ecc83') }}</p></header>
      <div v-if="loading" class="state">{{ $t('legacy.t_146ed793e945') }}</div>
      <div v-else-if="error" class="state error">{{ error }}</div>
      <template v-else-if="signature">
        <div class="contract-summary"><div><b>{{ signature.documentName }}</b><small>{{ $t('legacy.t_41d87c805f0b') }}{{ signature.signerName }} · {{ signature.signerEmailMasked }}</small></div><span :class="signature.canSign ? 'pending' : 'closed'">{{ statusLabel(signature.status) }}</span></div>
        <iframe class="document-preview" :src="documentUrl" :title="$t('legacy.t_148bcc204250')"></iframe>
        <a class="plain-link" :href="documentUrl" target="_blank" rel="noopener">{{ $t('legacy.t_f98d29666a90') }}</a>
        <section v-if="signature.canSign" class="sign-form">
          <label>{{ $t('legacy.t_a6c87526ef43') }}<input :value="signature.signerName" readonly aria-readonly="true"></label>
          <label>{{ $t('legacy.t_6e45895e5b53') }}<div class="code-row"><input v-model.trim="form.verificationCode" inputmode="numeric" maxlength="6" :placeholder="$t('legacy.t_da6babae7e20')"><button type="button" :disabled="resending" @click="resend">{{ resending ? $t('legacy.t_45c36a4dc86c') : $t('legacy.t_1f85ee70c9df') }}</button></div></label>
          <label>{{ $t('legacy.t_568929af7d59') }} <svg :key="signatureRenderKey" ref="signaturePad" class="signature-canvas" viewBox="0 0 560 150" :preserveAspectRatio="signatureSurface.preserveAspectRatio" role="img" :aria-label="$t('legacy.t_deed2713a928')" @pointerdown="startDraw" @pointermove="draw" @pointerup="stopDraw" @pointerleave="stopDraw" @pointercancel="stopDraw"><path ref="signatureInk" :d="signaturePath" fill="none" stroke="#133e70" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <button type="button" class="clear" @click="clear">{{ $t('legacy.t_b2c81aaf0562') }}</button>
            <small>{{ $t('legacy.t_c9dce2de8f5a') }}</small>
          </label>
          <label class="consent"><input v-model="form.consent" type="checkbox"> {{ $t('legacy.t_4cba83ddd315') }}</label>
          <p v-if="formError" class="error">{{ formError }}</p>
          <button class="sign-button" :disabled="submitting" @click="submit">{{ submitting ? $t('legacy.t_19f7885d21e8') : $t('legacy.t_0884dceb2b45') }}</button>
        </section>
        <section v-else class="completed"><p v-if="signature.status === 'signed'">{{ $t('legacy.t_e483a2dd1780') }} {{ formatDate(signature.signedAt) }} {{ $t('legacy.t_57a066da05f3') }}</p><p v-else>{{ $t('legacy.t_b8e3036bb3ef') }}</p><a v-if="signature.canDownloadSigned" class="sign-button download" :href="signedDocumentUrl">{{ $t('legacy.t_6834699b8ba3') }}</a></section>
      </template>
    </section>
  </main>
</template>

<script>
import { API_BASE_URL, fetchPublicSignature, resendPublicSignatureCode, signPublicSignature } from '../services/propertyApi';
import { appendSignaturePoint, nextSignatureRenderKey, signatureSurface, snapshotSignature, startSignatureStroke } from '../utils/signatureCanvas';
export default {
  name: 'SignaturePage',
  data() { return { token: '', signature: null, loading: true, error: '', resending: false, submitting: false, formError: '', drawing: false, drew: false, signatureDataUrl: '', signaturePath: '', signatureCanvas: null, signatureRenderKey: 0, signatureSurface, form: { verificationCode: '', consent: false } }; },
  computed: { documentUrl() { return this.signature?.status === 'signed' ? this.signedDocumentUrl : `${API_BASE_URL}/public/signatures/${encodeURIComponent(this.token)}/document`; }, signedDocumentUrl() { return `${API_BASE_URL}/public/signatures/${encodeURIComponent(this.token)}/signed-document`; } },
  async mounted() { this.token = decodeURIComponent(window.location.pathname.split('/').filter(Boolean).at(-1) || ''); await this.load(); this.signatureCanvas = document.createElement('canvas'); this.signatureCanvas.width = 560; this.signatureCanvas.height = 150; },
  methods: {
    async load() { this.loading = true; this.error = ''; try { this.signature = await fetchPublicSignature(this.token); } catch (error) { this.error = error.message || '無法載入簽署連結'; } finally { this.loading = false; } },
    canvasPoint(event) { const pad = this.$refs.signaturePad; const rect = pad.getBoundingClientRect(); return { x: (event.clientX - rect.left) * 560 / rect.width, y: (event.clientY - rect.top) * 150 / rect.height }; },
    refreshVisiblePath() { this.$refs.signatureInk?.setAttribute('d', this.signaturePath); },
    startDraw(event) { const pad = this.$refs.signaturePad; if (!pad || !this.signatureCanvas) return; pad.setPointerCapture?.(event.pointerId); const point = this.canvasPoint(event); const context = this.signatureCanvas.getContext('2d'); context.beginPath(); context.moveTo(point.x, point.y); context.lineWidth = 2.2; context.lineCap = 'round'; context.lineJoin = 'round'; context.strokeStyle = '#133e70'; this.signaturePath = startSignatureStroke(this.signaturePath, point); this.refreshVisiblePath(); this.drawing = true; this.drew = true; },
    draw(event) { if (!this.drawing || !this.signatureCanvas) return; const point = this.canvasPoint(event); this.signaturePath = appendSignaturePoint(this.signaturePath, point); this.refreshVisiblePath(); const context = this.signatureCanvas.getContext('2d'); context.lineTo(point.x, point.y); context.stroke(); },
    stopDraw() { if (!this.drawing) return; this.drawing = false; this.signatureDataUrl = snapshotSignature(this.signatureCanvas); this.drew = Boolean(this.signatureDataUrl); this.signatureRenderKey = nextSignatureRenderKey(this.signatureRenderKey); },
    clear() { if (this.signatureCanvas) this.signatureCanvas.getContext('2d').clearRect(0, 0, this.signatureCanvas.width, this.signatureCanvas.height); this.signatureDataUrl = ''; this.signaturePath = ''; this.signatureRenderKey = nextSignatureRenderKey(this.signatureRenderKey); this.drew = false; },
    async resend() { this.resending = true; this.formError = ''; try { await resendPublicSignatureCode(this.token); this.formError = '新的驗證碼已寄出，請查看電郵。'; } catch (error) { this.formError = error.message || '驗證碼寄送失敗'; } finally { this.resending = false; } },
    async submit() { this.formError = ''; const signatureDataUrl = this.signatureDataUrl || snapshotSignature(this.signatureCanvas); if (!this.drew || !signatureDataUrl) { this.formError = '請先寫下親筆簽名。'; return; } if (!this.form.consent) { this.formError = '請勾選同意後再簽署。'; return; } this.submitting = true; try { this.signature = await signPublicSignature(this.token, { ...this.form, signerName: this.signature.signerName, signatureDataUrl }); } catch (error) { this.formError = error.message || '簽署失敗'; } finally { this.submitting = false; } },
    statusLabel(value) { return ({ pending: '待簽署', signed: '已簽署', expired: '已到期', cancelled: '已取消', rejected: '已拒絕' })[value] || value; },
    formatDate(value) { return value ? String(value).replace('T', ' ') : '—'; }
  }
};
</script>

<style scoped>
.signature-page{min-height:100vh;background:linear-gradient(135deg,#eef6fa,#f6f8fb);padding:40px 16px;color:#163755}.signature-card{width:min(860px,100%);margin:auto;background:#fff;border:1px solid #d7e2ec;border-radius:16px;box-shadow:0 20px 55px #12345a16;overflow:hidden}.signature-card header{padding:28px 32px 18px;border-bottom:1px solid #e6edf3}.brand{display:inline-flex;padding:5px 9px;border-radius:6px;background:#073f67;color:#f7bf00;font-weight:800;letter-spacing:.5px}.signature-card h1{margin:12px 0 6px;font-size:24px}.signature-card p{color:#60758a;margin:0}.state,.sign-form,.completed,.contract-summary,.plain-link{margin:22px 32px}.error{color:#c53030}.contract-summary{display:flex;justify-content:space-between;gap:12px;align-items:center}.contract-summary small{display:block;margin-top:6px;color:#718096}.pending,.closed{padding:5px 10px;border-radius:99px;font-size:13px}.pending{background:#fff3cd;color:#9a6700}.closed{background:#edf2f7;color:#4b6175}.document-preview{display:block;width:calc(100% - 64px);height:520px;margin:0 32px;border:1px solid #d9e4ec;border-radius:8px}.plain-link{display:inline-block;color:#0d5a96;font-size:14px}.sign-form{display:grid;gap:15px;padding-top:20px;border-top:1px solid #e6edf3}.sign-form label{display:grid;gap:7px;font-weight:700}.sign-form input{border:1px solid #bcd0df;border-radius:7px;padding:10px;font:inherit}.code-row{display:flex;gap:8px}.code-row input{flex:1}.code-row button,.clear{border:1px solid #c4d5e2;border-radius:7px;background:#fff;color:#16466e;padding:8px 12px}.signature-canvas{display:block;width:100%;height:150px;border:1px dashed #8fb1c8;border-radius:8px;background:#fbfdff;touch-action:none;cursor:crosshair;contain:paint}.signature-preview{border:1px solid #9bc5a9;border-radius:8px;background:#f7fff8;padding:10px}.signature-preview img{display:block;max-width:100%;height:150px;object-fit:contain;background:#fff}.sign-form small{font-weight:400;color:#718096}.clear{justify-self:start;font-size:13px}.consent{display:flex!important;grid-template-columns:auto 1fr;align-items:start;font-size:14px;font-weight:500!important}.consent input{margin-top:2px}.sign-button{border:0;border-radius:8px;background:#d89500;color:#fff;font-weight:700;padding:12px 18px;font-size:15px;cursor:pointer}.sign-button:disabled{opacity:.6;cursor:not-allowed}.completed{padding:22px;border-top:1px solid #e6edf3}.download{display:inline-block;text-decoration:none;margin-top:16px}@media(max-width:620px){.signature-page{padding:0}.signature-card{border-radius:0;border:0}.signature-card header,.sign-form,.completed,.contract-summary,.plain-link{margin-left:18px;margin-right:18px}.document-preview{width:calc(100% - 36px);height:410px;margin:0 18px}}
</style>

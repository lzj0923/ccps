<template>
  <main class="signature-page">
    <div v-if="loading" class="viewer-state">{{ $t('legacy.t_146ed793e945') }}</div>
    <div v-else-if="error" class="viewer-state error" role="alert">{{ $lt(error) }}</div>
    <section v-else-if="signature" class="contract-viewer">
      <header class="viewer-toolbar">
        <div class="viewer-identity"><span class="brand">{{ $t('legacy.t_c6a41f9e64a4') }}</span><div><h1>{{ signature.documentName }}</h1><small>{{ $t('legacy.t_41d87c805f0b') }}{{ signature.signerName }}</small></div></div>
        <nav class="viewer-actions" :aria-label="$t('legacy.t_05dbd15b1ada')">
          <span :class="signature.canSign ? 'pending' : 'closed'">{{ $lt(statusLabel(signature.status)) }}</span>
          <a :href="previewDocumentUrl" target="_blank" rel="noopener">{{ $t('legacy.t_f98d29666a90') }}</a>
        </nav>
      </header>
      <SignaturePdfPreview :src="previewDocumentUrl" :initial-page="previewPage" />
    </section>

    <div v-if="signature" class="viewer-fabs" :aria-label="$t('legacy.t_d1aaf856b200')">
      <a class="file-save-fab" :href="downloadDocumentUrl" :download="signature.documentName" :aria-label="$t('rentalFiles.saveCurrentFile')" :title="$t('rentalFiles.saveCurrentFile')">
        <Download aria-hidden="true" :size="24" :stroke-width="2.2" />
        <span>{{ $t('rentalFiles.saveCurrentFile') }}</span>
      </a>
      <button v-if="signature.canSign" class="signature-fab" type="button" :aria-label="$t('rentalFiles.signCurrentFile')" :title="$t('rentalFiles.signCurrentFile')" :data-state="submitting ? 'loading' : formError ? 'error' : 'default'" @click="openSigning">
        <PenLine aria-hidden="true" :size="25" :stroke-width="2.2" />
        <span>{{ $t('rentalFiles.signCurrentFile') }}</span>
      </button>
    </div>

    <dialog v-if="signature?.canSign" ref="signDialog" class="sign-dialog" aria-labelledby="sign-dialog-title" @click="handleDialogBackdrop" @close="formError = ''">
      <header>
        <div><h2 id="sign-dialog-title">{{ $t('legacy.t_704497f32e22') }}</h2><p>{{ $t('legacy.t_e6e985943436') }}</p></div>
        <button class="dialog-close" type="button" :aria-label="$t('legacy.t_db29407f68c5')" @click="closeSigning"><X aria-hidden="true" :size="22" /></button>
      </header>
      <section class="sign-form">
        <div class="signer-name"><span>{{ $t('legacy.t_a6c87526ef43') }}</span><strong>{{ signature.signerName }}</strong></div>
        <label v-if="isWitness">{{ $t('legacy.t_05e6cd728d0e') }}<input v-model.trim="form.identityNo" maxlength="120" autocomplete="off" :placeholder="$t('legacy.t_dcbc65cfd8e8')" :aria-invalid="Boolean(formError && !form.identityNo)"><small>{{ $t('legacy.t_83b37c7ee2ed') }}</small></label>
        <label class="signature-field"><span>{{ $t('legacy.t_568929af7d59') }}</span><svg :key="signatureRenderKey" ref="signaturePad" class="signature-canvas" viewBox="0 0 560 150" :preserveAspectRatio="signatureSurface.preserveAspectRatio" role="img" :aria-label="$t('legacy.t_deed2713a928')" @pointerdown="startDraw" @pointermove="draw" @pointerup="stopDraw" @pointerleave="stopDraw" @pointercancel="stopDraw"><path ref="signatureInk" :d="signaturePath" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          <div class="signature-help"><small>{{ $t('legacy.t_c9dce2de8f5a') }}</small><button type="button" class="clear" @click="clear">{{ $t('legacy.t_b2c81aaf0562') }}</button></div>
        </label>
        <label class="consent"><input v-model="form.consent" type="checkbox"> <span>{{ $t('legacy.t_4cba83ddd315') }}</span></label>
        <p v-if="formError" class="error" role="alert">{{ $lt(formError) }}</p>
        <footer><button class="sign-button" :disabled="submitting" :data-state="submitting ? 'loading' : formError ? 'error' : drew && form.consent ? 'success' : 'default'" @click="submit">{{ submitting ? $t('legacy.t_19f7885d21e8') : $t('legacy.t_0884dceb2b45') }}</button></footer>
      </section>
    </dialog>
  </main>
</template>

<script>
import { API_BASE_URL, fetchPublicSignature, signPublicSignature } from '../services/propertyApi';
import { appendSignaturePoint, nextSignatureRenderKey, signatureSurface, snapshotSignature, startSignatureStroke } from '../utils/signatureCanvas';
import { formatDateTime } from '../utils/dateFormat';
import { pdfSignatureAnchor, signatureTargetPage } from '../utils/signatureTarget';
import { Download, PenLine, X } from '@lucide/vue';
import SignaturePdfPreview from '../components/SignaturePdfPreview.vue';
export default {
  name: 'SignaturePage',
  components: { Download, PenLine, X, SignaturePdfPreview },
  data() { return { token: '', signature: null, loading: true, error: '', submitting: false, formError: '', drawing: false, drew: false, signatureDataUrl: '', signaturePath: '', signatureCanvas: null, signatureRenderKey: 0, documentRevision: 0, previewPage: 1, signatureDistance: 0, lastSignaturePoint: null, signatureSurface, form: { identityNo: '', consent: false } }; },
  computed: {
    isWitness() { const role = this.signature?.signerRole; return (this.signature?.documentKind === 'property_management_agreement_draft' && role === 'customer_service') || (this.signature?.documentKind === 'rental_appointment_draft' && role === 'witness') || (this.signature?.documentKind === 'lease_contract' && ['owner_witness', 'tenant_witness'].includes(role)); },
    signaturePage() { return signatureTargetPage(this.signature); },
    currentDocumentBaseUrl() { const name = this.signature?.canDownloadSigned ? 'signed-document' : 'document'; return `${API_BASE_URL}/public/signatures/${encodeURIComponent(this.token)}/${name}`; },
    previewDocumentUrl() { return `${this.currentDocumentBaseUrl}?download=false&v=${this.documentRevision}${pdfSignatureAnchor(this.previewPage)}`; },
    downloadDocumentUrl() { return `${this.currentDocumentBaseUrl}?download=true`; }
  },
  async mounted() { this.token = decodeURIComponent(window.location.pathname.split('/').filter(Boolean).at(-1) || ''); await this.load(); this.signatureCanvas = document.createElement('canvas'); this.signatureCanvas.width = 560; this.signatureCanvas.height = 150; },
  methods: {
    async load() { this.loading = true; this.error = ''; try { this.signature = await fetchPublicSignature(this.token); } catch (error) { this.error = error.message || this.$lt('無法載入簽署連結'); } finally { this.loading = false; } },
    canvasPoint(event) { const pad = this.$refs.signaturePad; const rect = pad.getBoundingClientRect(); return { x: (event.clientX - rect.left) * 560 / rect.width, y: (event.clientY - rect.top) * 150 / rect.height }; },
    refreshVisiblePath() { this.$refs.signatureInk?.setAttribute('d', this.signaturePath); },
    openSigning() { this.formError = ''; this.$nextTick(() => this.$refs.signDialog?.showModal()); },
    closeSigning() { this.$refs.signDialog?.close(); },
    handleDialogBackdrop(event) { if (event.target === event.currentTarget) this.closeSigning(); },
    startDraw(event) { const pad = this.$refs.signaturePad; if (!pad || !this.signatureCanvas) return; pad.setPointerCapture?.(event.pointerId); const point = this.canvasPoint(event); const context = this.signatureCanvas.getContext('2d'); context.beginPath(); context.moveTo(point.x, point.y); context.lineWidth = 2.2; context.lineCap = 'round'; context.lineJoin = 'round'; context.strokeStyle = getComputedStyle(pad).color; this.signaturePath = startSignatureStroke(this.signaturePath, point); this.refreshVisiblePath(); this.lastSignaturePoint = point; this.drawing = true; },
    draw(event) { if (!this.drawing || !this.signatureCanvas) return; const point = this.canvasPoint(event); if (this.lastSignaturePoint) this.signatureDistance += Math.hypot(point.x - this.lastSignaturePoint.x, point.y - this.lastSignaturePoint.y); this.lastSignaturePoint = point; this.signaturePath = appendSignaturePoint(this.signaturePath, point); this.refreshVisiblePath(); const context = this.signatureCanvas.getContext('2d'); context.lineTo(point.x, point.y); context.stroke(); },
    stopDraw() { if (!this.drawing) return; this.drawing = false; this.lastSignaturePoint = null; this.signatureDataUrl = snapshotSignature(this.signatureCanvas); this.drew = this.signatureDistance >= 20 && Boolean(this.signatureDataUrl); this.signatureRenderKey = nextSignatureRenderKey(this.signatureRenderKey); },
    clear() { if (this.signatureCanvas) this.signatureCanvas.getContext('2d').clearRect(0, 0, this.signatureCanvas.width, this.signatureCanvas.height); this.signatureDataUrl = ''; this.signaturePath = ''; this.signatureDistance = 0; this.lastSignaturePoint = null; this.signatureRenderKey = nextSignatureRenderKey(this.signatureRenderKey); this.drew = false; },
    async submit() { this.formError = ''; const signatureDataUrl = this.signatureDataUrl || snapshotSignature(this.signatureCanvas); if (this.isWitness && !this.form.identityNo) { this.formError = this.$lt('尚未填寫見證人證件號碼，請填寫後再簽署。'); return; } if (!this.drew || !signatureDataUrl) { this.formError = this.$lt('尚未寫下親筆簽名，請在簽名區完成簽名。'); return; } if (!this.form.consent) { this.formError = this.$lt('尚未確認簽署同意，請勾選同意後再簽署。'); return; } this.submitting = true; try { const updated = await signPublicSignature(this.token, { ...this.form, signerName: this.signature.signerName, signatureDataUrl }); this.closeSigning(); this.signature = updated; this.previewPage = signatureTargetPage(updated); this.documentRevision = Date.now(); } catch (error) { this.formError = error.message || this.$lt('合同未能完成簽署，請檢查資料後重試。'); } finally { this.submitting = false; } },
    statusLabel(value) { return this.$lt(({ pending: '待簽署', signed: '已簽署', expired: '已到期', cancelled: '已取消', rejected: '已拒絕' })[value] || value); },
    formatDate(value) { return formatDateTime(value); }
  }
};
</script>

<style scoped>
/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V4 */
/* Hallmark · genre: modern-minimal · macrostructure: Workbench · theme: CCPS · enrichment: contract document · nav: none · footer: none · contrast: pass (40–41) · slop: pass (42–49) · mobile: pass (34, 49–57) */
:global(html),:global(body){overflow-x:clip}.signature-page{--color-paper:oklch(97% .008 215);--color-surface:oklch(99% .004 215);--color-surface-soft:oklch(95% .012 215);--color-ink:oklch(28% .06 238);--color-muted:oklch(48% .035 230);--color-rule:oklch(86% .018 215);--color-accent:oklch(69% .15 79);--color-accent-strong:oklch(58% .14 72);--color-accent-ink:oklch(22% .035 70);--color-focus:oklch(50% .13 225);--color-error:oklch(52% .18 25);--color-error-soft:oklch(96% .025 25);--color-success:oklch(48% .13 154);--color-pending:oklch(55% .12 75);--color-pending-soft:oklch(95% .04 82);--color-overlay:oklch(20% .035 230/.56);--shadow-float:0 12px 30px oklch(22% .035 230/.18);--font-display:"Microsoft JhengHei","PingFang TC",sans-serif;--font-body:"Segoe UI","PingFang TC","Microsoft JhengHei",sans-serif;--space-2xs:.25rem;--space-xs:.5rem;--space-sm:.75rem;--space-md:1rem;--space-lg:1.5rem;--space-xl:2.5rem;--text-xs:.8rem;--text-sm:1rem;--text-md:1.25rem;--radius-sm:.5rem;--radius-md:.75rem;--radius-round:50%;--dur-micro:120ms;--dur-short:220ms;--dur-long:300ms;--ease-out:cubic-bezier(.16,1,.3,1);--ease-in:cubic-bezier(.7,0,.84,0);--signature-ink:var(--color-ink);box-sizing:border-box;min-height:100dvh;background:var(--color-paper);color:var(--color-ink);font-family:var(--font-body);font-size:var(--text-sm);overflow-x:clip}.viewer-state{display:grid;min-height:100dvh;place-items:center;padding:var(--space-lg);color:var(--color-muted)}.viewer-state.error{color:var(--color-error)}.contract-viewer{display:grid;grid-template-rows:auto minmax(0,1fr);height:100dvh;min-height:0}.viewer-toolbar{display:flex;align-items:center;justify-content:space-between;gap:var(--space-md);min-width:0;padding:var(--space-sm) var(--space-md);border-bottom:1px solid var(--color-rule);background:var(--color-surface)}.viewer-identity{display:flex;align-items:center;min-width:0;gap:var(--space-sm)}.viewer-identity>div{display:grid;min-width:0;gap:var(--space-2xs)}.brand{display:grid;flex:0 0 auto;place-items:center;width:2.75rem;height:2.75rem;border-radius:var(--radius-round);background:var(--color-ink);color:var(--color-accent);font-family:var(--font-display);font-weight:800}.viewer-toolbar h1{overflow:hidden;margin:0;color:var(--color-ink);font-family:var(--font-display);font-size:var(--text-sm);font-style:normal;font-weight:700;letter-spacing:-.02em;text-overflow:ellipsis;white-space:nowrap}.viewer-toolbar small{overflow:hidden;color:var(--color-muted);font-size:var(--text-xs);text-overflow:ellipsis;white-space:nowrap}.viewer-actions{display:flex;align-items:center;flex:0 0 auto;gap:var(--space-xs)}.viewer-actions>a,.viewer-actions>span{display:inline-flex;align-items:center;min-height:2.75rem;padding-inline:var(--space-sm);border:1px solid var(--color-rule);border-radius:var(--radius-sm);background:var(--color-surface);color:var(--color-ink);font-size:var(--text-xs);font-weight:700;text-decoration:none;white-space:nowrap}.viewer-actions .pending{border-color:var(--color-accent);background:var(--color-pending-soft);color:var(--color-pending)}.viewer-actions .closed{background:var(--color-surface-soft);color:var(--color-muted)}.viewer-actions .download-link{border-color:var(--color-ink);background:var(--color-ink);color:var(--color-surface)}.document-preview{display:block;box-sizing:border-box;width:100%;height:100%;min-height:0;border:0;background:var(--color-surface)}.signature-fab{position:fixed;z-index:200;right:max(var(--space-lg),env(safe-area-inset-right));bottom:max(var(--space-lg),env(safe-area-inset-bottom));display:grid;place-items:center;width:4rem;height:4rem;border:1px solid var(--color-accent-strong);border-radius:var(--radius-round);outline:3px solid transparent;outline-offset:3px;background:var(--color-accent);color:var(--color-accent-ink);box-shadow:var(--shadow-float);cursor:pointer;transition:transform var(--dur-micro) var(--ease-out),background-color var(--dur-short) var(--ease-out)}.signature-fab span{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%)}.signature-fab:focus-visible,.viewer-actions a:focus-visible,.dialog-close:focus-visible,.clear:focus-visible,.sign-button:focus-visible,.sign-form input:focus-visible{outline:3px solid var(--color-focus);outline-offset:3px}.signature-fab:active{transform:translateY(1px) scale(.98)}.signature-fab:disabled,.signature-fab[data-state="loading"]{opacity:.55;cursor:not-allowed}.signature-fab[data-state="error"]{border-color:var(--color-error)}.signature-fab[data-state="success"]{border-color:var(--color-success)}.sign-dialog{position:fixed;inset:0;box-sizing:border-box;width:min(38rem,calc(100% - 2rem));max-height:min(88dvh,46rem);margin:auto;padding:0;border:1px solid var(--color-rule);border-radius:var(--radius-md);background:var(--color-surface);color:var(--color-ink);box-shadow:var(--shadow-float);font-family:var(--font-body);overflow:hidden}.sign-dialog::backdrop{background:var(--color-overlay)}.sign-dialog[open]{animation:dialog-in var(--dur-long) var(--ease-out)}.sign-dialog>header{display:flex;align-items:flex-start;justify-content:space-between;gap:var(--space-md);padding:var(--space-lg);border-bottom:1px solid var(--color-rule)}.sign-dialog h2{margin:0;color:var(--color-ink);font-family:var(--font-display);font-size:var(--text-md);font-style:normal;letter-spacing:-.02em}.sign-dialog header p{margin:var(--space-2xs) 0 0;color:var(--color-muted);font-size:var(--text-xs)}.dialog-close{display:grid;flex:0 0 auto;place-items:center;width:2.75rem;height:2.75rem;border:1px solid var(--color-rule);border-radius:var(--radius-sm);background:var(--color-surface);color:var(--color-ink);cursor:pointer}.sign-form{display:grid;gap:var(--space-md);max-height:calc(88dvh - 6.5rem);padding:var(--space-lg);overflow-y:auto}.signer-name{display:grid;gap:var(--space-2xs);padding-block-end:var(--space-md);border-bottom:1px solid var(--color-rule)}.signer-name span,.sign-form label>span{color:var(--color-muted);font-size:var(--text-xs);font-weight:700}.signer-name strong{font-family:var(--font-display);font-size:var(--text-md)}.sign-form label{display:grid;gap:var(--space-xs);font-weight:700}.sign-form input:not([type="checkbox"]){box-sizing:border-box;width:100%;height:2.75rem;border:1px solid var(--color-rule);border-radius:var(--radius-sm);outline:3px solid transparent;outline-offset:1px;background:var(--color-surface);padding-inline:var(--space-sm);color:var(--color-ink);font:inherit}.sign-form input[aria-invalid="true"]{border-color:var(--color-error);background:var(--color-error-soft)}.sign-form small{color:var(--color-muted);font-size:var(--text-xs);font-weight:400;line-height:1.5}.signature-field{color:var(--signature-ink)}.signature-canvas{display:block;box-sizing:border-box;width:100%;height:9.5rem;border:1px dashed var(--color-muted);border-radius:var(--radius-sm);background:var(--color-surface-soft);touch-action:none;cursor:crosshair;contain:paint}.signature-help{display:flex;align-items:center;justify-content:space-between;gap:var(--space-sm)}.clear{min-height:2.75rem;border:1px solid var(--color-rule);border-radius:var(--radius-sm);background:var(--color-surface);padding-inline:var(--space-sm);color:var(--color-ink);font:inherit;font-weight:700;white-space:nowrap;cursor:pointer}.consent{display:grid!important;grid-template-columns:auto minmax(0,1fr);align-items:start;gap:var(--space-xs);font-weight:400!important}.consent input{width:1.1rem;height:1.1rem;margin:.125rem 0 0;accent-color:var(--color-ink)}.sign-form .error{margin:0;padding:var(--space-sm);border:1px solid var(--color-error);border-radius:var(--radius-sm);background:var(--color-error-soft);color:var(--color-error);font-size:var(--text-xs);line-height:1.5}.sign-form footer{display:flex;justify-content:flex-end;padding-block-start:var(--space-xs)}.sign-button{min-width:9rem;min-height:2.75rem;border:1px solid var(--color-accent-strong);border-radius:var(--radius-sm);outline:3px solid transparent;outline-offset:3px;background:var(--color-accent);padding-inline:var(--space-lg);color:var(--color-accent-ink);font:inherit;font-weight:800;white-space:nowrap;cursor:pointer;transition:transform var(--dur-micro) var(--ease-out),background-color var(--dur-short) var(--ease-out)}.sign-button:active{transform:translateY(1px)}.sign-button:disabled,.sign-button[data-state="loading"]{opacity:.55;cursor:not-allowed}.sign-button[data-state="error"]{border-color:var(--color-error)}.sign-button[data-state="success"]{border-color:var(--color-success)}@keyframes dialog-in{from{opacity:0;transform:scale(.97)}to{opacity:1;transform:none}}@media(hover:hover) and (pointer:fine){.signature-fab:hover,.sign-button:hover{background:var(--color-accent-strong)}.viewer-actions a:hover,.dialog-close:hover,.clear:hover{background:var(--color-surface-soft)}}@media(min-width:40rem){.viewer-toolbar{padding-inline:var(--space-lg)}.sign-dialog{width:min(40rem,calc(100% - 3rem))}}@media(max-width:39.99rem){.viewer-toolbar{align-items:flex-start}.viewer-identity{max-width:calc(100% - 3.5rem)}.viewer-actions>a{display:none}.viewer-actions>span{min-height:2.25rem;padding-inline:var(--space-xs)}.document-preview{min-height:calc(100dvh - 4.75rem)}.signature-fab{right:max(var(--space-md),env(safe-area-inset-right));bottom:max(var(--space-md),env(safe-area-inset-bottom));width:3.5rem;height:3.5rem}.sign-dialog{width:100%;max-height:92dvh;margin:auto 0 0;border-width:1px 0 0;border-radius:var(--radius-md) var(--radius-md) 0 0}.sign-form{max-height:calc(92dvh - 6.5rem);padding:var(--space-md)}.sign-dialog>header{padding:var(--space-md)}.signature-help{align-items:flex-start;flex-direction:column}.clear{align-self:flex-end}}@media(prefers-reduced-motion:reduce){.sign-dialog[open]{animation:dialog-in-reduced 120ms linear}.signature-fab,.sign-button{transition-duration:120ms}@keyframes dialog-in-reduced{from{opacity:0}to{opacity:1}}}
  .viewer-fabs{
    position:fixed;
    z-index:200;
    right:max(var(--space-lg),env(safe-area-inset-right));
    bottom:max(var(--space-lg),env(safe-area-inset-bottom));
    display:flex;
    flex-direction:column;
    gap:var(--space-sm);
  }
  .viewer-fabs .signature-fab,.file-save-fab{
    position:static;
    display:grid;
    box-sizing:border-box;
    place-items:center;
    width:4rem;
    height:4rem;
    border-radius:var(--radius-round);
    outline:3px solid transparent;
    outline-offset:3px;
    box-shadow:var(--shadow-float);
    cursor:pointer;
    text-decoration:none;
    transition:transform var(--dur-micro) var(--ease-out),background-color var(--dur-short) var(--ease-out);
  }
  .file-save-fab{border:1px solid var(--color-ink);background:var(--color-ink);color:var(--color-surface)}
  .file-save-fab span{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%)}
  .file-save-fab:focus-visible{outline:3px solid var(--color-focus);outline-offset:3px}
  .file-save-fab:active{transform:translateY(1px) scale(.98)}
  @media(hover:hover) and (pointer:fine){.file-save-fab:hover{background:var(--color-muted)}}
  @media(max-width:39.99rem){
    .viewer-fabs{right:max(var(--space-md),env(safe-area-inset-right));bottom:max(var(--space-md),env(safe-area-inset-bottom))}
    .viewer-fabs .signature-fab,.file-save-fab{width:3.5rem;height:3.5rem}
  }
  @media(prefers-reduced-motion:reduce){.file-save-fab{transition-duration:120ms}}
  .sign-dialog{max-width:none}
</style>

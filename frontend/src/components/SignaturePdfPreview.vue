<template>
  <section class="pdf-preview" :aria-busy="loading">
    <nav class="pdf-controls" :aria-label="$t('rentalFiles.previewControls')">
      <button type="button" :disabled="loading || pageNumber <= 1" @click="changePage(-1)">{{ $t('rentalFiles.previewPrevious') }}</button>
      <span role="status">{{ pageNumber }} / {{ pageCount || '—' }}</span>
      <button type="button" :disabled="loading || pageNumber >= pageCount" @click="changePage(1)">{{ $t('rentalFiles.previewNext') }}</button>
      <button type="button" :disabled="loading || zoom <= 1" :aria-label="$t('rentalFiles.previewZoomOut')" @click="changeZoom(-0.5)">−</button>
      <button type="button" :disabled="loading || zoom >= 3" :aria-label="$t('rentalFiles.previewZoomIn')" @click="changeZoom(0.5)">+</button>
    </nav>
    <p v-if="loading" class="pdf-message" role="status">{{ $t('rentalFiles.previewLoading') }}</p>
    <div v-if="error" class="pdf-message" role="alert"><p>{{ $t('rentalFiles.previewFailed') }}</p><button type="button" @click="loadDocument">{{ $t('rentalFiles.previewRetry') }}</button></div>
    <div ref="scroll" class="pdf-scroll">
      <canvas v-if="!error" ref="canvas" :data-rendered="rendered" :aria-label="$t('rentalFiles.previewPage') + ' ' + pageNumber" />
    </div>
  </section>
</template>
<script>
import { markRaw } from 'vue';
import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist';
import pdfWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?url';
GlobalWorkerOptions.workerSrc = pdfWorker;
export default {
  name: 'SignaturePdfPreview',
  props: { src: { type: String, required: true }, initialPage: { type: Number, default: 1 } },
  data() { return { loading: true, error: false, rendered: false, pageNumber: 1, pageCount: 0, zoom: 1, documentTask: null, pdf: null, renderTask: null, revision: 0, paintRevision: 0, resizeObserver: null, resizeTimer: null, previewWidth: 0 }; },
  watch: { src() { this.loadDocument(); }, initialPage(value) { this.pageNumber = Math.max(1, Math.min(this.pageCount || 1, value)); this.renderPage(); } },
  mounted() {
    this.loadDocument();
    if (typeof ResizeObserver !== 'undefined') {
      this.resizeObserver = markRaw(new ResizeObserver(entries => { const width = entries[0]?.contentRect.width; if (!width || width === this.previewWidth) return; this.previewWidth = width; clearTimeout(this.resizeTimer); this.resizeTimer = setTimeout(() => this.renderPage(), 150); }));
      this.resizeObserver.observe(this.$refs.scroll);
    }
  },
  beforeUnmount() { this.revision++; this.paintRevision++; clearTimeout(this.resizeTimer); this.resizeObserver?.disconnect(); this.renderTask?.cancel(); this.documentTask?.destroy().catch(() => {}); },
  methods: {
    async loadDocument() {
      const revision = ++this.revision;
      this.paintRevision++; this.renderTask?.cancel();
      const previous = this.documentTask;
      this.documentTask = null; this.pdf = null; this.loading = true; this.error = false; this.rendered = false; this.pageCount = 0; this.zoom = 1;
      try {
        if (previous) await previous.destroy();
        if (revision !== this.revision) return;
        // Fetch and render locally; never navigate the browser to a PDF plugin or third-party viewer.
        const task = markRaw(getDocument({ url: this.src.split('#')[0], isEvalSupported: false }));
        this.documentTask = task;
        const pdf = await task.promise;
        if (revision !== this.revision) return;
        this.pdf = markRaw(pdf); this.pageCount = pdf.numPages;
        this.pageNumber = Math.max(1, Math.min(pdf.numPages, this.initialPage));
        await this.$nextTick(); await this.renderPage();
      } catch (_) { if (revision === this.revision) { this.error = true; this.loading = false; } }
    },
    async renderPage() {
      if (!this.pdf || !this.$refs.canvas) return;
      const revision = this.revision, paint = ++this.paintRevision;
      const previous = this.renderTask; previous?.cancel();
      this.loading = true; this.error = false; this.rendered = false;
      try {
        if (previous) await previous.promise.catch(() => {});
        const page = await this.pdf.getPage(this.pageNumber);
        if (revision !== this.revision || paint !== this.paintRevision) return;
        const base = page.getViewport({ scale: 1 });
        const width = Math.max(240, (this.$refs.scroll.clientWidth || 375) - 24);
        const scale = width / base.width * this.zoom;
        const viewport = page.getViewport({ scale });
        const ratio = Math.min(window.devicePixelRatio || 1, 2, 2400 / viewport.width, 3200 / viewport.height);
        const canvas = this.$refs.canvas;
        canvas.width = Math.ceil(viewport.width * ratio); canvas.height = Math.ceil(viewport.height * ratio);
        canvas.style.width = `${viewport.width}px`; canvas.style.height = `${viewport.height}px`;
        const task = markRaw(page.render({ canvasContext: canvas.getContext('2d'), viewport, transform: [ratio,0,0,ratio,0,0], background: '#ffffff' }));
        this.renderTask = task; await task.promise;
        if (revision === this.revision && paint === this.paintRevision) this.rendered = true;
      } catch (error) { if (revision === this.revision && paint === this.paintRevision && error.name !== 'RenderingCancelledException') this.error = true; }
      finally { if (revision === this.revision && paint === this.paintRevision) this.loading = false; }
    },
    changePage(delta) { this.pageNumber = Math.max(1, Math.min(this.pageCount, this.pageNumber + delta)); this.$refs.scroll.scrollTop = 0; this.renderPage(); },
    changeZoom(delta) { this.zoom = Math.max(1, Math.min(3, this.zoom + delta)); this.renderPage(); }
  }
};
</script>
<style scoped>
.pdf-preview{display:flex;flex-direction:column;min-height:0;min-width:0;height:100%;background:var(--color-paper,#f4f7f8);color:var(--color-ink,#083344)}
.pdf-controls{display:flex;flex-wrap:wrap;align-items:center;justify-content:center;gap:8px;padding:8px;background:var(--color-surface,#fff);border-bottom:1px solid var(--color-rule,#cbd5e1)}
.pdf-controls button,.pdf-message button{min-width:44px;min-height:44px;padding:8px;border:1px solid var(--color-rule,#94a3b8);border-radius:6px;background:var(--color-surface,#fff);color:inherit;font:inherit;cursor:pointer}
.pdf-controls button:disabled{opacity:.45;cursor:not-allowed}.pdf-preview button:focus-visible{outline:3px solid var(--color-focus,#0369a1);outline-offset:2px}
.pdf-scroll{flex:1;min-height:0;min-width:0;overflow:auto;padding:12px 12px 160px;box-sizing:border-box;overscroll-behavior:contain}
.pdf-scroll canvas{display:block;margin:0 auto;background:white;box-shadow:0 1px 6px #0002;max-width:none}
.pdf-message{margin:0;padding:12px;text-align:center}.pdf-message[role="alert"]{color:var(--color-error,#b91c1c)}
</style>

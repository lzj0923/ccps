<template>
  <dialog ref="dialog" class="owner-document-preview" @pointerdown.self="close" @close="onClosed">
    <article>
      <header>
        <div>
          <small>{{ $t('legacy.t_7621c3b8b170') }}</small>
          <h2>{{ file?.name || $t('legacy.t_31ba335515ca') }}</h2>
        </div>
        <button type="button" :aria-label="$t('legacy.t_50506c6b8939')" @click="close"><X :size="22" /></button>
      </header>

      <section class="owner-document-preview__body">
        <div v-if="loading" class="owner-document-preview__state"><LoaderCircle class="spinning" :size="26" /><strong>{{ $t('legacy.t_2b35ef3278ff') }}</strong><span>{{ $t('legacy.t_660efd81385a') }}</span></div>
        <div v-else-if="error" class="owner-document-preview__state is-error"><FileWarning :size="28" /><strong>{{ $t('legacy.t_d5f79812c1d1') }}</strong><span>{{ $lt(error) }}</span></div>
        <img v-else-if="kind === 'image' && objectUrl" :src="objectUrl" :alt="file?.name || $t('legacy.t_fc60a19bc4c1')">
        <div v-else-if="kind === 'pdf'" class="owner-document-preview__pdf"><canvas ref="pdfCanvas"></canvas></div>
        <div v-else class="owner-document-preview__state"><FileText :size="28" /><strong>{{ $t('legacy.t_c97bdd467a69') }}</strong><span>{{ $t('legacy.t_413203e5ebc6') }}</span></div>
      </section>

      <footer>
        <nav v-if="kind === 'pdf' && pages > 1" :aria-label="$t('legacy.t_56f8830a9b8f')">
          <button type="button" :disabled="page <= 1 || rendering" @click="changePage(-1)"><ChevronLeft :size="18" />{{ $t('legacy.t_b41561d80765') }}</button>
          <span>{{ page }} / {{ pages }}</span>
          <button type="button" :disabled="page >= pages || rendering" @click="changePage(1)">{{ $t('legacy.t_67a246a344ae') }}<ChevronRight :size="18" /></button>
        </nav>
        <button class="owner-document-preview__download" type="button" :disabled="!blob" @click="download"><Download :size="18" />{{ $t('legacy.t_2922ce3bc82e') }}</button>
        <slot name="actions" :ready="Boolean(blob && !loading && !error && !rendering)" />
      </footer>
    </article>
  </dialog>
</template>

<script>
import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist';
import pdfWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?url';
import { ChevronLeft, ChevronRight, Download, FileText, FileWarning, LoaderCircle, X } from '@lucide/vue';
import { markRaw } from 'vue';
import { fetchOwnerDocumentFile } from '../../services/propertyApi';

GlobalWorkerOptions.workerSrc = pdfWorker;

export default {
  components: { ChevronLeft, ChevronRight, Download, FileText, FileWarning, LoaderCircle, X },
  props: { loadFile: { type: Function, default: null } },
  emits: ['toast', 'ready', 'closed'],
  data: () => ({ file: null, blob: null, objectUrl: '', kind: '', loading: false, error: '', pdf: null, page: 1, pages: 0, rendering: false, renderTask: null, loadRevision: 0 }),
  beforeUnmount() { this.reset(); },
  methods: {
    async open(file) {
      if (!file?.id) return;
      this.reset();
      const revision = this.loadRevision;
      this.file = file;
      this.loading = true;
      this.$refs.dialog?.showModal();
      try {
        const result = this.loadFile ? await this.loadFile(file) : await fetchOwnerDocumentFile(file.id, false, file.source || 'document');
        if (revision !== this.loadRevision) return;
        this.blob = result.blob;
        const mimeType = String(result.blob?.type || file.mimeType || '').toLowerCase();
        const name = String(file.name || '').toLowerCase();
        if (mimeType.startsWith('image/')) {
          this.kind = 'image';
          this.objectUrl = URL.createObjectURL(result.blob);
        } else if (mimeType === 'application/pdf' || name.endsWith('.pdf')) {
          this.kind = 'pdf';
          const bytes = new Uint8Array(await result.blob.arrayBuffer());
          if (revision !== this.loadRevision) return;
          const pdf = await getDocument({
            data: bytes,
            cMapUrl: new URL('/pdfjs/cmaps/', window.location.origin).href,
            cMapPacked: true,
            standardFontDataUrl: new URL('/pdfjs/standard_fonts/', window.location.origin).href,
            useSystemFonts: true
          }).promise;
          if (revision !== this.loadRevision) { pdf.destroy(); return; }
          this.pdf = markRaw(pdf);
          this.pages = this.pdf.numPages;
          this.page = 1;
          this.loading = false;
          await this.$nextTick();
          await this.renderPage();
          if (revision === this.loadRevision) this.$emit('ready', file.id);
        } else {
          this.kind = 'other';
        }
      } catch (error) {
        if (revision === this.loadRevision) this.error = error.message || '文件读取失败，请稍后重试。';
      } finally {
        if (revision === this.loadRevision) this.loading = false;
      }
    },
    async renderPage() {
      if (!this.pdf || !this.$refs.pdfCanvas) return;
      const revision = this.loadRevision;
      this.rendering = true;
      try {
        const pdfPage = await this.pdf.getPage(this.page);
        if (revision !== this.loadRevision || !this.$refs.pdfCanvas) return;
        const baseViewport = pdfPage.getViewport({ scale: 1 });
        const availableWidth = Math.max(280, Math.min(window.innerWidth - 32, 760));
        const viewport = pdfPage.getViewport({ scale: Math.min(2, availableWidth / baseViewport.width) });
        const canvas = this.$refs.pdfCanvas;
        const ratio = Math.min(window.devicePixelRatio || 1, 2);
        canvas.width = Math.floor(viewport.width * ratio);
        canvas.height = Math.floor(viewport.height * ratio);
        canvas.style.width = `${Math.floor(viewport.width)}px`;
        canvas.style.height = `${Math.floor(viewport.height)}px`;
        const context = canvas.getContext('2d');
        this.renderTask = markRaw(pdfPage.render({ canvasContext: context, viewport, transform: ratio === 1 ? null : [ratio, 0, 0, ratio, 0, 0] }));
        await this.renderTask.promise;
      } finally {
        if (revision === this.loadRevision) { this.renderTask = null; this.rendering = false; }
      }
    },
    async changePage(offset) {
      const next = Math.min(this.pages, Math.max(1, this.page + offset));
      if (next === this.page) return;
      this.page = next;
      await this.renderPage();
      this.$refs.pdfCanvas?.scrollIntoView({ block: 'start', behavior: 'smooth' });
    },
    download() {
      if (!this.blob) return;
      const url = URL.createObjectURL(this.blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = this.file?.name || 'CCPS-业主文件';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      this.$emit('toast', this.$lt('文件下载已开始'));
    },
    close() { this.$refs.dialog?.close(); this.$emit('closed'); },
    onClosed() { if (!this.$refs.dialog?.open) this.reset(); },
    reset() {
      this.loadRevision++;
      this.renderTask?.cancel?.();
      this.renderTask = null;
      this.pdf?.destroy?.();
      this.pdf = null;
      if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = '';
      this.file = null;
      this.blob = null;
      this.kind = '';
      this.loading = false;
      this.error = '';
      this.page = 1;
      this.pages = 0;
      this.rendering = false;
    }
  }
};
</script>

<style scoped>
.owner-document-preview{box-sizing:border-box;width:min(calc(100vw - 1rem),52rem);min-width:0;max-width:calc(100vw - 1rem);height:min(92dvh,58rem);max-height:none;margin:auto;border:0;border-radius:1.25rem;background:#f7f4ed;padding:0;color:#172238;box-shadow:0 1.5rem 4rem rgb(10 30 50 / .28)}
.owner-document-preview::backdrop{background:rgb(8 26 40 / .66);backdrop-filter:blur(.2rem)}
.owner-document-preview>article{display:grid;width:100%;min-width:0;height:100%;grid-template-columns:minmax(0,1fr);grid-template-rows:auto minmax(0,1fr) auto;overflow:hidden}
.owner-document-preview header{display:flex;width:100%;min-width:0;max-width:100%;box-sizing:border-box;align-items:center;justify-content:space-between;gap:1rem;overflow:hidden;border-bottom:1px solid #d8d8d1;background:#fff;padding:1rem 1.1rem}
.owner-document-preview header div{min-width:0;flex:1}.owner-document-preview header small{color:#67707d;font-size:.75rem;font-weight:700}.owner-document-preview h2{max-width:100%;margin:.2rem 0 0;overflow:hidden;font-size:1rem;text-overflow:ellipsis;white-space:nowrap}
.owner-document-preview header button{display:grid;width:2.75rem;height:2.75rem;flex:0 0 2.75rem;place-items:center;border:1px solid #d8d8d1;border-radius:.75rem;background:#fff;color:#172238}
.owner-document-preview__body{width:100%;min-width:0;min-height:0;box-sizing:border-box;overflow:auto;padding:1rem}.owner-document-preview__body>img{display:block;max-width:100%;height:auto;margin:auto;border-radius:.75rem;background:#fff;box-shadow:0 .4rem 1.4rem rgb(20 36 50 / .12)}
.owner-document-preview__pdf{display:grid;justify-content:center}.owner-document-preview__pdf canvas{max-width:100%;height:auto!important;background:#fff;box-shadow:0 .4rem 1.4rem rgb(20 36 50 / .14)}
.owner-document-preview__state{display:grid;min-height:16rem;place-items:center;align-content:center;gap:.55rem;text-align:center}.owner-document-preview__state svg{color:#008e94}.owner-document-preview__state span{max-width:24rem;color:#67707d;font-size:.85rem;line-height:1.6}.owner-document-preview__state.is-error svg{color:#b43a3a}
.owner-document-preview footer{display:flex;width:100%;min-width:0;max-width:100%;box-sizing:border-box;align-items:center;justify-content:space-between;gap:.75rem;overflow:hidden;border-top:1px solid #d8d8d1;background:#fff;padding:.8rem 1rem calc(.8rem + env(safe-area-inset-bottom,0px))}.owner-document-preview footer nav{display:flex;min-width:0;align-items:center;gap:.55rem}.owner-document-preview footer button{display:flex;min-height:2.75rem;align-items:center;gap:.3rem;border:1px solid #d8d8d1;border-radius:.75rem;background:#fff;padding:0 .8rem;color:#172238;font-weight:750}.owner-document-preview footer button:disabled{opacity:.45}.owner-document-preview__download{margin-left:auto!important;border-color:#008e94!important;background:#008e94!important;color:#fff!important}
@media(max-width:30rem){.owner-document-preview{width:100vw;max-width:100vw;height:100dvh;border-radius:0}.owner-document-preview footer{align-items:stretch;flex-direction:column}.owner-document-preview footer nav{justify-content:space-between}.owner-document-preview footer nav button{padding:0 .6rem}.owner-document-preview__download{width:100%;justify-content:center}}
</style>

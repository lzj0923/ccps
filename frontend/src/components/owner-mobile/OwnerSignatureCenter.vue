<template>
  <section v-if="showEntry" class="owner-signature-entry">
    <button type="button" @click="open"><PenLine :size="22" aria-hidden="true" /><span>{{ $t('ownerApp.signing.title') }}<small>{{ $t('ownerApp.signing.pendingCount', { n: pending.length }) }}</small></span><ChevronRight :size="20" aria-hidden="true" /></button>
  </section>
  <dialog ref="reminder" class="owner-signature-dialog" aria-labelledby="owner-sign-reminder" @cancel="dismiss" @close="reminderOpen = false">
    <header><PenLine :size="26" aria-hidden="true" /><h2 id="owner-sign-reminder">{{ $t('ownerApp.signing.reminder') }}</h2></header>
    <p>{{ $t('ownerApp.signing.pendingCount', { n: pending.length }) }}</p><p class="document-name">{{ pending[0]?.documentName }}</p>
    <footer><button autofocus type="button" @click="dismiss">{{ $t('ownerApp.signing.later') }}</button><button class="primary" type="button" @click="dismiss(); open()">{{ $t('ownerApp.signing.viewNow') }}</button></footer>
  </dialog>
  <dialog ref="center" class="owner-signature-dialog is-list" aria-labelledby="owner-sign-center" @close="centerOpen = false">
    <header><h2 id="owner-sign-center">{{ $t('ownerApp.signing.title') }}</h2><button type="button" :aria-label="$t('ownerApp.close')" @click="$refs.center.close()"><X :size="20" /></button></header>
    <p v-if="loading" role="status">{{ $t('ownerApp.signing.loading') }}</p><p v-if="error" role="alert">{{ error }}</p>
    <button type="button" :disabled="loading" @click="refresh">{{ $t('ownerApp.refresh') }}</button>
    <p v-if="!loading && !error && !tasks.length">{{ $t('ownerApp.signing.empty') }}</p>
    <article v-for="task in tasks" :key="task.id" class="owner-signature-task">
      <strong>{{ task.documentName }}</strong><span>{{ task.signerName }}</span><small>{{ $t('ownerApp.signing.expires') }} {{ formatDateTime(task.expiresAt) }}</small>
      <div><span>{{ $t('ownerApp.signing.status.' + status(task)) }}</span><button v-if="status(task) === 'pending' || status(task) === 'signed'" type="button" :disabled="opening" @click="view(task)">{{ $t(status(task) === 'signed' ? 'ownerApp.signing.viewSigned' : 'ownerApp.signing.viewSign') }}</button></div>
    </article>
    <p v-if="success" role="status">{{ $t('ownerApp.signing.success') }}</p>
  </dialog>
  <OwnerDocumentPreview ref="preview" :load-file="loadFile" @ready="previewed = $event === selected?.requestId">
    <template #actions="{ ready }"><button v-if="selected?.canSign" class="owner-sign-action" type="button" :disabled="!ready || !previewed" @click="beginSigning">{{ $t('ownerApp.signing.goSign') }}</button></template>
  </OwnerDocumentPreview>
  <dialog ref="sign" class="owner-signature-dialog" aria-labelledby="owner-sign-form" @cancel="cancelSigning">
    <header><h2 id="owner-sign-form">{{ $t('ownerApp.signing.goSign') }}</h2><button type="button" :disabled="submitting" :aria-label="$t('ownerApp.close')" @click="closeSigning"><X :size="20" /></button></header>
    <OwnerSigningForm v-if="selected && signingOpen" :key="selected.requestId + ':' + formRevision" :signature="selected" :busy="submitting" :error="formError" @invalid="formError = $event" @sign="submit" />
  </dialog>
</template>
<script>
import { PenLine, ChevronRight, X } from '@lucide/vue';
import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';
import OwnerDocumentPreview from './OwnerDocumentPreview.vue';
import OwnerSigningForm from './OwnerSigningForm.vue';
import { fetchOwnerSignatures, fetchOwnerSignature, fetchOwnerSignatureFile, signOwnerSignature } from '../../services/propertyApi';
import { pendingOwnerSignatures, unseenOwnerSignatures, ownerSignatureStatus } from '../../utils/ownerSignatures';
import { formatDateTime } from '../../utils/dateFormat';
export default {
  components: { PenLine, ChevronRight, X, OwnerDocumentPreview, OwnerSigningForm },
  props: { showEntry: Boolean, blocked: Boolean },
  data: () => ({ tasks: [], loading: false, error: '', seen: new Set(), reminderOpen: false, centerOpen: false,
    opening: false, selected: null, previewed: false, signingOpen: false, submitting: false, formError: '',
    formRevision: 0, success: false, disposed: false, timer: null, appListener: null, revision: 0 }),
  computed: { pending() { return pendingOwnerSignatures(this.tasks); } },
  mounted() {
    this.refresh(); this.timer = window.setInterval(this.onForeground, 30000);
    document.addEventListener('visibilitychange', this.onForeground); window.addEventListener('focus', this.onForeground);
    if (Capacitor.isNativePlatform()) App.addListener('appStateChange', state => { if (state.isActive) this.onForeground(); })
      .then(listener => { if (this.disposed) listener.remove(); else this.appListener = listener; }).catch(() => {});
  },
  beforeUnmount() {
    this.disposed = true; this.revision++; window.clearInterval(this.timer); this.appListener?.remove();
    document.removeEventListener('visibilitychange', this.onForeground); window.removeEventListener('focus', this.onForeground);
    for (const key of ['reminder','center','sign']) this.$refs[key]?.close(); this.$refs.preview?.close();
  },
  methods: {
    formatDateTime, status: ownerSignatureStatus,
    onForeground() { if (!document.hidden && !this.disposed) this.refresh(); },
    async refresh() {
      if (this.loading || this.disposed || this.submitting) return;
      this.loading = true; const revision = this.revision;
      try {
        const tasks = await fetchOwnerSignatures(); if (this.disposed || revision !== this.revision) return;
        this.tasks = Array.isArray(tasks) ? tasks : []; this.error = '';
        if (!this.pending.length) this.$refs.reminder?.close();
        // Never interrupt a document, form, or another dialog.
        if (!this.blocked && !this.centerOpen && !this.reminderOpen && !document.hidden && !document.querySelector('dialog[open]')
            && unseenOwnerSignatures(this.tasks, this.seen).length) {
          for (const task of this.pending) this.seen.add(String(task.id));
          this.reminderOpen = true; this.$refs.reminder.showModal();
        }
      } catch (e) { if (!this.disposed) this.error = e.message || this.$t('ownerApp.signing.loadFailed'); }
      finally { this.loading = false; }
    },
    dismiss() { this.$refs.reminder.close(); this.reminderOpen = false; },
    open() { this.centerOpen = true; this.$refs.center.showModal(); this.refresh(); },
    async view(task) {
      if (this.opening) return; this.opening = true; this.error = ''; this.success = false; this.previewed = false;
      const revision = ++this.revision;
      try { const selected = await fetchOwnerSignature(task.id); if (this.disposed || revision !== this.revision) return;
        this.selected = selected; await this.$refs.preview.open({ id: task.id, name: selected.documentName, mimeType: 'application/pdf', signed: selected.canDownloadSigned });
      } catch (e) { this.error = e.message; } finally { this.opening = false; }
    },
    loadFile(file) { return fetchOwnerSignatureFile(file.id, file.signed); },
    beginSigning() { if (!this.previewed || !this.selected?.canSign) return; this.$refs.preview.close(); this.formError=''; this.formRevision++; this.signingOpen=true; this.$refs.sign.showModal(); },
    cancelSigning(event) { event.preventDefault(); this.closeSigning(); },
    closeSigning() { if (this.submitting) return; this.$refs.sign.close(); this.signingOpen=false; },
    async submit(payload) {
      if (this.submitting || !this.previewed || !this.selected?.canSign) return;
      this.submitting=true; this.formError='';
      try { const signed = await signOwnerSignature(this.selected.requestId, payload); if (this.disposed) return;
        this.selected=signed; this.success=true; this.$refs.sign.close(); this.signingOpen=false;
        this.tasks=this.tasks.map(task => task.id === signed.requestId ? { ...task, status:signed.status, canSign:false, signedAt:signed.signedAt } : task);
      } catch (e) { this.formError=e.message || this.$t('ownerApp.signing.submitFailed'); }
      finally { this.submitting=false; if (!this.disposed) this.refresh(); }
    }
  }
};
</script>
<style scoped>
.owner-sign-action{min-height:48px;flex:none;padding:.75rem 1rem;border:1px solid var(--color-native-accent,#008e94);border-radius:12px;background:var(--color-native-accent,#008e94);color:#fff;font:inherit;font-weight:600;cursor:pointer}.owner-sign-action:disabled{opacity:.5}.owner-sign-action:focus-visible{outline:2px solid var(--color-native-accent,#008e94);outline-offset:3px}
.owner-signature-entry>button{display:flex;align-items:center;gap:.75rem;width:100%;min-height:64px;padding:1rem;border:1px solid var(--color-native-rule);border-radius:var(--radius-native-card);background:var(--color-native-paper);color:var(--color-native-ink);text-align:start;font:inherit}.owner-signature-entry span{flex:1}.owner-signature-entry small{display:block;margin-top:.25rem;color:var(--color-native-muted)}
.owner-signature-dialog{box-sizing:border-box;width:min(34rem,calc(100% - 2rem));max-width:none;max-height:calc(100dvh - env(safe-area-inset-top,0px) - env(safe-area-inset-bottom,0px) - 3rem);margin:auto;padding:1.25rem;border:1px solid var(--color-native-rule,#ddd);border-radius:18px;background:var(--color-native-paper,#fff);color:var(--color-native-ink,#172238);overflow:auto}.owner-signature-dialog::backdrop{background:rgb(8 26 40 / .6)}.owner-signature-dialog header{display:flex;align-items:center;justify-content:space-between;gap:.75rem;margin-bottom:1rem}.owner-signature-dialog h2{margin:0;font-size:1.2rem}.owner-signature-dialog p{line-height:1.6}.owner-signature-dialog .document-name{overflow-wrap:anywhere}.owner-signature-dialog footer{display:flex;justify-content:end;flex-wrap:wrap;gap:.75rem}.owner-signature-dialog button{min-height:48px;padding:.6rem .9rem;border:1px solid var(--color-native-rule,#ddd);border-radius:10px;background:var(--color-native-paper,#fff);color:inherit;font:inherit;cursor:pointer}.owner-signature-dialog button.primary{background:var(--color-native-accent,#008e94);color:#fff}.owner-signature-dialog button:disabled{opacity:.5}.owner-signature-dialog :focus-visible,.owner-signature-entry :focus-visible{outline:2px solid var(--color-native-accent,#008e94);outline-offset:3px}.owner-signature-task{display:grid;gap:.5rem;padding:1rem 0;border-bottom:1px solid var(--color-native-rule,#ddd);overflow-wrap:anywhere}.owner-signature-task>div{display:flex;align-items:center;justify-content:space-between;gap:.5rem}.owner-signature-task small{color:var(--color-native-muted,#67707d)}.owner-signature-dialog [role=alert]{color:#ad2828}
</style>

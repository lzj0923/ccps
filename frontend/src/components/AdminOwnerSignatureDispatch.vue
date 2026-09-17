<template>
  <div class="owner-signature-dispatch">
    <button v-if="!choosing && !sent" type="button" :disabled="busy" :aria-busy="busy" @click="load">{{ $t(busy ? 'ownerApp.signing.loading' : 'ownerApp.signing.sendApp') }}</button>
    <template v-if="choosing && !sent">
      <label>{{ $t('ownerApp.signing.recipient') }}
        <select v-model="ownerId" :disabled="busy"><option value="">{{ $t('ownerApp.signing.chooseOwner') }}</option><option v-for="owner in owners" :key="owner.ownerId" :value="owner.ownerId">{{ owner.fullName }} · #{{ owner.ownerId }}</option></select>
      </label>
      <button type="button" :disabled="busy || !ownerId" :aria-busy="busy" @click="send">{{ $t(busy ? 'ownerApp.signing.sending' : 'ownerApp.signing.confirmSend') }}</button>
    </template>
    <span v-if="sent" role="status">{{ $t('ownerApp.signing.sent') }}</span>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-else-if="choosing && !busy && !owners.length">{{ $t('ownerApp.signing.noAccount') }}</p>
  </div>
</template>
<script>
import { fetchAdminSignatureOwnerRecipients, dispatchAdminOwnerSignature } from '../services/propertyApi';
export default {
  props: { requestId: { type: Number, required: true } },
  data: () => ({ choosing: false, owners: [], ownerId: '', busy: false, sent: false, error: '' }),
  methods: {
    async load() { this.busy = true; this.error = ''; try { this.owners = await fetchAdminSignatureOwnerRecipients(this.requestId); this.choosing = true; } catch (e) { this.error = e.message; } finally { this.busy = false; } },
    async send() { if (this.busy || !this.ownerId) return; this.busy = true; this.error = ''; try { await dispatchAdminOwnerSignature(this.requestId, this.ownerId); this.sent = true; } catch (e) { this.error = e.message; } finally { this.busy = false; } }
  }
};
</script>
<style scoped>
.owner-signature-dispatch{display:flex;flex-wrap:wrap;align-items:end;gap:12px;min-width:0}
.owner-signature-dispatch label{display:grid;gap:6px;flex:1 1 220px;min-width:0;color:#526b78;font-size:13px;font-weight:600}
.owner-signature-dispatch button,.owner-signature-dispatch select{box-sizing:border-box;min-height:44px;max-width:100%;border:1px solid #08747c;border-radius:8px;padding:10px 16px;font:inherit;font-size:14px;line-height:1.5}
.owner-signature-dispatch button{background:#08747c;color:#fff;font-weight:600;cursor:pointer}
.owner-signature-dispatch select{width:100%;min-width:0;border-color:#c9dce2;background:#fff;color:#264858}
.owner-signature-dispatch button:hover:not(:disabled){background:#05636b}
.owner-signature-dispatch button:disabled{opacity:.55;cursor:not-allowed}
.owner-signature-dispatch p,.owner-signature-dispatch [role="status"]{box-sizing:border-box;flex-basis:100%;margin:0;padding:10px 12px;border-radius:8px;background:#f2f7f8;color:#526b78;font-size:13px;line-height:1.6;overflow-wrap:anywhere}
.owner-signature-dispatch [role="alert"]{border:1px solid #f0cccc;background:#fff5f5;color:#a32f2f}
.owner-signature-dispatch [role="status"]{background:#edf8f1;color:#216442}
.owner-signature-dispatch :focus-visible{outline:3px solid #12375f;outline-offset:3px}
@media(max-width:520px){.owner-signature-dispatch button{width:100%}}
</style>

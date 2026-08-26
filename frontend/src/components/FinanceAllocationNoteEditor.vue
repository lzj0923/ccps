<template>
  <section class="allocation-note-editor">
    <div class="allocation-note-heading">
      <div><strong>收支备注</strong><small>记录付款来源、承担情况或其他说明，不影响账单金额与余额。</small></div>
      <button v-if="!editing" type="button" @click="beginEdit">{{ record?.allocationNote ? '修改备注' : '添加备注' }}</button>
    </div>
    <p v-if="!editing" :class="{ empty: !record?.allocationNote, success: saved }" aria-live="polite">{{ record?.allocationNote || '暂无备注记录' }}</p>
    <form v-else @submit.prevent="save">
      <textarea v-model.trim="note" rows="4" maxlength="500" :aria-invalid="Boolean(error)" placeholder="例如：本次由租户转账；费用已与业主确认；租户已付 RM 80"></textarea>
      <label><input v-model="reuseEnabled" type="checkbox"> 后续同一房产、同一资金类型自动沿用此备注</label>
      <span v-if="error" class="error" role="alert">{{ error }}</span>
      <div><button type="button" @click="editing=false">取消</button><button class="primary" :disabled="saving">{{ saving ? '保存中…' : '保存备注' }}</button></div>
    </form>
  </section>
</template>

<script>
import { updateAdminFinanceAllocationNote } from '../services/propertyApi';
export default {
  props: { record: { type: Object, required: true } },
  emits: ['saved'],
  data() { return { editing: false, note: '', reuseEnabled: true, saving: false, saved: false, error: '' }; },
  methods: {
    beginEdit() { this.note = this.record?.allocationNote || ''; this.reuseEnabled = true; this.saved = false; this.error = ''; this.editing = true; },
    async save() { this.saving = true; this.saved = false; this.error = ''; try { await updateAdminFinanceAllocationNote(this.record.id, { note: this.note || null, reuseEnabled: this.reuseEnabled }); this.record.allocationNote = this.note || null; this.saved = true; this.editing = false; this.$emit('saved', this.record.allocationNote); } catch (error) { this.error = error.message || '收支备注保存失败'; } finally { this.saving = false; } }
  }
};
</script>

<style scoped>
/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V4 */
/* Hallmark · component: finance-note-panel · genre: modern-minimal · theme: existing CCPS
 * states: default · hover · focus · active · disabled · loading · error · success
 * contrast: pass
 */
.allocation-note-editor{
  --note-accent:oklch(52% .105 194);
  --note-accent-strong:oklch(43% .105 194);
  --note-accent-soft:oklch(96.5% .018 194);
  --note-accent-ink:oklch(99.5% .003 195);
  --note-border:oklch(84% .035 196);
  --note-border-soft:oklch(91% .018 205);
  --note-surface:oklch(98.5% .008 195);
  --note-paper:oklch(99.5% .003 195);
  --note-error-soft:oklch(96% .025 25);
  --note-ink:oklch(34% .055 240);
  --note-muted:oklch(53% .035 235);
  --note-error:oklch(54% .19 25);
  --note-space-2xs:4px;
  --note-space-xs:8px;
  --note-space-sm:12px;
  --note-space-md:16px;
  --note-space-lg:20px;
  --note-radius-sm:8px;
  --note-radius-md:12px;
  --note-dur-micro:120ms;
  --note-ease-out:cubic-bezier(.16,1,.3,1);
  margin:var(--note-space-sm);
  padding:var(--note-space-sm);
  border:1px solid var(--note-border);
  border-radius:var(--note-radius-md);
  background:var(--note-surface)
}
.allocation-note-heading{display:grid;grid-template-columns:minmax(0,1fr);align-items:start;gap:var(--note-space-sm)}
.allocation-note-heading>div{display:grid;min-width:0;gap:var(--note-space-2xs)}
.allocation-note-heading strong{color:var(--note-ink);font-size:16px;line-height:1.25}
.allocation-note-heading small{max-width:44em;color:var(--note-muted);font-size:12px;line-height:1.65}
.allocation-note-heading button,.allocation-note-editor form button{min-width:88px;min-height:44px;padding:0 var(--note-space-sm);border:1px solid var(--note-border);border-radius:var(--note-radius-sm);background:var(--note-paper);color:var(--note-accent-strong);font:inherit;font-weight:700;line-height:1;white-space:nowrap;cursor:pointer;transition:transform var(--note-dur-micro) var(--note-ease-out),background-color var(--note-dur-micro) var(--note-ease-out),border-color var(--note-dur-micro) var(--note-ease-out)}
.allocation-note-heading button{justify-self:start}
@media(hover:hover) and (pointer:fine){.allocation-note-heading button:hover,.allocation-note-editor form button:hover{border-color:var(--note-accent);background:var(--note-accent-soft)}}
.allocation-note-heading button:focus-visible,.allocation-note-editor form button:focus-visible,.allocation-note-editor textarea:focus-visible,.allocation-note-editor input:focus-visible{outline:2px solid var(--note-accent);outline-offset:2px}
.allocation-note-heading button:active,.allocation-note-editor form button:active{transform:translateY(1px)}
.allocation-note-heading button:disabled,.allocation-note-editor form button:disabled{cursor:not-allowed;opacity:.58}
.allocation-note-editor>p{display:flex;align-items:center;min-height:44px;margin:var(--note-space-sm) 0 0;padding:var(--note-space-sm) 0 0;border-block-start:1px solid var(--note-border-soft);color:var(--note-ink);font-size:13px;line-height:1.6;white-space:pre-wrap}
.allocation-note-editor>p.empty{color:var(--note-muted)}
.allocation-note-editor>p.empty::before{content:'—';margin-inline-end:var(--note-space-xs);color:var(--note-border);font-weight:800}
.allocation-note-editor>p.success{color:var(--note-ink)}
.allocation-note-editor>p.success::before{content:'✓';margin-inline-end:var(--note-space-xs);color:var(--note-accent-strong);font-weight:800}
.allocation-note-editor form{display:grid;gap:var(--note-space-sm);margin-top:var(--note-space-sm)}
.allocation-note-editor textarea{box-sizing:border-box;width:100%;min-height:96px;padding:var(--note-space-sm);border:1px solid var(--note-border);border-radius:var(--note-radius-sm);background:var(--note-paper);color:var(--note-ink);resize:vertical;font:inherit;line-height:1.55}
.allocation-note-editor label{display:flex;align-items:center;gap:var(--note-space-xs);color:var(--note-muted);font-size:12px;line-height:1.5}
.allocation-note-editor form>div{display:flex;flex-wrap:wrap;justify-content:flex-end;gap:var(--note-space-xs)}
.allocation-note-editor form>div button{flex:1 1 auto}
.allocation-note-editor form button.primary{border-color:var(--note-accent);background:var(--note-accent);color:var(--note-accent-ink)}
@media(hover:hover) and (pointer:fine){.allocation-note-editor form button.primary:hover{border-color:var(--note-accent-strong);background:var(--note-accent-strong)}}
.allocation-note-editor .error{padding:var(--note-space-xs) var(--note-space-sm);border-radius:var(--note-radius-sm);background:var(--note-error-soft);color:var(--note-error);font-size:12px;font-weight:700}
@media(min-width:26.25rem){.allocation-note-editor{margin:var(--note-space-md) var(--note-space-lg);padding:var(--note-space-md)}.allocation-note-heading{grid-template-columns:minmax(0,1fr) auto;gap:var(--note-space-md)}.allocation-note-heading button{justify-self:end}.allocation-note-editor form>div button{flex:0 1 auto}}
@media(prefers-reduced-motion:reduce){.allocation-note-heading button,.allocation-note-editor form button{transition-duration:0ms}}
</style>

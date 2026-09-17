<template>
  <form class="owner-signing-form" @submit.prevent="submit">
    <p>{{ $t('ownerApp.signing.signer') }} <strong>{{ signature.signerName }}</strong></p>
    <label>{{ $t('ownerApp.signing.handwrite') }}
      <svg ref="pad" viewBox="0 0 560 180" preserveAspectRatio="none" role="img" :aria-label="$t('ownerApp.signing.handwrite')"
        @pointerdown="start" @pointermove="draw" @pointerup="stop" @pointercancel="stop" @lostpointercapture="stop">
        <path :d="path" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </label>
    <button type="button" :disabled="busy" @click="clear">{{ $t('ownerApp.signing.clear') }}</button>
    <label class="owner-signing-consent"><input v-model="consent" :disabled="busy" type="checkbox">{{ $t('ownerApp.signing.consent') }}</label>
    <p v-if="error" role="alert">{{ error }}</p>
    <button class="primary" type="submit" :disabled="busy">{{ $t(busy ? 'ownerApp.signing.submitting' : 'ownerApp.signing.submit') }}</button>
  </form>
</template>
<script>
import { appendSignaturePoint, startSignatureStroke, snapshotSignature } from '../../utils/signatureCanvas';
export default {
  props: { signature: { type: Object, required: true }, busy: Boolean, error: String },
  emits: ['sign', 'invalid'],
  data: () => ({ path: '', consent: false, drawing: false, distance: 0, previous: null, canvas: null }),
  mounted() { this.canvas = document.createElement('canvas'); this.canvas.width = 560; this.canvas.height = 180; },
  methods: {
    point(e) { const r = this.$refs.pad.getBoundingClientRect(); return { x: (e.clientX-r.left)*560/r.width, y: (e.clientY-r.top)*180/r.height }; },
    start(e) { if (this.busy || this.drawing || e.isPrimary === false) return; e.preventDefault(); this.$refs.pad.setPointerCapture(e.pointerId); this.previous = this.point(e); this.drawing = true; const c=this.canvas.getContext('2d'); c.beginPath(); c.moveTo(this.previous.x,this.previous.y); c.lineWidth=2.4; c.lineCap='round'; c.lineJoin='round'; c.strokeStyle='#172238'; this.path=startSignatureStroke(this.path,this.previous); },
    draw(e) { if (!this.drawing || this.busy) return; const p=this.point(e); this.distance+=Math.hypot(p.x-this.previous.x,p.y-this.previous.y); this.previous=p; this.path=appendSignaturePoint(this.path,p); const c=this.canvas.getContext('2d'); c.lineTo(p.x,p.y); c.stroke(); },
    stop() { this.drawing=false; },
    clear() { this.canvas?.getContext('2d').clearRect(0,0,560,180); this.path=''; this.distance=0; this.drawing=false; },
    submit() { if (this.busy) return; if (this.distance<20 || !this.consent) { this.$emit('invalid',this.$t('ownerApp.signing.completeForm')); return; } this.$emit('sign',{ signerName:this.signature.signerName, consent:true, signatureDataUrl:snapshotSignature(this.canvas) }); }
  }
};
</script>
<style scoped>
.owner-signing-form{display:grid;gap:1rem}.owner-signing-form p{margin:0}.owner-signing-form label{display:grid;gap:.5rem}.owner-signing-form svg{box-sizing:border-box;width:100%;height:180px;border:1px dashed var(--color-native-muted,#67707d);border-radius:12px;background:#fff;color:#172238;touch-action:none}.owner-signing-form .owner-signing-consent{display:flex;align-items:start;line-height:1.6}.owner-signing-consent input{flex:none;width:22px;height:22px}.owner-signing-form [role=alert]{color:#ad2828}.owner-signing-form button{min-height:48px;border:1px solid var(--color-native-rule,#ddd);border-radius:10px;background:var(--color-native-paper,#fff);color:inherit;font:inherit;padding:.6rem 1rem}.owner-signing-form button.primary{background:var(--color-native-accent,#008e94);color:#fff}.owner-signing-form :focus-visible{outline:2px solid var(--color-native-accent,#008e94);outline-offset:3px}.owner-signing-form button:disabled{opacity:.5}
</style>

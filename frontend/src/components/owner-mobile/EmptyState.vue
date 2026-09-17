<template>
  <section class="owner-app-state owner-empty-state" :class="{ 'is-compact': compact }">
    <div class="owner-empty-art" aria-hidden="true">
      <svg class="owner-empty-scene" viewBox="0 0 160 104" fill="none" focusable="false">
        <ellipse cx="80" cy="55" rx="65" ry="42" class="owner-empty-wash" />
        <path d="M16 88H144M22 77V62L31 52L40 62V77M26 70H36M120 80V63M120 71C105 70 105 55 105 55C120 55 120 71 120 71ZM120 64C135 63 135 48 135 48C120 49 120 64 120 64Z" class="owner-empty-lines" />
        <circle cx="125" cy="24" r="8" class="owner-empty-sun" />
        <path d="M36 24H46M41 19V29M143 42H149M146 39V45" class="owner-empty-lines" />
        <rect x="52" y="22" width="56" height="62" rx="10" class="owner-empty-sheet" />
      </svg>
      <component :is="iconComponent" class="owner-empty-symbol" :size="32" :stroke-width="1.5" aria-hidden="true" />
    </div>
    <strong>{{ title }}</strong><span>{{ text }}</span>
  </section>
</template>
<script>
import { Bell, Building2, ChartNoAxesColumnIncreasing, FileText, History, Image as ImageIcon, KeyRound } from '@lucide/vue';
export default { components: { Bell, Building2, ChartNoAxesColumnIncreasing, FileText, History, ImageIcon, KeyRound }, props: { icon: { type: String, default: 'file' }, title: { type: String, required: true }, text: { type: String, required: true }, compact: Boolean }, computed: { iconComponent() { return { bell: Bell, building: Building2, chart: ChartNoAxesColumnIncreasing, file: FileText, history: History, image: ImageIcon, key: KeyRound }[this.icon] || FileText; } } };
</script>
<style scoped>
.owner-empty-art { position:relative; width:10rem; max-width:100%; aspect-ratio:20 / 13; flex:none; margin:0 auto .25rem; }
.owner-empty-scene { display:block; width:100%; height:100%; }
.owner-empty-wash { fill:var(--color-native-surface); }
.owner-empty-lines { stroke:var(--color-native-accent); stroke-width:1.5; stroke-linecap:round; stroke-linejoin:round; opacity:.55; }
.owner-empty-sun { fill:var(--color-native-brand-yellow); opacity:.65; }
.owner-empty-sheet { fill:var(--color-native-paper); stroke:var(--color-native-rule); stroke-width:1.5; }
.owner-empty-symbol { position:absolute; inset-inline-start:50%; inset-block-start:50%; transform:translate(-50%,-50%); width:2rem; height:2rem; color:var(--color-native-accent); }
.is-compact .owner-empty-art { width:7rem; }
</style>

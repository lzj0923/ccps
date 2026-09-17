<template>
  <div class="owner-brand-masthead">
    <img :src="logo" alt="CCPS 家慶佳業" width="573" height="137" decoding="async">
    <svg class="owner-brand-architecture" viewBox="0 0 180 120" fill="none" aria-hidden="true" focusable="false">
      <path d="M14 120V48L62 20L110 48V120M62 20V120M110 120V68L154 42L178 56V120" />
      <path d="M22 60L54 42M22 78L54 60M22 96L54 78M70 42L102 60M70 60L102 78M70 78L102 96M118 80L146 64M118 98L146 82M154 42V120" />
      <path class="owner-brand-horizon" d="M0 115H180" />
      <circle cx="134" cy="20" r="9" class="owner-brand-sun" />
    </svg>
  </div>
</template>
<script>
import logo from '../../../ccps-logo.png';
export default { data: () => ({ logo }) };
</script>
<style scoped>
.owner-brand-masthead { position:relative; display:flex; align-items:center; min-height:8rem; padding:1.75rem; overflow:hidden; isolation:isolate; background:var(--color-native-ink-strong); box-sizing:border-box; }
.owner-brand-masthead img { position:relative; z-index:1; width:9.5rem; max-width:70%; height:auto; object-fit:contain; }
.owner-brand-architecture { position:absolute; inset-block-end:0; inset-inline-end:-.75rem; width:46%; height:100%; color:var(--color-native-brand); stroke:currentColor; stroke-width:1; opacity:.45; z-index:0; }
.owner-brand-horizon { stroke:var(--color-native-brand-yellow); }
.owner-brand-sun { fill:var(--color-native-brand-yellow); stroke:none; }
</style>

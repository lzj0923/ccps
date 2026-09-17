<template>
  <figure v-if="!failed" class="owner-editorial" :class="{ 'is-living': !isCity && kind !== 'architecture', 'is-architecture': kind === 'architecture' }">
    <img :key="imageUrl" :src="imageUrl" alt="" :width="isCity || kind === 'architecture' ? 960 : 800" :height="isCity ? 420 : 480" loading="eager" decoding="async" @error="failed = true">
    <figcaption>
      <span class="owner-editorial-brand">CCPS <span aria-hidden="true">/</span> {{ $t('ownerApp.imagery.living') }}</span>
      <strong>{{ $t(kind === 'architecture' ? 'ownerApp.imagery.architecture' : isCity ? 'ownerApp.imagery.city' : 'ownerApp.imagery.home') }}</strong>
    </figcaption>
  </figure>
</template>

<script>
import cityImage from '../../assets/owner/kuala-lumpur.webp';
import livingImage from '../../assets/owner/living-room.webp';
import architectureImage from '../../assets/owner/architecture.webp';

// Editorial imagery is deliberately separate from authenticated property photos.
export default {
  props: { country: { type: String, default: 'MY' }, kind: { type: String, default: 'city' } },
  data: () => ({ failed: false }),
  computed: {
    isCity() { return this.kind === 'city' && this.country === 'MY'; },
    imageUrl() { return this.kind === 'architecture' ? architectureImage : this.isCity ? cityImage : livingImage; }
  },
  watch: { imageUrl() { this.failed = false; } }
};
</script>

<style scoped>
.owner-editorial { margin:0; min-width:0; overflow:hidden; border-radius:var(--radius-native-card); background:var(--color-native-paper); border:1px solid var(--color-native-rule); }
.owner-editorial img { display:block; width:100%; height:auto; aspect-ratio:24 / 10; object-fit:cover; }
.owner-editorial figcaption { display:flex; flex-wrap:wrap; align-items:center; gap:.25rem .65rem; padding:.65rem .875rem; color:var(--color-native-ink); }
.owner-editorial-brand { color:var(--color-native-accent); font-size:.6875rem; letter-spacing:.06em; font-weight:600; }
.owner-editorial-brand > span { margin-inline:.25rem; color:var(--color-native-muted); }
.owner-editorial strong { font-size:.875rem; font-weight:600; }
.owner-editorial.is-living { display:grid; grid-template-columns:minmax(0,1fr) 42%; }
.owner-editorial.is-living img { grid-column:2; grid-row:1; height:100%; min-height:7.5rem; aspect-ratio:auto; object-position:60% center; }
.owner-editorial.is-living figcaption { grid-column:1; grid-row:1; display:flex; flex-direction:column; align-items:flex-start; justify-content:center; padding:1rem; gap:.4rem; }
.owner-editorial.is-architecture img { aspect-ratio:3 / 1; object-position:center 45%; }
@media (min-width:40rem) { .owner-editorial:not(.is-living) img { max-height:12rem; } }
</style>

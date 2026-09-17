<template>
  <div class="owner-project-monogram owner-archived-cover" :class="{ 'has-photo': !!url }" :aria-busy="loading">
    <img v-if="url" :src="url" :alt="property.projectName" width="640" height="360" decoding="async" @error="clear">
    <div v-else class="owner-cover-placeholder" :title="$t('ownerApp.coverMissing')">
      <Building2 :size="48" :stroke-width="1.2" aria-hidden="true" />
      <span>{{ loading ? $t('legacy.t_03a17d236ff2') : $t('ownerApp.redesign.photoUnavailable') }}</span>
    </div>
  </div>
</template>
<script>
import { fetchOwnerDocumentFile } from '../../services/propertyApi';
import { Building2 } from '@lucide/vue';
export default {
  components: { Building2 },
  props: { property: { type: Object, required: true } },
  data: () => ({ url: '', sequence: 0, loading: false }),
  watch: { 'property.coverDocumentId': { immediate: true, async handler(id) {
    const sequence = ++this.sequence;
    this.clear();
    this.loading = !!id;
    if (!id) return;
    try {
      const { blob } = await fetchOwnerDocumentFile(id);
      if (sequence === this.sequence && blob.type.startsWith('image/')) this.url = URL.createObjectURL(blob);
    } catch { /* 无源文件时显示明确占位，不使用虚构照片。 */ }
    finally { if (sequence === this.sequence) this.loading = false; }
  } } },
  beforeUnmount() { this.sequence++; this.clear(); },
  methods: { clear() { if (this.url) URL.revokeObjectURL(this.url); this.url = ''; } }
};
</script>
<style scoped>
.owner-archived-cover { overflow: hidden; flex-shrink: 0; }
.owner-archived-cover img { width: 100%; height: 100%; object-fit: cover; }
.owner-cover-placeholder { display:grid; justify-items:center; align-content:center; gap:var(--space-native-xs); width:100%; height:100%; color:var(--color-native-accent); }
.owner-cover-placeholder span { color:var(--color-native-muted); font:400 var(--text-native-caption) var(--font-native-body); }
</style>

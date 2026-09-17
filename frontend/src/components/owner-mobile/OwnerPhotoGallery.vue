<template>
  <section class="owner-photo-gallery">
    <h2>{{ $t('ownerApp.photos') }}</h2>
    <p>{{ $t('ownerApp.photoHint') }}</p>
    <label class="owner-app-field"><span>{{ $t('ownerApp.tenant') }}</span><select v-model="leaseId"><option value="">{{ $t('ownerApp.allLeases') }}</option><option v-for="lease in leases" :key="lease.id" :value="String(lease.id)">{{ lease.tenantName }} · {{ lease.leaseNo || formatDate(lease.startDate) }}</option></select></label>
    <nav class="owner-detail-tabs" :aria-label="$t('ownerApp.photos')"><button v-for="key in ['all', 'before', 'after', 'current']" :key="key" type="button" :aria-pressed="stage === key" @click="stage = key">{{ $t(`ownerApp.${key}`) }}</button></nav>
    <div v-if="visiblePhotos.length" class="owner-photo-thumbnails">
      <article v-for="photo in pagePhotos" :key="photo.id">
        <button type="button" class="owner-photo-open" @click="$emit('open', photo)">
          <img v-if="urls[photo.id]" :src="urls[photo.id]" :alt="photo.name" width="240" height="180" loading="lazy">
          <span v-else class="owner-photo-placeholder"><ImageIcon :size="26" /><small>{{ failures[photo.id] ? $t('ownerApp.photoFailed') : $t('legacy.t_03a17d236ff2') }}</small></span>
          <strong>{{ photo.name }}</strong><small>{{ $t(`ownerApp.${stageGroup(photo.rentalStage)}`) }} · {{ photo.versionMonth ? formatMonth(photo.versionMonth) : formatDate(photo.createdAt) }}</small>
        </button>
        <button v-if="failures[photo.id]" type="button" @click="loadThumbnail(photo, generation)">{{ $t('ownerApp.retry') }}</button>
      </article>
    </div>
    <EmptyState v-else icon="image" :title="$t('ownerApp.noPhotos')" text="" compact />
    <nav v-if="visiblePhotos.length > pageSize" class="owner-detail-toolbar" :aria-label="$t('ownerApp.photos')"><button type="button" :disabled="page === 0" @click="page--">{{ $t('legacy.t_b41561d80765') }}</button><span>{{ page + 1 }} / {{ Math.ceil(visiblePhotos.length / pageSize) }}</span><button type="button" :disabled="(page + 1) * pageSize >= visiblePhotos.length" @click="page++">{{ $t('legacy.t_67a246a344ae') }}</button></nav>
  </section>
</template>

<script>
import { formatDate, formatDateTime, formatMonth } from '../../utils/dateFormat';
import { Image as ImageIcon } from '@lucide/vue';
import { fetchOwnerDocumentFile } from '../../services/propertyApi';
import { filterPropertyPhotos, photoStageGroup } from '../../utils/ownerPortfolio';
import EmptyState from './EmptyState.vue';

export default {
  components: { ImageIcon, EmptyState },
  props: { photos: { type: Array, default: () => [] }, leases: { type: Array, default: () => [] }, selectedLeaseId: { type: [Number, String], default: '' } },
  emits: ['open'],
  data: () => ({ leaseId: '', stage: 'all', page: 0, pageSize: 8, urls: {}, failures: {}, generation: 0 }),
  computed: {
    visiblePhotos() { return filterPropertyPhotos(this.photos, this.leaseId, this.stage); },
    pagePhotos() { return this.visiblePhotos.slice(this.page * this.pageSize, (this.page + 1) * this.pageSize); }
  },
  watch: {
    selectedLeaseId: { immediate: true, handler(value) { this.leaseId = value ? String(value) : ''; } },
    leaseId() { this.page = 0; }, stage() { this.page = 0; }, photos() { this.page = 0; },
    pagePhotos: { immediate: true, async handler(photos) {
      const generation = ++this.generation;
      this.release();
      for (let offset = 0; offset < photos.length; offset += 3) {
        if (generation !== this.generation) return;
        await Promise.all(photos.slice(offset, offset + 3).map(photo => this.loadThumbnail(photo, generation)));
      }
    } }
  },
  beforeUnmount() { this.generation++; this.release(); },
  methods: {
    formatDate, formatDateTime, formatMonth,
    stageGroup: photoStageGroup,
    release() { Object.values(this.urls).forEach(url => URL.revokeObjectURL(url)); this.urls = {}; this.failures = {}; },
    async loadThumbnail(photo, generation) {
      this.failures[photo.id] = false;
      try {
        const { blob } = await fetchOwnerDocumentFile(photo.id, false);
        if (generation !== this.generation) return;
        if (!blob.type.startsWith('image/')) throw new Error('Not an image');
        if (this.urls[photo.id]) URL.revokeObjectURL(this.urls[photo.id]);
        this.urls[photo.id] = URL.createObjectURL(blob);
      } catch { if (generation === this.generation) this.failures[photo.id] = true; }
    }
  }
};
</script>

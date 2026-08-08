<template>
  <div ref="map" class="leaflet-map" aria-label="马来西亚州属经营实景地图"></div>
</template>

<script>
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import malaysiaStatesRaw from '../data/malaysiaStates.geojson?raw';
import { normalizeMalaysiaStateName } from '../utils/malaysiaMap';

const malaysiaStates = JSON.parse(malaysiaStatesRaw);
const malaysiaOuterRings = malaysiaStates.features.flatMap(feature => feature.geometry.type === 'Polygon'
  ? [feature.geometry.coordinates[0]]
  : feature.geometry.coordinates.map(polygon => polygon[0]));
const malaysiaFocusMask = {
  type: 'Feature',
  geometry: {
    type: 'Polygon',
    coordinates: [
      [[-180, -85], [180, -85], [180, 85], [-180, 85], [-180, -85]],
      ...malaysiaOuterRings.map(ring => [...ring].reverse())
    ]
  },
  properties: {}
};

export default {
  props: {
    regions: { type: Array, default: () => [] },
    selected: { type: String, default: '' }
  },
  emits: ['select'],
  data() { return { map: null, maskLayer: null, stateLayer: null }; },
  watch: {
    regions: { deep: true, handler() { this.renderStates(); } },
    selected() { this.renderStates(); }
  },
  mounted() {
    this.map = L.map(this.$refs.map, {
      zoomControl: true, attributionControl: true, minZoom: 4, maxZoom: 13, zoomSnap: .25,
      worldCopyJump: false, maxBounds: [[-1.2, 96], [10, 122]], maxBoundsViscosity: 1
    });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);
    this.map.createPane('malaysiaMaskPane');
    this.map.getPane('malaysiaMaskPane').style.zIndex = 350;
    this.map.getPane('malaysiaMaskPane').style.pointerEvents = 'none';
    this.maskLayer = L.geoJSON(malaysiaFocusMask, {
      pane: 'malaysiaMaskPane',
      interactive: false,
      style: { color: 'transparent', weight: 0, fillColor: '#06182b', fillOpacity: .68, fillRule: 'evenodd' }
    }).addTo(this.map);
    this.map.setView([4.15, 109.45], 6);
    this.renderStates();
    window.setTimeout(() => this.map?.invalidateSize(), 80);
  },
  beforeUnmount() { this.map?.remove(); this.map = null; },
  methods: {
    rowFor(feature) {
      const name = normalizeMalaysiaStateName(feature.properties.shapeName);
      return this.regions.find(row => row.regionName === name) || { regionName: name, unitCount: 0, occupancyRate: 0, totalRent: 0, averageRent: 0 };
    },
    renderStates() {
      if (!this.map) return;
      if (this.stateLayer) this.stateLayer.remove();
      const maxUnits = Math.max(...this.regions.map(row => Number(row.unitCount || 0)), 1);
      this.stateLayer = L.geoJSON(malaysiaStates, {
        style: feature => {
          const row = this.rowFor(feature);
          const active = row.regionName === this.selected;
          return {
            color: active ? '#f3a800' : '#008b93',
            weight: active ? 3.2 : row.unitCount ? 2.2 : 1.25,
            opacity: .95,
            fillColor: active ? '#ffd166' : '#11aeb4',
            fillOpacity: active ? .52 : row.unitCount ? .2 + Number(row.unitCount || 0) / maxUnits * .2 : .035
          };
        },
        onEachFeature: (feature, layer) => {
          const row = this.rowFor(feature);
          layer.bindTooltip(`<strong>${row.regionName}</strong><br>${row.unitCount} 套 · 出租率 ${Number(row.occupancyRate || 0).toFixed(1)}%`, { sticky: true, direction: 'top' });
          layer.on('click', () => this.$emit('select', row.regionName));
          layer.on('mouseover', event => event.target.setStyle({ weight: 3, fillOpacity: Math.max(event.target.options.fillOpacity, .28) }));
          layer.on('mouseout', () => this.stateLayer?.resetStyle(layer));
        }
      }).addTo(this.map);
    }
  }
};
</script>

<style scoped>
.leaflet-map{width:100%;height:430px;background:#d6e7ef}.leaflet-map:deep(.leaflet-container){font-family:Inter,"Microsoft YaHei",Arial,sans-serif}.leaflet-map:deep(.leaflet-control-zoom){overflow:hidden;border:1px solid rgba(4,70,82,.3);border-radius:7px;box-shadow:0 5px 14px rgba(3,38,54,.2)}.leaflet-map:deep(.leaflet-control-zoom a){color:#075c67;background:#fff}.leaflet-map:deep(.leaflet-control-attribution){font-size:10px;color:#40616d;background:rgba(255,255,255,.84)}.leaflet-map:deep(.leaflet-tooltip){padding:8px 10px;border:1px solid #79cbd0;border-radius:7px;color:#163d4c;background:rgba(255,255,255,.96);box-shadow:0 8px 22px rgba(3,44,60,.22);font-size:12px;line-height:1.55}.leaflet-map:deep(.leaflet-tooltip strong){color:#057781;font-size:13px}
</style>

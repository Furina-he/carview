<template>
  <DataCard title="全国故障车辆分布" class="map-card">
    <div ref="mapRef" class="map-view"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, shallowRef, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ positions?: any[] }>()
const mapRef = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)
const markerLayer = shallowRef<L.LayerGroup | null>(null)

function renderMarkers(data: any[]) {
  if (!map.value || !markerLayer.value) return
  markerLayer.value.clearLayers()
  data.forEach(v => {
    if (!v.lng || !v.lat) return
    L.circleMarker([v.lat, v.lng], {
      radius: 5, fillColor: '#ff6b6b', color: '#ff4d4f',
      weight: 1.5, opacity: 0.8, fillOpacity: 0.6
    }).bindTooltip(`${v.vehicleId}`, {
      className: 'dark-tooltip'
    }).addTo(markerLayer.value!)
  })
}

watch(() => props.positions, (val) => {
  if (val && val.length > 0) renderMarkers(val)
}, { deep: true })

onMounted(() => {
  if (!mapRef.value) return
  delete (L.Icon.Default.prototype as any)._getIconUrl
  L.Icon.Default.mergeOptions({ iconRetinaUrl: '', iconUrl: '', shadowUrl: '' })
  map.value = L.map(mapRef.value, {
    center: [34.0, 112.0], zoom: 5,
    zoomControl: false, attributionControl: false
  })
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', { maxZoom: 19 }).addTo(map.value)
  L.control.zoom({ position: 'topright' }).addTo(map.value)
  markerLayer.value = L.layerGroup().addTo(map.value)
  if (props.positions?.length) renderMarkers(props.positions)
})

onUnmounted(() => { map.value?.remove() })
</script>

<style scoped lang="scss">
.map-card { flex: 1; display: flex; flex-direction: column; }
.map-view { flex: 1; border-radius: 6px; overflow: hidden; background: #0d1b2a; min-height: 0; }
</style>

<style>
.dark-tooltip {
  background: rgba(10, 17, 40, 0.9) !important;
  border: 1px solid rgba(0, 242, 255, 0.3) !important;
  color: #e6f7ff !important;
  font-size: 12px !important;
}
</style>

<template>
  <DataCard title="车辆实时分布" class="map-card">
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

function getColor(speed: number): string {
  if (speed > 120) return '#ff4d4f'
  if (speed > 80) return '#faad14'
  return '#70ff00'
}

function renderMarkers(data: any[]) {
  if (!map.value || !markerLayer.value) return
  markerLayer.value.clearLayers()
  data.forEach(v => {
    if (!v.lng || !v.lat) return
    const color = getColor(v.speed || 0)
    L.circleMarker([v.lat, v.lng], {
      radius: 5, fillColor: color, color: color,
      weight: 1.5, opacity: 0.9, fillOpacity: 0.7
    }).bindTooltip(`${v.vehicleId}<br/>速度: ${(v.speed || 0).toFixed(0)} km/h`, {
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
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    maxZoom: 19
  }).addTo(map.value)
  L.control.zoom({ position: 'topright' }).addTo(map.value)
  markerLayer.value = L.layerGroup().addTo(map.value)

  if (props.positions && props.positions.length > 0) {
    renderMarkers(props.positions)
  }
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
  padding: 4px 8px !important;
  border-radius: 4px !important;
}
</style>

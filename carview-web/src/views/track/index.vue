<template>
  <div class="track-page">
    <div class="track-sidebar">
      <DataCard title="查询条件">
        <el-form label-width="70px" size="small">
          <el-form-item label="车牌号">
            <el-input v-model="vehicleId" placeholder="如 京A12345" />
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="startTime" type="datetime" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker v-model="endTime" type="datetime" style="width: 100%" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="queryTrack" :loading="loading">查询轨迹</el-button>
          </el-form-item>
        </el-form>
      </DataCard>

      <DataCard title="轨迹信息" v-if="trackPoints.length > 0">
        <div class="track-info">
          <p>轨迹点数: {{ trackPoints.length }}</p>
          <p>起始时间: {{ trackPoints[0]?.eventTime }}</p>
          <p>结束时间: {{ trackPoints[trackPoints.length - 1]?.eventTime }}</p>
        </div>
        <PlayerControl :total="trackPoints.length" @seek="onSeek" />
      </DataCard>
    </div>

    <div class="track-map">
      <DataCard title="轨迹回放" class="map-card">
        <div ref="mapRef" class="map-container"></div>
      </DataCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, shallowRef, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getTrack } from '@/api/statistics'
import DataCard from '@/components/DataCard.vue'
import PlayerControl from './components/PlayerControl.vue'

const vehicleId = ref('')
const startTime = ref('')
const endTime = ref('')
const loading = ref(false)
const trackPoints = ref<any[]>([])
const mapRef = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)
let polyline: L.Polyline | null = null
let cursorMarker: L.CircleMarker | null = null

onMounted(() => {
  if (!mapRef.value) return
  map.value = L.map(mapRef.value, {
    center: [39.916, 116.397],
    zoom: 12,
    zoomControl: false,
    attributionControl: false
  })
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    maxZoom: 19
  }).addTo(map.value)
  L.control.zoom({ position: 'topright' }).addTo(map.value)
})

onUnmounted(() => { map.value?.remove() })

function formatDate(d: any): string {
  if (!d) return ''
  const date = new Date(d)
  return date.toISOString().replace('T', ' ').substring(0, 19)
}

async function queryTrack() {
  if (!vehicleId.value) {
    ElMessage.warning('请输入车牌号')
    return
  }
  loading.value = true
  try {
    const res: any = await getTrack(vehicleId.value, formatDate(startTime.value), formatDate(endTime.value))
    trackPoints.value = res || []
    if (trackPoints.value.length === 0) {
      ElMessage.info('未查询到轨迹数据')
      return
    }
    drawTrack()
  } finally {
    loading.value = false
  }
}

function drawTrack() {
  if (!map.value) return
  // Clear previous
  if (polyline) map.value.removeLayer(polyline)
  if (cursorMarker) map.value.removeLayer(cursorMarker)

  const latlngs: [number, number][] = trackPoints.value.map((p: any) => [p.lat, p.lng])
  polyline = L.polyline(latlngs, { color: '#00f2ff', weight: 3, opacity: 0.8 }).addTo(map.value)
  map.value.fitBounds(polyline.getBounds(), { padding: [30, 30] })

  // Cursor at start
  const first = trackPoints.value[0]
  cursorMarker = L.circleMarker([first.lat, first.lng], {
    radius: 8, fillColor: '#70ff00', color: '#70ff00', weight: 2, fillOpacity: 0.9
  }).addTo(map.value)
}

function onSeek(index: number) {
  const point = trackPoints.value[index]
  if (point && cursorMarker) {
    cursorMarker.setLatLng([point.lat, point.lng])
  }
}
</script>

<style scoped lang="scss">
.track-page {
  display: flex;
  height: 100%;
  padding: 12px;
  gap: 16px;
}
.track-sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.track-map { flex: 1; }
.map-card { height: 100%; display: flex; flex-direction: column; }
.map-container {
  flex: 1;
  background: #0d1b2a;
  border-radius: 6px;
  overflow: hidden;
  min-height: 400px;
}
.track-info p {
  font-size: 13px;
  color: rgba(230, 247, 255, 0.65);
  margin-bottom: 4px;
}
</style>

<template>
  <div class="fence-page">
    <div class="fence-sidebar">
      <DataCard title="围栏管理">
        <el-button type="primary" size="small" @click="openCreate" style="width: 100%; margin-bottom: 12px">
          + 添加围栏
        </el-button>

        <div v-if="fenceList.length === 0" class="empty-tip">暂无围栏数据</div>

        <div class="fence-list">
          <div
            v-for="fence in fenceList"
            :key="fence.id"
            class="fence-item"
            :class="{ active: selectedId === fence.id }"
            @click="selectFence(fence)"
          >
            <div class="fence-item-header">
              <span class="fence-name">{{ fence.fenceName }}</span>
              <el-tag :type="fence.enabled === 1 ? 'success' : 'info'" size="small" effect="dark">
                {{ fence.enabled === 1 ? '启用' : '禁用' }}
              </el-tag>
            </div>
            <div class="fence-item-meta">
              <span class="fence-type">{{ fence.fenceType === 'RECTANGLE' ? '矩形' : '圆形' }}</span>
              <span v-if="fence.fenceType === 'CIRCLE'" class="fence-radius">半径 {{ fence.radius }}m</span>
            </div>
            <div class="fence-item-actions">
              <el-button link size="small" @click.stop="openEdit(fence)">编辑</el-button>
              <el-button link size="small" type="danger" @click.stop="handleDelete(fence)">删除</el-button>
            </div>
          </div>
        </div>
      </DataCard>
    </div>

    <div class="fence-map">
      <DataCard title="围栏区域" class="map-card">
        <div ref="mapRef" class="map-container"></div>
      </DataCard>
    </div>

    <FenceFormDialog
      v-model:visible="dialogVisible"
      :edit-data="editingFence"
      @success="loadFences"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, shallowRef, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getFenceList, deleteFence, type FenceRule } from '@/api/fence'
import DataCard from '@/components/DataCard.vue'
import FenceFormDialog from './components/FenceFormDialog.vue'

const fenceList = ref<FenceRule[]>([])
const selectedId = ref<number | null>(null)
const dialogVisible = ref(false)
const editingFence = ref<FenceRule | null>(null)

const mapRef = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)
const layerGroup = shallowRef<L.LayerGroup | null>(null)

onMounted(() => {
  initMap()
  loadFences()
})

onUnmounted(() => { map.value?.remove() })

function initMap() {
  if (!mapRef.value) return
  map.value = L.map(mapRef.value, {
    center: [39.916, 116.397],
    zoom: 11,
    zoomControl: false,
    attributionControl: false
  })
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    maxZoom: 19
  }).addTo(map.value)
  L.control.zoom({ position: 'topright' }).addTo(map.value)
  layerGroup.value = L.layerGroup().addTo(map.value)
}

async function loadFences() {
  try {
    const res: any = await getFenceList()
    fenceList.value = res || []
  } catch {
    fenceList.value = []
  }
  drawFences()
}

function drawFences() {
  if (!map.value || !layerGroup.value) return
  layerGroup.value.clearLayers()

  fenceList.value.forEach(fence => {
    const isEnabled = fence.enabled === 1
    const color = isEnabled ? '#00f2ff' : '#666'
    const fillColor = isEnabled ? 'rgba(0, 242, 255, 0.1)' : 'rgba(102, 102, 102, 0.1)'

    let layer: L.Layer | null = null

    if (fence.fenceType === 'RECTANGLE' && fence.minLat != null && fence.minLng != null && fence.maxLat != null && fence.maxLng != null) {
      layer = L.rectangle(
        [[fence.minLat, fence.minLng], [fence.maxLat, fence.maxLng]],
        { color, fillColor, weight: 2, fillOpacity: 0.3 }
      )
    } else if (fence.fenceType === 'CIRCLE' && fence.centerLat != null && fence.centerLng != null && fence.radius != null) {
      layer = L.circle(
        [fence.centerLat, fence.centerLng],
        { radius: fence.radius, color, fillColor, weight: 2, fillOpacity: 0.3 }
      )
    }

    if (layer) {
      (layer as any)._fenceId = fence.id
      layer.bindTooltip(
        `<b>${fence.fenceName}</b><br/>类型: ${fence.fenceType === 'RECTANGLE' ? '矩形' : '圆形'}<br/>状态: ${isEnabled ? '启用' : '禁用'}`,
        { className: 'dark-tooltip' }
      )
      layer.on('click', () => {
        selectedId.value = fence.id!
        highlightFence(fence.id!)
      })
      layerGroup.value!.addLayer(layer)
    }
  })
}

function highlightFence(id: number) {
  if (!layerGroup.value) return
  layerGroup.value.eachLayer((layer: any) => {
    const fence = fenceList.value.find(f => f.id === layer._fenceId)
    if (!fence) return
    const isEnabled = fence.enabled === 1
    const isSelected = layer._fenceId === id
    if (layer.setStyle) {
      layer.setStyle({
        color: isSelected ? '#ffeb3b' : (isEnabled ? '#00f2ff' : '#666'),
        weight: isSelected ? 3 : 2
      })
    }
  })
}

function selectFence(fence: FenceRule) {
  selectedId.value = fence.id!
  highlightFence(fence.id!)

  if (!map.value) return
  if (fence.fenceType === 'RECTANGLE' && fence.minLat != null && fence.minLng != null && fence.maxLat != null && fence.maxLng != null) {
    map.value.fitBounds([[fence.minLat, fence.minLng], [fence.maxLat, fence.maxLng]], { padding: [50, 50] })
  } else if (fence.fenceType === 'CIRCLE' && fence.centerLat != null && fence.centerLng != null) {
    map.value.setView([fence.centerLat, fence.centerLng], 13)
  }
}

function openCreate() {
  editingFence.value = null
  dialogVisible.value = true
}

function openEdit(fence: FenceRule) {
  editingFence.value = { ...fence }
  dialogVisible.value = true
}

async function handleDelete(fence: FenceRule) {
  try {
    await ElMessageBox.confirm(`确定删除围栏「${fence.fenceName}」？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteFence(fence.id!)
    ElMessage.success('删除成功')
    loadFences()
  } catch {
    // cancelled or error handled by interceptor
  }
}

watch(fenceList, () => drawFences(), { deep: true })
</script>

<style scoped lang="scss">
.fence-page {
  display: flex;
  height: 100%;
  padding: 12px;
  gap: 16px;
}

.fence-sidebar {
  width: 320px;
  flex-shrink: 0;
  overflow-y: auto;
}

.fence-map {
  flex: 1;
}

.map-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.map-container {
  flex: 1;
  background: #0d1b2a;
  border-radius: 6px;
  overflow: hidden;
  min-height: 400px;
}

.empty-tip {
  text-align: center;
  color: rgba(230, 247, 255, 0.4);
  font-size: 13px;
  padding: 24px 0;
}

.fence-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fence-item {
  background: rgba(0, 242, 255, 0.03);
  border: 1px solid rgba(0, 242, 255, 0.1);
  border-radius: 6px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: rgba(0, 242, 255, 0.3);
    background: rgba(0, 242, 255, 0.06);
  }

  &.active {
    border-color: #ffeb3b;
    background: rgba(255, 235, 59, 0.05);
  }
}

.fence-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.fence-name {
  font-size: 14px;
  color: #e6f7ff;
  font-weight: 500;
}

.fence-item-meta {
  font-size: 12px;
  color: rgba(230, 247, 255, 0.5);
  margin-bottom: 6px;

  .fence-radius {
    margin-left: 8px;
  }
}

.fence-item-actions {
  display: flex;
  gap: 8px;
}
</style>

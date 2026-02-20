<template>
  <div class="monitor-screen">
    <!-- 左列 -->
    <div class="col col-left">
      <KpiCards :data="monitorData" />
      <MonthlyVehicleChart :data="monitorData.monthlyActiveVehicles" />
      <SpeedDistChart :data="monitorData.speedDistribution" />
    </div>
    <!-- 中列 -->
    <div class="col col-center">
      <div class="center-kpi">
        <div class="kpi-item">
          <div class="kpi-num">{{ monitorData.totalVehicles || 0 }}</div>
          <div class="kpi-label">平台车辆总数</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-num accent-green">{{ monitorData.onlineVehicles || 0 }}</div>
          <div class="kpi-label">平台在线车辆</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-num accent-yellow">{{ monitorData.drivingVehicles || 0 }}</div>
          <div class="kpi-label">平台行驶中车辆</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-num accent-red">{{ monitorData.todayAlarms || 0 }}</div>
          <div class="kpi-label">今日告警数</div>
        </div>
      </div>
      <VehicleMap :positions="monitorData.vehiclePositions" />
    </div>
    <!-- 右列 -->
    <div class="col col-right">
      <BrandPieChart :data="monitorData.brandDistribution" />
      <MileageTrendChart :data="monitorData.monthlyMileageTrend" />
      <AlarmTypeChart :data="monitorData.alarmTypeStats" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getMonitorData } from '@/api/dashboard'
import KpiCards from './components/KpiCards.vue'
import VehicleMap from './components/VehicleMap.vue'
import BrandPieChart from './components/BrandPieChart.vue'
import MonthlyVehicleChart from './components/MonthlyVehicleChart.vue'
import MileageTrendChart from './components/MileageTrendChart.vue'
import SpeedDistChart from './components/SpeedDistChart.vue'
import AlarmTypeChart from './components/AlarmTypeChart.vue'

const monitorData = ref<any>({})
let refreshTimer: number | null = null

const defaultMonitorData = {
  totalVehicles: 30,
  onlineVehicles: 22,
  drivingVehicles: 15,
  todayAlarms: 8,
  drivingSummary: { totalDuration: 152400, avgDuration: 10000, totalMileage: 722200, avgMileage: 722 },
  brandDistribution: [
    { name: '一汽大众', value: 4 }, { name: '比亚迪', value: 3 },
    { name: '丰田', value: 3 }, { name: '特斯拉', value: 2 },
    { name: '宝马', value: 2 }, { name: '本田', value: 2 },
    { name: '吉利', value: 2 }, { name: '其他', value: 12 }
  ],
  monthlyActiveVehicles: Array.from({ length: 12 }, (_, i) => ({
    month: `2025-${String(i + 1).padStart(2, '0')}`, count: 15 + Math.floor(Math.random() * 15)
  })),
  monthlyMileageTrend: Array.from({ length: 12 }, (_, i) => ({
    month: `2025-${String(i + 1).padStart(2, '0')}`, totalMileage: 5000 + Math.floor(Math.random() * 10000)
  })),
  alarmTypeStats: [
    { type: 'OVERSPEED', count: 45 }, { type: 'FENCE_OUT', count: 23 },
    { type: 'FAULT', count: 18 }, { type: 'ENGINE_OVERHEAT', count: 12 },
    { type: 'LOW_BATTERY', count: 8 }
  ],
  speedDistribution: { range_0_30: 5, range_31_60: 8, range_61_90: 12, range_91_120: 6, range_120_plus: 2 },
  vehiclePositions: [
    { vehicleId: '京A12345', lng: 116.40, lat: 39.92, speed: 65 },
    { vehicleId: '京A23456', lng: 116.45, lat: 39.88, speed: 80 },
    { vehicleId: '京A34567', lng: 116.35, lat: 39.95, speed: 45 },
    { vehicleId: '京B12345', lng: 121.48, lat: 31.24, speed: 110 },
    { vehicleId: '京B23456', lng: 121.43, lat: 31.20, speed: 55 },
    { vehicleId: '京C12345', lng: 113.28, lat: 23.14, speed: 72 },
    { vehicleId: '京C23456', lng: 113.32, lat: 23.10, speed: 90 },
    { vehicleId: '京D12345', lng: 114.08, lat: 22.56, speed: 130 },
    { vehicleId: '京D23456', lng: 114.12, lat: 22.52, speed: 60 },
    { vehicleId: '京E12345', lng: 104.08, lat: 30.68, speed: 35 },
    { vehicleId: '京E23456', lng: 104.12, lat: 30.64, speed: 75 },
    { vehicleId: '京F12345', lng: 114.32, lat: 30.60, speed: 68 },
    { vehicleId: '京F23456', lng: 120.17, lat: 30.30, speed: 52 },
    { vehicleId: '京F34567', lng: 118.82, lat: 32.08, speed: 88 },
    { vehicleId: '京F45678', lng: 108.96, lat: 34.28, speed: 73 },
    { vehicleId: '京F56789', lng: 106.57, lat: 29.58, speed: 62 }
  ]
}

function mergeWithDefault(apiData: any, defaults: any): any {
  const result = { ...defaults }
  if (!apiData || typeof apiData !== 'object') return result
  for (const key of Object.keys(apiData)) {
    const val = apiData[key]
    if (val === null || val === undefined) continue
    if (Array.isArray(val)) {
      if (val.length > 0) result[key] = val
    } else if (typeof val === 'object') {
      const hasValue = Object.values(val).some(v => v !== 0 && v !== null && v !== undefined)
      if (hasValue) result[key] = val
    } else if (typeof val === 'number') {
      if (val > 0) result[key] = val
    } else {
      result[key] = val
    }
  }
  return result
}

async function fetchData() {
  try {
    const res = await getMonitorData()
    monitorData.value = mergeWithDefault(res, defaultMonitorData)
  } catch (e) {
    console.warn('使用 mock 数据', e)
    monitorData.value = { ...defaultMonitorData }
  }
}

onMounted(() => {
  monitorData.value = { ...defaultMonitorData }
  fetchData()
  refreshTimer = window.setInterval(fetchData, 10000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped lang="scss">
.monitor-screen {
  height: 100%;
  display: grid;
  grid-template-columns: 280px 1fr 300px;
  gap: 12px;
  padding: 12px;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.col-center {
  .center-kpi {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
  }
}

.kpi-item {
  background: rgba(20, 30, 60, 0.7);
  border: 1px solid rgba(0, 242, 255, 0.15);
  border-radius: 8px;
  padding: 12px;
  text-align: center;

  .kpi-num {
    font-size: 28px;
    font-weight: bold;
    color: #00f2ff;
    font-variant-numeric: tabular-nums;
    &.accent-green { color: #70ff00; }
    &.accent-yellow { color: #faad14; }
    &.accent-red { color: #ff4d4f; }
  }
  .kpi-label {
    font-size: 12px;
    color: rgba(230, 247, 255, 0.65);
    margin-top: 4px;
  }
}
</style>

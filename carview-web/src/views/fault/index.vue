<template>
  <div class="fault-screen">
    <!-- 左列 -->
    <div class="col col-left">
      <FaultKpiCards :alarmSummary="faultData.alarmSummary"
                     :faultVehicleSummary="faultData.faultVehicleSummary" />
      <AlarmLevelPie :data="faultData.alarmLevelDistribution" />
      <FaultTypeRanking :data="faultData.faultTypeRanking" />
    </div>
    <!-- 中列 -->
    <div class="col col-center">
      <FaultMap :positions="faultData.vehiclePositions" />
      <RecentFaultTable :data="faultData.recentFaultVehicles" />
    </div>
    <!-- 右列 -->
    <div class="col col-right">
      <DailyAlarmTrend :data="faultData.dailyAlarmTrend" />
      <FaultByBrandChart :data="faultData.faultByBrand" />
      <AlarmLevelPie :data="faultData.alarmLevelDistribution" title="故障等级占比(近一月)" variant="doughnut" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getFaultData } from '@/api/dashboard'
import FaultKpiCards from './components/FaultKpiCards.vue'
import FaultMap from './components/FaultMap.vue'
import AlarmLevelPie from './components/AlarmLevelPie.vue'
import DailyAlarmTrend from './components/DailyAlarmTrend.vue'
import FaultTypeRanking from './components/FaultTypeRanking.vue'
import RecentFaultTable from './components/RecentFaultTable.vue'
import FaultByBrandChart from './components/FaultByBrandChart.vue'

const defaultFaultData = {
  alarmSummary: { monthCount: 187, yearCount: 2345, todayCount: 23 },
  faultVehicleSummary: { monthFaultVehicles: 68, yearFaultVehicles: 412, todayFaultVehicles: 9 },
  alarmLevelDistribution: [
    { level: 1, count: 45 },
    { level: 2, count: 98 },
    { level: 3, count: 156 }
  ],
  faultTypeRanking: [
    { faultName: 'OVERSPEED', faultCount: 78 },
    { faultName: 'ENGINE_OVERHEAT', faultCount: 52 },
    { faultName: 'LOW_BATTERY', faultCount: 41 },
    { faultName: 'FENCE_OUT', faultCount: 33 },
    { faultName: 'FAULT', faultCount: 18 }
  ],
  vehiclePositions: [
    { vehicleId: '京A88001', lat: 39.92, lng: 116.46, speed: 0 },
    { vehicleId: '沪B66012', lat: 31.23, lng: 121.47, speed: 12 },
    { vehicleId: '粤C55023', lat: 23.13, lng: 113.26, speed: 0 },
    { vehicleId: '川D44034', lat: 30.57, lng: 104.07, speed: 5 },
    { vehicleId: '鄂E33045', lat: 30.59, lng: 114.31, speed: 0 },
    { vehicleId: '浙F22056', lat: 30.27, lng: 120.15, speed: 8 },
    { vehicleId: '苏G11067', lat: 32.06, lng: 118.78, speed: 0 },
    { vehicleId: '鲁H99078', lat: 36.67, lng: 116.98, speed: 0 },
    { vehicleId: '豫J77089', lat: 34.76, lng: 113.65, speed: 3 },
    { vehicleId: '湘K66090', lat: 28.23, lng: 112.94, speed: 0 }
  ],
  dailyAlarmTrend: Array.from({ length: 30 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() - (29 - i))
    return { date: d.toISOString().slice(0, 10), count: Math.floor(Math.random() * 15) + 3 }
  }),
  recentFaultVehicles: [
    { vehicleId: '京A88001', vin: 'LSVAU2180N2012345', vehicleModel: 'Model 3', eventTime: '2026-02-19 14:23:01', faultName: 'ENGINE_OVERHEAT', level: 1 },
    { vehicleId: '沪B66012', vin: 'LSVAU2180N2023456', vehicleModel: 'ES6', eventTime: '2026-02-19 14:18:45', faultName: 'LOW_BATTERY', level: 2 },
    { vehicleId: '粤C55023', vin: 'LSVAU2180N2034567', vehicleModel: 'Han EV', eventTime: '2026-02-19 14:12:30', faultName: 'OVERSPEED', level: 3 },
    { vehicleId: '川D44034', vin: 'LSVAU2180N2045678', vehicleModel: 'Aion S', eventTime: '2026-02-19 13:58:12', faultName: 'FENCE_OUT', level: 2 },
    { vehicleId: '鄂E33045', vin: 'LSVAU2180N2056789', vehicleModel: 'P7', eventTime: '2026-02-19 13:45:00', faultName: 'FAULT', level: 1 },
    { vehicleId: '浙F22056', vin: 'LSVAU2180N2067890', vehicleModel: 'ZEEKR 001', eventTime: '2026-02-19 13:30:22', faultName: 'ENGINE_OVERHEAT', level: 2 },
    { vehicleId: '苏G11067', vin: 'LSVAU2180N2078901', vehicleModel: 'ET5', eventTime: '2026-02-19 13:15:44', faultName: 'LOW_BATTERY', level: 3 },
    { vehicleId: '鲁H99078', vin: 'LSVAU2180N2089012', vehicleModel: 'Seal', eventTime: '2026-02-19 12:58:33', faultName: 'OVERSPEED', level: 1 },
    { vehicleId: '豫J77089', vin: 'LSVAU2180N2090123', vehicleModel: 'iX3', eventTime: '2026-02-19 12:40:11', faultName: 'FENCE_OUT', level: 3 },
    { vehicleId: '湘K66090', vin: 'LSVAU2180N2001234', vehicleModel: 'EQC', eventTime: '2026-02-19 12:25:08', faultName: 'ENGINE_OVERHEAT', level: 2 }
  ],
  faultByBrand: [
    { brand: '特斯拉', count: 28 },
    { brand: '蔚来', count: 22 },
    { brand: '比亚迪', count: 35 },
    { brand: '小鹏', count: 18 },
    { brand: '理想', count: 15 },
    { brand: '极氪', count: 12 },
    { brand: '宝马', count: 8 }
  ]
}

const faultData = ref<any>({ ...defaultFaultData })
let refreshTimer: number | null = null

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
    const res = await getFaultData()
    faultData.value = mergeWithDefault(res, defaultFaultData)
  } catch (e) {
    console.warn('使用默认故障数据', e)
  }
}

onMounted(() => {
  faultData.value = { ...defaultFaultData }
  fetchData()
  refreshTimer = window.setInterval(fetchData, 10000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped lang="scss">
.fault-screen {
  height: 100%;
  display: grid;
  grid-template-columns: 280px 1fr 300px;
  grid-template-rows: 1fr;
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
  display: grid;
  grid-template-rows: 1fr auto;
  gap: 12px;
}
</style>

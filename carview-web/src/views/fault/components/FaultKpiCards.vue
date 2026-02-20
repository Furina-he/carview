<template>
  <DataCard class="fault-kpi">
    <div class="kpi-row">
      <div class="kpi-block">
        <div class="kpi-title">全国故障次数</div>
        <div class="kpi-nums">
          <div class="kpi-cell">
            <span class="num">{{ alarm.monthCount || 0 }}</span>
            <span class="dim">当月</span>
          </div>
          <div class="kpi-cell">
            <span class="num">{{ alarm.yearCount || 0 }}</span>
            <span class="dim">当年</span>
          </div>
          <div class="kpi-cell">
            <span class="num accent-red">{{ alarm.todayCount || 0 }}</span>
            <span class="dim">当日</span>
          </div>
        </div>
      </div>
      <div class="kpi-block">
        <div class="kpi-title">故障车辆数</div>
        <div class="kpi-nums">
          <div class="kpi-cell">
            <span class="num">{{ fv.monthFaultVehicles || 0 }}</span>
            <span class="dim">当月</span>
          </div>
          <div class="kpi-cell">
            <span class="num">{{ fv.yearFaultVehicles || 0 }}</span>
            <span class="dim">当年</span>
          </div>
          <div class="kpi-cell">
            <span class="num accent-yellow">{{ fv.todayFaultVehicles || 0 }}</span>
            <span class="dim">当日</span>
          </div>
        </div>
      </div>
    </div>
  </DataCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ alarmSummary?: any; faultVehicleSummary?: any }>()
const alarm = computed(() => props.alarmSummary || {})
const fv = computed(() => props.faultVehicleSummary || {})
</script>

<style scoped lang="scss">
.fault-kpi { padding: 12px; }
.kpi-row { display: flex; flex-direction: column; gap: 14px; }
.kpi-block {
  .kpi-title {
    font-size: 12px;
    color: rgba(230, 247, 255, 0.55);
    margin-bottom: 8px;
  }
}
.kpi-nums {
  display: flex;
  gap: 12px;
  .kpi-cell {
    flex: 1;
    text-align: center;
    .num {
      display: block;
      font-size: 24px;
      font-weight: bold;
      color: #00f2ff;
      font-variant-numeric: tabular-nums;
      &.accent-red { color: #ff4d4f; }
      &.accent-yellow { color: #faad14; }
    }
    .dim {
      font-size: 11px;
      color: rgba(230, 247, 255, 0.45);
    }
  }
}
</style>

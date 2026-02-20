<template>
  <div class="kpi-cards">
    <DataCard glow>
      <div class="big-kpi">
        <div class="big-num">{{ formatNum(summary.totalDuration) }}</div>
        <div class="big-label">行驶时长总数 (h)</div>
      </div>
    </DataCard>
    <DataCard glow>
      <div class="big-kpi">
        <div class="big-num accent-green">{{ formatNum(summary.avgDuration) }}</div>
        <div class="big-label">行驶时长平均数 (h)</div>
      </div>
    </DataCard>
    <DataCard glow>
      <div class="big-kpi">
        <div class="big-num accent-yellow">{{ formatNum(summary.totalMileage) }}</div>
        <div class="big-label">行驶总里程 (km)</div>
      </div>
    </DataCard>
    <DataCard glow>
      <div class="big-kpi">
        <div class="big-num accent-purple">{{ formatNum(summary.avgMileage) }}</div>
        <div class="big-label">行驶平均里程 (km)</div>
      </div>
    </DataCard>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data: any }>()

const summary = computed(() => props.data?.drivingSummary || {})

function formatNum(n: any): string {
  const num = Number(n) || 0
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}
</script>

<style scoped lang="scss">
.kpi-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.big-kpi {
  text-align: center;
  padding: 8px 0;
  .big-num {
    font-size: 28px;
    font-weight: bold;
    color: #00f2ff;
    line-height: 1.3;
    font-variant-numeric: tabular-nums;
    &.accent-green { color: #70ff00; }
    &.accent-yellow { color: #faad14; }
    &.accent-purple { color: #b37feb; }
  }
  .big-label {
    font-size: 11px;
    color: rgba(230, 247, 255, 0.55);
    margin-top: 4px;
  }
}
</style>

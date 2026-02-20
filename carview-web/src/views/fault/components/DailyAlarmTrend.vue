<template>
  <DataCard title="告警趋势(近30天)" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[] }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

function buildOption(rawData: any[]) {
  const data = rawData || []
  return {
    tooltip: { trigger: 'axis' as const },
    grid: { top: 10, right: 10, bottom: 24, left: 40 },
    xAxis: {
      type: 'category' as const,
      data: data.map(d => (d.date || '').substring(5)),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 9, rotate: 30 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    yAxis: {
      type: 'value' as const,
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    series: [{
      type: 'line', data: data.map(d => d.count || 0),
      smooth: true, symbol: 'none',
      lineStyle: { color: '#ff4d4f', width: 2 },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(255,77,79,0.3)' },
            { offset: 1, color: 'rgba(255,77,79,0.02)' }
          ]
        }
      }
    }]
  }
}

onMounted(() => { init(buildOption(props.data || [])) })
watch(() => props.data, (val) => { if (val) setOption(buildOption(val)) }, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>

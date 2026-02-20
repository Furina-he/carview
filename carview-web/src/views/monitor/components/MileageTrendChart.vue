<template>
  <DataCard title="月度里程趋势" class="chart-card">
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
  const sorted = [...(rawData || [])].reverse()
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 24, left: 50 },
    xAxis: {
      type: 'category',
      data: sorted.map(d => d.month?.substring(5) || ''),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    series: [{
      type: 'line', data: sorted.map(d => d.totalMileage || 0),
      smooth: true, symbol: 'circle', symbolSize: 6,
      lineStyle: { color: '#faad14', width: 2 },
      itemStyle: { color: '#faad14' },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(250,173,20,0.25)' },
            { offset: 1, color: 'rgba(250,173,20,0.02)' }
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

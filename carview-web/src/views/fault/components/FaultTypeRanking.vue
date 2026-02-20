<template>
  <DataCard title="故障类型排行" class="chart-card">
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

const typeMap: Record<string, string> = {
  OVERSPEED: '超速', FENCE_OUT: '围栏越界', FAULT: '故障码',
  ENGINE_OVERHEAT: '发动机过热', LOW_BATTERY: '电池低压'
}

function buildOption(rawData: any[]) {
  const data = [...(rawData || [])].reverse()
  return {
    tooltip: { trigger: 'axis' as const },
    grid: { top: 8, right: 40, bottom: 8, left: 80 },
    xAxis: {
      type: 'value' as const,
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    yAxis: {
      type: 'category' as const,
      data: data.map(d => typeMap[d.faultName] || d.faultName),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 11 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    series: [{
      type: 'bar', data: data.map(d => d.faultCount || 0),
      itemStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: 'rgba(255,77,79,0.3)' },
            { offset: 1, color: '#ff4d4f' }
          ]
        },
        borderRadius: [0, 4, 4, 0]
      },
      barWidth: '55%',
      label: { show: true, position: 'right', color: 'rgba(230,247,255,0.75)', fontSize: 11 }
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

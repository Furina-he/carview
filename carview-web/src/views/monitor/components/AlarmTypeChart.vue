<template>
  <DataCard title="车辆报警统计" class="chart-card">
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
  OVERSPEED: '超速', FENCE_OUT: '越界', FAULT: '故障',
  ENGINE_OVERHEAT: '过热', LOW_BATTERY: '低电压'
}

function buildOption(rawData: any[]) {
  const data = [...(rawData || [])].reverse()
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 8, right: 30, bottom: 8, left: 60 },
    xAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    yAxis: {
      type: 'category',
      data: data.map(d => typeMap[d.type] || d.type),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 11 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    series: [{
      type: 'bar',
      data: data.map(d => d.count || 0),
      itemStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: 'rgba(0,242,255,0.3)' },
            { offset: 1, color: '#00f2ff' }
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

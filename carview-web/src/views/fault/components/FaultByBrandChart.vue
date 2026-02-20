<template>
  <DataCard title="各品牌故障数(近一月)" class="chart-card">
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

const colors = ['#00f2ff', '#70ff00', '#faad14', '#ff4d4f', '#b37feb', '#36cfc9', '#ff85c0', '#597ef7']

function buildOption(rawData: any[]) {
  const data = rawData || []
  return {
    tooltip: { trigger: 'axis' as const },
    grid: { top: 10, right: 10, bottom: 30, left: 40 },
    xAxis: {
      type: 'category' as const,
      data: data.map(d => d.brand || ''),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10, rotate: 20 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    yAxis: {
      type: 'value' as const,
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    series: [{
      type: 'bar',
      data: data.map((d, i) => ({
        value: d.count || 0,
        itemStyle: { color: colors[i % colors.length] }
      })),
      barWidth: '55%',
      itemStyle: { borderRadius: [4, 4, 0, 0] }
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

<template>
  <DataCard title="实时速度分布" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

function buildOption(d: any) {
  const data = d || {}
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 24, left: 35 },
    xAxis: {
      type: 'category',
      data: ['0-30', '31-60', '61-90', '91-120', '>120'],
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    series: [{
      type: 'bar',
      data: [
        data.range_0_30 || 0, data.range_31_60 || 0,
        data.range_61_90 || 0, data.range_91_120 || 0,
        data.range_120_plus || 0
      ],
      itemStyle: {
        color: (params: any) => {
          const colors = ['#70ff00', '#00f2ff', '#faad14', '#ff85c0', '#ff4d4f']
          return colors[params.dataIndex] || '#00f2ff'
        },
        borderRadius: [4, 4, 0, 0]
      },
      barWidth: '50%'
    }]
  }
}

onMounted(() => { init(buildOption(props.data)) })
watch(() => props.data, (val) => { setOption(buildOption(val)) }, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>

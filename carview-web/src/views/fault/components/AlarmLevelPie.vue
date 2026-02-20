<template>
  <DataCard :title="title || '故障等级分布'" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[]; title?: string; variant?: string }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

const levelNames: Record<number, string> = { 1: '一级(严重)', 2: '二级(警告)', 3: '三级(提示)' }
const levelColors: Record<number, string> = { 1: '#ff4d4f', 2: '#faad14', 3: '#00f2ff' }

function buildOption(rawData: any[]) {
  const data = (rawData || []).map(d => ({
    name: levelNames[d.level] || `级别${d.level}`,
    value: d.count || 0,
    itemStyle: { color: levelColors[d.level] || '#00f2ff' }
  }))
  const radius = props.variant === 'doughnut' ? ['45%', '70%'] : ['0%', '65%']
  return {
    tooltip: { trigger: 'item' as const, formatter: '{b}: {c} ({d}%)' },
    legend: {
      bottom: 5, left: 'center',
      textStyle: { color: 'rgba(230,247,255,0.65)', fontSize: 11 }
    },
    series: [{
      type: 'pie', radius,
      label: { show: true, color: 'rgba(230,247,255,0.65)', fontSize: 11, formatter: '{d}%' },
      data, itemStyle: { borderColor: '#0a1128', borderWidth: 2 }
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

<template>
  <DataCard title="车辆品牌分布" class="chart-card">
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

const colors = ['#00f2ff', '#70ff00', '#faad14', '#ff4d4f', '#b37feb', '#36cfc9', '#ff85c0', '#597ef7', '#ffc53d', '#95de64']

function buildOption(data: any[]) {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    color: colors,
    legend: {
      orient: 'vertical', right: 10, top: 'center',
      textStyle: { color: 'rgba(230,247,255,0.65)', fontSize: 11 }
    },
    series: [{
      type: 'pie', radius: ['40%', '65%'],
      center: ['35%', '50%'],
      label: { show: false },
      data: data || [],
      itemStyle: { borderColor: '#0a1128', borderWidth: 2 }
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

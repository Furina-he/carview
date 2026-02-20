import { ref, onMounted, onUnmounted, shallowRef } from 'vue'
import * as echarts from 'echarts'

export function useEcharts(el: () => HTMLElement | null) {
  const chart = shallowRef<echarts.ECharts | null>(null)

  function init(option: any) {
    const dom = el()
    if (!dom) return
    chart.value = echarts.init(dom)
    chart.value.setOption(option)
  }

  function setOption(option: any) {
    chart.value?.setOption(option, { notMerge: false })
  }

  function handleResize() {
    chart.value?.resize()
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    chart.value?.dispose()
  })

  return { chart, init, setOption }
}

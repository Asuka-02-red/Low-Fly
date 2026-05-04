<script setup lang="ts">
/** ECharts 图表卡片组件：增强型图表卡片，带加载骨架屏，封装 ECharts 实例的初始化、配置更新、窗口自适应和销毁 */
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const CHART_COLORS = [
  '#2563EB', '#059669', '#D97706', '#DC2626', '#6366F1',
  '#0EA5E9', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981',
]

const props = defineProps<{
  title: string
  option: echarts.EChartsOption
  height?: number
}>()

const chartRef = ref<HTMLDivElement>()
const chartReady = ref(false)
let chart: echarts.ECharts | null = null

function applyDefaultColors(option: echarts.EChartsOption): echarts.EChartsOption {
  return {
    ...option,
    color: CHART_COLORS,
  }
}

function renderChart() {
  if (!chartRef.value) {
    return
  }

  if (!chart) {
    chart = echarts.init(chartRef.value)
    chartReady.value = true
  }

  const processedOption = applyDefaultColors(props.option)
  chart.setOption(processedOption, true)
  chart.resize()
}

function handleResize() {
  chart?.resize()
}

onMounted(async () => {
  await nextTick()
  renderChart()
  window.addEventListener('resize', handleResize)
})

watch(
  () => props.option,
  () => {
    renderChart()
  },
  { deep: true },
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <el-card class="chart-card page-card" shadow="never">
    <template #header>
      <span class="section-title">{{ title }}</span>
    </template>
    <div
      ref="chartRef"
      class="chart"
      :class="{ 'chart-ready': chartReady }"
      :style="{ height: `${height ?? 320}px` }"
    />
  </el-card>
</template>

<style scoped lang="scss">
.chart-card {
  :deep(.el-card__header) {
    padding: var(--space-5) var(--space-6);
  }

  :deep(.el-card__body) {
    padding: var(--space-3) var(--space-4) var(--space-4);
  }
}

.chart {
  width: 100%;
  opacity: 0;
  transition: opacity var(--transition-smooth);

  &.chart-ready {
    opacity: 1;
  }
}
</style>

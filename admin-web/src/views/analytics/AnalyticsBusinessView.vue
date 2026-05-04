<script setup lang="ts">
/** 经营分析页面：展示营收趋势柱状图、用户活跃度折线图和项目健康度饼图，附带核心经营指标卡片 */
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { EChartsOption } from 'echarts'
import EChartCard from '@/components/EChartCard.vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { fetchAdminAnalytics } from '@/api/admin'
import type { AnalyticsPayload } from '@/types'

const loading = ref(false)
const payload = ref<AnalyticsPayload>({
  businessMetrics: [],
  performanceMetrics: [],
  revenueTrend: [],
  userActivity: [],
  projectHealth: [],
  servicePerformance: [],
  operatorLoad: [],
})

const revenueOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: payload.value.revenueTrend.map((item) => item.label) },
  yAxis: { type: 'value' },
  series: [{
    type: 'bar',
    barWidth: 28,
    itemStyle: { borderRadius: [6, 6, 0, 0] },
    data: payload.value.revenueTrend.map((item) => item.value),
  }],
}))

const userOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: payload.value.userActivity.map((item) => item.label) },
  yAxis: { type: 'value' },
  series: [{
    type: 'line',
    smooth: true,
    lineStyle: { width: 2.5 },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(5, 150, 105, 0.2)' },
          { offset: 1, color: 'rgba(5, 150, 105, 0.02)' },
        ],
      },
    },
    data: payload.value.userActivity.map((item) => item.value),
  }],
}))

const healthOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: '68%',
    data: payload.value.projectHealth,
    emphasis: {
      itemStyle: {
        shadowBlur: 10,
        shadowOffsetX: 0,
        shadowColor: 'rgba(0, 0, 0, 0.1)',
      },
    },
  }],
}))

async function loadData() {
  loading.value = true
  try {
    payload.value = await fetchAdminAnalytics()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载经营分析失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

void loadData()
</script>

<template>
  <PageContainer
    title="经营分析"
    description="查看营收走势、用户活跃度与项目健康度。"
    back-to="/analytics"
    back-label="返回分析"
  >
    <template #actions>
      <el-button type="primary" :loading="loading" @click="loadData">刷新图表</el-button>
    </template>

    <div class="metrics-grid">
      <StatCard v-for="metric in payload.businessMetrics" :key="metric.label" :metric="metric" />
    </div>

    <div class="chart-grid" v-loading="loading">
      <EChartCard title="月度营收趋势" :option="revenueOption" />
      <EChartCard title="用户活跃度趋势" :option="userOption" />
      <EChartCard title="项目健康度分布" :option="healthOption" />
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--space-5);
}
</style>

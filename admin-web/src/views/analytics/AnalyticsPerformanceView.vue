<script setup lang="ts">
/** 性能分析页面：展示服务响应时间与可用性双轴图、运维任务负载环形图，附带核心性能指标卡片 */
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

const serviceOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: {},
  xAxis: { type: 'category', data: payload.value.servicePerformance.map((item) => item.label) },
  yAxis: [
    { type: 'value', name: '响应时间(ms)' },
    { type: 'value', name: '可用性(%)', max: 100 },
  ],
  series: [
    {
      name: '响应时间',
      type: 'bar',
      data: payload.value.servicePerformance.map((item) => item.response),
    },
    {
      name: '可用性',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      data: payload.value.servicePerformance.map((item) => item.availability),
    },
  ],
}))

const loadOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['40%', '72%'], data: payload.value.operatorLoad }],
}))

async function loadData() {
  loading.value = true
  try {
    payload.value = await fetchAdminAnalytics()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载性能分析失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

void loadData()
</script>

<template>
  <PageContainer
    title="性能分析"
    description="查看服务响应时间、可用性和运维负载。"
    back-to="/analytics"
    back-label="返回分析"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">刷新性能数据</el-button>
    </template>

    <div class="metrics-grid">
      <StatCard v-for="metric in payload.performanceMetrics" :key="metric.label" :metric="metric" />
    </div>

    <div class="chart-grid" v-loading="loading">
      <EChartCard title="服务性能监控" :option="serviceOption" />
      <EChartCard title="运维任务负载" :option="loadOption" />
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

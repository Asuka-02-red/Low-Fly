<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { EChartsOption } from 'echarts'
import EChartCard from '@/components/EChartCard.vue'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import { fetchAdminOverview } from '@/api/admin'
import type { OverviewPayload } from '@/types'

const loading = ref(false)
const overview = ref<OverviewPayload>({
  metrics: [],
  activities: [],
  notices: [],
  deviceStats: [],
  projectDistribution: [],
  progressTrend: [],
})

const deviceOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: overview.value.deviceStats.map((item) => item.label) },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'bar',
      barWidth: 26,
      data: overview.value.deviceStats.map((item) => Number.parseFloat(item.value)),
    },
  ],
}))

async function loadData() {
  loading.value = true
  try {
    overview.value = await fetchAdminOverview()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载平台态势失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

void loadData()
</script>

<template>
  <PageContainer
    title="平台态势"
    description="对比终端访问、在线服务和近期待处理事项。"
    back-to="/overview"
    back-label="返回总览"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">刷新态势</el-button>
    </template>

    <div class="metrics-grid">
      <StatCard v-for="metric in overview.deviceStats" :key="metric.label" :metric="metric" />
    </div>

    <div class="command-grid" v-loading="loading">
      <EChartCard title="平台运营指标" :option="deviceOption" />

      <el-card class="page-card" shadow="never">
        <template #header>
          <span class="section-title">动态通知</span>
        </template>
        <el-alert
          v-for="notice in overview.notices"
          :key="notice.title"
          :title="notice.title"
          :type="notice.level === '高' ? 'error' : notice.level === '中' ? 'warning' : 'success'"
          :description="notice.time"
          :closable="false"
          show-icon
        />
      </el-card>

      <el-card class="page-card" shadow="never">
        <template #header>
          <span class="section-title">近期关键动作</span>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="activity in overview.activities"
            :key="`${activity.title}-${activity.time}`"
            :timestamp="activity.time"
          >
            <strong>{{ activity.title }}</strong>
            <p>{{ activity.content }}</p>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.command-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;

  :deep(.el-alert + .el-alert) {
    margin-top: 12px;
  }
}

@media (max-width: 1080px) {
  .command-grid {
    grid-template-columns: 1fr;
  }
}
</style>

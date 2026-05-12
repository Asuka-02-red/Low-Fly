<script setup lang="ts">
/** 项目中心页面：展示项目全生命周期管理视图，包含指标卡片、关键词搜索和项目详情表格（状态、进度、预算、合规、风险、结算） */
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAdminProjects } from '@/api/admin'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import type { ProjectRecord, SummaryMetric } from '@/types'

const loading = ref(false)
const keyword = ref('')
const projects = ref<ProjectRecord[]>([])

const metrics = computed<SummaryMetric[]>(() => {
  const total = projects.value.length
  const executing = projects.value.filter((item) => item.status === '执行中').length
  const highRisk = projects.value.filter((item) => item.riskLevel === '高').length
  const settled = projects.value.filter((item) => item.paymentStatus === '已结算').length

  return [
    { label: '项目总数', value: String(total), trend: '来自真实任务/订单聚合', status: 'success' },
    { label: '执行中项目', value: String(executing), trend: '按任务发布状态换算', status: 'info' },
    { label: '高风险项目', value: String(highRisk), trend: '第一轮按规则估算', status: 'warning' },
    { label: '已结算项目', value: String(settled), trend: '基于订单支付状态聚合', status: 'danger' },
  ]
})

const filteredProjects = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) {
    return projects.value
  }

  return projects.value.filter((project) =>
    [project.id, project.name, project.owner, project.region, project.status]
      .join(' ')
      .toLowerCase()
      .includes(text),
  )
})

async function loadData() {
  loading.value = true
  try {
    projects.value = await fetchAdminProjects()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载项目中心失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

void loadData()
</script>

<template>
  <PageContainer
    title="项目中心"
    description="查看真实任务与订单聚合后的项目态势、风险与结算信息。"
    back-to="/projects"
    back-label="返回项目管理"
  >
    <template #actions>
      <el-input v-model="keyword" placeholder="搜索项目/区域/负责人" clearable class="toolbar-search" prefix-icon="Search" />
      <el-button type="primary" :loading="loading" @click="loadData">刷新</el-button>
      <el-button type="primary" :loading="loading" @click="createProject">新增项目</el-button>
    </template>

    <div class="metrics-grid">
      <StatCard v-for="metric in metrics" :key="metric.label" :metric="metric" />
    </div>

    <el-card class="page-card" shadow="never" v-loading="loading">
      <el-table :data="filteredProjects" border stripe>
        <el-table-column prop="id" label="项目编号" min-width="120" />
        <el-table-column prop="name" label="项目名称" min-width="220" />
        <el-table-column prop="owner" label="负责人" min-width="120" />
        <el-table-column prop="region" label="区域" min-width="140" />
        <el-table-column label="状态" min-width="130">
          <template #default="{ row }">
            <el-tag
              :type="
                row.status === '执行中'
                  ? 'primary'
                  : row.status === '已完成'
                    ? 'success'
                    : row.status === '已暂停'
                      ? 'warning'
                      : 'info'
              "
            >
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" min-width="170">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :stroke-width="12" />
          </template>
        </el-table-column>
        <el-table-column label="预算" min-width="140">
          <template #default="{ row }">
            <span class="money-value">¥ {{ row.budget.toLocaleString('zh-CN') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="合规" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.complianceStatus === '正常' ? 'success' : row.complianceStatus === '高风险' ? 'danger' : 'warning'">
              {{ row.complianceStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.riskLevel === '低' ? 'success' : row.riskLevel === '中' ? 'warning' : 'danger'">
              {{ row.riskLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结算" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.paymentStatus === '已结算' ? 'success' : row.paymentStatus === '部分结算' ? 'warning' : 'info'">
              {{ row.paymentStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
      </el-table>
    </el-card>
  </PageContainer>
</template>

<style scoped lang="scss">
.toolbar-search {
  width: min(280px, 100%);
}

.money-value {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  color: var(--color-text-primary);
}
</style>

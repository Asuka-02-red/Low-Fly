<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { fetchAdminProjects } from '@/api/admin'
import type { ProjectRecord } from '@/types'

const loading = ref(false)
const projects = ref<ProjectRecord[]>([])

const complianceRows = computed(() =>
  projects.value.map((project) => ({
    id: project.id,
    name: project.name,
    complianceStatus: project.complianceStatus,
    riskLevel: project.riskLevel,
    updatedAt: project.updatedAt,
  })),
)

async function loadData() {
  loading.value = true
  try {
    projects.value = await fetchAdminProjects()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载合规风控数据失败。')
  } finally {
    loading.value = false
  }
}

void loadData()
</script>

<template>
  <PageContainer
    title="合规风控"
    description="聚合项目合规状态、风险等级和最近处置时间。"
    back-to="/projects"
    back-label="返回项目管理"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">刷新合规视图</el-button>
    </template>

    <el-card class="page-card" shadow="never" v-loading="loading">
      <el-table :data="complianceRows" border>
        <el-table-column prop="id" label="项目编号" min-width="120" />
        <el-table-column prop="name" label="项目名称" min-width="220" />
        <el-table-column label="合规状态" min-width="130">
          <template #default="{ row }">
            <el-tag :type="row.complianceStatus === '正常' ? 'success' : row.complianceStatus === '待复核' ? 'warning' : 'danger'">
              {{ row.complianceStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.riskLevel === '低' ? 'success' : row.riskLevel === '中' ? 'warning' : 'danger'">
              {{ row.riskLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最近更新时间" min-width="180" />
      </el-table>
    </el-card>
  </PageContainer>
</template>

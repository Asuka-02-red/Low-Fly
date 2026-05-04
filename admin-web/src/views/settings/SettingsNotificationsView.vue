<script setup lang="ts">
/** 通知规则设置页面：以表格形式管理通知规则的名称、渠道、触发条件和启停状态 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { fetchAdminSettings, updateAdminNotificationRules } from '@/api/admin'
import type { NotificationRule } from '@/types'

const loading = ref(false)
const saving = ref(false)
const rules = ref<NotificationRule[]>([])

async function loadData() {
  loading.value = true
  try {
    const payload = await fetchAdminSettings()
    rules.value = payload.notifications
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载通知规则失败。')
  } finally {
    loading.value = false
  }
}

async function saveData() {
  saving.value = true
  try {
    await updateAdminNotificationRules(rules.value)
    ElMessage.success('通知规则已保存。')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存通知规则失败。')
  } finally {
    saving.value = false
  }
}

void loadData()
</script>

<template>
  <PageContainer
    title="通知规则"
    description="定义通知渠道、启停状态和触发条件。"
    back-to="/settings"
    back-label="返回系统设置"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">刷新规则</el-button>
      <el-button type="primary" :loading="saving" @click="saveData">保存规则</el-button>
    </template>

    <el-card class="page-card" shadow="never" v-loading="loading">
      <el-table :data="rules" border>
        <el-table-column prop="name" label="规则名称" min-width="180" />
        <el-table-column prop="channel" label="通知渠道" min-width="120" />
        <el-table-column prop="trigger" label="触发条件" min-width="260" />
        <el-table-column label="启用" min-width="120">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </PageContainer>
</template>

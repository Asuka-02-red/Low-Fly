<script setup lang="ts">
/** 基础参数设置页面：配置平台名称、服务热线、默认区域和移动端概览看板开关 */
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { fetchAdminSettings, updateAdminBasicSettings } from '@/api/admin'
import type { BasicSettings } from '@/types'

const loading = ref(false)
const saving = ref(false)
const form = reactive<BasicSettings>({
  stationName: '',
  serviceHotline: '',
  defaultRegion: '',
  mobileDashboardEnabled: false,
})

async function loadData() {
  loading.value = true
  try {
    const payload = await fetchAdminSettings()
    Object.assign(form, payload.basic)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载基础参数失败。')
  } finally {
    loading.value = false
  }
}

async function saveData() {
  saving.value = true
  try {
    await updateAdminBasicSettings({ ...form })
    ElMessage.success('基础参数已保存。')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败。')
  } finally {
    saving.value = false
  }
}

void loadData()
</script>

<template>
  <PageContainer
    title="基础参数"
    description="配置平台名称、服务热线、默认区域和移动端开关。"
    back-to="/settings"
    back-label="返回系统设置"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">重置表单</el-button>
      <el-button type="primary" :loading="saving" @click="saveData">保存参数</el-button>
    </template>

    <el-card class="page-card" shadow="never" v-loading="loading">
      <el-form label-position="top" class="settings-form">
        <div class="form-grid">
          <el-form-item label="平台名称">
            <el-input v-model="form.stationName" placeholder="请输入平台名称" />
          </el-form-item>
          <el-form-item label="服务热线">
            <el-input v-model="form.serviceHotline" placeholder="请输入服务热线号码" />
          </el-form-item>
          <el-form-item label="默认区域">
            <el-input v-model="form.defaultRegion" placeholder="请输入默认区域" />
          </el-form-item>
          <el-form-item label="移动端概览看板">
            <div class="switch-field">
              <el-switch v-model="form.mobileDashboardEnabled" />
              <span class="switch-hint">{{ form.mobileDashboardEnabled ? '已启用' : '已关闭' }}</span>
            </div>
          </el-form-item>
        </div>
      </el-form>
    </el-card>
  </PageContainer>
</template>

<style scoped lang="scss">
.settings-form {
  max-width: 720px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 var(--space-8);
}

.switch-field {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  height: 36px;
}

.switch-hint {
  font-size: var(--font-sm);
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>

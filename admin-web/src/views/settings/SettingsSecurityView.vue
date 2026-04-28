<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { fetchAdminSettings, updateAdminSecuritySettings } from '@/api/admin'
import type { SecuritySettings } from '@/types'

const loading = ref(false)
const saving = ref(false)
const form = reactive<SecuritySettings>({
  passwordValidityDays: 90,
  loginRetryLimit: 5,
  ipWhitelist: '',
  mfaRequired: true,
})

async function loadData() {
  loading.value = true
  try {
    const payload = await fetchAdminSettings()
    Object.assign(form, payload.security)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载安全策略失败。')
  } finally {
    loading.value = false
  }
}

async function saveData() {
  saving.value = true
  try {
    await updateAdminSecuritySettings({ ...form })
    ElMessage.success('安全策略已保存。')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存安全策略失败。')
  } finally {
    saving.value = false
  }
}

void loadData()
</script>

<template>
  <PageContainer
    title="安全策略"
    description="配置密码策略、登录限制、IP 白名单和多因子认证。"
    back-to="/settings"
    back-label="返回系统设置"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">重新加载</el-button>
      <el-button type="primary" :loading="saving" @click="saveData">保存策略</el-button>
    </template>

    <el-card class="page-card" shadow="never" v-loading="loading">
      <el-form label-position="top">
        <el-form-item label="密码有效期（天）">
          <el-input-number v-model="form.passwordValidityDays" :min="1" :max="365" />
        </el-form-item>
        <el-form-item label="登录重试上限">
          <el-input-number v-model="form.loginRetryLimit" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="IP 白名单">
          <el-input v-model="form.ipWhitelist" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="启用多因子认证">
          <el-switch v-model="form.mfaRequired" />
        </el-form-item>
      </el-form>
    </el-card>
  </PageContainer>
</template>

<script setup lang="ts">
/** 安全策略设置页面：配置密码有效期、登录重试上限、IP 白名单和多因子认证开关 */
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
      <el-form label-position="top" class="settings-form">
        <div class="form-grid">
          <el-form-item label="密码有效期（天）">
            <el-input-number v-model="form.passwordValidityDays" :min="1" :max="365" class="full-width" />
          </el-form-item>
          <el-form-item label="登录重试上限">
            <el-input-number v-model="form.loginRetryLimit" :min="1" :max="10" class="full-width" />
          </el-form-item>
        </div>
        <el-form-item label="IP 白名单">
          <el-input v-model="form.ipWhitelist" type="textarea" :rows="4" placeholder="每行一个 IP 地址或 CIDR 网段" class="full-width" />
        </el-form-item>
        <el-form-item label="启用多因子认证">
          <div class="switch-field">
            <el-switch v-model="form.mfaRequired" />
            <span class="switch-hint">{{ form.mfaRequired ? '已启用多因子认证' : '未启用多因子认证' }}</span>
          </div>
        </el-form-item>
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
  margin-bottom: var(--space-2);
}

.full-width {
  width: 100%;
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

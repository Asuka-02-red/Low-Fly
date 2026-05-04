﻿<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const AUTH_FLASH_KEY = 'admin_auth_flash'
const router = useRouter()
const userStore = useUserStore()
const loginForm = ref({ username: '', password: '' })
const rememberPassword = ref(false)
const submitting = ref(false)

onMounted(() => {
  const rememberedLogin = userStore.getRememberedLogin()
  rememberPassword.value = rememberedLogin.rememberPassword
  loginForm.value.username = rememberedLogin.username
  loginForm.value.password = rememberedLogin.password

  const flash = sessionStorage.getItem(AUTH_FLASH_KEY)
  if (flash) {
    ElMessage.warning(flash)
    sessionStorage.removeItem(AUTH_FLASH_KEY)
  }
})

const handleLogin = async () => {
  if (!loginForm.value.username.trim() || !loginForm.value.password.trim()) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  submitting.value = true
  try {
    await userStore.login({
      username: loginForm.value.username.trim(),
      password: loginForm.value.password,
    })
    userStore.syncRememberedLogin(
      { username: loginForm.value.username.trim(), password: loginForm.value.password },
      rememberPassword.value,
    )
    ElMessage.success('登录成功')
    await router.push('/overview/dashboard')
  } catch (error) {
    console.error(error)
    ElMessage.error('登录失败，请检查账号密码或稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-root">
    <div class="login-bg" aria-hidden="true">
      <div class="bg-blob bg-blob--pink"></div>
      <div class="bg-blob bg-blob--mint"></div>
      <div class="bg-blob bg-blob--lavender"></div>
      <div class="bg-grid"></div>
    </div>

    <div class="login-card">
      <div class="login-header">
        <div class="login-logo" aria-hidden="true">
          <div class="logo-ring"></div>
          <svg width="30" height="30" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 19h20L12 2z" fill="currentColor" opacity="0.9"/>
            <circle cx="12" cy="17" r="2" fill="currentColor"/>
          </svg>
        </div>
        <h1>低空驿站管理系统</h1>
        <p>一站式数字化服务平台 · 管理员登录</p>
      </div>

      <el-form :model="loginForm" label-position="top" class="login-form" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入管理员账号"
            autocomplete="username"
          >
            <template #prefix>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <circle cx="8" cy="5" r="3" stroke="currentColor" stroke-width="1.5"/>
                <path d="M2 14c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <rect x="3" y="7" width="10" height="7" rx="1.5" stroke="currentColor" stroke-width="1.5"/>
                <path d="M5 7V5a3 3 0 016 0v2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </template>
          </el-input>
        </el-form-item>
        <div class="form-footer">
          <el-checkbox v-model="rememberPassword">记住密码</el-checkbox>
        </div>
        <button type="submit" class="login-btn" :disabled="submitting">
          <span v-if="submitting" class="btn-spinner"></span>
          <span v-else>登 录</span>
        </button>
      </el-form>

      <div class="login-footer">
        <p>低空驿站 v2.0 · 版权所有</p>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-root {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(150deg, #FFF0F3 0%, #F5F0F8 40%, #F2FAF5 70%, #FFF5F7 100%);
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  animation: blobFloat 20s ease-in-out infinite;
}
.bg-blob--pink {
  width: 500px;
  height: 500px;
  top: -150px;
  right: -100px;
  background: rgba(56, 189, 248, 0.25);
  animation-delay: 0s;
}
.bg-blob--mint {
  width: 400px;
  height: 400px;
  bottom: -120px;
  left: -80px;
  background: rgba(181, 234, 215, 0.25);
  animation-delay: -7s;
}
.bg-blob--lavender {
  width: 350px;
  height: 350px;
  top: 45%;
  left: 55%;
  background: rgba(199, 206, 234, 0.2);
  animation-delay: -14s;
}

@keyframes blobFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33%      { transform: translate(30px, -30px) scale(1.1); }
  66%      { transform: translate(-20px, 20px) scale(0.95); }
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(200, 185, 195, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(200, 185, 195, 0.08) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse 70% 50% at 50% 50%, black 30%, transparent 60%);
}

.login-card {
  position: relative;
  z-index: 1;
  width: 440px;
  padding: var(--space-10);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px) saturate(140%);
  -webkit-backdrop-filter: blur(24px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 8px 32px rgba(56, 189, 248, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.04);
  animation: cardEnter 0.7s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes cardEnter {
  from { opacity: 0; transform: translateY(24px) scale(0.95); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.login-header {
  text-align: center;
  margin-bottom: var(--space-8);
}

.login-logo {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  margin: 0 auto var(--space-4);
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-sky-strong), var(--color-periwinkle));
  color: #fff;
  position: relative;
}
.logo-ring {
  position: absolute;
  inset: -4px;
  border-radius: calc(var(--radius-lg) + 4px);
  border: 2px solid rgba(56, 189, 248, 0.35);
  animation: ringPulse 2.5s ease-in-out infinite;
}
@keyframes ringPulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50%      { transform: scale(1.08); opacity: 0.2; }
}

.login-header h1 {
  font-size: var(--font-xl);
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2);
  letter-spacing: -0.01em;
}
.login-header p {
  font-size: var(--font-sm);
  color: var(--color-text-muted);
  margin: 0;
}

.login-form {
  :deep(.el-form-item) { margin-bottom: var(--space-5); }
  :deep(.el-form-item__label) {
    font-size: var(--font-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
    margin-bottom: var(--space-2);
  }
}

.form-footer {
  display: flex;
  justify-content: flex-start;
  margin-bottom: var(--space-6);
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: var(--font-md);
  font-weight: 700;
  letter-spacing: 0.08em;
  border: none;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-sky-strong), var(--color-periwinkle), var(--color-blue-light));
  background-size: 200% 200%;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(56, 189, 248, 0.35);
  transition: all var(--transition-base);
  animation: bgPulse 3s ease infinite;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 24px rgba(56, 189, 248, 0.5);
  }
  &:active { transform: translateY(0); }
  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    animation: none;
  }
}
@keyframes bgPulse {
  0%, 100% { background-position: 0% 50%; }
  50%      { background-position: 100% 50%; }
}

.btn-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.login-footer {
  margin-top: var(--space-8);
  text-align: center;
  p {
    font-size: var(--font-xs);
    color: var(--color-text-caption);
    margin: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .bg-blob, .logo-ring, .login-btn { animation: none; }
  .login-card { animation: none; }
}

@media (max-width: 768px) {
  .login-card {
    width: 90%;
    max-width: 400px;
    padding: var(--space-8) var(--space-6);
  }
}
</style>

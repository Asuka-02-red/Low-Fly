<script setup lang="ts">
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
      {
        username: loginForm.value.username.trim(),
        password: loginForm.value.password,
      },
      rememberPassword.value,
    )
    ElMessage.success('登录成功')
    await router.push('/overview/dashboard')
  }
  catch (error) {
    console.error(error)
    ElMessage.error('登录失败，请检查账号密码或稍后重试')
  }
  finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="background-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    
    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="login-header">
        <h1>低空驿站管理系统</h1>
        <p>管理员登录</p>
      </div>
      
      <el-form :model="loginForm" label-position="top" class="login-form">
        <el-form-item label="账号">
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入账号" 
            class="glass-input"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码" 
            show-password 
            class="glass-input"
          />
        </el-form-item>
        
        <div class="form-footer">
          <el-checkbox v-model="rememberPassword" class="remember-checkbox">记住密码</el-checkbox>
        </div>
        
        <el-button 
          type="primary" 
          class="login-btn" 
          :loading="submitting" 
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
      
    </div>
  </div>
</template>

<style scoped>
/* 莫妮卡蓝色调 */
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #EFF6FF 0%, #FFFFFF 100%);
  overflow: hidden;
}

/* 背景装饰 */
.background-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.background-decoration .circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(96, 165, 250, 0.1);
  filter: blur(40px);
}

.background-decoration .circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -100px;
}

.background-decoration .circle-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  left: -50px;
}

.background-decoration .circle-3 {
  width: 150px;
  height: 150px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/* 登录卡片 - 玻璃拟态 */
.login-card {
  position: relative;
  z-index: 1;
  width: 450px;
  padding: 40px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.15);
  transition: all 0.3s ease;
}

.login-card:hover {
  box-shadow: 0 12px 40px 0 rgba(31, 38, 135, 0.2);
  transform: translateY(-5px);
}

/* 登录头部 */
.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #3B82F6;
  margin: 0 0 8px 0;
}

.login-header p {
  font-size: 16px;
  color: var(--color-text-muted);
  margin: 0;
}

/* 登录表单 */
.login-form .el-form-item {
  margin-bottom: 20px;
}

.login-form .el-form-item__label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

/* 玻璃输入框 */
.glass-input .el-input__wrapper {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: all 0.3s ease;
}

.glass-input .el-input__wrapper:hover {
  border-color: #60A5FA;
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.1);
}

.glass-input .el-input__wrapper.is-focus {
  border-color: #60A5FA;
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.2);
}

.glass-input .el-input__inner {
  font-size: 16px;
  color: var(--color-text-primary);
}

/* 表单底部 */
.form-footer {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 24px;
}

/* 记住密码 checkbox */
.remember-checkbox .el-checkbox__label {
  font-size: 14px;
  color: var(--color-text-muted);
}

.remember-checkbox .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #60A5FA;
  border-color: #60A5FA;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #60A5FA 0%, #3B82F6 100%);
  border: none;
  transition: all 0.3s ease;
}

.login-btn:hover {
  background: linear-gradient(135deg, #3B82F6 0%, #60A5FA 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

/* 登录页脚 */
.login-footer {
  margin-top: 32px;
  text-align: center;
}

.login-footer p {
  font-size: 12px;
  color: var(--color-text-caption);
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-card {
    width: 90%;
    max-width: 400px;
    padding: 32px 24px;
  }
  
  .login-header h1 {
    font-size: 24px;
  }
}
</style>

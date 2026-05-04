/** 用户状态管理（Pinia Store）：管理登录 Token、用户资料、记住密码等状态，提供登录、登出、获取资料和同步记住密码等操作 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getProfileApi, loginApi } from '@/api/auth'
import type { LoginPayload, UserProfile } from '@/types'

const TOKEN_KEY = 'admin_token'
const PROFILE_KEY = 'admin_profile'
const REMEMBER_PASSWORD_KEY = 'admin_remember_password'
const REMEMBER_USERNAME_KEY = 'admin_remembered_username'
const REMEMBER_VALUE_KEY = 'admin_remembered_password'

export interface RememberedLogin {
  username: string
  password: string
  rememberPassword: boolean
}

function readProfile(): UserProfile | null {
  const raw = localStorage.getItem(PROFILE_KEY)

  if (!raw) {
    return null
  }

  return JSON.parse(raw) as UserProfile
}

function readRememberedLogin(): RememberedLogin {
  const rememberPassword = localStorage.getItem(REMEMBER_PASSWORD_KEY) === 'true'

  return {
    username: rememberPassword ? (localStorage.getItem(REMEMBER_USERNAME_KEY) ?? '') : '',
    password: rememberPassword ? (localStorage.getItem(REMEMBER_VALUE_KEY) ?? '') : '',
    rememberPassword,
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const profile = ref<UserProfile | null>(readProfile())

  const isLoggedIn = computed(() => Boolean(token.value))

  function setSession(sessionToken: string, sessionProfile: UserProfile) {
    token.value = sessionToken
    profile.value = sessionProfile

    localStorage.setItem(TOKEN_KEY, sessionToken)
    localStorage.setItem(PROFILE_KEY, JSON.stringify(sessionProfile))
  }

  async function login(payload: LoginPayload) {
    const result = await loginApi(payload)
    setSession(result.token, result.user)
  }

  function getRememberedLogin() {
    return readRememberedLogin()
  }

  function syncRememberedLogin(payload: LoginPayload, rememberPassword: boolean) {
    if (rememberPassword) {
      localStorage.setItem(REMEMBER_PASSWORD_KEY, 'true')
      localStorage.setItem(REMEMBER_USERNAME_KEY, payload.username)
      localStorage.setItem(REMEMBER_VALUE_KEY, payload.password)
      return
    }

    localStorage.removeItem(REMEMBER_PASSWORD_KEY)
    localStorage.removeItem(REMEMBER_USERNAME_KEY)
    localStorage.removeItem(REMEMBER_VALUE_KEY)
  }

  async function fetchProfile() {
    if (!token.value) {
      return null
    }

    const userProfile = await getProfileApi()
    profile.value = userProfile
    localStorage.setItem(PROFILE_KEY, JSON.stringify(userProfile))

    return userProfile
  }

  function logout() {
    token.value = ''
    profile.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(PROFILE_KEY)
  }

  return {
    token,
    profile,
    isLoggedIn,
    login,
    getRememberedLogin,
    syncRememberedLogin,
    fetchProfile,
    logout,
  }
})

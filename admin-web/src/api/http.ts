/** HTTP 请求基础模块：基于 Axios 封装统一的请求实例，配置请求/响应拦截器（自动注入 Token、401 跳转登录），并提供 API 响应解包（unwrap）和模拟请求（mockRequest）工具函数 */
import axios from 'axios'

const TOKEN_KEY = 'admin_token'
const PROFILE_KEY = 'admin_profile'
const AUTH_FLASH_KEY = 'admin_auth_flash'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)

  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(PROFILE_KEY)
      sessionStorage.setItem(AUTH_FLASH_KEY, '登录已失效，请重新登录。')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  },
)

export async function unwrap<T>(promise: Promise<ApiEnvelope<T>>): Promise<T> {
  const response = await promise
  if (response.code !== 200) {
    throw new Error(response.message || '请求失败，请稍后重试。')
  }
  return response.data
}

export function mockRequest<T>(data: T, delay = 320): Promise<T> {
  return new Promise((resolve) => {
    window.setTimeout(() => {
      resolve(JSON.parse(JSON.stringify(data)) as T)
    }, delay)
  })
}

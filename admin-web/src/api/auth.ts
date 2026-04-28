import type { ApiEnvelope } from '@/api/http'
import { request } from '@/api/http'
import type { LoginPayload, LoginResult, UserProfile } from '@/types'

export async function loginApi(payload: LoginPayload): Promise<LoginResult> {
  const response = (await request.post('/auth/login', payload)) as ApiEnvelope<{
    token: string
    refreshToken: string
    userInfo: {
      realName: string
      role: string
      companyName?: string
      username: string
    }
  }>
  const data = response.data

  return {
    token: data.token,
    user: {
      name: data.userInfo.realName || data.userInfo.username,
      role: data.userInfo.role,
      organization: data.userInfo.companyName ?? '低空驿站运营中心',
      phone: '由服务端返回',
    },
  }
}

export async function getProfileApi(): Promise<UserProfile> {
  const response = (await request.get('/users/me')) as ApiEnvelope<{
    realName: string
    role: string
    companyName?: string
    username: string
    phone?: string
  }>
  const data = response.data

  return {
    name: data.realName || data.username,
    role: data.role,
    organization: data.companyName ?? '低空驿站运营中心',
    phone: data.phone ?? '未配置',
  }
}

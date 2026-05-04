/** 路由鉴权守卫单元测试：验证匿名用户重定向、已登录用户跳转、过期 Token 处理和非管理员拦截逻辑 */
import { describe, expect, it } from 'vitest'
import { resolveAuthRedirect } from '@/router/authGuard'

describe('resolveAuthRedirect', () => {
  it('redirects anonymous users to login for protected routes', () => {
    expect(resolveAuthRedirect('/overview/dashboard', null)).toBe('/login')
  })

  it('allows anonymous users to stay on login page', () => {
    expect(resolveAuthRedirect('/login', null)).toBe(true)
  })

  it('redirects logged-in users away from login page', () => {
    expect(resolveAuthRedirect('/login', 'real-token', 'ADMIN')).toBe('/overview/dashboard')
  })

  it('invalidates expired mock tokens', () => {
    expect(resolveAuthRedirect('/projects/center', 'mock-token-expired')).toBe('/login')
  })

  it('allows expired mock token cleanup on login route itself', () => {
    expect(resolveAuthRedirect('/login', 'mock-token-expired')).toBe(true)
  })

  it('redirects non-admin users away from admin routes', () => {
    expect(resolveAuthRedirect('/overview/dashboard', 'real-token', 'PILOT')).toBe('/login')
  })

  it('allows admin users to stay on protected routes', () => {
    expect(resolveAuthRedirect('/overview/dashboard', 'real-token', 'ADMIN')).toBe(true)
  })
})

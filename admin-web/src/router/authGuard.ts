/** 路由鉴权守卫：根据 Token 有效性和用户角色判断路由访问权限，处理未登录跳转、过期 Token 清理和非管理员拦截 */
export function resolveAuthRedirect(path: string, token: string | null, role?: string | null) {
  const isExpiredMockToken = Boolean(token?.startsWith('mock-token-'))
  const hasToken = Boolean(token) && !isExpiredMockToken
  const isAdminRoute = path !== '/login'
  const isAdmin = role === 'ADMIN'

  if (isExpiredMockToken) {
    return path === '/login' ? true : '/login'
  }
  if (path !== '/login' && !hasToken) {
    return '/login'
  }
  if (hasToken && isAdminRoute && !isAdmin) {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_profile')
    }
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem('admin_auth_flash', '仅管理员账号可访问 Web 管理端，请重新登录。')
    }
    return '/login'
  }
  if (path === '/login' && hasToken) {
    return '/overview/dashboard'
  }
  return true
}

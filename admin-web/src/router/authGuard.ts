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

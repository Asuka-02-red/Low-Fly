import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layout/AdminLayout.vue'
import { menuData } from '@/config/navigation'
import { resolveAuthRedirect } from '@/router/authGuard'

function createSectionRoutes() {
  return menuData.flatMap((section) => [
    ...section.children.map((child) => ({
      path: child.path.slice(1),
      name: child.path.split('/').pop() ? `${section.name}-${child.path.split('/').pop()}` : undefined,
      component: child.component,
      meta: {
        title: child.title,
        description: child.description,
        sectionName: section.name,
        icon: child.icon,
        parentPath: section.path,
        parentTitle: section.title,
      },
    })),
  ])
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/overview/dashboard',
      children: [
        ...createSectionRoutes(),
      ],
    },
    {
      path: '/overview',
      redirect: '/overview/dashboard',
    },
    {
      path: '/users',
      redirect: '/users/directory',
    },
    {
      path: '/projects',
      redirect: '/projects/center',
    },
    {
      path: '/analytics',
      redirect: '/analytics/business',
    },
    {
      path: '/settings',
      redirect: '/settings/basic',
    },
    {
      path: '/logs',
      redirect: '/logs/audit',
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/overview/dashboard',
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('admin_token')
  const profileRaw = localStorage.getItem('admin_profile')
  const role = profileRaw ? (JSON.parse(profileRaw) as { role?: string }).role ?? null : null

  if (token?.startsWith('mock-token-')) {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_profile')
    return resolveAuthRedirect(to.path, token, role)
  }

  return resolveAuthRedirect(to.path, token, role)
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '管理后台')} - 低空驿站`
})

export default router

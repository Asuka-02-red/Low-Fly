/** 导航菜单配置：定义管理后台的侧边栏菜单结构、路由映射和页面描述，供布局和路由模块统一引用 */
import type { Component } from 'vue'
import type { IconKey } from '@/types'

export interface MenuChild {
  title: string
  path: string
  component: () => Promise<Component>
  icon: IconKey
  description?: string
  tag?: string
}

export interface MenuItem {
  name: string
  path: string
  title: string
  description: string
  icon: IconKey
  children: MenuChild[]
}

export const menuData: MenuItem[] = [
  {
    name: 'overview',
    path: '/overview',
    title: '系统概览',
    description: '查看平台总览与关键状态。',
    icon: 'overview',
    children: [
      {
        title: '总览看板',
        path: '/overview/dashboard',
        component: () => import('@/views/overview/OverviewDashboardView.vue'),
        description: '展示核心指标、项目分布与近期运营动态。',
        icon: 'dashboard',
        tag: '推荐',
      },
      {
        title: '平台态势',
        path: '/overview/command',
        component: () => import('@/views/overview/OverviewCommandView.vue'),
        description: '聚合设备在线、风险等级和移动端/PC 访问态势。',
        icon: 'command',
      },
    ],
  },
  {
    name: 'users',
    path: '/users',
    title: '用户管理',
    description: '管理账号、角色与权限。',
    icon: 'users',
    children: [
      {
        title: '用户列表',
        path: '/users/directory',
        component: () => import('@/views/users/UsersDirectoryView.vue'),
        description: '统一管理管理员账号、组织归属和活跃状态。',
        icon: 'directory',
        tag: '核心',
      },
    ],
  },
  {
    name: 'projects',
    path: '/projects',
    title: '项目管理',
    description: '管理项目流程与交付状态。',
    icon: 'projects',
    children: [
      {
        title: '项目中心',
        path: '/projects/center',
        component: () => import('@/views/projects/ProjectsCenterView.vue'),
        description: '执行项目全生命周期管理和进度跟踪。',
        icon: 'folder',
        tag: '核心',
      },
      {
        title: '合规风控',
        path: '/projects/assurance',
        component: () => import('@/views/projects/ProjectsAssuranceView.vue'),
        description: '查看项目合规状态、风险分层和处置建议。',
        icon: 'shield',
      },
      {
        title: '订单记录',
        path: '/projects/orders',
        component: () => import('@/views/projects/ProjectsOrdersView.vue'),
        description: '查看并管理订单详细信息。',
        icon: 'rocket',
      },
    ],
  },
  {
    name: 'analytics',
    path: '/analytics',
    title: '数据分析',
    description: '查看经营与性能数据。',
    icon: 'analytics',
    children: [
      {
        title: '经营分析',
        path: '/analytics/business',
        component: () => import('@/views/analytics/AnalyticsBusinessView.vue'),
        description: '分析营收趋势、项目健康度和活跃用户走势。',
        icon: 'chart',
        tag: '推荐',
      },
      {
        title: '性能分析',
        path: '/analytics/performance',
        component: () => import('@/views/analytics/AnalyticsPerformanceView.vue'),
        description: '监控服务响应时间、可用性和运维负载情况。',
        icon: 'server',
      },
    ],
  },
  {
    name: 'settings',
    path: '/settings',
    title: '系统设置',
    description: '维护参数、策略与通知。',
    icon: 'settings',
    children: [
      {
        title: '基础参数',
        path: '/settings/basic',
        component: () => import('@/views/settings/SettingsBasicView.vue'),
        description: '配置平台名称、热线、默认区域与移动端看板。',
        icon: 'sliders',
        tag: '常用',
      },
      {
        title: '安全策略',
        path: '/settings/security',
        component: () => import('@/views/settings/SettingsSecurityView.vue'),
        description: '设置密码有效期、登录重试和多因子认证。',
        icon: 'lock',
      },
      {
        title: '通知规则',
        path: '/settings/notifications',
        component: () => import('@/views/settings/SettingsNotificationsView.vue'),
        description: '定义通知触达渠道、启停状态和触发场景。',
        icon: 'bell',
      },
    ],
  },
  {
    name: 'logs',
    path: '/logs',
    title: '日志管理',
    description: '查询审计留痕与导出记录。',
    icon: 'logs',
    children: [
      {
        title: '操作审计',
        path: '/logs/audit',
        component: () => import('@/views/logs/LogsAuditView.vue'),
        description: '筛选管理员关键操作记录并查看执行结果。',
        icon: 'audit',
        tag: '核心',
      },
      {
        title: '反馈工单',
        path: '/logs/workorders',
        component: () => import('@/views/logs/LogsWorkOrdersView.vue'),
        description: '查看用户反馈、回复工单并完成关闭处理。',
        icon: 'bell',
      },
      {
        title: '导出中心',
        path: '/logs/export',
        component: () => import('@/views/logs/LogsExportView.vue'),
        description: '按筛选条件导出日志数据与测试功能清单。',
        icon: 'download',
      },
    ],
  },
]

export function findSectionByName(name: string) {
  return menuData.find((item) => item.name === name)
}

export function findSectionByPath(path: string) {
  return menuData.find((item) => path === item.path || path.startsWith(`${item.path}/`))
}

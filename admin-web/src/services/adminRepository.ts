/** 管理后台数据仓库：提供本地模拟的后端数据服务，包含用户、角色、权限组、项目、设置和审计日志的增删改查，支持 localStorage 持久化和种子数据注入 */
import type {
  AnalyticsPayload,
  AuditLogRecord,
  BasicSettings,
  ExportPackage,
  NotificationRule,
  OverviewPayload,
  PermissionGroup,
  ProjectRecord,
  RoleRecord,
  SecuritySettings,
  SummaryMetric,
  UserRecord,
  UserView,
} from '@/types'

const STORAGE_KEY = 'admin_repository_state'

interface AdminState {
  users: UserRecord[]
  roles: RoleRecord[]
  permissionGroups: PermissionGroup[]
  projects: ProjectRecord[]
  basicSettings: BasicSettings
  securitySettings: SecuritySettings
  notificationRules: NotificationRule[]
  auditLogs: AuditLogRecord[]
}

type StorageLike = Pick<Storage, 'getItem' | 'setItem'>

export interface ProjectDraft {
  name: string
  owner: string
  region: string
  budget: number
  status: ProjectRecord['status']
}

interface AuditInput {
  module: string
  action: string
  detail: string
  result?: AuditLogRecord['result']
}

function deepClone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function now() {
  return new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
}

function delay(ms = 220) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

function createDefaultState(): AdminState {
  return {
    users: [
      {
        id: 'U-001',
        name: '陈思远',
        organization: '低空驿站运营中心',
        phone: '13800001201',
        status: '启用',
        lastActiveAt: '2026-04-21 18:25:00',
        roleIds: ['role-super-admin'],
        permissionGroupId: 'group-full-control',
      },
      {
        id: 'U-002',
        name: '林雨桐',
        organization: '深圳南山站',
        phone: '13800001202',
        status: '启用',
        lastActiveAt: '2026-04-21 17:42:00',
        roleIds: ['role-project-manager'],
        permissionGroupId: 'group-project-control',
      },
      {
        id: 'U-003',
        name: '周景文',
        organization: '珠海横琴站',
        phone: '13800001203',
        status: '待审核',
        lastActiveAt: '2026-04-21 16:14:00',
        roleIds: ['role-auditor'],
        permissionGroupId: 'group-audit-readonly',
      },
      {
        id: 'U-004',
        name: '黄诗晴',
        organization: '广州黄埔站',
        phone: '13800001204',
        status: '停用',
        lastActiveAt: '2026-04-20 14:08:00',
        roleIds: ['role-ops'],
        permissionGroupId: 'group-project-control',
      },
    ],
    roles: [
      {
        id: 'role-super-admin',
        name: '超级管理员',
        description: '拥有所有模块访问权限与配置权限。',
        permissions: ['*'],
        userCount: 1,
      },
      {
        id: 'role-project-manager',
        name: '项目管理员',
        description: '负责项目生命周期、培训与结算协调。',
        permissions: ['project:create', 'project:update', 'project:status', 'analytics:view'],
        userCount: 1,
      },
      {
        id: 'role-auditor',
        name: '审计员',
        description: '负责权限复核、日志审计与异常追踪。',
        permissions: ['user:review', 'logs:view', 'logs:export'],
        userCount: 1,
      },
      {
        id: 'role-ops',
        name: '运维工程师',
        description: '负责配置发布、系统维护与性能监控。',
        permissions: ['settings:update', 'analytics:performance', 'logs:view'],
        userCount: 1,
      },
    ],
    permissionGroups: [
      {
        id: 'group-full-control',
        name: '全量管控组',
        description: '适用于平台核心管理员。',
        permissions: ['用户全量维护', '项目全生命周期', '系统配置发布', '日志导出'],
      },
      {
        id: 'group-project-control',
        name: '项目运营组',
        description: '适用于区域项目负责人。',
        permissions: ['项目创建编辑', '项目状态变更', '培训支付跟进', '经营报表查看'],
      },
      {
        id: 'group-audit-readonly',
        name: '审计只读组',
        description: '适用于风控与审计复核角色。',
        permissions: ['权限组查看', '日志筛选', '日志导出', '风险记录跟踪'],
      },
    ],
    projects: [
      {
        id: 'P-2026-001',
        name: '深圳南山城市巡检',
        owner: '林雨桐',
        region: '深圳南山',
        status: '执行中',
        progress: 82,
        budget: 286000,
        complianceStatus: '正常',
        riskLevel: '低',
        trainingCompletion: 96,
        paymentStatus: '部分结算',
        updatedAt: '2026-04-21 17:30:00',
      },
      {
        id: 'P-2026-002',
        name: '珠海横琴跨区物流',
        owner: '周景文',
        region: '珠海横琴',
        status: '规划中',
        progress: 34,
        budget: 318000,
        complianceStatus: '待复核',
        riskLevel: '中',
        trainingCompletion: 72,
        paymentStatus: '待结算',
        updatedAt: '2026-04-21 15:40:00',
      },
      {
        id: 'P-2026-003',
        name: '广州黄埔应急演练',
        owner: '陈思远',
        region: '广州黄埔',
        status: '已完成',
        progress: 100,
        budget: 198000,
        complianceStatus: '正常',
        riskLevel: '低',
        trainingCompletion: 100,
        paymentStatus: '已结算',
        updatedAt: '2026-04-20 19:12:00',
      },
      {
        id: 'P-2026-004',
        name: '佛山顺德园区配送',
        owner: '黄诗晴',
        region: '佛山顺德',
        status: '已暂停',
        progress: 56,
        budget: 245000,
        complianceStatus: '高风险',
        riskLevel: '高',
        trainingCompletion: 68,
        paymentStatus: '待结算',
        updatedAt: '2026-04-21 11:25:00',
      },
    ],
    basicSettings: {
      stationName: '低空驿站一站式数字化服务平台',
      serviceHotline: '400-820-2026',
      defaultRegion: '深圳南山',
      mobileDashboardEnabled: true,
    },
    securitySettings: {
      passwordValidityDays: 90,
      loginRetryLimit: 5,
      ipWhitelist: '10.0.0.0/24, 172.16.8.0/24',
      mfaRequired: true,
    },
    notificationRules: [
      {
        id: 'notice-001',
        name: '项目逾期预警',
        channel: '站内信',
        enabled: true,
        trigger: '项目进度低于计划 10%',
      },
      {
        id: 'notice-002',
        name: '高风险事件升级',
        channel: '短信',
        enabled: true,
        trigger: '风险等级为高且 30 分钟未处理',
      },
      {
        id: 'notice-003',
        name: '配置发布完成',
        channel: '邮件',
        enabled: false,
        trigger: '系统配置发布后通知运维负责人',
      },
    ],
    auditLogs: [
      {
        id: 'LOG-001',
        time: '2026-04-21 18:20:00',
        module: '登录认证',
        action: '管理员登录',
        operator: '平台管理员',
        result: '成功',
        detail: '通过 Web 管理端完成登录。',
      },
      {
        id: 'LOG-002',
        time: '2026-04-21 17:50:00',
        module: '项目管理',
        action: '变更项目状态',
        operator: '平台管理员',
        result: '成功',
        detail: '将项目 P-2026-001 状态更新为执行中。',
      },
      {
        id: 'LOG-003',
        time: '2026-04-21 17:15:00',
        module: '用户权限',
        action: '更新权限组',
        operator: '平台管理员',
        result: '成功',
        detail: '为用户 U-002 指定项目运营组。',
      },
    ],
  }
}

export class AdminRepository {
  private state: AdminState
  private readonly storage?: StorageLike

  constructor(storage?: StorageLike) {
    this.storage = storage
    this.state = this.loadState()
  }

  private loadState() {
    const raw = this.storage?.getItem(STORAGE_KEY)
    if (!raw) {
      return createDefaultState()
    }

    try {
      return {
        ...createDefaultState(),
        ...(JSON.parse(raw) as AdminState),
      }
    } catch {
      return createDefaultState()
    }
  }

  private persist() {
    this.storage?.setItem(STORAGE_KEY, JSON.stringify(this.state))
  }

  reset() {
    this.state = createDefaultState()
    this.persist()
  }

  hydrate(seed: Partial<AdminState>) {
    this.state = {
      ...createDefaultState(),
      ...deepClone(seed),
    }
  }

  private syncRoleUserCount() {
    this.state.roles = this.state.roles.map((role) => ({
      ...role,
      userCount: this.state.users.filter((user) => user.roleIds.includes(role.id)).length,
    }))
  }

  private appendAuditLog(input: AuditInput) {
    this.state.auditLogs.unshift({
      id: `LOG-${Date.now()}`,
      time: now(),
      module: input.module,
      action: input.action,
      operator: '平台管理员',
      result: input.result ?? '成功',
      detail: input.detail,
    })
    this.persist()
  }

  private mapUsers(): UserView[] {
    return this.state.users.map((user) => {
      const roleNames = this.state.roles
        .filter((role) => user.roleIds.includes(role.id))
        .map((role) => role.name)
      const permissionGroup = this.state.permissionGroups.find(
        (group) => group.id === user.permissionGroupId,
      )

      return {
        ...user,
        username: user.id,
        email: '未配置',
        roleNames,
        permissionGroupName: permissionGroup?.name ?? '未分配',
        roleCode: 'ADMIN',
      }
    })
  }

  async getOverview(): Promise<OverviewPayload> {
    await delay()

    const totalBudget = this.state.projects.reduce((sum, project) => sum + project.budget, 0)
    const activeProjects = this.state.projects.filter((project) => project.status === '执行中').length
    const pendingUsers = this.state.users.filter((user) => user.status === '待审核').length

    return {
      metrics: [
        { label: '进行中项目', value: String(activeProjects), trend: '本周新增 2 个', status: 'success' },
        { label: '待审核账号', value: String(pendingUsers), trend: '需尽快完成权限配置', status: 'warning' },
        {
          label: '总预算规模',
          value: `¥ ${(totalBudget / 10000).toFixed(1)} 万`,
          trend: '项目预算持续增长',
          status: 'info',
        },
        {
          label: '审计留痕',
          value: `${this.state.auditLogs.length} 条`,
          trend: '关键操作已全量记录',
          status: 'success',
        },
      ],
      deviceStats: [
        { label: 'PC 端访问占比', value: '68%', trend: '后台主力终端', status: 'success' },
        { label: '移动端访问占比', value: '32%', trend: '移动巡检持续增长', status: 'info' },
        { label: '在线服务实例', value: '23', trend: '运行状态稳定', status: 'success' },
      ],
      activities: this.state.auditLogs.slice(0, 5).map((log) => ({
        title: `${log.module} / ${log.action}`,
        content: log.detail,
        time: log.time.slice(11),
        tag: log.result,
      })),
      notices: [
        { title: '高风险项目需要在 24 小时内完成负责人复核。', level: '高', time: '今天' },
        { title: '移动端概览看板已开启响应式优化。', level: '中', time: '今天' },
        { title: '审计日志支持 CSV 导出。', level: '低', time: '昨天' },
      ],
      projectDistribution: this.state.projects.map((project) => ({
        name: project.region,
        value: project.progress,
      })),
      progressTrend: [
        { label: '周一', value: 63 },
        { label: '周二', value: 68 },
        { label: '周三', value: 70 },
        { label: '周四', value: 74 },
        { label: '周五', value: 79 },
        { label: '周六', value: 82 },
      ],
    }
  }

  async listUsers(): Promise<{
    users: UserView[]
    roles: RoleRecord[]
    permissionGroups: PermissionGroup[]
    metrics: SummaryMetric[]
  }> {
    await delay()
    this.syncRoleUserCount()

    return {
      users: this.mapUsers(),
      roles: deepClone(this.state.roles),
      permissionGroups: deepClone(this.state.permissionGroups),
      metrics: [
        { label: '活跃管理员', value: '26', trend: '本周新增 3 人', status: 'success' },
        {
          label: '待审核账号',
          value: String(this.state.users.filter((user) => user.status === '待审核').length),
          trend: '需完成权限审批',
          status: 'warning',
        },
        { label: '角色模板', value: String(this.state.roles.length), trend: '覆盖 4 类岗位', status: 'info' },
      ],
    }
  }

  async assignUserRoles(userId: string, roleIds: string[]) {
    await delay()

    const user = this.state.users.find((item) => item.id === userId)
    if (!user) {
      throw new Error('未找到目标用户，无法分配角色。')
    }

    user.roleIds = [...roleIds]
    user.status = '启用'
    user.lastActiveAt = now()
    this.syncRoleUserCount()
    this.persist()
    this.appendAuditLog({
      module: '用户权限',
      action: '角色分配',
      detail: `为用户 ${user.name} 重新分配角色，共 ${roleIds.length} 个。`,
    })
  }

  async updateUserPermissionGroup(userId: string, permissionGroupId: string) {
    await delay()

    const user = this.state.users.find((item) => item.id === userId)
    const group = this.state.permissionGroups.find((item) => item.id === permissionGroupId)
    if (!user || !group) {
      throw new Error('权限组不存在，无法保存当前配置。')
    }

    user.permissionGroupId = permissionGroupId
    user.lastActiveAt = now()
    this.persist()
    this.appendAuditLog({
      module: '用户权限',
      action: '更新权限组',
      detail: `将用户 ${user.name} 权限组调整为 ${group.name}。`,
    })
  }

  async listProjects(): Promise<{
    projects: ProjectRecord[]
    metrics: SummaryMetric[]
  }> {
    await delay()

    const executing = this.state.projects.filter((project) => project.status === '执行中').length
    const highRisk = this.state.projects.filter((project) => project.riskLevel === '高').length

    return {
      projects: deepClone(this.state.projects),
      metrics: [
        { label: '项目总数', value: String(this.state.projects.length), trend: '覆盖 4 个区域', status: 'success' },
        { label: '执行中项目', value: String(executing), trend: '推进节奏稳定', status: 'info' },
        { label: '高风险项目', value: String(highRisk), trend: '需要重点跟踪', status: 'warning' },
      ],
    }
  }

  async createProject(draft: ProjectDraft) {
    await delay()

    if (!draft.name.trim() || !draft.owner.trim()) {
      throw new Error('项目名称和负责人不能为空。')
    }

    const project: ProjectRecord = {
      id: `P-${new Date().getFullYear()}-${String(this.state.projects.length + 1).padStart(3, '0')}`,
      name: draft.name.trim(),
      owner: draft.owner.trim(),
      region: draft.region.trim(),
      status: draft.status,
      progress: draft.status === '已完成' ? 100 : draft.status === '执行中' ? 30 : 10,
      budget: draft.budget,
      complianceStatus: '待复核',
      riskLevel: '中',
      trainingCompletion: 0,
      paymentStatus: '待结算',
      updatedAt: now(),
    }

    this.state.projects.unshift(project)
    this.persist()
    this.appendAuditLog({
      module: '项目管理',
      action: '创建项目',
      detail: `创建项目 ${project.name}，区域 ${project.region}。`,
    })

    return deepClone(project)
  }

  async updateProject(projectId: string, draft: ProjectDraft) {
    await delay()

    const project = this.state.projects.find((item) => item.id === projectId)
    if (!project) {
      throw new Error('项目不存在，无法保存。')
    }

    Object.assign(project, {
      name: draft.name.trim(),
      owner: draft.owner.trim(),
      region: draft.region.trim(),
      budget: draft.budget,
      status: draft.status,
      updatedAt: now(),
    })

    if (project.status === '已完成') {
      project.progress = 100
      project.paymentStatus = '已结算'
      project.trainingCompletion = 100
      project.complianceStatus = '正常'
    }

    this.persist()
    this.appendAuditLog({
      module: '项目管理',
      action: '编辑项目',
      detail: `更新项目 ${project.name} 的基础信息。`,
    })
  }

  async deleteProject(projectId: string) {
    await delay()

    const project = this.state.projects.find((item) => item.id === projectId)
    if (!project) {
      throw new Error('项目不存在，无法删除。')
    }

    this.state.projects = this.state.projects.filter((item) => item.id !== projectId)
    this.persist()
    this.appendAuditLog({
      module: '项目管理',
      action: '删除项目',
      detail: `删除项目 ${project.name}。`,
    })
  }

  async changeProjectStatus(projectId: string, status: ProjectRecord['status']) {
    await delay()

    const project = this.state.projects.find((item) => item.id === projectId)
    if (!project) {
      throw new Error('目标项目不存在，状态切换失败。')
    }

    project.status = status
    project.updatedAt = now()
    if (status === '执行中' && project.progress < 30) {
      project.progress = 30
    }
    if (status === '已完成') {
      project.progress = 100
      project.trainingCompletion = 100
      project.complianceStatus = '正常'
      project.paymentStatus = '已结算'
    }
    this.persist()
    this.appendAuditLog({
      module: '项目管理',
      action: '变更项目状态',
      detail: `将项目 ${project.name} 状态更新为 ${status}。`,
    })
  }

  async getAnalytics(): Promise<AnalyticsPayload> {
    await delay()

    return {
      businessMetrics: [
        { label: '月度营收', value: '¥ 328.6 万', trend: '同比 +21.4%', status: 'success' },
        { label: '项目健康度', value: '91.2%', trend: '较上月提升 3.2%', status: 'success' },
        { label: '用户活跃度', value: '82%', trend: '移动端活跃占比提升', status: 'info' },
      ],
      performanceMetrics: [
        { label: '平均响应时间', value: '186 ms', trend: '较昨日优化 12 ms', status: 'success' },
        { label: '系统可用性', value: '99.94%', trend: '核心服务运行稳定', status: 'success' },
        { label: '告警闭环时长', value: '34 分钟', trend: '仍有优化空间', status: 'warning' },
      ],
      revenueTrend: [
        { label: '1 月', value: 128 },
        { label: '2 月', value: 156 },
        { label: '3 月', value: 184 },
        { label: '4 月', value: 212 },
      ],
      userActivity: [
        { label: '周一', value: 228 },
        { label: '周二', value: 246 },
        { label: '周三', value: 261 },
        { label: '周四', value: 274 },
        { label: '周五', value: 289 },
      ],
      projectHealth: [
        { name: '健康项目', value: this.state.projects.filter((item) => item.riskLevel === '低').length },
        { name: '需关注项目', value: this.state.projects.filter((item) => item.riskLevel === '中').length },
        { name: '高风险项目', value: this.state.projects.filter((item) => item.riskLevel === '高').length },
      ],
      servicePerformance: [
        { label: '认证服务', response: 132, availability: 99.98 },
        { label: '项目服务', response: 186, availability: 99.92 },
        { label: '日志服务', response: 154, availability: 99.96 },
        { label: '分析服务', response: 221, availability: 99.88 },
      ],
      operatorLoad: [
        { name: '监控告警', value: 34 },
        { name: '权限复核', value: 21 },
        { name: '配置发布', value: 17 },
        { name: '项目跟进', value: 28 },
      ],
    }
  }

  async getSettings() {
    await delay()

    return {
      basic: deepClone(this.state.basicSettings),
      security: deepClone(this.state.securitySettings),
      notifications: deepClone(this.state.notificationRules),
    }
  }

  async saveBasicSettings(payload: BasicSettings) {
    await delay()

    this.state.basicSettings = deepClone(payload)
    this.persist()
    this.appendAuditLog({
      module: '系统设置',
      action: '保存基础参数',
      detail: `更新平台名称为 ${payload.stationName}，默认区域为 ${payload.defaultRegion}。`,
    })
  }

  async saveSecuritySettings(payload: SecuritySettings) {
    await delay()

    if (payload.loginRetryLimit < 1 || payload.passwordValidityDays < 1) {
      throw new Error('安全策略参数必须大于 0。')
    }

    this.state.securitySettings = deepClone(payload)
    this.persist()
    this.appendAuditLog({
      module: '系统设置',
      action: '保存安全策略',
      detail: `更新登录重试上限为 ${payload.loginRetryLimit}，MFA ${payload.mfaRequired ? '启用' : '关闭'}。`,
    })
  }

  async saveNotificationRules(rules: NotificationRule[]) {
    await delay()

    this.state.notificationRules = deepClone(rules)
    this.persist()
    this.appendAuditLog({
      module: '系统设置',
      action: '保存通知规则',
      detail: `更新通知规则 ${rules.length} 条。`,
    })
  }

  async listAuditLogs(keyword = '') {
    await delay()

    const text = keyword.trim().toLowerCase()
    const logs = !text
      ? this.state.auditLogs
      : this.state.auditLogs.filter((item) =>
          [item.module, item.action, item.operator, item.result, item.detail]
            .join(' ')
            .toLowerCase()
            .includes(text),
        )

    return deepClone(logs)
  }

  async exportAuditLogs(keyword = ''): Promise<ExportPackage> {
    await delay()
    const logs = await this.listAuditLogs(keyword)
    const header = '时间,模块,动作,操作人,结果,详情'
    const rows = logs.map((item) =>
      [item.time, item.module, item.action, item.operator, item.result, item.detail]
        .map((cell) => `"${String(cell).replace(/"/g, '""')}"`)
        .join(','),
    )

    this.appendAuditLog({
      module: '日志管理',
      action: '导出日志',
      detail: `导出日志 ${logs.length} 条。`,
    })

    return {
      fileName: `admin-audit-${Date.now()}.csv`,
      content: [header, ...rows].join('\n'),
    }
  }
}

const browserStorage =
  typeof window !== 'undefined' && window.localStorage ? window.localStorage : undefined

export const adminRepository = new AdminRepository(browserStorage)

export function createAdminRepository(seed?: Partial<AdminState>) {
  const repository = new AdminRepository()
  if (seed) {
    repository.hydrate(seed)
  }
  return repository
}

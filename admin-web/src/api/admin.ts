import { request, unwrap } from '@/api/http'
import type { ApiEnvelope } from '@/api/http'
import type {
  AdminOrderRecord,
  AdminUserFormPayload,
  AnalyticsPayload,
  AuditLogRecord,
  BasicSettings,
  ExportPackage,
  FeedbackTicketRecord,
  NotificationRule,
  OverviewPayload,
  ProjectRecord,
  RealtimeWeatherPayload,
  SecuritySettings,
  SectionSummary,
  SummaryMetric,
  UserView,
} from '@/types'

export async function fetchSectionSummaries(): Promise<SectionSummary[]> {
  return unwrap(request.get('/admin/sections-summary') as Promise<ApiEnvelope<SectionSummary[]>>)
}

interface AdminMetricDto {
  label: string
  value: string
  trend: string
  status?: SummaryMetric['status']
}

interface AdminOverviewDto {
  metrics: AdminMetricDto[]
  deviceStats: AdminMetricDto[]
  activities: Array<{
    title: string
    content: string
    time: string
    tag: string
  }>
  notices: Array<{
    title: string
    level: string
    time: string
  }>
  projectDistribution: Array<{ name: string; value: number }>
  progressTrend: Array<{ label: string; value: number }>
}

interface AdminFlightConditionCheckDto {
  label: string
  currentValue: string
  threshold: string
  passed: boolean
}

interface AdminFlightSuitabilityDto {
  result: RealtimeWeatherPayload['suitability']['result']
  level: string
  summary: string
  checks: AdminFlightConditionCheckDto[]
  conditionNotes: string[]
  recommendations: string[]
}

interface AdminRealtimeWeatherDto {
  serviceName: string
  locationName: string
  adcode: string
  weather: string
  reportTime: string
  fetchedAt: string
  refreshInterval: string
  temperature: number
  humidity: number
  windDirection: string
  windPower: string
  windSpeed: number
  visibility: number
  precipitationProbability: number
  precipitationIntensity: number
  thunderstormRisk: string
  sourceNote: string
  suitability: AdminFlightSuitabilityDto
}

interface AdminUserDto {
  id: string
  username: string
  email: string
  name: string
  organization: string
  phone: string
  status: UserView['status']
  createTime: string
  roleNames: string[]
  permissionGroupName: string
  roleCode: UserView['roleCode']
}

interface AdminProjectDto {
  id: string
  name: string
  owner: string
  region: string
  status: ProjectRecord['status']
  progress: number
  budget: number
  complianceStatus: ProjectRecord['complianceStatus']
  riskLevel: ProjectRecord['riskLevel']
  trainingCompletion: number
  paymentStatus: ProjectRecord['paymentStatus']
  updatedAt: string
}

interface AdminAuditDto {
  requestId: string
  actorUserId: number | null
  actorRole: string | null
  bizType: string
  bizId: string
  eventType: string
  payload: string
  createTime: string
}

interface AdminFeedbackTicketDto {
  id: number
  ticketNo: string
  submitterName: string
  submitterRole: string
  contact: string
  detail: string
  status: FeedbackTicketRecord['status']
  reply: string
  createTime: string
  updateTime: string
  closedTime: string
}

interface AdminTrendPointDto {
  label: string
  value: number
}

interface AdminDistributionDto {
  name: string
  value: number
}

interface AdminServiceMetricDto {
  label: string
  response: number
  availability: number
}

interface AdminAnalyticsDto {
  businessMetrics: AdminMetricDto[]
  performanceMetrics: AdminMetricDto[]
  revenueTrend: AdminTrendPointDto[]
  userActivity: AdminTrendPointDto[]
  projectHealth: AdminDistributionDto[]
  servicePerformance: AdminServiceMetricDto[]
  operatorLoad: AdminDistributionDto[]
}

interface AdminBasicSettingsDto {
  stationName: string
  serviceHotline: string
  defaultRegion: string
  mobileDashboardEnabled: boolean
}

interface AdminSecuritySettingsDto {
  passwordValidityDays: number
  loginRetryLimit: number
  ipWhitelist: string
  mfaRequired: boolean
}

interface AdminNotificationRuleDto {
  id: string
  name: string
  channel: NotificationRule['channel']
  enabled: boolean
  trigger: string
}

interface AdminSettingsDto {
  basic: AdminBasicSettingsDto
  security: AdminSecuritySettingsDto
  notifications: AdminNotificationRuleDto[]
}

interface AdminOrderDto {
  id: string
  orderNo: string
  projectName: string
  amount: number
  status: AdminOrderRecord['status']
  createTime: string
  paymentMethod: string
  details: string
}

function normalizeMetric(item: AdminMetricDto): SummaryMetric {
  return {
    label: item.label,
    value: item.value,
    trend: item.trend,
    status: item.status ?? 'info',
  }
}

export async function fetchAdminOverview(): Promise<OverviewPayload> {
  const data = (await unwrap(
    request.get('/admin/overview') as Promise<ApiEnvelope<AdminOverviewDto>>,
  )) ?? {
    metrics: [],
    deviceStats: [],
    activities: [],
    notices: [],
    projectDistribution: [],
    progressTrend: [],
  }

  return {
    metrics: Array.isArray(data.metrics) ? data.metrics.map(normalizeMetric) : [],
    deviceStats: Array.isArray(data.deviceStats) ? data.deviceStats.map(normalizeMetric) : [],
    activities: Array.isArray(data.activities) ? data.activities : [],
    notices: Array.isArray(data.notices) ? data.notices : [],
    projectDistribution: Array.isArray(data.projectDistribution) ? data.projectDistribution : [],
    progressTrend: Array.isArray(data.progressTrend) ? data.progressTrend : [],
  }
}

export async function fetchAdminRealtimeWeather(payload: {
  longitude: number
  latitude: number
}): Promise<RealtimeWeatherPayload | null> {
  const data = await unwrap(
    request.get('/admin/weather/realtime', {
      params: payload,
    }) as Promise<ApiEnvelope<AdminRealtimeWeatherDto>>,
  )

  if (!data) {
    return null
  }

  return {
    serviceName: data.serviceName ?? '实时天气',
    locationName: data.locationName ?? '当前位置',
    adcode: data.adcode ?? '',
    weather: data.weather ?? '未知',
    reportTime: data.reportTime ?? '-',
    fetchedAt: data.fetchedAt ?? '-',
    refreshInterval: data.refreshInterval ?? '15 分钟自动刷新',
    temperature: typeof data.temperature === 'number' ? data.temperature : 0,
    humidity: typeof data.humidity === 'number' ? data.humidity : 0,
    windDirection: data.windDirection ?? '未知',
    windPower: data.windPower ?? '0',
    windSpeed: typeof data.windSpeed === 'number' ? data.windSpeed : 0,
    visibility: typeof data.visibility === 'number' ? data.visibility : 0,
    precipitationProbability: typeof data.precipitationProbability === 'number' ? data.precipitationProbability : 0,
    precipitationIntensity: typeof data.precipitationIntensity === 'number' ? data.precipitationIntensity : 0,
    thunderstormRisk: data.thunderstormRisk ?? '未知',
    sourceNote: data.sourceNote ?? '天气数据暂未返回完整说明。',
    suitability: {
      result: data.suitability?.result ?? '不适宜飞行',
      level: data.suitability?.level ?? '待评估',
      summary: data.suitability?.summary ?? '天气服务未返回完整适飞评估，请稍后重试。',
      checks: Array.isArray(data.suitability?.checks) ? data.suitability.checks : [],
      conditionNotes: Array.isArray(data.suitability?.conditionNotes)
        ? data.suitability.conditionNotes
        : [],
      recommendations: Array.isArray(data.suitability?.recommendations)
        ? data.suitability.recommendations
        : [],
    },
  }
}

export async function fetchAdminAnalytics(): Promise<AnalyticsPayload> {
  const data = await unwrap(
    request.get('/admin/analytics') as Promise<ApiEnvelope<AdminAnalyticsDto>>,
  )

  const mapMetric = (item: AdminMetricDto): SummaryMetric => ({
    label: item.label,
    value: item.value,
    trend: item.trend,
    status: item.status ?? 'info',
  })

  return {
    businessMetrics: Array.isArray(data.businessMetrics) ? data.businessMetrics.map(mapMetric) : [],
    performanceMetrics: Array.isArray(data.performanceMetrics) ? data.performanceMetrics.map(mapMetric) : [],
    revenueTrend: Array.isArray(data.revenueTrend) ? data.revenueTrend : [],
    userActivity: Array.isArray(data.userActivity) ? data.userActivity : [],
    projectHealth: Array.isArray(data.projectHealth) ? data.projectHealth : [],
    servicePerformance: Array.isArray(data.servicePerformance) ? data.servicePerformance : [],
    operatorLoad: Array.isArray(data.operatorLoad) ? data.operatorLoad : [],
  }
}

export async function fetchAdminUsers(): Promise<UserView[]> {
  const data = await unwrap(request.get('/admin/users') as Promise<ApiEnvelope<AdminUserDto[] | null>>)
  const items = Array.isArray(data) ? data : []

  return items.map((item) => {
    const roleNames = Array.isArray(item.roleNames) ? item.roleNames : []

    return {
      id: item.id,
      username: item.username,
      email: item.email ?? '未配置',
      name: item.name,
      organization: item.organization ?? '-',
      phone: item.phone ?? '-',
      status: item.status,
      lastActiveAt: item.createTime,
      roleIds: roleNames,
      permissionGroupId: item.permissionGroupName ?? '未分配',
      roleNames,
      permissionGroupName: item.permissionGroupName ?? '未分配',
      roleCode: item.roleCode,
    }
  })
}

function normalizeAdminUser(item: AdminUserDto): UserView {
  const roleNames = Array.isArray(item.roleNames) ? item.roleNames : []

  return {
    id: item.id,
    username: item.username,
    email: item.email ?? '未配置',
    name: item.name,
    organization: item.organization ?? '-',
    phone: item.phone ?? '-',
    status: item.status,
    lastActiveAt: item.createTime,
    roleIds: roleNames,
    permissionGroupId: item.permissionGroupName ?? '未分配',
    roleNames,
    permissionGroupName: item.permissionGroupName ?? '未分配',
    roleCode: item.roleCode,
  }
}

export async function createAdminUser(payload: AdminUserFormPayload): Promise<UserView> {
  const data = await unwrap(
    request.post('/admin/users', payload) as Promise<ApiEnvelope<AdminUserDto>>,
  )
  return normalizeAdminUser(data)
}

export async function updateAdminUser(userId: string, payload: AdminUserFormPayload): Promise<UserView> {
  const data = await unwrap(
    request.put(`/admin/users/${userId}`, payload) as Promise<ApiEnvelope<AdminUserDto>>,
  )
  return normalizeAdminUser(data)
}

export async function deleteAdminUser(userId: string): Promise<void> {
  await unwrap(
    request.delete(`/admin/users/${userId}`) as Promise<ApiEnvelope<null>>,
  )
}

export async function fetchAdminProjects(): Promise<ProjectRecord[]> {
  const data = await unwrap(
    request.get('/admin/projects') as Promise<ApiEnvelope<AdminProjectDto[]>>,
  )

  return data.map((item) => ({
    id: item.id,
    name: item.name,
    owner: item.owner,
    region: item.region,
    status: item.status,
    progress: item.progress,
    budget: item.budget,
    complianceStatus: item.complianceStatus,
    riskLevel: item.riskLevel,
    trainingCompletion: item.trainingCompletion,
    paymentStatus: item.paymentStatus,
    updatedAt: item.updatedAt,
  }))
}

export async function fetchAdminOrders(): Promise<AdminOrderRecord[]> {
  const data = await unwrap(
    request.get('/admin/orders') as Promise<ApiEnvelope<AdminOrderDto[]>>,
  )

  const items = Array.isArray(data) ? data : []
  return items.map((item) => ({
    id: item.id,
    orderNo: item.orderNo,
    projectName: item.projectName,
    amount: item.amount,
    status: item.status,
    createTime: item.createTime,
    paymentMethod: item.paymentMethod,
    details: item.details,
  }))
}

export async function fetchAdminOrderDetail(orderId: string): Promise<AdminOrderRecord> {
  const data = await unwrap(
    request.get(`/admin/orders/${orderId}`) as Promise<ApiEnvelope<AdminOrderDto>>,
  )

  return {
    id: data.id,
    orderNo: data.orderNo,
    projectName: data.projectName,
    amount: data.amount,
    status: data.status,
    createTime: data.createTime,
    paymentMethod: data.paymentMethod,
    details: data.details,
  }
}

export async function fetchAdminAuditLogs(): Promise<AuditLogRecord[]> {
  const data = await unwrap(
    request.get('/admin/audit-events') as Promise<ApiEnvelope<AdminAuditDto[]>>,
  )

  return data.map((item) => ({
    id: item.requestId || `${item.bizType}-${item.bizId}-${item.eventType}`,
    time: item.createTime?.replace('T', ' ') ?? '-',
    module: item.bizType,
    action: item.eventType,
    operator: item.actorRole || 'SYSTEM',
    result: '成功',
    detail: item.payload,
  }))
}

export async function fetchAdminSettings(): Promise<{
  basic: BasicSettings
  security: SecuritySettings
  notifications: NotificationRule[]
}> {
  const data = await unwrap(
    request.get('/admin/settings') as Promise<ApiEnvelope<AdminSettingsDto>>,
  )

  return {
    basic: data.basic,
    security: data.security,
    notifications: Array.isArray(data.notifications) ? data.notifications : [],
  }
}

export async function updateAdminBasicSettings(payload: BasicSettings): Promise<void> {
  await unwrap(
    request.put('/admin/settings/basic', payload) as Promise<ApiEnvelope<AdminSettingsDto>>,
  )
}

export async function updateAdminSecuritySettings(payload: SecuritySettings): Promise<void> {
  await unwrap(
    request.put('/admin/settings/security', payload) as Promise<ApiEnvelope<AdminSettingsDto>>,
  )
}

export async function updateAdminNotificationRules(rules: NotificationRule[]): Promise<void> {
  await unwrap(
    request.put('/admin/settings/notifications', { rules }) as Promise<ApiEnvelope<AdminSettingsDto>>,
  )
}

export async function exportAdminAuditLogs(keyword = ''): Promise<ExportPackage> {
  const logs = await fetchAdminAuditLogs()
  const text = keyword.trim().toLowerCase()
  const filtered = !text
    ? logs
    : logs.filter((item) =>
        [item.module, item.action, item.operator, item.result, item.detail]
          .join(' ')
          .toLowerCase()
          .includes(text),
      )

  const header = '时间,模块,动作,操作人,结果,详情'
  const rows = filtered.map((item) =>
    [item.time, item.module, item.action, item.operator, item.result, item.detail]
      .map((cell) => `"${String(cell).replace(/"/g, '""')}"`)
      .join(','),
  )

  return {
    fileName: `admin-audit-${Date.now()}.csv`,
    content: [header, ...rows].join('\n'),
  }
}

export async function fetchAdminFeedbackTickets(): Promise<FeedbackTicketRecord[]> {
  const data = await unwrap(
    request.get('/admin/feedback-tickets') as Promise<ApiEnvelope<AdminFeedbackTicketDto[]>>,
  )

  return data.map((item) => ({
    id: String(item.id),
    ticketNo: item.ticketNo,
    submitterName: item.submitterName,
    submitterRole: item.submitterRole,
    contact: item.contact,
    detail: item.detail,
    status: item.status,
    reply: item.reply,
    createTime: item.createTime,
    updateTime: item.updateTime,
    closedTime: item.closedTime,
  }))
}

export async function updateAdminFeedbackTicket(
  ticketId: string,
  payload: { status: FeedbackTicketRecord['status']; reply: string },
): Promise<FeedbackTicketRecord> {
  const data = await unwrap(
    request.put(`/admin/feedback-tickets/${ticketId}`, payload) as Promise<
      ApiEnvelope<AdminFeedbackTicketDto>
    >,
  )

  return {
    id: String(data.id),
    ticketNo: data.ticketNo,
    submitterName: data.submitterName,
    submitterRole: data.submitterRole,
    contact: data.contact,
    detail: data.detail,
    status: data.status,
    reply: data.reply,
    createTime: data.createTime,
    updateTime: data.updateTime,
    closedTime: data.closedTime,
  }
}

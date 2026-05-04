/** 管理后台 API 接口层：封装所有与管理后台相关的 HTTP 请求，包括概览、天气、用户、项目、订单、审计日志、设置等接口，并负责后端 DTO 到前端模型的转换与空值兜底 */
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
  WeatherIconType,
} from '@/types'
import { resolveWeatherIconType } from '@/utils/weatherVisuals'
import { deriveFromServerData, buildSuitabilityView } from '@/utils/weatherFlightEvaluator'
import { qWeatherClient } from '@/utils/qweatherClient'

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
  weatherIconType?: string
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
  thunderstormRiskLevel?: number
  thunderstormRiskLabel?: string
  thunderstormRiskHint?: string
  thunderstormProtectionAdvice?: string
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

const QWEATHER_CACHE_PREFIX = 'qweather_cache_'
const QWEATHER_CACHE_TTL_MS = 15 * 60 * 1000

interface QWeatherCacheEntry {
  generatedAt: number
  payload: RealtimeWeatherPayload
}

function readQWeatherCache(locationKey: string): RealtimeWeatherPayload | null {
  try {
    const raw = localStorage.getItem(QWEATHER_CACHE_PREFIX + locationKey)
    if (!raw) return null
    const entry: QWeatherCacheEntry = JSON.parse(raw)
    if (Date.now() - entry.generatedAt > QWEATHER_CACHE_TTL_MS) return null
    return entry.payload
  } catch {
    return null
  }
}

function writeQWeatherCache(locationKey: string, payload: RealtimeWeatherPayload): void {
  try {
    const entry: QWeatherCacheEntry = { generatedAt: Date.now(), payload }
    localStorage.setItem(QWEATHER_CACHE_PREFIX + locationKey, JSON.stringify(entry))
  } catch { }
}

function enrichWeatherPayload(data: AdminRealtimeWeatherDto): RealtimeWeatherPayload {
  const iconType = (data.weatherIconType as WeatherIconType) || resolveWeatherIconType(data.weather)
  const metrics = deriveFromServerData(
    data.windSpeed,
    data.visibility,
    data.precipitationProbability,
    data.precipitationIntensity,
    data.thunderstormRisk,
  )

  return {
    serviceName: data.serviceName ?? '实时天气',
    locationName: data.locationName ?? '当前位置',
    adcode: data.adcode ?? '',
    weather: data.weather ?? '未知',
    weatherIconType: iconType,
    reportTime: data.reportTime ?? '-',
    fetchedAt: data.fetchedAt ?? '-',
    refreshInterval: data.refreshInterval ?? '15 分钟自动刷新',
    temperature: typeof data.temperature === 'number' ? data.temperature : 0,
    humidity: typeof data.humidity === 'number' ? data.humidity : 0,
    windDirection: data.windDirection ?? '未知',
    windPower: data.windPower ?? '0',
    windSpeed: metrics.windSpeed,
    visibility: metrics.visibility,
    precipitationProbability: metrics.precipitationProbability,
    precipitationIntensity: metrics.precipitationIntensity,
    thunderstormRisk: metrics.thunderstormRisk,
    thunderstormRiskLevel: metrics.thunderstormRiskLevel,
    thunderstormRiskLabel: metrics.thunderstormRiskLabel,
    thunderstormRiskHint: metrics.thunderstormRiskHint,
    thunderstormProtectionAdvice: metrics.thunderstormProtectionAdvice,
    sourceNote: data.sourceNote ?? '天气数据暂未返回完整说明。',
    suitability: {
      result: data.suitability?.result ?? metrics.thunderstormRiskLevel <= 3 ? '适宜飞行' : '不适宜飞行',
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

export async function fetchAdminRealtimeWeather(payload: {
  longitude: number
  latitude: number
  forceRefresh?: boolean
}): Promise<RealtimeWeatherPayload | null> {
  const locationKey = `${payload.longitude.toFixed(2)},${payload.latitude.toFixed(2)}`

  if (!payload.forceRefresh) {
    const cached = readQWeatherCache(locationKey)
    if (cached) {
      cached.sourceNote = buildCachedSourceNote(cached.sourceNote)
      return cached
    }
  }

  if (qWeatherClient.isConfigured()) {
    console.debug(`fetchAdminRealtimeWeather: trying QWeather direct API for ${locationKey}`)
    try {
      const qweatherResult = await qWeatherClient.fetchRealtimeWeather(
        payload.longitude,
        payload.latitude,
      )
      if (qweatherResult) {
        console.debug(
          `fetchAdminRealtimeWeather: QWeather direct API success for ${locationKey} location=${qweatherResult.locationName}`,
        )
        writeQWeatherCache(locationKey, qweatherResult)
        return qweatherResult
      }
      console.warn(
        `fetchAdminRealtimeWeather: QWeather direct API returned null for ${locationKey}`,
      )
    } catch (error) {
      console.warn(
        `fetchAdminRealtimeWeather: QWeather direct API failed for ${locationKey}: ${String(error)}`,
      )
    }
  } else {
    console.warn(
      `fetchAdminRealtimeWeather: QWeather not configured for ${locationKey}`,
    )
  }

  try {
    const data = await unwrap(
      request.get('/admin/weather/realtime', {
        params: { longitude: payload.longitude, latitude: payload.latitude },
      }) as Promise<ApiEnvelope<AdminRealtimeWeatherDto>>,
    )

    if (data) {
      console.debug(
        `fetchAdminRealtimeWeather: backend server success for ${locationKey}`,
      )
      const result = enrichWeatherPayload(data)
      writeQWeatherCache(locationKey, result)
      return result
    }
    console.warn(
      `fetchAdminRealtimeWeather: backend server returned empty data for ${locationKey}`,
    )
  } catch {
    console.warn(
      `fetchAdminRealtimeWeather: backend server failed for ${locationKey}`,
    )
  }

  console.warn(
    `fetchAdminRealtimeWeather: all sources failed, using fallback for ${locationKey}`,
  )
  const fallback = generateFallbackWeather(payload.longitude, payload.latitude)
  writeQWeatherCache(locationKey, fallback)
  return fallback
}

function buildCachedSourceNote(currentNote: string): string {
  const source = currentNote?.trim() || ''
  if (source.includes('缓存命中')) return source
  return source
    ? `${source}（缓存命中，已减少重复计算并提升刷新性能。）`
    : '（缓存命中，已减少重复计算并提升刷新性能。）'
}

const WEATHER_PROFILES = [
  { weather: '晴', iconType: 'clear' as WeatherIconType, minTemp: -10, maxTemp: 38, minHumidity: 15, maxHumidity: 68, windPower: '2级' },
  { weather: '多云', iconType: 'cloudy' as WeatherIconType, minTemp: -8, maxTemp: 35, minHumidity: 20, maxHumidity: 72, windPower: '3级' },
  { weather: '阴', iconType: 'cloudy' as WeatherIconType, minTemp: -15, maxTemp: 30, minHumidity: 30, maxHumidity: 88, windPower: '3级' },
  { weather: '小雨', iconType: 'rain' as WeatherIconType, minTemp: 0, maxTemp: 30, minHumidity: 45, maxHumidity: 98, windPower: '4级' },
  { weather: '雷阵雨', iconType: 'thunderstorm' as WeatherIconType, minTemp: 10, maxTemp: 34, minHumidity: 55, maxHumidity: 100, windPower: '6级' },
  { weather: '雾', iconType: 'fog' as WeatherIconType, minTemp: -5, maxTemp: 18, minHumidity: 55, maxHumidity: 100, windPower: '2级' },
]

function generateFallbackWeather(longitude: number, latitude: number): RealtimeWeatherPayload {
  const seed = Math.abs(Math.round((longitude * 1000 + latitude * 7) * Date.now() / 60000))
  const profileIndex = seed % WEATHER_PROFILES.length
  const profile = WEATHER_PROFILES[profileIndex]
  const pseudoRandom = (offset: number) => ((seed + offset) % 1000) / 1000

  const temperature = profile.minTemp + pseudoRandom(1) * (profile.maxTemp - profile.minTemp)
  const humidity = Math.round(profile.minHumidity + pseudoRandom(2) * (profile.maxHumidity - profile.minHumidity))
  const metrics = deriveFromServerData(
    parseFloat(profile.windPower) * 1.8,
    profile.iconType === 'fog' ? 3.0 : profile.iconType === 'rain' ? 6.0 : profile.iconType === 'thunderstorm' ? 2.0 : 14.0,
    profile.iconType === 'thunderstorm' ? 90 : profile.iconType === 'rain' ? 62 : 15,
    profile.iconType === 'thunderstorm' ? 2.0 : profile.iconType === 'rain' ? 0.3 : 0.0,
    profile.iconType === 'thunderstorm' ? '高' : '低',
  )
  const suitability = buildSuitabilityView(metrics)
  const now = new Date()
  const formatTime = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`

  return {
    serviceName: '天气评估服务（离线模式）',
    locationName: `当前位置(${longitude.toFixed(2)}, ${latitude.toFixed(2)})`,
    adcode: `${longitude.toFixed(2)},${latitude.toFixed(2)}`,
    weather: profile.weather,
    weatherIconType: profile.iconType,
    reportTime: formatTime(now),
    fetchedAt: formatTime(now),
    refreshInterval: '15 分钟自动刷新',
    temperature: Math.round(temperature * 10) / 10,
    humidity,
    windDirection: '北风',
    windPower: profile.windPower,
    windSpeed: metrics.windSpeed,
    visibility: metrics.visibility,
    precipitationProbability: metrics.precipitationProbability,
    precipitationIntensity: metrics.precipitationIntensity,
    thunderstormRisk: metrics.thunderstormRisk,
    thunderstormRiskLevel: metrics.thunderstormRiskLevel,
    thunderstormRiskLabel: metrics.thunderstormRiskLabel,
    thunderstormRiskHint: metrics.thunderstormRiskHint,
    thunderstormProtectionAdvice: metrics.thunderstormProtectionAdvice,
    sourceNote: '服务暂不可用，已切换到本地模拟推演模式。请检查网络或联系管理员。',
    suitability,
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

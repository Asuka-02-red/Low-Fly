export type MetricStatus = 'success' | 'warning' | 'danger' | 'info'

export interface UserProfile {
  name: string
  role: string
  organization: string
  phone: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  user: UserProfile
}

export interface SummaryMetric {
  label: string
  value: string
  trend: string
  status?: MetricStatus
}

export interface ActivityItem {
  title: string
  content: string
  time: string
  tag: string
}

export interface NoticeItem {
  title: string
  level: string
  time: string
}

export interface DashboardData {
  stats: SummaryMetric[]
  activities: ActivityItem[]
  notices: NoticeItem[]
}

export type IconKey =
  | 'overview'
  | 'users'
  | 'projects'
  | 'analytics'
  | 'settings'
  | 'logs'
  | 'dashboard'
  | 'command'
  | 'directory'
  | 'permission'
  | 'folder'
  | 'shield'
  | 'rocket'
  | 'chart'
  | 'server'
  | 'sliders'
  | 'lock'
  | 'bell'
  | 'audit'
  | 'download'
  | 'back'
  | 'menu'
  | 'search'
  | 'refresh'
  | 'plus'
  | 'edit'
  | 'delete'
  | 'swap'
  | 'export'
  | 'spark'
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'
  | 'logout'
  | 'mobile'
  | 'desktop'

export type SectionKey =
  | 'overview'
  | 'users'
  | 'projects'
  | 'analytics'
  | 'settings'
  | 'logs'

export type PageKey =
  | 'overview-dashboard'
  | 'overview-command'
  | 'users-directory'
  | 'users-permission'
  | 'projects-center'
  | 'projects-assurance'
  | 'projects-enablement'
  | 'analytics-business'
  | 'analytics-performance'
  | 'settings-basic'
  | 'settings-security'
  | 'settings-notifications'
  | 'logs-audit'
  | 'logs-workorders'
  | 'logs-export'

export interface SectionChild {
  key: PageKey
  path: string
  title: string
  description: string
  icon: IconKey
  tag?: string
}

export interface SectionDefinition {
  key: SectionKey
  path: string
  title: string
  description: string
  icon: IconKey
  children: SectionChild[]
}

export interface SectionSummaryMetric {
  label: string
  value: string
  trend: string
  status?: MetricStatus
}

export interface SectionSummary {
  sectionKey: SectionKey
  title: string
  headline: string
  buttonLabel: string
  metrics: SectionSummaryMetric[]
  highlights: string[]
}

export interface RoleRecord {
  id: string
  name: string
  description: string
  permissions: string[]
  userCount: number
}

export interface PermissionGroup {
  id: string
  name: string
  description: string
  permissions: string[]
}

export interface UserRecord {
  id: string
  name: string
  organization: string
  phone: string
  status: '启用' | '待审核' | '停用'
  lastActiveAt: string
  roleIds: string[]
  permissionGroupId: string
}

export interface UserView extends UserRecord {
  username: string
  email: string
  roleNames: string[]
  permissionGroupName: string
  roleCode: 'ADMIN' | 'ENTERPRISE' | 'PILOT' | 'INSTITUTION'
}

export interface AdminUserFormPayload {
  username: string
  password?: string
  phone: string
  email: string
  role: UserView['roleCode']
  realName: string
  companyName: string
  status: 0 | 1
}

export interface ProjectRecord {
  id: string
  name: string
  owner: string
  region: string
  status: '规划中' | '执行中' | '已完成' | '已暂停'
  progress: number
  budget: number
  complianceStatus: '正常' | '待复核' | '高风险'
  riskLevel: '低' | '中' | '高'
  trainingCompletion: number
  paymentStatus: '待结算' | '部分结算' | '已结算'
  updatedAt: string
}

export interface AdminOrderRecord {
  id: string
  orderNo: string
  projectName: string
  amount: number
  status: '已支付' | '待支付'
  createTime: string
  paymentMethod: string
  details: string
}

export interface OverviewPayload {
  metrics: SummaryMetric[]
  activities: ActivityItem[]
  notices: NoticeItem[]
  deviceStats: SummaryMetric[]
  projectDistribution: Array<{ name: string; value: number }>
  progressTrend: Array<{ label: string; value: number }>
}

export interface FlightConditionCheck {
  label: string
  currentValue: string
  threshold: string
  passed: boolean
}

export interface FlightSuitability {
  result: '适宜飞行' | '不适宜飞行'
  level: string
  summary: string
  checks: FlightConditionCheck[]
  conditionNotes: string[]
  recommendations: string[]
}

export interface RealtimeWeatherPayload {
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
  suitability: FlightSuitability
}

export interface AnalyticsPayload {
  businessMetrics: SummaryMetric[]
  performanceMetrics: SummaryMetric[]
  revenueTrend: Array<{ label: string; value: number }>
  userActivity: Array<{ label: string; value: number }>
  projectHealth: Array<{ name: string; value: number }>
  servicePerformance: Array<{ label: string; response: number; availability: number }>
  operatorLoad: Array<{ name: string; value: number }>
}

export interface BasicSettings {
  stationName: string
  serviceHotline: string
  defaultRegion: string
  mobileDashboardEnabled: boolean
}

export interface SecuritySettings {
  passwordValidityDays: number
  loginRetryLimit: number
  ipWhitelist: string
  mfaRequired: boolean
}

export interface NotificationRule {
  id: string
  name: string
  channel: '站内信' | '短信' | '邮件'
  enabled: boolean
  trigger: string
}

export interface AuditLogRecord {
  id: string
  time: string
  module: string
  action: string
  operator: string
  result: '成功' | '失败'
  detail: string
}

export interface FeedbackTicketRecord {
  id: string
  ticketNo: string
  submitterName: string
  submitterRole: string
  contact: string
  detail: string
  status: '待处理' | '处理中' | '已关闭'
  reply: string
  createTime: string
  updateTime: string
  closedTime: string
}

export interface ExportPackage {
  fileName: string
  content: string
}

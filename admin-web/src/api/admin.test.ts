/** 管理后台 API 接口单元测试：验证用户列表空值兜底、概览数据映射、实时天气空值处理及业务错误抛出 */
import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchAdminOverview, fetchAdminRealtimeWeather, fetchAdminUsers } from '@/api/admin'
import { request } from '@/api/http'

describe('admin api', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('returns an empty list when backend user data is null', async () => {
    vi.spyOn(request, 'get').mockResolvedValue({
      code: 200,
      message: 'ok',
      data: null,
      timestamp: Date.now(),
    } as never)

    await expect(fetchAdminUsers()).resolves.toEqual([])
  })

  it('normalizes nullable user fields from backend', async () => {
    vi.spyOn(request, 'get').mockResolvedValue({
      code: 200,
      message: 'ok',
      data: [
        {
          id: 'U-001',
          username: 'admin',
          email: null,
          name: '管理员',
          organization: null,
          phone: null,
          status: '启用',
          createTime: '2026-04-22 10:00:00',
          roleNames: null,
          permissionGroupName: null,
          roleCode: 'ADMIN',
        },
      ],
      timestamp: Date.now(),
    } as never)

    await expect(fetchAdminUsers()).resolves.toEqual([
      {
        id: 'U-001',
        username: 'admin',
        email: '未配置',
        name: '管理员',
        organization: '-',
        phone: '-',
        status: '启用',
        lastActiveAt: '2026-04-22 10:00:00',
        roleIds: [],
        permissionGroupId: '未分配',
        roleNames: [],
        permissionGroupName: '未分配',
        roleCode: 'ADMIN',
      },
    ])
  })

  it('maps complete overview payload from backend', async () => {
    vi.spyOn(request, 'get').mockResolvedValue({
      code: 200,
      message: 'ok',
      data: {
        metrics: [
          { label: '用户总量', value: '4', trend: '启用账号 3', status: 'success' },
        ],
        deviceStats: [
          { label: '审计日志', value: '12', trend: '关键操作留痕', status: 'warning' },
        ],
        activities: [
          { title: 'AUTH / LOGIN', content: '管理员登录', time: '2026-04-22 10:00', tag: 'ADMIN' },
        ],
        notices: [
          { title: '高风险告警 1 条', level: '高', time: '实时' },
        ],
        projectDistribution: [
          { name: '重庆江北区', value: 2 },
        ],
        progressTrend: [
          { label: '4/22', value: 68 },
        ],
      },
      timestamp: Date.now(),
    } as never)

    await expect(fetchAdminOverview()).resolves.toEqual({
      metrics: [{ label: '用户总量', value: '4', trend: '启用账号 3', status: 'success' }],
      deviceStats: [{ label: '审计日志', value: '12', trend: '关键操作留痕', status: 'warning' }],
      activities: [{ title: 'AUTH / LOGIN', content: '管理员登录', time: '2026-04-22 10:00', tag: 'ADMIN' }],
      notices: [{ title: '高风险告警 1 条', level: '高', time: '实时' }],
      projectDistribution: [{ name: '重庆江北区', value: 2 }],
      progressTrend: [{ label: '4/22', value: 68 }],
    })
  })

  it('returns null when realtime weather data is empty', async () => {
    vi.spyOn(request, 'get').mockResolvedValue({
      code: 200,
      message: 'ok',
      data: null,
      timestamp: Date.now(),
    } as never)

    await expect(fetchAdminRealtimeWeather({ longitude: 106.55, latitude: 29.56 })).resolves.toBeNull()
  })

  it('throws when backend returns business failure envelope', async () => {
    vi.spyOn(request, 'get').mockResolvedValue({
      code: 403,
      message: '仅管理员可访问',
      data: null,
      timestamp: Date.now(),
    } as never)

    await expect(fetchAdminOverview()).rejects.toThrow('仅管理员可访问')
  })
})

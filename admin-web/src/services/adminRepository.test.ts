import { describe, expect, it } from 'vitest'
import { AdminRepository, createAdminRepository } from '@/services/adminRepository'

function createMemoryStorage() {
  const store = new Map<string, string>()
  return {
    getItem(key: string) {
      return store.get(key) ?? null
    },
    setItem(key: string, value: string) {
      store.set(key, value)
    },
  }
}

function createBrokenStorage() {
  return {
    getItem() {
      return '{broken-json'
    },
    setItem() {},
  }
}

describe('AdminRepository', () => {
  it('creates a project and records an audit log', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const before = await repository.listProjects()

    await repository.createProject({
      name: '测试项目',
      owner: '测试负责人',
      region: '深圳龙岗',
      budget: 188000,
      status: '规划中',
    })

    const after = await repository.listProjects()
    const logs = await repository.listAuditLogs('创建项目')

    expect(after.projects).toHaveLength(before.projects.length + 1)
    expect(after.projects[0].name).toBe('测试项目')
    expect(logs[0]?.detail).toContain('测试项目')
  })

  it('updates user roles and permission groups', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const payload = await repository.listUsers()
    const targetUser = payload.users[1]
    const role = payload.roles[0]
    const group = payload.permissionGroups[0]

    await repository.assignUserRoles(targetUser.id, [role.id])
    await repository.updateUserPermissionGroup(targetUser.id, group.id)

    const after = await repository.listUsers()
    const updatedUser = after.users.find((item) => item.id === targetUser.id)

    expect(updatedUser?.roleIds).toEqual([role.id])
    expect(updatedUser?.permissionGroupId).toBe(group.id)
  })

  it('shows fallback permission group name when group is missing', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const payload = await repository.listUsers()
    const targetUser = payload.users[0]

    repository.hydrate({
      users: [
        {
          ...targetUser,
          permissionGroupId: 'missing-group',
        },
      ],
      roles: payload.roles,
      permissionGroups: [],
    })

    const after = await repository.listUsers()
    expect(after.users[0]?.permissionGroupName).toBe('未分配')
  })

  it('exports audit logs as csv content', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const file = await repository.exportAuditLogs()

    expect(file.fileName.endsWith('.csv')).toBe(true)
    expect(file.content).toContain('时间,模块,动作,操作人,结果,详情')
    expect(file.content).toContain('管理员登录')
  })

  it('updates a project and completes settlement when finished', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const before = await repository.listProjects()
    const target = before.projects[0]

    await repository.updateProject(target.id, {
      name: '深圳南山城市巡检-升级版',
      owner: '林雨桐',
      region: '深圳南山',
      budget: 300000,
      status: '已完成',
    })

    const after = await repository.listProjects()
    const updated = after.projects.find((item) => item.id === target.id)

    expect(updated?.name).toBe('深圳南山城市巡检-升级版')
    expect(updated?.progress).toBe(100)
    expect(updated?.paymentStatus).toBe('已结算')
  })

  it('creates projects with running and finished defaults', async () => {
    const repository = new AdminRepository(createMemoryStorage())

    const running = await repository.createProject({
      name: '执行中项目',
      owner: '调度员',
      region: '成都',
      budget: 20000,
      status: '执行中',
    })
    const finished = await repository.createProject({
      name: '已完成项目',
      owner: '调度员',
      region: '成都',
      budget: 30000,
      status: '已完成',
    })

    expect(running.progress).toBe(30)
    expect(finished.progress).toBe(100)
  })

  it('updates a project without forcing completion fields for non-finished status', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const target = (await repository.listProjects()).projects[0]

    await repository.updateProject(target.id, {
      name: '执行中项目-更新',
      owner: '林雨桐',
      region: '深圳南山',
      budget: 220000,
      status: '执行中',
    })

    const updated = (await repository.listProjects()).projects.find((item) => item.id === target.id)
    expect(updated?.status).toBe('执行中')
    expect(updated?.paymentStatus).not.toBe('已结算')
  })

  it('changes project status and records audit information', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const target = (await repository.listProjects()).projects[1]

    await repository.changeProjectStatus(target.id, '执行中')

    const after = await repository.listProjects()
    const updated = after.projects.find((item) => item.id === target.id)
    const logs = await repository.listAuditLogs('变更项目状态')

    expect(updated?.status).toBe('执行中')
    expect((updated?.progress ?? 0) >= 30).toBe(true)
    expect(logs[0]?.detail).toContain(target.name)
  })

  it('raises low-progress projects to execution baseline when status becomes running', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    repository.hydrate({
      projects: [
        {
          id: 'P-LOW-001',
          name: '低进度项目',
          owner: '测试负责人',
          region: '重庆',
          status: '规划中',
          progress: 10,
          budget: 10000,
          complianceStatus: '待复核',
          riskLevel: '中',
          trainingCompletion: 0,
          paymentStatus: '待结算',
          updatedAt: '2026-04-22 10:00:00',
        },
      ],
    })

    await repository.changeProjectStatus('P-LOW-001', '执行中')

    const updated = (await repository.listProjects()).projects[0]
    expect(updated?.progress).toBe(30)
  })

  it('deletes a project from repository state', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const target = (await repository.listProjects()).projects[0]

    await repository.deleteProject(target.id)

    const after = await repository.listProjects()
    expect(after.projects.find((item) => item.id === target.id)).toBeUndefined()
  })

  it('returns overview and analytics payloads for dashboard pages', async () => {
    const repository = new AdminRepository(createMemoryStorage())

    const overview = await repository.getOverview()
    const analytics = await repository.getAnalytics()

    expect(overview.metrics.length).toBeGreaterThan(0)
    expect(overview.notices.length).toBeGreaterThan(0)
    expect(analytics.businessMetrics.length).toBeGreaterThan(0)
    expect(analytics.performanceMetrics.length).toBeGreaterThan(0)
  })

  it('saves basic, security and notification settings', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const settings = await repository.getSettings()

    await repository.saveBasicSettings({
      ...settings.basic,
      stationName: '低空驿站生产版',
    })
    await repository.saveSecuritySettings({
      ...settings.security,
      loginRetryLimit: 8,
    })
    await repository.saveNotificationRules([
      {
        id: 'notice-new',
        name: '发布完成通知',
        channel: '邮件',
        enabled: true,
        trigger: '生产发布完成',
      },
    ])

    const updated = await repository.getSettings()
    expect(updated.basic.stationName).toBe('低空驿站生产版')
    expect(updated.security.loginRetryLimit).toBe(8)
    expect(updated.notifications).toHaveLength(1)
  })

  it('records security settings when MFA is disabled', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const settings = await repository.getSettings()

    await repository.saveSecuritySettings({
      ...settings.security,
      mfaRequired: false,
      loginRetryLimit: 6,
    })

    const logs = await repository.listAuditLogs('保存安全策略')
    expect(logs[0]?.detail).toContain('关闭')
  })

  it('filters audit logs by keyword', async () => {
    const repository = new AdminRepository(createMemoryStorage())

    const logs = await repository.listAuditLogs('管理员登录')

    expect(logs).toHaveLength(1)
    expect(logs[0]?.action).toBe('管理员登录')
  })

  it('throws when assigning roles for a missing user', async () => {
    const repository = new AdminRepository(createMemoryStorage())

    await expect(repository.assignUserRoles('missing-user', ['role-super-admin']))
      .rejects.toThrow('未找到目标用户')
  })

  it('throws when saving invalid security settings', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const settings = await repository.getSettings()

    await expect(repository.saveSecuritySettings({
      ...settings.security,
      loginRetryLimit: 0,
    })).rejects.toThrow('安全策略参数必须大于 0')
  })

  it('throws when creating a project with empty required fields', async () => {
    const repository = new AdminRepository(createMemoryStorage())

    await expect(repository.createProject({
      name: ' ',
      owner: ' ',
      region: '深圳',
      budget: 1000,
      status: '规划中',
    })).rejects.toThrow('项目名称和负责人不能为空')
  })

  it('throws when updating or deleting a missing project', async () => {
    const repository = new AdminRepository(createMemoryStorage())

    await expect(repository.updateProject('missing', {
      name: '不存在项目',
      owner: '陈伶',
      region: '深圳',
      budget: 1000,
      status: '规划中',
    })).rejects.toThrow('项目不存在')

    await expect(repository.deleteProject('missing')).rejects.toThrow('项目不存在')
  })

  it('throws when changing status of a missing project', async () => {
    const repository = new AdminRepository(createMemoryStorage())

    await expect(repository.changeProjectStatus('missing', '执行中'))
      .rejects.toThrow('目标项目不存在')
  })

  it('throws when updating a missing permission group', async () => {
    const repository = new AdminRepository(createMemoryStorage())

    await expect(repository.updateUserPermissionGroup('U-001', 'missing-group'))
      .rejects.toThrow('权限组不存在')
  })

  it('can reset and hydrate repository state', async () => {
    const repository = new AdminRepository(createMemoryStorage())

    repository.hydrate({
      projects: [
        {
          id: 'P-CUSTOM-001',
          name: '自定义项目',
          owner: '测试负责人',
          region: '重庆',
          status: '规划中',
          progress: 10,
          budget: 10000,
          complianceStatus: '待复核',
          riskLevel: '中',
          trainingCompletion: 0,
          paymentStatus: '待结算',
          updatedAt: '2026-04-22 09:00:00',
        },
      ],
    })

    let projects = await repository.listProjects()
    expect(projects.projects).toHaveLength(1)
    expect(projects.projects[0]?.name).toBe('自定义项目')

    repository.reset()
    projects = await repository.listProjects()
    expect(projects.projects.length).toBeGreaterThan(1)
  })

  it('falls back to default state when persisted storage is invalid', async () => {
    const repository = new AdminRepository(createBrokenStorage())

    const projects = await repository.listProjects()
    expect(projects.projects.length).toBeGreaterThan(1)
  })

  it('marks project as completed when changing status to finished', async () => {
    const repository = new AdminRepository(createMemoryStorage())
    const target = (await repository.listProjects()).projects[0]

    await repository.changeProjectStatus(target.id, '已完成')

    const updated = (await repository.listProjects()).projects.find((item) => item.id === target.id)
    expect(updated?.progress).toBe(100)
    expect(updated?.trainingCompletion).toBe(100)
    expect(updated?.paymentStatus).toBe('已结算')
  })

  it('creates repository instances with and without seed data', async () => {
    const defaultRepository = createAdminRepository()
    const seededRepository = createAdminRepository({
      projects: [
        {
          id: 'P-SEED-001',
          name: '种子项目',
          owner: '种子负责人',
          region: '珠海',
          status: '执行中',
          progress: 80,
          budget: 8000,
          complianceStatus: '正常',
          riskLevel: '低',
          trainingCompletion: 90,
          paymentStatus: '部分结算',
          updatedAt: '2026-04-22 10:00:00',
        },
      ],
    })

    expect((await defaultRepository.listProjects()).projects.length).toBeGreaterThan(1)
    expect((await seededRepository.listProjects()).projects[0]?.name).toBe('种子项目')
  })
})

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAdminUser, deleteAdminUser, fetchAdminUsers, updateAdminUser } from '@/api/admin'
import PageContainer from '@/components/PageContainer.vue'
import StatCard from '@/components/StatCard.vue'
import type { AdminUserFormPayload, SummaryMetric, UserView } from '@/types'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const users = ref<UserView[]>([])
const dialogVisible = ref(false)
const editingUserId = ref('')

const roleOptions: Array<{ label: string; value: UserView['roleCode'] }> = [
  { label: '管理员', value: 'ADMIN' },
  { label: '企业', value: 'ENTERPRISE' },
  { label: '飞手', value: 'PILOT' },
  { label: '机构', value: 'INSTITUTION' },
]

const statusOptions: Array<{ label: string; value: 0 | 1 }> = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

function createEmptyForm(): AdminUserFormPayload {
  return {
    username: '',
    password: '',
    phone: '',
    email: '',
    role: 'ENTERPRISE',
    realName: '',
    companyName: '',
    status: 1,
  }
}

const form = reactive<AdminUserFormPayload>(createEmptyForm())

const dialogTitle = computed(() => (editingUserId.value ? '编辑用户' : '新增用户'))

const metrics = computed<SummaryMetric[]>(() => {
  const total = users.value.length
  const active = users.value.filter((item) => item.status === '启用').length
  const pending = users.value.filter((item) => item.status === '待审核').length
  const adminCount = users.value.filter((item) => item.roleNames.includes('管理员')).length

  return [
    { label: '账号总量', value: String(total), trend: '来自真实后台用户表', status: 'success' },
    { label: '启用账号', value: String(active), trend: '当前可登录后台账号', status: 'info' },
    { label: '待审核账号', value: String(pending), trend: '待补审批流对接', status: 'warning' },
    { label: '管理员角色', value: String(adminCount), trend: '具备后台管理权限', status: 'danger' },
  ]
})

const filteredUsers = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) {
    return users.value
  }

  return users.value.filter((user) =>
    [
      user.id,
      user.username,
      user.email,
      user.name,
      user.organization,
      user.phone,
      user.status,
      user.permissionGroupName,
      user.roleNames.join(' '),
    ]
      .join(' ')
      .toLowerCase()
      .includes(text),
  )
})

async function loadData() {
  loading.value = true
  try {
    users.value = await fetchAdminUsers()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载用户目录失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, createEmptyForm())
  editingUserId.value = ''
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(user: UserView) {
  editingUserId.value = user.id
  Object.assign(form, {
    username: user.username,
    password: '',
    phone: user.phone === '未配置' ? '' : user.phone,
    email: user.email === '未配置' ? '' : user.email,
    role: user.roleCode,
    realName: user.name,
    companyName: user.organization === '-' ? '' : user.organization,
    status: user.status === '启用' ? 1 : 0,
  })
  dialogVisible.value = true
}

async function saveUser() {
  if (!form.username.trim() || !form.realName.trim() || !form.phone.trim()) {
    ElMessage.warning('请填写账号、姓名和手机号。')
    return
  }
  if (!editingUserId.value && !form.password?.trim()) {
    ElMessage.warning('新增用户时必须设置密码。')
    return
  }

  saving.value = true
  try {
    const payload: AdminUserFormPayload = {
      username: form.username.trim(),
      password: form.password?.trim() ?? '',
      phone: form.phone.trim(),
      email: form.email.trim(),
      role: form.role,
      realName: form.realName.trim(),
      companyName: form.companyName.trim(),
      status: form.status,
    }

    if (editingUserId.value) {
      await updateAdminUser(editingUserId.value, payload)
      ElMessage.success('用户信息已更新。')
    } else {
      await createAdminUser(payload)
      ElMessage.success('用户已创建。')
    }

    dialogVisible.value = false
    resetForm()
    await loadData()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存用户失败，请稍后重试。')
  } finally {
    saving.value = false
  }
}

async function removeUser(user: UserView) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${user.name}」吗？此操作不可撤销。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteAdminUser(user.id)
    ElMessage.success('用户已删除。')
    await loadData()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '删除用户失败，请稍后重试。')
  }
}

void loadData()
</script>

<template>
  <PageContainer
    title="用户目录"
    description="基于真实后台账号表展示管理员、企业、飞手与机构账号的基本信息。"
    back-to="/users"
    back-label="返回用户管理"
  >
    <template #actions>
      <el-input v-model="keyword" placeholder="搜索姓名/组织/手机号/权限组" clearable class="toolbar-search" />
      <el-button type="primary" plain @click="openCreateDialog">新增用户</el-button>
      <el-button type="primary" :loading="loading" @click="loadData">刷新目录</el-button>
    </template>

    <div class="metrics-grid">
      <StatCard v-for="metric in metrics" :key="metric.label" :metric="metric" />
    </div>

    <el-card class="page-card" shadow="never" v-loading="loading">
      <el-table :data="filteredUsers" border>
        <el-table-column prop="id" label="账号 ID" min-width="100" />
        <el-table-column prop="username" label="账号" min-width="140" />
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="organization" label="组织归属" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag v-for="role in row.roleNames" :key="role" type="danger">{{ role }}</el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column prop="permissionGroupName" label="权限组" min-width="160" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '启用' ? 'success' : row.status === '待审核' ? 'warning' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastActiveAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeUser(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" @closed="resetForm">
      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item :label="editingUserId ? '密码（留空表示不修改）' : '初始密码'">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="组织归属">
          <el-input v-model="form.companyName" placeholder="请输入组织名称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" class="dialog-select">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="dialog-select">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped lang="scss">
.toolbar-search {
  width: min(320px, 100%);
}

.dialog-select {
  width: 100%;
}
</style>

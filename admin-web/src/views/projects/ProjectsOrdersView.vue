<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { fetchAdminOrderDetail, fetchAdminOrders } from '@/api/admin'
import type { AdminOrderRecord } from '@/types'

const orders = ref<AdminOrderRecord[]>([])
const loading = ref(false)
const detailLoading = ref(false)
const drawerVisible = ref(false)
const currentOrder = ref<AdminOrderRecord | null>(null)

async function loadData() {
  loading.value = true
  try {
    orders.value = await fetchAdminOrders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载订单记录失败。')
  } finally {
    loading.value = false
  }
}

async function handleViewDetails(row: AdminOrderRecord) {
  detailLoading.value = true
  drawerVisible.value = true
  try {
    currentOrder.value = await fetchAdminOrderDetail(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载订单详情失败。')
    drawerVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

void loadData()
</script>

<template>
  <PageContainer
    title="订单记录"
    description="查看系统中的真实订单记录，并可点击查看数据库中的订单详情。"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">刷新订单</el-button>
    </template>

    <div class="page-content">
      <el-table :data="orders" v-loading="loading" border style="width: 100%" @row-click="handleViewDetails" highlight-current-row>
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="projectName" label="项目名称" />
        <el-table-column prop="amount" label="金额 (元)" width="120">
          <template #default="{ row }">
            ¥{{ row.amount.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="支付状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PAID' ? 'success' : 'warning'">
              {{ row.status === 'PAID' ? '已支付' : '待支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleViewDetails(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-drawer v-model="drawerVisible" title="订单详情" size="400px" v-loading="detailLoading">
        <div v-if="currentOrder" class="order-details">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="订单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="项目名称">{{ currentOrder.projectName }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">¥{{ currentOrder.amount.toLocaleString() }}</el-descriptions-item>
            <el-descriptions-item label="支付状态">
              <el-tag :type="currentOrder.status === '已支付' ? 'success' : 'warning'" size="small">
                {{ currentOrder.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">{{ currentOrder.paymentMethod }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
            <el-descriptions-item label="详情描述">{{ currentOrder.details }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-drawer>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.page-content {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}
.order-details {
  padding: 0 20px;
}
</style>

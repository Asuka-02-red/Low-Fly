<script setup lang="ts">
import type { SummaryMetric } from '@/types'

defineProps<{
  metric: SummaryMetric
}>()
</script>

<template>
  <el-card class="stat-card page-card" shadow="hover">
    <div class="stat-label">{{ metric.label }}</div>
    <div class="stat-value">{{ metric.value }}</div>
    <el-tag :type="metric.status ?? 'info'" effect="light" round>
      {{ metric.trend }}
    </el-tag>
  </el-card>
</template>

<style scoped lang="scss">
.stat-card {
  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
}

.stat-label {
  color: var(--color-text-muted);
  font-size: var(--font-medium);
}

.stat-value {
  font-size: calc(var(--font-large) * 1.4);
  line-height: 1.2;
  font-weight: 700;
  color: var(--color-text-primary);
}
</style>

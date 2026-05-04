﻿<script setup lang="ts">
import { computed } from 'vue'
import type { SummaryMetric } from '@/types'

const props = defineProps<{
  metric: SummaryMetric
}>()

const statusClass = computed(() => {
  const map: Record<string, string> = {
    success: 'accent-success',
    warning: 'accent-warning',
    danger: 'accent-danger',
    info: 'accent-info',
  }
  return map[props.metric.status ?? 'info'] ?? 'accent-info'
})

const statusRingColor = computed(() => {
  const map: Record<string, string> = {
    success: 'var(--color-sky-strong)',
    warning: 'var(--color-steel-strong)',
    danger: 'var(--color-steel-strong)',
    info: 'var(--color-periwinkle-strong)',
  }
  return map[props.metric.status ?? 'info'] ?? map.info
})
</script>

<template>
  <div class="stat-card glass-card" :style="{ '--accent-ring': statusRingColor }">
    <div class="stat-ring" aria-hidden="true" :class="statusClass"></div>
    <div class="stat-body">
      <div class="stat-label">{{ metric.label }}</div>
      <div class="stat-value">{{ metric.value }}</div>
      <div class="stat-trend" :class="statusClass">
        <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
          <path d="M2 8l3-4 2 2 3-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        {{ metric.trend }}
      </div>
    </div>
    <div class="stat-glow" aria-hidden="true" :class="statusClass"></div>
  </div>
</template>

<style scoped lang="scss">
.stat-card {
  position: relative;
  overflow: hidden;
  cursor: default;
  padding: var(--space-5) var(--space-6);
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
}

.stat-ring {
  position: absolute;
  top: -20px;
  right: -20px;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 3px solid var(--accent-ring);
  opacity: 0.18;
  transition: all var(--transition-smooth);

  .stat-card:hover & {
    transform: scale(1.3);
    opacity: 0.28;
  }
}

.stat-body {
  position: relative;
  z-index: 1;
  flex: 1;
}

.stat-label {
  color: var(--color-text-muted);
  font-size: var(--font-sm);
  font-weight: 600;
  letter-spacing: var(--tracking-wider);
  text-transform: uppercase;
}

.stat-value {
  font-size: var(--font-2xl);
  line-height: 1.1;
  font-weight: 800;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  margin-top: var(--space-2);
  transition: transform var(--transition-base);

  .stat-card:hover & {
    transform: scale(1.05);
  }
}

.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  margin-top: var(--space-3);
  font-size: var(--font-xs);
  font-weight: 600;
  padding: 2px 10px;
  border-radius: var(--radius-full);

  &.accent-success { background: var(--color-success-soft); color: var(--color-success); }
  &.accent-warning { background: var(--color-warning-soft); color: var(--color-warning); }
  &.accent-danger  { background: var(--color-danger-soft); color: var(--color-danger); }
  &.accent-info    { background: var(--color-info-soft); color: var(--color-info); }
}

.stat-glow {
  position: absolute;
  bottom: -15px;
  left: -15px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  opacity: 0.06;
  transition: all var(--transition-smooth);
  filter: blur(20px);

  .stat-card:hover & {
    transform: scale(1.5);
    opacity: 0.12;
  }

  &.accent-success { background: var(--color-blue-light); }
  &.accent-warning { background: var(--color-frost); }
  &.accent-danger  { background: var(--color-blue); }
  &.accent-info    { background: var(--color-periwinkle); }
}
</style>

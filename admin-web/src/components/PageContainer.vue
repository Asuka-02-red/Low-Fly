﻿/** 页面容器组件：玻璃拟态标题栏 + 动画入场 + 响应式间距 */
<script setup lang="ts">
defineProps<{
  title: string
  description: string
  backTo?: string
  backLabel?: string
  hideTitle?: boolean
}>()
</script>

<template>
  <section class="page-container">
    <header class="page-header glass-panel" v-if="!$props.hideTitle">
      <div class="page-header-content">
        <div class="page-title-row">
          <div class="page-title-accent" aria-hidden="true"></div>
          <div>
            <h2>{{ title }}</h2>
            <p>{{ description }}</p>
          </div>
        </div>
      </div>
      <div class="page-actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="page-body">
      <slot />
    </div>
  </section>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  animation: fadeSlideUp var(--transition-smooth) ease-out;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
  padding: var(--space-6);
}

.page-header-content {
  flex: 1;
}

.page-title-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.page-title-accent {
  width: 3px;
  height: 38px;
  border-radius: var(--radius-full);
  background: linear-gradient(180deg, var(--color-sky-strong), var(--color-periwinkle));
  flex-shrink: 0;
  margin-top: 3px;
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.3);
}

h2 {
  margin: 0 0 4px;
  font-size: var(--font-xl);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.01em;
}

p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-md);
  line-height: var(--leading-normal);
}

.page-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.page-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}
</style>

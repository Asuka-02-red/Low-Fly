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
    <header class="page-header">
      <div>
        <h2 v-if="!$props.hideTitle">{{ title }}</h2>
        <p>{{ description }}</p>
      </div>
      <div class="page-actions">
        <slot name="actions" />
      </div>
    </header>

    <slot />
  </section>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;

  h2 {
    margin: 0 0 6px;
    font-size: calc(var(--font-large) * 1.2);
    color: var(--color-text-primary);
  }

  p {
    margin: 0;
    color: var(--color-text-muted);
    font-size: var(--font-medium);
  }
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>

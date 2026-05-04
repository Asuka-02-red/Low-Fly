﻿<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TopNavbar from '@/components/TopNavbar.vue'
import { findSectionByPath } from '@/config/navigation'

const route = useRoute()

const pageTitle = computed(() => String(route.meta.title ?? '管理后台'))
const breadcrumb = computed(() => {
  const currentSection = findSectionByPath(route.path)
  const pages = [currentSection?.title, String(route.meta.title ?? '')].filter(
    (item, index, array) => Boolean(item) && item !== array[index - 1],
  )
  return pages.join(' / ')
})
</script>

<template>
  <div class="layout-root">
    <TopNavbar />

    <div class="layout-page">
      <header class="layout-page-header">
        <div class="page-header-left">
          <nav class="page-breadcrumb" aria-label="面包屑导航">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true" class="breadcrumb-home">
              <path d="M2 5.5L7 2l5 3.5V12H2V5.5z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/>
            </svg>
            <span>{{ breadcrumb }}</span>
          </nav>
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>
      </header>

      <main class="layout-main">
        <RouterView v-slot="{ Component }">
          <transition name="page-transition" mode="out-in">
            <component :is="Component" />
          </transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.layout-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-canvas);
  position: relative;

  &::before {
    content: '';
    position: fixed;
    inset: 0;
    z-index: 0;
    pointer-events: none;
    background:
      radial-gradient(ellipse 80% 60% at 20% 10%, rgba(56, 189, 248, 0.12), transparent 60%),
      radial-gradient(ellipse 60% 50% at 80% 30%, rgba(181, 234, 215, 0.1), transparent 60%),
      radial-gradient(ellipse 50% 40% at 50% 80%, rgba(199, 206, 234, 0.1), transparent 60%);
  }

  &::after {
    content: '';
    position: fixed;
    inset: 0;
    z-index: 0;
    pointer-events: none;
    background-image:
      linear-gradient(rgba(200, 185, 195, 0.06) 1px, transparent 1px),
      linear-gradient(90deg, rgba(200, 185, 195, 0.06) 1px, transparent 1px);
    background-size: 64px 64px;
    mask-image: radial-gradient(ellipse 60% 50% at 50% 50%, black 30%, transparent 70%);
  }
}

.layout-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
}

.layout-page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: var(--space-6) var(--space-8) var(--space-5);
  gap: var(--space-4);
  flex-wrap: wrap;
}

.page-header-left {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-sm);
  color: var(--color-text-muted);
  letter-spacing: var(--tracking-wide);
}

.breadcrumb-home {
  color: var(--color-text-caption);
}

.page-title {
  font-size: var(--font-2xl);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.01em;
  margin: 0;
  background: linear-gradient(135deg, var(--color-text-primary), var(--color-sky-strong));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.layout-main {
  flex: 1;
  padding: 0 var(--space-8) var(--space-8);
}

/* ═── Page Transition ── */
.page-transition-enter-active {
  animation: pageIn 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.page-transition-leave-active {
  animation: pageOut 0.2s ease-in;
}
@keyframes pageIn {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
@keyframes pageOut {
  to {
    opacity: 0;
    transform: translateY(-4px);
  }
}

@media (max-width: 768px) {
  .layout-page-header {
    padding: var(--space-4) var(--space-4) var(--space-3);
  }
  .layout-main {
    padding: 0 var(--space-4) var(--space-4);
  }
  .page-title {
    font-size: var(--font-xl);
  }
}
</style>

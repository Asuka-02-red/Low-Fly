﻿<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import { findSectionByPath, menuData } from '@/config/navigation'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const mobileMenuOpen = ref(false)
const activeDropdown = ref<string | null>(null)
const scrolled = ref(false)

let scrollTimer: number | undefined

const activeSectionPath = computed(() => findSectionByPath(route.path)?.path ?? '/overview')

function navigateToSection(path: string) {
  mobileMenuOpen.value = false
  activeDropdown.value = null
  const section = menuData.find(m => m.path === path)
  if (section?.children?.[0]) {
    void router.push(section.children[0].path)
  } else {
    void router.push(path)
  }
}

function navigateToChild(childPath: string) {
  mobileMenuOpen.value = false
  activeDropdown.value = null
  void router.push(childPath)
}



async function handleLogout() {
  await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
  userStore.logout()
  ElMessage.success('已退出登录')
  await router.push('/login')
}

function onScroll() {
  scrolled.value = window.scrollY > 8
}

function closeAll() {
  activeDropdown.value = null
}

onMounted(() => {
  if (!userStore.profile) {
    void userStore.fetchProfile()
  }
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  if (scrollTimer) clearTimeout(scrollTimer)
})
</script>

<template>
  <div class="topnav-backdrop" v-if="activeDropdown" @click="closeAll" />
  <div class="topnav-mask" v-if="mobileMenuOpen" @click="mobileMenuOpen = false" />

  <header class="topnav" :class="{ 'topnav--scrolled': scrolled }">
    <div class="topnav-inner">
      <div class="topnav-left">
        <button class="topnav-hamburger" aria-label="打开菜单" @click="mobileMenuOpen = !mobileMenuOpen">
          <span></span><span></span><span></span>
        </button>
        <div class="topnav-brand" @click="router.push('/overview/dashboard')">
          <div class="topnav-logo" aria-hidden="true">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 19h20L12 2z" fill="currentColor" opacity="0.9"/>
              <circle cx="12" cy="17" r="2" fill="currentColor"/>
            </svg>
          </div>
          <span class="topnav-brand-text">
            <strong>低空驿站</strong>
            <small>控制台</small>
          </span>
        </div>

        <nav class="topnav-menu">
          <div
            v-for="item in menuData"
            :key="item.path"
            class="topnav-item"
            :class="{
              'is-active': activeSectionPath === item.path,
              'is-open': activeDropdown === item.path
            }"
            @mouseenter="activeDropdown = item.path"
            @mouseleave="activeDropdown = null"
          >
            <button
              class="topnav-link"
              @click="navigateToSection(item.path)"
            >
              <AppIcon :name="item.icon" />
              <span>{{ item.title }}</span>
              <svg class="topnav-chevron" width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
                <path d="M3 5l3 3 3-3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>

            <transition name="dropdown">
              <div v-if="activeDropdown === item.path" class="topnav-dropdown">
                <div class="topnav-dropdown-inner">
                  <div class="dropdown-header">
                    <AppIcon :name="item.icon" />
                    <span>{{ item.description }}</span>
                  </div>
                  <div class="dropdown-items">
                    <button
                      v-for="child in item.children"
                      :key="child.path"
                      class="dropdown-item"
                      :class="{ 'is-active': route.path === child.path }"
                      @click="navigateToChild(child.path)"
                    >
                      <div class="dropdown-item-content">
                        <strong>{{ child.title }}</strong>
                        <small v-if="child.description">{{ child.description }}</small>
                      </div>
                      <span v-if="child.tag" class="dropdown-tag" :class="`tag-${child.tag}`">{{ child.tag }}</span>
                    </button>
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </nav>
      </div>

      <div class="topnav-right">
        <el-badge :value="3" :max="9" class="topnav-notify">
          <button class="topnav-icon-btn" aria-label="通知" @click="router.push('/logs/audit')">
            <AppIcon name="bell" />
          </button>
        </el-badge>

        <div class="topnav-user">
          <div class="topnav-avatar" aria-hidden="true">
            {{ (userStore.profile?.name ?? '管')[0] }}
          </div>
          <div class="topnav-user-meta">
            <strong>{{ userStore.profile?.name ?? '管理员' }}</strong>
            <span>{{ userStore.profile?.role ?? '平台角色' }}</span>
          </div>
          <button class="topnav-logout" title="退出登录" @click="handleLogout">
            <AppIcon name="logout" />
          </button>
        </div>
      </div>
    </div>
  </header>

  <transition name="slide-menu">
    <div v-if="mobileMenuOpen" class="topnav-mobile-menu">
      <div class="mobile-menu-header">
        <span>导航菜单</span>
        <button class="mobile-menu-close" @click="mobileMenuOpen = false">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M4 4l10 10M14 4L4 14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
      <div v-for="item in menuData" :key="item.path" class="mobile-item">
        <button
          class="mobile-section"
          :class="{ active: activeSectionPath === item.path }"
          @click="navigateToSection(item.path)"
        >
          <AppIcon :name="item.icon" />
          <span>{{ item.title }}</span>
        </button>
        <div v-if="activeSectionPath === item.path" class="mobile-sub">
          <button
            v-for="child in item.children"
            :key="child.path"
            class="mobile-sub-item"
            :class="{ active: route.path === child.path }"
            @click="navigateToChild(child.path)"
          >
            <span class="mobile-sub-dot"></span>
            {{ child.title }}
            <span v-if="child.tag" class="mobile-sub-tag">{{ child.tag }}</span>
          </button>
        </div>
      </div>
      <div class="mobile-user">
        <div class="topnav-avatar" aria-hidden="true">
          {{ (userStore.profile?.name ?? '管')[0] }}
        </div>
        <div>
          <strong>{{ userStore.profile?.name ?? '管理员' }}</strong>
          <small>{{ userStore.profile?.role ?? '平台角色' }}</small>
        </div>
        <button class="topnav-icon-btn" @click="handleLogout">
          <AppIcon name="logout" />
        </button>
      </div>
    </div>
  </transition>
</template>

<style scoped lang="scss">
.topnav-backdrop {
  position: fixed;
  inset: 0;
  z-index: 28;
}
.topnav-mask {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(58, 51, 64, 0.3);
  backdrop-filter: blur(4px);
  z-index: 31;
}

/* ═══ Main TopNav ═══ */
.topnav {
  position: sticky;
  top: 0;
  z-index: var(--z-nav);
  transition: all var(--transition-smooth);
}
.topnav--scrolled .topnav-inner {
  box-shadow: 0 2px 20px rgba(56, 189, 248, 0.12);
}

.topnav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 var(--space-8);
  background: var(--color-nav-bg);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-bottom: 1px solid var(--color-nav-border);
  animation: fadeSlideDown 0.4s ease-out;
}

/* ═══ Left Section ═══ */
.topnav-left {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  height: 100%;
}

.topnav-hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);

  &:hover { background: var(--color-nav-hover-bg); }
  span {
    display: block;
    width: 20px;
    height: 2px;
    border-radius: 2px;
    background: var(--color-nav-text);
    transition: transform var(--transition-fast);
  }
}

.topnav-brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
  transition: opacity var(--transition-fast);
  &:hover { opacity: 0.85; }
}

.topnav-logo {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-sky-strong), var(--color-blue-light));
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 2px 10px rgba(56, 189, 248, 0.3);
}

.topnav-brand-text {
  display: flex;
  flex-direction: column;
  strong {
    font-size: var(--font-md);
    font-weight: 700;
    color: var(--color-text-primary);
    letter-spacing: 0.02em;
    line-height: 1.2;
  }
  small {
    font-size: 10px;
    color: var(--color-text-muted);
    letter-spacing: 0.06em;
  }
}

/* ═══ Desktop Menu ═══ */
.topnav-menu {
  display: flex;
  align-items: stretch;
  height: 100%;
  gap: 2px;
}

.topnav-item {
  position: relative;
  display: flex;
  align-items: stretch;
}

.topnav-link {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-4);
  height: 100%;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-nav-text);
  font-size: var(--font-sm);
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-base);

  &:hover {
    color: var(--color-nav-active-text);
    background: var(--color-nav-hover-bg);
  }
  .topnav-chevron {
    opacity: 0.4;
    transition: transform var(--transition-fast);
  }
}

.topnav-item.is-active .topnav-link {
  color: var(--color-nav-active-text);
  background: var(--color-nav-active-bg);
  font-weight: 600;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: var(--space-4);
    right: var(--space-4);
    height: 3px;
    border-radius: 3px 3px 0 0;
    background: linear-gradient(90deg, var(--color-sky-strong), var(--color-blue-light));
  }
}

.topnav-item.is-open .topnav-link .topnav-chevron {
  transform: rotate(180deg);
}

/* ═══ Dropdown ═══ */
.topnav-dropdown {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  z-index: var(--z-dropdown);
  padding-top: 6px;
}

.topnav-dropdown-inner {
  min-width: 280px;
  padding: var(--space-3);
  border-radius: var(--radius-lg);
  background: var(--glass-bg-strong);
  backdrop-filter: blur(var(--glass-blur-strong)) saturate(var(--glass-saturate));
  -webkit-backdrop-filter: blur(var(--glass-blur-strong)) saturate(var(--glass-saturate));
  border: 1px solid var(--glass-border-strong);
  box-shadow: var(--shadow-dropdown), var(--shadow-lg);
  animation: scaleIn 0.2s ease-out;
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3) var(--space-3);
  border-bottom: 1px solid var(--color-nav-divider);
  color: var(--color-nav-text-muted);
  font-size: var(--font-xs);
}

.dropdown-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: var(--space-1);
}

.dropdown-item {
  display: flex;
  align-items: center;
  width: 100%;
  padding: var(--space-3);
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    background: var(--color-nav-hover-bg);
  }
  &.is-active {
    background: var(--color-nav-active-bg);
    strong { color: var(--color-nav-active-text); }
  }
}

.dropdown-item-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  strong {
    font-size: var(--font-sm);
    font-weight: 500;
    color: var(--color-text-primary);
  }
  small {
    font-size: var(--font-xs);
    color: var(--color-text-muted);
    margin-top: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.dropdown-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  letter-spacing: 0.04em;
  flex-shrink: 0;
  &.tag-推荐 { background: rgba(56, 189, 248, 0.2); color: var(--color-sky-strong); }
  &.tag-核心 { background: rgba(197, 206, 234, 0.3); color: #8B9DDF; }
  &.tag-常用 { background: rgba(181, 234, 215, 0.35); color: #4CAF7C; }
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ═══ Right Section ═══ */
.topnav-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-shrink: 0;
}

.topnav-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-accent-dark);
    border-color: var(--color-border-accent);
    background: var(--color-accent-soft);
  }
}

.topnav-notify {
  :deep(.el-badge__content) {
    font-size: 10px;
    border: none;
    background: var(--color-steel-strong);
  }
}

.topnav-user {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-1) var(--space-3) var(--space-1) var(--space-1);
  border-radius: var(--radius-full);
  background: var(--color-bg-section);
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-fast);

  &:hover {
    background: var(--color-bg-highlight);
    border-color: var(--color-border-accent);
  }
}

.topnav-avatar {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-sky-strong), var(--color-blue-light));
  color: #fff;
  font-weight: 700;
  font-size: var(--font-sm);
  flex-shrink: 0;
}

.topnav-user-meta {
  display: flex;
  flex-direction: column;
  strong {
    color: var(--color-text-primary);
    font-size: var(--font-sm);
    font-weight: 600;
    line-height: 1.2;
  }
  span {
    color: var(--color-text-muted);
    font-size: var(--font-xs);
  }
}

.topnav-logout {
  display: inline-flex;
  align-items: center;
  padding: var(--space-1);
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-danger);
    background: var(--color-danger-soft);
  }
}

/* ═══ Mobile Menu ═══ */
.topnav-mobile-menu {
  display: none;
}

.slide-menu-enter-active,
.slide-menu-leave-active {
  transition: all 0.3s ease;
}
.slide-menu-enter-from,
.slide-menu-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* ═══ Responsive ═══ */
@media (max-width: 960px) {
  .topnav-inner {
    padding: 0 var(--space-4);
  }
  .topnav-hamburger { display: flex; }
  .topnav-menu { display: none; }
  .topnav-brand-text { display: none; }
  .topnav-user-meta { display: none; }
  .topnav-user {
    padding: var(--space-1);
    border: none;
    background: transparent;
    &:hover { background: transparent; }
  }

  .topnav-mask { display: block; }
  .topnav-mobile-menu {
    display: flex;
    flex-direction: column;
    position: fixed;
    top: 60px;
    left: 0;
    right: 0;
    max-height: calc(100vh - 60px);
    overflow-y: auto;
    z-index: 32;
    background: var(--glass-bg-strong);
    backdrop-filter: blur(var(--glass-blur-strong)) saturate(var(--glass-saturate));
    -webkit-backdrop-filter: blur(var(--glass-blur-strong)) saturate(var(--glass-saturate));
    border-bottom: 1px solid var(--color-nav-border);
    box-shadow: var(--shadow-xl);
    padding: var(--space-3) var(--space-4);
    gap: 2px;
  }
}

.mobile-menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-nav-divider);
  font-size: var(--font-sm);
  font-weight: 600;
  color: var(--color-text-muted);
}

.mobile-menu-close {
  display: flex;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: var(--space-1);
  border-radius: var(--radius-sm);
  &:hover { color: var(--color-text-primary); background: var(--color-nav-hover-bg); }
}

.mobile-item {
  display: flex;
  flex-direction: column;
}

.mobile-section {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-3) var(--space-3);
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-nav-text);
  font-size: var(--font-md);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover { background: var(--color-nav-hover-bg); }
  &.active {
    background: var(--color-nav-active-bg);
    color: var(--color-nav-active-text);
    font-weight: 600;
  }
}

.mobile-sub {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--space-1) 0 var(--space-2) 48px;
  border-left: 1px solid var(--color-nav-divider);
  margin-left: var(--space-4);
}

.mobile-sub-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-nav-text-muted);
  font-size: var(--font-sm);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover { color: var(--color-nav-text); background: var(--color-nav-hover-bg); }
  &.active {
    color: var(--color-nav-active-text);
    background: var(--color-nav-active-bg);
    font-weight: 500;
    .mobile-sub-dot { background: var(--color-sky-strong); }
  }
}

.mobile-sub-dot {
  width: 5px;
  height: 5px;
  border-radius: var(--radius-full);
  background: var(--color-text-caption);
  flex-shrink: 0;
}

.mobile-sub-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: rgba(56, 189, 248, 0.2);
  color: var(--color-sky-strong);
}

.mobile-user {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-3);
  margin-top: var(--space-3);
  border-top: 1px solid var(--color-nav-divider);
  strong { font-size: var(--font-sm); color: var(--color-text-primary); }
  small { font-size: var(--font-xs); color: var(--color-text-muted); display: block; }
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import { findSectionByPath, menuData } from '@/config/navigation'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const menuOpen = ref(false)

const pageTitle = computed(() => String(route.meta.title ?? '管理后台'))
const activeSectionPath = computed(() => findSectionByPath(route.path)?.path ?? '/overview')
const breadcrumb = computed(() => {
  const currentSection = findSectionByPath(route.path)
  const pages = [currentSection?.title, String(route.meta.title ?? '')].filter(
    (item, index, array) => Boolean(item) && item !== array[index - 1],
  )
  return pages.join(' / ')
})

async function handleLogout() {
  await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', {
    type: 'warning',
  })

  userStore.logout()
  ElMessage.success('已退出登录')
  await router.push('/login')
}

function navigate(path: string) {
  menuOpen.value = false
  const item = menuData.find(m => m.path === path)
  if (item && item.children && item.children.length > 0) {
    void router.push(item.children[0].path)
  } else {
    void router.push(path)
  }
}

onMounted(() => {
  if (!userStore.profile) {
    void userStore.fetchProfile()
  }
})
</script>

<template>
  <div class="layout-shell">
    <aside class="layout-aside" :class="{ open: menuOpen }">
      <div class="brand">
        <div class="brand-logo">LA</div>
        <div>
          <strong>低空驿站</strong>
          <span>项目管理员 Web 控制台</span>
        </div>
      </div>

      <nav class="menu">
        <div v-for="item in menuData" :key="item.path" class="menu-group">
          <button
            type="button"
            class="menu-item"
            :class="{ active: activeSectionPath === item.path }"
            @click="navigate(item.path)"
          >
            <span class="menu-icon">
              <AppIcon :name="item.icon" />
            </span>
            <span class="menu-content">
              <strong>{{ item.title }}</strong>
            </span>
          </button>

          <div v-if="activeSectionPath === item.path" class="submenu">
            <button
              v-for="child in item.children"
              :key="child.path"
              type="button"
              class="submenu-item"
              :class="{ active: route.path === child.path }"
              @click="navigate(child.path)"
            >
              <span class="submenu-dot"></span>
              <span>{{ child.title }}</span>
            </button>
          </div>
        </div>
      </nav>
    </aside>

    <div v-if="menuOpen" class="layout-mask" @click="menuOpen = false" />

    <div class="layout-content">
      <header class="layout-header">
        <div class="header-left">
          <el-button class="menu-trigger" circle @click="menuOpen = !menuOpen">
            <AppIcon name="menu" />
          </el-button>
          <div>
            <div class="header-label">后台管理 / {{ breadcrumb }}</div>
            <h1>{{ pageTitle }}</h1>
          </div>
        </div>

        <div class="header-actions">
          <el-button circle class="icon-button" @click="navigate('/logs/audit')">
            <AppIcon name="bell" />
          </el-button>

          <div class="user-panel page-card">
            <div class="user-meta">
              <strong>{{ userStore.profile?.name ?? '管理员' }}</strong>
              <span>{{ userStore.profile?.role ?? '平台角色' }}</span>
            </div>

            <el-button type="primary" link @click="handleLogout">
              <AppIcon name="logout" />
              <span>退出</span>
            </el-button>
          </div>
        </div>
      </header>

      <main class="layout-main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.layout-shell {
  min-height: 100vh;
  display: flex;
}

.layout-aside {
  position: sticky;
  top: 0;
  z-index: 30;
  width: 272px;
  height: 100vh;
  background: linear-gradient(180deg, var(--color-bg-sidebar-start), var(--color-bg-sidebar-end));
  color: var(--color-text-primary);
  padding: 24px 16px;
  overflow-y: auto;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;

  strong,
  span {
    display: block;
  }

  strong {
    color: var(--color-text-primary);
  }

  span {
    margin-top: 4px;
    color: var(--color-text-muted);
    font-size: var(--font-small);
  }
}

.brand-logo {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #60a5fa);
  font-weight: 700;
  color: #fff;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.menu-group {
  display: flex;
  flex-direction: column;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 12px;
  border: 1px solid transparent;
  border-radius: 16px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  text-align: left;
  transition:
    background 0.2s ease,
    transform 0.2s ease,
    border-color 0.2s ease;

  &:hover {
    background: rgba(37, 99, 235, 0.06);
    transform: translateX(2px);
  }

  &:active {
    transform: translateX(0);
  }

  &.active {
    background: rgba(37, 99, 235, 0.12);
    border-color: rgba(37, 99, 235, 0.2);
  }
}

.menu-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: rgba(37, 99, 235, 0.08);
  color: var(--color-accent);
}

.menu-content {
  display: flex;
  flex-direction: column;

  strong {
    font-size: var(--font-medium);
    color: var(--color-text-primary);
  }

  small {
    color: var(--color-text-muted);
  }
}

.submenu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
  padding-left: 64px;
}

.submenu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: var(--color-text-muted);
  font-size: var(--font-medium);
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;

  &:hover {
    color: var(--color-text-primary);
    background: rgba(37, 99, 235, 0.06);
  }

  &.active {
    color: var(--color-accent);
    background: rgba(37, 99, 235, 0.1);
    font-weight: 500;

    .submenu-dot {
      background: var(--color-accent);
      box-shadow: 0 0 8px rgba(37, 99, 235, 0.4);
    }
  }
}

.submenu-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-border-light);
  transition: all 0.2s ease;
}

.layout-content {
  flex: 1;
  min-width: 0;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 88px;
  padding: 20px 24px;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);

  h1 {
    margin: 4px 0 0;
    font-size: calc(var(--font-large) * 1.3);
    color: var(--color-text-primary);
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-label {
  color: var(--color-text-muted);
  font-size: var(--font-medium);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-button,
.menu-trigger {
  border: 1px solid var(--color-border);
}

.menu-trigger {
  display: none;
}

.user-panel {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  background: var(--color-bg-card);
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;

  strong {
    color: var(--color-text-primary);
  }

  span {
    color: var(--color-text-muted);
    font-size: var(--font-small);
  }
}

.layout-main {
  padding: 24px;
  background: var(--color-bg-canvas);
}

.layout-mask {
  display: none;
}

@media (max-width: 960px) {
  .layout-aside {
    position: fixed;
    left: 0;
    transform: translateX(-100%);
    transition: transform 0.2s ease;

    &.open {
      transform: translateX(0);
    }
  }

  .layout-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.38);
    z-index: 20;
  }

  .layout-header {
    padding: 18px 16px;
    align-items: flex-start;
    flex-direction: column;
  }

  .header-left,
  .header-actions {
    width: 100%;
  }

  .header-actions {
    justify-content: space-between;
  }

  .menu-trigger {
    display: inline-flex;
  }

  .layout-main {
    padding: 16px;
  }
}
</style>

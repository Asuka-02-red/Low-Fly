import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '@/stores/user'

const storage = (() => {
  const store = new Map<string, string>()

  return {
    getItem(key: string) {
      return store.has(key) ? store.get(key)! : null
    },
    setItem(key: string, value: string) {
      store.set(key, value)
    },
    removeItem(key: string) {
      store.delete(key)
    },
    clear() {
      store.clear()
    },
  }
})()

Object.defineProperty(globalThis, 'localStorage', {
  value: storage,
  configurable: true,
})

describe('useUserStore remembered login', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('stores username and password when remember password is enabled', () => {
    const userStore = useUserStore()

    userStore.syncRememberedLogin(
      {
        username: 'admin',
        password: 'Secret123',
      },
      true,
    )

    expect(userStore.getRememberedLogin()).toEqual({
      username: 'admin',
      password: 'Secret123',
      rememberPassword: true,
    })
  })

  it('keeps remembered password after logout', () => {
    const userStore = useUserStore()

    userStore.syncRememberedLogin(
      {
        username: 'ops-admin',
        password: 'Password!234',
      },
      true,
    )
    localStorage.setItem('admin_token', 'token')
    localStorage.setItem(
      'admin_profile',
      JSON.stringify({
        name: '管理员',
        role: 'ADMIN',
        organization: '低空驿站',
        phone: '13800000000',
      }),
    )

    userStore.logout()

    expect(userStore.getRememberedLogin()).toEqual({
      username: 'ops-admin',
      password: 'Password!234',
      rememberPassword: true,
    })
  })

  it('clears remembered login when remember password is disabled', () => {
    const userStore = useUserStore()

    userStore.syncRememberedLogin(
      {
        username: 'admin',
        password: 'Secret123',
      },
      true,
    )
    userStore.syncRememberedLogin(
      {
        username: 'admin',
        password: 'Secret123',
      },
      false,
    )

    expect(userStore.getRememberedLogin()).toEqual({
      username: '',
      password: '',
      rememberPassword: false,
    })
  })
})

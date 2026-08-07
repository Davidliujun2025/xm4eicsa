<template>
  <div class="layout">
    <aside class="sidebar">
      <img src="@/assets/logo.png" alt="CarePilot AI" class="logo" />
      <nav>
        <button type="button">
          <svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m12 14 4-4"></path><path d="M3.34 19a10 10 0 1 1 17.32 0"></path></svg>
          <span>管理看板</span>
        </button>
        <button type="button" @click="go('users')">
          <svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><path d="M16 3.128a4 4 0 0 1 0 7.744"></path><path d="M22 21v-2a4 4 0 0 0-3-3.87"></path><circle cx="9" cy="7" r="4"></circle></svg>
          <span>用户管理</span>
        </button>
        <button type="button" class="active" @click="go('tokens')">
          <svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z"></path><circle cx="16.5" cy="7.5" r=".5" fill="currentColor"></circle></svg>
          <span>Token 管理</span>
        </button>
        <button type="button" @click="go('forbidden')">
          <svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"></path><path d="M12 8v4"></path><path d="M12 16h.01"></path></svg>
          <span>违禁词管理</span>
        </button>
        <button type="button">
          <svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect width="8" height="4" x="8" y="2" rx="1" ry="1"></rect><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"></path><path d="m9 14 2 2 4-4"></path></svg>
          <span>评估报告</span>
        </button>
        <button type="button">
          <svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path><path d="M3 3v5h5"></path><path d="M12 7v5l4 2"></path></svg>
          <span>操作日志</span>
        </button>
        <button type="button">
          <svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915"></path><circle cx="12" cy="12" r="3"></circle></svg>
          <span>系统设置</span>
        </button>
      </nav>
    </aside>

    <div class="main">
      <header>
        <div>
          <h1>Token 配额管理</h1>
          <p>管理所有客服账号的每日 Token 配额</p>
        </div>
        <div class="header-actions">
          <span class="service-status"><span></span>AI服务正常</span>
          <div class="current-user-wrap" ref="userMenuRef">
            <button type="button" class="current-user" @click="toggleUserMenu">
              <div class="current-user__avatar">{{ avatar }}</div>
              <span class="current-user-name">{{ currentUser?.username || '正在加载' }}</span>
              <svg class="current-user-arrow" :class="{ 'is-open': userMenuOpen }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6"></path></svg>
            </button>

            <div v-if="userMenuOpen" class="user-dropdown">
              <button type="button" @click="openSwitchModal">切换账号</button>
              <button type="button" @click="openLogoutModal">退出登录</button>
            </div>
          </div>
        </div>
      </header>
      <main><slot /></main>

      <div v-if="accountModalType" class="confirm-overlay">
        <div class="confirm-box">
          <h3>{{ accountModalType === 'logout' ? '确认退出登录' : '确认切换账号' }}</h3>
          <div class="confirm-actions">
            <button type="button" class="confirm-cancel" @click="closeAccountModal" :disabled="accountSubmitting">否</button>
            <button type="button" class="confirm-ok" @click="confirmAccountAction" :disabled="accountSubmitting">{{ accountSubmitting ? '处理中...' : '是' }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

type CurrentUser = {
  userId: number
  username: string
  roleType: string
}

const currentUser = ref<CurrentUser | null>(null)
const avatar = computed(() =>
  (currentUser.value?.username || '?').slice(0, 1),
)
const isLocalDevelopment =
  window.location.hostname === '127.0.0.1' || window.location.hostname === 'localhost'
const localUrl = (port: number) => `${window.location.protocol}//${window.location.hostname}:${port}/`

const links: Record<string, string> = {
  users: import.meta.env.VITE_ADMIN_USERS_URL || (isLocalDevelopment ? localUrl(5176) : '/admin/users/'),
  tokens: import.meta.env.VITE_ADMIN_TOKENS_URL || (isLocalDevelopment ? localUrl(5177) : '/admin/tokens/'),
  forbidden: import.meta.env.VITE_ADMIN_FORBIDDEN_WORDS_URL || (isLocalDevelopment ? localUrl(5179) : '/admin/forbidden-words/'),
}

function go(key: string) {
  const target = links[key]
  if (target) window.location.href = target
}

// ===== account dropdown + logout (unified with workbench) =====
const userMenuOpen = ref(false)
const accountModalType = ref<'' | 'logout' | 'switch'>('')
const accountSubmitting = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

function handleClickOutside(e: MouseEvent) {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target as Node)) {
    userMenuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

function openSwitchModal() {
  userMenuOpen.value = false
  accountModalType.value = 'switch'
}

function openLogoutModal() {
  userMenuOpen.value = false
  accountModalType.value = 'logout'
}

function closeAccountModal() {
  accountModalType.value = ''
}

function readCookie(name: string) {
  const cookie = document.cookie.split('; ').find((item) => item.startsWith(`${name}=`))
  return cookie ? decodeURIComponent(cookie.slice(name.length + 1)) : ''
}

async function ensureCsrfToken() {
  if (readCookie('XSRF-TOKEN')) return readCookie('XSRF-TOKEN')
  await fetch('/api/v1/public/login-config', { credentials: 'include' })
  return readCookie('XSRF-TOKEN')
}

async function logoutAndRedirect(returnUrl: string) {
  try {
    const csrfToken = await ensureCsrfToken()
    try {
      await fetch('/api/v1/auth/logout', {
        method: 'POST',
        credentials: 'include',
        headers: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : undefined,
      })
    } catch {
      // ignore network errors and still redirect
    }
  } finally {
    const loginUrl = import.meta.env.VITE_LOGIN_URL || (isLocalDevelopment ? localUrl(15173) : `${window.location.origin}/`)
    const url = new URL(loginUrl, window.location.origin)
    url.searchParams.set('returnUrl', returnUrl)
    window.location.replace(url.toString())
  }
}

async function confirmAccountAction() {
  accountSubmitting.value = true
  try {
    await logoutAndRedirect(window.location.href)
  } finally {
    accountSubmitting.value = false
  }
}

onMounted(async () => {
  try {
    const response = await fetch('/api/v1/auth/me', { credentials: 'include' })
    if (response.status === 401) {
      window.location.href = `/?returnUrl=${encodeURIComponent(window.location.pathname)}`
      return
    }
    if (!response.ok) throw new Error('无法获取当前用户')
    const payload = await response.json()
    currentUser.value = payload.data
  } catch {
    currentUser.value = null
  }
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; background: #f5f7fb; color: #102a56; }
.sidebar { width: 232px; flex: 0 0 232px; border-right: 1px solid #e5eaf2; background: #fff; }
.logo { display: block; box-sizing: border-box; width: 100%; padding: 24px 20px; object-fit: contain; }
nav { display: grid; gap: 4px; padding: 0 12px; }
nav button { border: 0; border-radius: 12px; background: transparent; color: #64748b; padding: 12px 16px; text-align: left; cursor: pointer; display: flex; align-items: center; gap: 12px; font-size: 15px; font-weight: 500; line-height: 1.5; }
nav button svg { width: 22px; height: 22px; flex: 0 0 22px; }
nav button:hover { background: #f8fafc; color: #1e293b; }
nav button.active { background: #eff6ff; color: #2563eb; font-weight: 600; }
.main { min-width: 0; flex: 1; }
header { display: flex; min-height: 88px; align-items: center; justify-content: space-between; border-bottom: 1px solid #e5eaf2; background: #fff; padding: 0 32px; }
h1 { margin: 0; font-size: 22px; }
header p { margin: 5px 0 0; color: #667085; font-size: 13px; }
.header-actions, .current-user, .service-status { display: flex; align-items: center; }
.header-actions { gap: 20px; }
.service-status { min-height: 40px; padding: 0 13px; gap: 8px; border: 1px solid #dde5f0; border-radius: 8px; background: #fff; color: #22b573; font-size: 13px; font-weight: 600; }
.service-status span { width: 8px; height: 8px; border-radius: 50%; background: #22b573; }
.current-user { gap: 8px; color: #0f1f3d; font-size: 14px; font-weight: 600; }
.current-user__avatar { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 50%; background: #eaf2ff; color: #1769f6; }
main { padding: 24px; }
@media (max-width: 800px) { .sidebar { display: none; } header { padding: 0 18px; } .service-status { display: none; } main { padding: 16px; } }
.current-user-wrap { position: relative; display: flex; align-items: center; }
.current-user { border: 0; background: transparent; cursor: pointer; padding: 4px 0; font-family: inherit; }
.current-user-name { font-weight: 600; }
.current-user-arrow { width: 16px; height: 16px; color: #94a3b8; transition: transform 0.2s ease; }
.current-user-arrow.is-open { transform: rotate(180deg); }
.user-dropdown { position: absolute; right: 0; top: calc(100% + 8px); z-index: 60; width: 120px; padding: 8px 0; border: 1px solid #e5eaf2; border-radius: 12px; background: #fff; box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12); }
.user-dropdown button { width: 100%; border: 0; padding: 8px 16px; background: transparent; color: #334155; font-size: 14px; text-align: left; cursor: pointer; }
.user-dropdown button:hover { background: #f1f5f9; }
.user-dropdown button:last-child:hover { color: #ef4444; }
.confirm-overlay { position: fixed; inset: 0; z-index: 200; display: flex; align-items: center; justify-content: center; background: rgba(15, 23, 42, 0.4); }
.confirm-box { width: 360px; padding: 24px; border-radius: 12px; background: #fff; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2); }
.confirm-box h3 { margin: 0 0 24px; font-size: 16px; font-weight: 500; color: #1e293b; }
.confirm-actions { display: flex; justify-content: flex-end; gap: 12px; }
.confirm-actions button { padding: 8px 20px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; color: #475569; font-size: 14px; cursor: pointer; }
.confirm-actions .confirm-ok { border-color: #3b82f6; background: #3b82f6; color: #fff; }
.confirm-actions button:disabled { opacity: 0.6; cursor: default; }
</style>

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
          <span class="service-status"><i></i>AI 服务正常</span>
          <span class="current-user">
            <b>{{ avatar }}</b>
            {{ currentUser?.username || '正在加载' }}
          </span>
        </div>
      </header>
      <main><slot /></main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

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
.header-actions, .current-user, .service-status { display: flex; align-items: center; gap: 9px; }
.header-actions { gap: 20px; }
.service-status { color: #07883f; }
.service-status i { width: 8px; height: 8px; border-radius: 50%; background: currentColor; }
.current-user b { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 50%; background: #eaf2ff; color: #1769f6; }
main { padding: 24px; }
@media (max-width: 800px) { .sidebar { display: none; } header { padding: 0 18px; } .service-status { display: none; } main { padding: 16px; } }
</style>

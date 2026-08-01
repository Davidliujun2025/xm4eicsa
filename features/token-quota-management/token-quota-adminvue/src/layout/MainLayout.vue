<template>
  <div class="layout">
    <aside class="sidebar">
      <img src="@/assets/logo.png" alt="CarePilot AI" class="logo" />
      <nav>
        <button type="button">管理看板</button>
        <button type="button" @click="go('users')">用户管理</button>
        <button type="button" class="active" @click="go('tokens')">Token 管理</button>
        <button type="button" @click="go('forbidden')">违禁词管理</button>
        <button type="button">评估报告</button>
        <button type="button">操作日志</button>
        <button type="button">系统设置</button>
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

const links: Record<string, string> = {
  users: import.meta.env.VITE_ADMIN_USERS_URL || '/admin/users/',
  tokens: import.meta.env.VITE_ADMIN_TOKENS_URL || '/admin/tokens/',
  forbidden: import.meta.env.VITE_ADMIN_FORBIDDEN_WORDS_URL || '/admin/forbidden-words/',
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
.logo { box-sizing: border-box; width: 100%; padding: 24px 20px; object-fit: contain; }
nav { display: grid; gap: 5px; padding: 0 12px; }
nav button { border: 0; border-radius: 10px; background: transparent; color: #667085; padding: 12px 16px; text-align: left; cursor: pointer; font: inherit; }
nav button:hover, nav button.active { background: #eff6ff; color: #1769f6; }
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

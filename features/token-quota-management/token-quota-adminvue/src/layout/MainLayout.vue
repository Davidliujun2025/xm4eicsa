<template>
  <div class="layout-wrap">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <img src="@/assets/logo.png" alt="CarePilot AI" class="logo-icon" />
      </div>

      <nav class="sidebar-menu">
        <button class="menu-item" type="button" @click="goAdminPage('dashboard')">
          <svg viewBox="0 0 24 24" aria-hidden="true" class="menu-icon-svg"><path d="m12 14 4-4"></path><path d="M3.34 19a10 10 0 1 1 17.32 0"></path></svg>
          <span>管理看板</span>
        </button>
        <button class="menu-item" type="button" @click="goAdminPage('users')">
          <svg viewBox="0 0 24 24" aria-hidden="true" class="menu-icon-svg"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><path d="M16 3.128a4 4 0 0 1 0 7.744"></path><path d="M22 21v-2a4 4 0 0 0-3-3.87"></path><circle cx="9" cy="7" r="4"></circle></svg>
          <span>用户管理</span>
        </button>

        <button class="menu-item active" type="button" aria-current="page" @click="goAdminPage('token')">
          <svg viewBox="0 0 24 24" aria-hidden="true" class="menu-icon-svg"><path d="M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z"></path><circle cx="16.5" cy="7.5" r=".5" fill="currentColor"></circle></svg>
          <span>Token 管理</span>
        </button>

        <button class="menu-item" type="button" @click="goAdminPage('forbidden')">
          <svg viewBox="0 0 24 24" aria-hidden="true" class="menu-icon-svg"><path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"></path><path d="M12 8v4"></path><path d="M12 16h.01"></path></svg>
          <span>违禁词管理</span>
        </button>
        <button class="menu-item" type="button">
          <svg viewBox="0 0 24 24" aria-hidden="true" class="menu-icon-svg"><rect width="8" height="4" x="8" y="2" rx="1" ry="1"></rect><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"></path><path d="m9 14 2 2 4-4"></path></svg>
          <span>评估报告</span>
        </button>
        <button class="menu-item" type="button">
          <svg viewBox="0 0 24 24" aria-hidden="true" class="menu-icon-svg"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path><path d="M3 3v5h5"></path><path d="M12 7v5l4 2"></path></svg>
          <span>操作日志</span>
        </button>
        <button class="menu-item" type="button">
          <svg viewBox="0 0 24 24" aria-hidden="true" class="menu-icon-svg"><path d="M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915"></path><circle cx="12" cy="12" r="3"></circle></svg>
          <span>系统设置</span>
        </button>
      </nav>
    </aside>

    <div class="main-wrap">
      <header class="page-header">
        <div>
          <h1>Token额度管理</h1>
          <p>管理所有客服账号的每日 Token 配额</p>
        </div>
        <div class="page-header-right">
          <div class="service-status"><span></span>AI服务正常</div>
          <div class="current-user">
            <div class="current-user-avatar">陈</div>
            <span>陈一冉</span>
            <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="m6 9 6 6 6-6"></path></svg>
          </div>
        </div>
      </header>

      <main class="page-content">
        <slot></slot>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
const adminLinks: Record<string, string> = {
  users: 'http://localhost:5175/',
  token: 'http://localhost:5173/',
  forbidden: 'http://localhost:5500/'
}

function goAdminPage(key: string) {
  const target = adminLinks[key]
  if (target) {
    window.location.href = target
  }
}
</script>

<style scoped>
.layout-wrap {
  display: flex;
  min-height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 232px;
  background: #fff;
  border-right: 1px solid #e8edf4;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.sidebar-logo {
  padding: 24px 20px;
  display: flex;
  align-items: center;
}
.logo-icon {
  display: block;
  width: 100%;
  max-width: 100%;
  height: auto;
  object-fit: contain;
}

.sidebar-menu {
  flex: 1;
  padding: 0 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.menu-item {
  width: 100%;
  border: 0;
  background: transparent;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #64748b;
  line-height: 1.5;
  letter-spacing: normal;
  word-spacing: normal;
  text-align: left;
}

.menu-item:hover {
  background: #f8fafc;
  color: #1e293b;
}

.menu-item.active {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 500;
}
.menu-icon-svg {
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  stroke: currentColor;
  stroke-width: 2;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.main-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.page-header {
  min-height: 88px;
  border-bottom: 1px solid #e8edf4;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: #fff;
}
.page-header h1 {
  margin: 0;
  color: #0f1f3d;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}
.page-header p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}
.page-header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.service-status {
  min-height: 40px;
  padding: 0 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  color: #22b573;
  font-size: 13px;
  font-weight: 600;
}
.service-status > span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22b573;
}
.current-user {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f1f3d;
  font-size: 14px;
  font-weight: 600;
}

.current-user-avatar {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  border-radius: 50%;
  background: #eaf2ff;
  color: #1769f6;
  justify-content: center;
}

.current-user svg {
  stroke: currentColor;
  stroke-width: 2;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.page-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
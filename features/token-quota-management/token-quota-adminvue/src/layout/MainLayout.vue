<template>
  <div class="layout-wrap">
    <!-- 左侧侧边栏 -->
    <aside class="sidebar">
      <!-- 左上角仅Logo图片，无文字 -->
      <div class="sidebar-logo">
        <img src="@/assets/logo.jpg" alt="CarePilot AI" class="logo-icon" />
      </div>

      <!-- 导航菜单 严格复刻图二顺序与图标 -->
      <nav class="sidebar-menu">
        <div class="menu-item">
          <span class="menu-icon">▦</span>
          <span>管理看板</span>
        </div>
        <div class="menu-item">
          <span class="menu-icon">👥</span>
          <span>用户管理</span>
        </div>

        <!-- 可折叠 Token管理 -->
        <div class="menu-item fold-menu" @click="toggleTokenMenu">
          <div class="menu-item-inner">
            <span class="menu-icon">ⓞ</span>
            <span>Token 管理</span>
          </div>
          <span class="arrow">{{ tokenOpen ? '▼' : '▶' }}</span>
        </div>
        <div class="sub-menu" v-if="tokenOpen">
          <div class="sub-menu-item active">Token额度管理</div>
        </div>

        <div class="menu-item">
          <span class="menu-icon">🛡</span>
          <span>违禁词管理</span>
        </div>
        <div class="menu-item">
          <span class="menu-icon">📄</span>
          <span>评估报告</span>
        </div>
        <div class="menu-item">
          <span class="menu-icon">🕒</span>
          <span>操作日志</span>
        </div>
        <div class="menu-item">
          <span class="menu-icon">⚙</span>
          <span>系统设置</span>
        </div>
      </nav>
    </aside>

    <!-- 右侧主区域 -->
    <div class="main-wrap">
      <!-- 顶部导航栏 -->
      <header class="page-header">
        <div></div>
        <div class="header-right">
          <span class="header-item">
            <span class="header-icon">❓</span>
            帮助中心
          </span>
          <span class="header-item bell-wrap">
            🔔
            <sup>2</sup>
          </span>
          <div class="user-header">
            <span class="avatar">倩</span>
            <span>张小倩</span>
            <span class="dropdown-arrow">▼</span>
          </div>
        </div>
      </header>

      <!-- 页面内容插槽 -->
      <main class="page-content">
        <slot></slot>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
// Token管理折叠状态
const tokenOpen = ref(true)
const toggleTokenMenu = () => {
  tokenOpen.value = !tokenOpen.value
}
</script>

<style scoped>
.layout-wrap {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏容器 */
.sidebar {
  width: 240px;
  background: var(--bg-white);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}
.sidebar-logo {
  padding: 18px 16px;
  border-bottom: 1px solid var(--border-color);
}
.logo-icon {
  width: 100%;
  max-width: 180px;
}

.sidebar-menu {
  flex: 1;
  padding: 16px 12px;
}
.menu-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.fold-menu {
  justify-content: space-between;
}
.menu-item-inner {
  display: flex;
  align-items: center;
  gap: 8px;
}
.menu-icon {
  font-size: 20px;
  color: #666;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}
.menu-item:hover:not(.active) {
  background: #f3f4f6;
}
.fold-menu .arrow {
  font-size: 12px;
  color: #888;
}
.sub-menu {
  padding-left: 16px;
  margin-top: 4px;
}
.sub-menu-item {
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  margin-bottom: 2px;
}
.sub-menu-item.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 500;
}

/* 右侧布局 */
.main-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.page-header {
  height: 64px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #fff;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 26px;
}
.header-item {
  cursor: pointer;
  font-size: 15px;
  color: var(--text-normal);
  display: flex;
  align-items: center;
  gap: 6px;
}
.header-icon {
  font-size: 18px;
  color: #666;
}
.bell-wrap {
  position: relative;
}
.bell-wrap sup {
  position: absolute;
  top: -4px;
  right: -6px;
  background: #ef4444;
  color: white;
  font-size: 10px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.user-header {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.dropdown-arrow {
  font-size: 14px;
  color: #666;
}

.page-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
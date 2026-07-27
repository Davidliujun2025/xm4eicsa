<script setup lang="ts">
import { ref } from 'vue'

const count = ref(0)

// 品牌配置（与 LoginPage 一致）
const bgImage = '/login-bg.png'
const config = {
  sloganTitle: '让每一位客服都拥有AI超能力',
  sloganSub: '专为电商打造的智能客服助手，实现秒级响应，提升转化效率，让沟通更智能、更高效。'
}
</script>

<template>
  <div class="app-container">
    <!-- ======== 左侧品牌区（与 LoginPage 完全一致） ======== -->
    <div class="brand-panel">
      <div class="bg-layer" :style="{ backgroundImage: `url(${bgImage})` }"></div>
      <div class="glass-overlay"></div>
      <div class="brand-content">
        <div class="brand-header">
          <div class="brand-logo">
            <img src="/logo.png" alt="CarePilot AI Logo" />
          </div>
        </div>
        <div class="slogan-wrapper">
          <div class="slogan-title">{{ config.sloganTitle }}</div>
          <div class="slogan-sub">{{ config.sloganSub }}</div>
        </div>
        <div class="brand-footer">
          <span class="trust-badge">
            <i class="fas fa-check-circle"></i> 超过 10,000+ 电商企业正在使用
          </span>
        </div>
      </div>
    </div>

    <!-- ======== 右侧内容区（欢迎 & 计数器，样式与 LoginPage 统一） ======== -->
    <div class="content-panel">
      <div class="welcome-box">
        <h1 class="welcome-title">欢迎使用 CarePilot AI</h1>
        <p class="welcome-desc">
          开始构建您的智能客服助手，体验高效、智能的对话管理。
        </p>

        <div class="demo-card">
          <h2>快速体验</h2>
          <p>点击下方按钮测试响应式交互：</p>
          <button class="counter-btn" @click="count++">
            点击次数：{{ count }}
          </button>
        </div>

        <div class="quick-links">
          <a href="https://vite.dev/" target="_blank" class="link-item">
            <svg class="icon" viewBox="0 0 24 24" width="20" height="20">
              <use href="/icons.svg#documentation-icon"></use>
            </svg>
            探索 Vite
          </a>
          <a href="https://vuejs.org/" target="_blank" class="link-item">
            <svg class="icon" viewBox="0 0 24 24" width="20" height="20">
              <use href="/icons.svg#documentation-icon"></use>
            </svg>
            Vue 文档
          </a>
          <a href="https://github.com/vitejs/vite" target="_blank" class="link-item">
            <svg class="icon" viewBox="0 0 24 24" width="20" height="20">
              <use href="/icons.svg#github-icon"></use>
            </svg>
            GitHub
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 全局重置（与 LoginPage 一致） ===== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

/* ===== 主容器 ===== */
.app-container {
  width: 1200px;
  max-width: 100%;
  min-height: 680px;
  background: #ffffff;
  border-radius: 32px;
  box-shadow: 0 30px 60px rgba(18, 52, 106, 0.12);
  display: flex;
  overflow: hidden;
  transition: all 0.2s;
}

/* ===== 左侧品牌区（完全复制 LoginPage） ===== */
.brand-panel {
  flex: 0 0 55%;
  position: relative;
  overflow: hidden;
  color: #ffffff;
}

.bg-layer {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  z-index: 0;
}

.glass-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0-0.3px);
  z-index: 1;
}

.brand-content {
  position: relative;
  z-index: 2;
  padding: 48px 50px 40px 50px;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 680px;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}

.brand-logo {
  width: auto;
  height: auto;
  max-width: 160px;
  max-height: 80px;
  overflow: visible;
}
.brand-logo img {
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  mix-blend-mode: multiply;
}

.slogan-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.slogan-title {
  font-size: 38px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 16px;
  letter-spacing: -0.5px;
  color: #0a2a4a;
}

.slogan-sub {
  font-size: 16px;
  line-height: 1.7;
  opacity: 0.7;
  max-width: 85%;
  margin-bottom: 36px;
  font-weight: 400;
  color: #1a3a5a;
}

.brand-footer {
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.20);
}

.trust-badge {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  gap: 8px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.trust-badge i {
  color: #ffffff;
}

/* ===== 右侧内容区（与 LoginPage 的 .login-panel 风格统一） ===== */
.content-panel {
  flex: 1;
  padding: 48px 50px 40px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #ffffff;
  min-width: 350px;
}

.welcome-box {
  width: 100%;
  max-width: 440px;
}

.welcome-title {
  font-size: 28px;
  font-weight: 700;
  color: #0a2a4a;
  letter-spacing: -0.3px;
  margin-bottom: 6px;
}

.welcome-desc {
  font-size: 15px;
  color: #6b7a8f;
  margin-top: 6px;
  margin-bottom: 30px;
  font-weight: 400;
}

.demo-card {
  background: #f8faff;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #dce3ef;
  margin-bottom: 28px;
}
.demo-card h2 {
  font-size: 18px;
  font-weight: 600;
  color: #0a2a4a;
  margin-bottom: 6px;
}
.demo-card p {
  font-size: 14px;
  color: #6b7a8f;
  margin-bottom: 16px;
}

.counter-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  background: #2a6df4;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
}
.counter-btn:hover {
  background: #1a4fbf;
  box-shadow: 0 6px 20px rgba(42, 109, 244, 0.25);
}

.quick-links {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  border-top: 1px solid #e8edf5;
  padding-top: 24px;
}

.link-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #2a6df4;
  font-weight: 500;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}
.link-item:hover {
  color: #1a4fbf;
  text-decoration: underline;
}
.link-item .icon {
  fill: currentColor;
}

/* ===== 响应式（与 LoginPage 一致） ===== */
@media (max-width: 1024px) {
  .app-container {
    flex-direction: column;
    border-radius: 24px;
    min-height: auto;
  }
  .brand-panel {
    flex: 0 0 auto;
    border-radius: 24px 24px 0 0;
    min-height: 320px;
  }
  .brand-content {
    min-height: 320px;
    padding: 32px 36px;
  }
  .content-panel {
    padding: 32px;
    min-width: unset;
  }
  .welcome-box {
    max-width: 100%;
  }
  .slogan-title {
    font-size: 30px;
  }
  .slogan-sub {
    max-width: 100%;
  }
}
@media (max-width: 640px) {
  .brand-content {
    padding: 24px 20px;
    min-height: 260px;
  }
  .brand-header {
    margin-bottom: 20px;
  }
  .slogan-title {
    font-size: 24px;
  }
  .slogan-sub {
    font-size: 14px;
  }
  .content-panel {
    padding: 24px 20px;
  }
  .welcome-title {
    font-size: 24px;
  }
  .quick-links {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
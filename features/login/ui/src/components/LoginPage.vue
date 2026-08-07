<template>
  <div class="app-container">
    <!-- ======== 左侧品牌区（图片背景 + 虚化） ======== -->
    <div class="brand-panel">
      <!-- 背景图片层 -->
      <div class="bg-layer" :style="{ backgroundImage: `url(${bgImage})` }"></div>
      <!-- 毛玻璃遮罩层 -->
      <div class="glass-overlay"></div>

      <!-- 内容区域（始终在顶层） -->
      <div class="brand-content">
        <div class="brand-header">
          <div class="brand-logo">
            <img src="/logo.png" alt="ServicePilot Logo" />
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

    <!-- ======== 右侧登录区（保持不变） ======== -->
    <div class="login-panel">
      <h1 class="page-title">欢迎回来</h1>
      <p class="page-sub">登录您的 CarePilot AI 账号</p>

      <!-- 错误信息 -->
      <div class="error-msg" :class="{ hidden: !loginError }">
        <i class="fas fa-exclamation-circle"></i> {{ loginError }}
      </div>

      <!-- 账号输入框 -->
      <div class="form-group">
        <label for="accountInput">账号</label>
        <div class="input-wrapper">
          <span class="prefix-icon"><i class="fas fa-user"></i></span>
          <input
            id="accountInput"
            type="text"
            v-model="loginForm.account"
            placeholder="请输入账号、手机号或邮箱"
            @input="clearError"
            @keydown.enter="handleLogin"
            autocomplete="username"
          />
        </div>
      </div>

      <!-- 密码输入框 -->
      <div class="form-group">
        <label for="pwdInput">密码</label>
        <div class="input-wrapper">
          <span class="prefix-icon"><i class="fas fa-lock"></i></span>
          <input
            id="pwdInput"
            :type="pwdVisible ? 'text' : 'password'"
            v-model="loginForm.password"
            placeholder="请输入密码"
            @input="clearError"
            @focus="pwdHintFocused = true"
            @blur="pwdHintFocused = false"
            @keydown.enter="handleLogin"
            autocomplete="current-password"
          />
          <button
            type="button"
            class="toggle-pwd"
            :aria-label="pwdVisible ? '隐藏密码' : '显示密码'"
            :title="pwdVisible ? '隐藏密码' : '显示密码'"
            @click="pwdVisible = !pwdVisible"
          >
            <svg
              v-if="pwdVisible"
              viewBox="0 0 24 24"
              aria-hidden="true"
              focusable="false"
            >
              <path d="M3 3l18 18" />
              <path d="M10.6 6.2A10.8 10.8 0 0 1 12 6c6.5 0 10 6 10 6a17.2 17.2 0 0 1-3 3.7" />
              <path d="M14.1 14.2a3 3 0 0 1-4.2-4.3" />
              <path d="M6.2 6.3C3.5 8.2 2 12 2 12s3.5 6 10 6a10.7 10.7 0 0 0 3.8-.7" />
            </svg>
            <svg
              v-else
              viewBox="0 0 24 24"
              aria-hidden="true"
              focusable="false"
            >
              <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
          </button>
        </div>
        <div class="pwd-hint" :class="pwdHintClass">
          <span class="hint-text">{{ pwdHintText }}</span>
        </div>
      </div>

      <!-- 记住我 + 忘记密码 -->
      <div class="row-actions">
        <label class="remember-me">
          <input type="checkbox" v-model="loginForm.remember" />
          <span>记住我</span>
        </label>
        <button class="forgot-link" @click="goToForgot">忘记密码？</button>
      </div>

      <!-- 登录按钮 -->
      <button class="btn-login" :disabled="!canLogin" @click="handleLogin">
        <i class="fas fa-sign-in-alt" style="margin-right:8px;"></i> 登录
      </button>

      <!-- 底部版权 -->
      <div class="footer-links">
        <div class="left-links">
          <a href="#">服务条款</a>
          <a href="#">隐私政策</a>
        </div>
        <div class="right-copy">© 2026 CarePilot AI</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { apiClient } from '../utils/api'
import type { LoginForm, PwdChecks } from '../types'

const isLocalDevelopment =
  window.location.hostname === '127.0.0.1' || window.location.hostname === 'localhost'
const localUrl = (port: number, path = '/') =>
  `${window.location.protocol}//${window.location.hostname}:${port}${path}`

const defaultForgotUrl =
  import.meta.env.VITE_FORGOT_PASSWORD_URL ||
  (isLocalDevelopment ? localUrl(5175) : `${window.location.origin}/forgot-password/`)
const workbenchUrl =
  import.meta.env.VITE_WORKBENCH_URL ||
  (isLocalDevelopment ? localUrl(5174) : `${window.location.origin}/workbench/`)
const adminUsersUrl =
  import.meta.env.VITE_ADMIN_USERS_URL ||
  (isLocalDevelopment ? localUrl(5176) : `${window.location.origin}/admin/users/`)
const SAFE_LOCAL_PORTS = new Set(['5173', '5174', '5175', '5176', '5177', '5178', '5179', '5180'])

const FORGOT_PASSWORD_URL =
  new URLSearchParams(window.location.search).get('forgotUrl') ||
  defaultForgotUrl
const RETURN_URL =
  new URLSearchParams(window.location.search).get('returnUrl') || ''

// =============================================================
// 背景图片（可配置，此处使用在线示例图）
// =============================================================
const bgImage = ref('/login-bg.png')

// =============================================================
// 1. 可配置的宣传文案
// =============================================================
const config = reactive({
  sloganTitle: '让每一位客服都拥有AI超能力',
  sloganSub: '专为电商打造的智能客服助手，实现秒级响应，提升转化效率，让沟通更智能、更高效。'
})

// =============================================================
// 2. 登录表单数据（同原代码）
// =============================================================
const loginForm = reactive<LoginForm>({
  account: '',
  password: '',
  remember: false
})

const pwdVisible = ref(false)
const pwdHintFocused = ref(false)
const loginError = ref('')

// =============================================================
// 3. 密码强度校验（同原代码）
// =============================================================
const pwdChecks = reactive<PwdChecks>({
  lengthOk: false,
  hasUpper: false,
  hasLower: false,
  hasNumber: false,
  hasSpecial: false,
  classCountOk: false
})

const isPwdValid = computed(() => {
  const p = loginForm.password
  const len = p.length
  const hasUpper = /[A-Z]/.test(p)
  const hasLower = /[a-z]/.test(p)
  const hasNumber = /[0-9]/.test(p)
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(p)
  const lengthOk = len >= 12 && len <= 20
  const classCount = [hasUpper, hasLower, hasNumber, hasSpecial].filter(Boolean).length
  const classCountOk = classCount >= 3

  pwdChecks.lengthOk = lengthOk
  pwdChecks.hasUpper = hasUpper
  pwdChecks.hasLower = hasLower
  pwdChecks.hasNumber = hasNumber
  pwdChecks.hasSpecial = hasSpecial
  pwdChecks.classCountOk = classCountOk

  return lengthOk && classCountOk
})

// =============================================================
// 4. 密码提示（同原代码）
// =============================================================
const pwdHintText = computed(() => {
  const pwd = loginForm.password
  if (pwd.length === 0) {
    return '密码需12-20位，包含大写、小写、数字、特殊符号中至少三类'
  }
  if (isPwdValid.value) {
    return '● 密码符合安全要求'
  } else {
    let missing: string[] = []
    if (!pwdChecks.lengthOk) missing.push('长度12-20位')
    if (!pwdChecks.classCountOk) missing.push('至少包含三类字符（大写/小写/数字/特殊符号）')
    if (missing.length) {
      return '○ 需满足：' + missing.join('、')
    }
    return '○ 密码不符合安全要求'
  }
})

const pwdHintClass = computed(() => {
  if (loginForm.password.length === 0) return ''
  return isPwdValid.value ? 'valid' : 'invalid'
})

// =============================================================
// 5. 登录按钮可用性（同原代码）
// =============================================================
const canLogin = computed(() => {
  return loginForm.account.trim() !== '' &&
         loginForm.password.trim() !== ''
})

// =============================================================
// 6. 账号格式校验（同原代码）
// =============================================================
function isValidAccountFormat(account: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  const phoneRegex = /^1[3-9]\d{9}$/
  const usernameRegex = /^[\u4e00-\u9fffA-Za-z0-9_@.-]{2,64}$/
  return emailRegex.test(account) || phoneRegex.test(account) || usernameRegex.test(account)
}

function normalizeReturnUrl(rawReturnUrl: string): string {
  if (!rawReturnUrl) {
    return ''
  }

  try {
    const targetUrl = new URL(rawReturnUrl, window.location.origin)
    const sameOrigin = targetUrl.origin === window.location.origin
    const trustedLocalApp =
      isLocalDevelopment &&
      targetUrl.protocol === window.location.protocol &&
      targetUrl.hostname === window.location.hostname &&
      SAFE_LOCAL_PORTS.has(targetUrl.port)

    if (!sameOrigin && !trustedLocalApp) {
      return ''
    }

    const loginRoot = new URL(window.location.origin)
    const isLoginPage =
      targetUrl.origin === loginRoot.origin &&
      targetUrl.pathname === '/'

    if (isLoginPage) {
      return ''
    }

    return targetUrl.toString()
  } catch {
    return ''
  }
}

// =============================================================
// 7. 登录逻辑（同原代码）
// =============================================================
async function handleLogin() {
  loginError.value = ''
  const account = loginForm.account.trim()
  const password = loginForm.password

  if (!account || !password) {
    loginError.value = '请输入账号和密码'
    return
  }

  if (!isValidAccountFormat(account)) {
    loginError.value = '账号不存在，请找管理员'
    return
  }

  try {
    const response = await apiClient.post('/auth/login', {
      account,
      password,
      rememberMe: loginForm.remember
    })

    const { code, message, data } = response.data

    if (code === 'OK') {
      localStorage.removeItem('carepilot_token')
      localStorage.removeItem('accessToken')
      if (data?.user) {
        localStorage.setItem('carepilot_user', JSON.stringify(data.user))
      }

      if (loginForm.remember) {
        localStorage.setItem('carepilot_remember', 'true')
        localStorage.setItem('carepilot_account', account)
      } else {
        localStorage.removeItem('carepilot_remember')
        localStorage.removeItem('carepilot_account')
      }

      localStorage.removeItem('carepilot_password')

      const roleType = typeof data?.user?.roleType === 'string' ? data.user.roleType : ''
      const backendPath = typeof data?.redirectPath === 'string' ? data.redirectPath : ''
      const normalizedPath = backendPath === '/ai-customer-service' ? '/workbench/' : backendPath
      const normalizedReturnUrl = normalizeReturnUrl(RETURN_URL)
      const redirectUrl = normalizedReturnUrl
        ? normalizedReturnUrl
        : roleType === 'CUSTOMER_SERVICE'
          ? workbenchUrl
          : roleType === 'ADMIN'
            ? adminUsersUrl
            : normalizedPath && normalizedPath.startsWith('/')
              ? `${window.location.origin}${normalizedPath}`
              : `${window.location.origin}/`
      window.location.href = redirectUrl
    } else {
      if (message.includes('不存在')) {
        loginError.value = '账号不存在，请找管理员'
      } else if (message.includes('密码') || message.includes('不正确')) {
        loginError.value = '账号或密码不正确'
      } else {
        loginError.value = message || '登录失败，请稍后重试'
      }
    }
  } catch (error: any) {
    console.error('登录请求失败:', error)
    if (error.response) {
      const msg = error.response.data?.message || '服务器错误'
      loginError.value = msg
    } else if (error.request) {
      loginError.value = '网络异常，请检查后端服务是否启动'
    } else {
      loginError.value = '请求配置错误，请检查 API 地址'
    }
  }
}

function clearError() {
  loginError.value = ''
}

function loadRemembered() {
  const remember = localStorage.getItem('carepilot_remember') === 'true'
  if (remember) {
    const account = localStorage.getItem('carepilot_account') || ''
    const password = ''
    loginForm.account = account
    loginForm.password = password
    loginForm.remember = true
    isPwdValid.value
  }
}

function goToForgot() {
  const loginReturnUrl = `${window.location.origin}/`
  const loginUrl = encodeURIComponent(loginReturnUrl)
  const sep = FORGOT_PASSWORD_URL.includes('?') ? '&' : '?'
  window.location.href = `${FORGOT_PASSWORD_URL}${sep}loginUrl=${loginUrl}`
}

onMounted(() => {
  if (RETURN_URL && !normalizeReturnUrl(RETURN_URL)) {
    const currentUrl = new URL(window.location.href)
    currentUrl.searchParams.delete('returnUrl')
    window.history.replaceState({}, '', currentUrl.toString())
  }
  loadRemembered()
})
</script>

<style scoped>
/* ===== 全局重置（保持不变） ===== */
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

/* ===== 左侧品牌区（图片背景 + 虚化） ===== */
.brand-panel {
  flex: 0 0 55%;
  position: relative;
  overflow: hidden;
  color: #ffffff;
  /* 文字颜色改为白色，以在深色背景上清晰显示 */
}

/* 背景图片层：绝对定位，铺满，并应用模糊 */
.bg-layer {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  z-index: 0;
}

/* 毛玻璃遮罩层：半透明 + 轻微模糊，增强质感 */
.glass-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0-0.3px);
  z-index: 1;
}

/* 内容区域：置于最上层，相对定位 */
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
  max-width: 160px;      /* 允许最大宽度，超过则等比缩放 */
  max-height: 80px;
  overflow: visible;
}

.brand-logo img {
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;  /* 在最大尺寸内保持比例 */
  mix-blend-mode: normal;   /* 添加此行，去除白色背景 */
}


.brand-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: #ffffff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.brand-name span {
  font-weight: 300;
  opacity: 0.8;
  font-size: 18px;
  margin-left: 2px;
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

/* ===== 右侧登录区（样式保持不变） ===== */
.login-panel {
  flex: 1;
  padding: 48px 50px 40px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #ffffff;
  min-width: 350px;
}

.login-panel .page-title {
  font-size: 28px;
  font-weight: 700;
  color: #0a2a4a;
  letter-spacing: -0.3px;
}

.login-panel .page-sub {
  font-size: 15px;
  color: #6b7a8f;
  margin-top: 6px;
  margin-bottom: 30px;
  font-weight: 400;
}

.form-group {
  margin-bottom: 18px;
  position: relative;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1f2a44;
  margin-bottom: 6px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  border: 1.5px solid #dce3ef;
  border-radius: 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #f8faff;
}

.input-wrapper:focus-within {
  border-color: #2a6df4;
  box-shadow: 0 0 0 4px rgba(42, 109, 244, 0.10);
  background: #ffffff;
}

.input-wrapper .prefix-icon {
  padding: 0 0 0 16px;
  color: #8f9bb3;
  font-size: 16px;
  transition: color 0.2s;
}

.input-wrapper:focus-within .prefix-icon {
  color: #2a6df4;
}

.input-wrapper input {
  width: 100%;
  padding: 14px 16px 14px 12px;
  border: none;
  background: transparent;
  font-size: 15px;
  color: #0a2a4a;
  outline: none;
  font-weight: 450;
  letter-spacing: 0.2px;
}

.input-wrapper input::placeholder {
  color: #b0bed6;
  font-weight: 400;
  font-size: 14px;
}

.input-wrapper input::-ms-reveal,
.input-wrapper input::-ms-clear {
  display: none;
}

.input-wrapper .toggle-pwd {
  width: 44px;
  height: 44px;
  padding: 0 16px 0 8px;
  border: 0;
  background: transparent;
  color: #8f9bb3;
  cursor: pointer;
  transition: color 0.2s;
  user-select: none;
  flex: 0 0 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.input-wrapper .toggle-pwd:hover {
  color: #2a6df4;
}

.input-wrapper .toggle-pwd:focus-visible {
  outline: 2px solid #2a6df4;
  outline-offset: -4px;
  border-radius: 8px;
}

.input-wrapper .toggle-pwd svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
  pointer-events: none;
}

.pwd-hint {
  margin-top: 8px;
  font-size: 13px;
  padding: 6px 10px;
  border-radius: 8px;
  background: #f8faff;
  border-left: 3px solid #2a6df4;
  transition: all 0.2s;
  color: #1f2a44;
  min-height: 32px;
  display: flex;
  align-items: center;
}

.pwd-hint.valid {
  border-left-color: #0c8f4a;
  background: #f0faf3;
  color: #0a5a2a;
}

.pwd-hint.invalid {
  border-left-color: #d32f2f;
  background: #fff5f5;
  color: #a02020;
}

.pwd-hint .hint-text {
  line-height: 1.4;
}

.row-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 6px 0 22px 0;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #1f2a44;
  cursor: pointer;
  user-select: none;
}

.remember-me input[type="checkbox"] {
  width: 17px;
  height: 17px;
  accent-color: #2a6df4;
  cursor: pointer;
  border-radius: 4px;
  border: 1.5px solid #b0bed6;
  flex-shrink: 0;
}

.forgot-link {
  font-size: 14px;
  color: #2a6df4;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
  background: none;
  border: none;
  padding: 4px 0;
}

.forgot-link:hover {
  color: #1a4fbf;
  text-decoration: underline;
}

.btn-login {
  width: 100%;
  padding: 15px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: #2a6df4;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s, opacity 0.2s, box-shadow 0.2s;
  letter-spacing: 0.4px;
  margin-bottom: 4px;
}

.btn-login:hover:not(:disabled) {
  background: #1a4fbf;
  box-shadow: 0 8px 24px rgba(42, 109, 244, 0.30);
  transform: translateY(-1px);
}

.btn-login:active:not(:disabled) {
  transform: scale(0.98);
}

.btn-login:disabled {
  background: #b0bed6;
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
  transform: none;
}

.error-msg {
  color: #d32f2f;
  font-size: 14px;
  font-weight: 500;
  min-height: 24px;
  margin: 0 0 10px 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff0f0;
  border-radius: 8px;
  border-left: 3px solid #d32f2f;
}

.error-msg.hidden {
  visibility: hidden;
  min-height: 24px;
  padding: 6px 12px;
}

.footer-links {
  margin-top: 36px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #8f9bb3;
  border-top: 1px solid #e8edf5;
  padding-top: 22px;
  flex-wrap: wrap;
  gap: 10px;
}

.footer-links a {
  color: #8f9bb3;
  text-decoration: none;
  transition: color 0.2s;
  cursor: pointer;
}

.footer-links a:hover {
  color: #2a6df4;
}

.footer-links .left-links {
  display: flex;
  gap: 20px;
}

/* ===== 响应式 ===== */
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

  .login-panel {
    padding: 32px;
    min-width: unset;
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

  .login-panel {
    padding: 24px 20px;
  }

  .page-title {
    font-size: 24px;
  }

  .footer-links {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .row-actions {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>

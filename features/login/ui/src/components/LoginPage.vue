<template>
  <div class="app-container">
    <!-- ======== 左侧品牌区 ======== -->
    <div class="brand-panel">
      <div class="brand-header">
        <div class="brand-logo">
          <img src="/logo.png" alt="CarePilot AI Logo" />
        </div>
        <div class="brand-name">CarePilot <span>AI</span></div>
      </div>
      <div class="slogan-wrapper">
        <div class="slogan-title">{{ config.sloganTitle }}</div>
        <div class="slogan-sub">{{ config.sloganSub }}</div>
      </div>
    </div>

    <!-- ======== 右侧登录区 ======== -->
    <div class="login-panel">
      <h1 class="page-title">欢迎回来</h1>
      <p class="page-sub">登录您的 CarePilot AI 账号</p>

      <!-- 错误信息 -->
      <div class="error-msg" :class="{ hidden: !loginError }" role="alert">
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
            placeholder="请输入手机号或邮箱"
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
          <span class="toggle-pwd" @click="pwdVisible = !pwdVisible">
            <i :class="pwdVisible ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
          </span>
        </div>

        <!-- 密码强度提示（一行小字，圆形符号） -->
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
      <button class="btn-login" :disabled="!canLogin || loginLoading" @click="handleLogin">
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
import type { LoginForm, LoginResponse, PwdChecks } from '../types'

// =============================================================
// 1. 可配置的宣传文案
// =============================================================
const config = reactive({
  sloganTitle: '让每一位客服都拥有AI超能力',
  sloganSub: '专为电商打造的智能客服助手，实现秒级响应，提升转化效率，让沟通更智能、更高效。',
  forgotPasswordPath: '/forgot-password'
})

// =============================================================
// 2. 登录表单数据
// =============================================================
const loginForm = reactive<LoginForm>({
  account: '',
  password: '',
  remember: false
})

const pwdVisible = ref(false)
const pwdHintFocused = ref(false)
const loginError = ref('')
const loginLoading = ref(false)

// =============================================================
// 3. 密码强度校验（用于提示）
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
// 4. 密码提示文字（使用圆形符号 ● 和 ○）
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
// 5. 登录按钮可用性
// =============================================================
const canLogin = computed(() => {
  return loginForm.account.trim() !== '' &&
         loginForm.password.trim() !== '' &&
         isPwdValid.value
})

// =============================================================
// 6. 登录配置与 CSRF Cookie
// =============================================================
async function loadLoginConfig() {
  const response = await apiClient.get('/v1/public/login-config')
  const data = response.data?.data
  if (data?.promoCopy) config.sloganTitle = data.promoCopy
  if (data?.forgotPasswordPath) config.forgotPasswordPath = data.forgotPasswordPath
}

// =============================================================
// 7. 登录逻辑（调用后端 API）
// =============================================================
async function handleLogin() {
  if (!canLogin.value || loginLoading.value) return

  loginError.value = ''
  const account = loginForm.account.trim()
  const password = loginForm.password

  loginLoading.value = true
  try {
    const response = await apiClient.post<LoginResponse>('/v1/auth/login', {
      account,
      password,
      rememberMe: loginForm.remember
    })

    const { code, data } = response.data

    if (code === 'OK') {
      if (loginForm.remember) {
        localStorage.setItem('carepilot_remember', 'true')
        localStorage.setItem('carepilot_account', account)
      } else {
        localStorage.removeItem('carepilot_remember')
        localStorage.removeItem('carepilot_account')
      }

      // Spring Security 登录成功后会轮换 CSRF Token，重新获取一次。
      await loadLoginConfig()
      const redirectPath = data?.redirectPath || '/ai-customer-service'
      window.location.href = `/dashboard.html?path=${encodeURIComponent(redirectPath)}`
    }
  } catch (error: any) {
    if (error.response) {
      const code = error.response.data?.code
      const messages: Record<string, string> = {
        ACCOUNT_NOT_FOUND: '账号不存在，请找管理员',
        INVALID_CREDENTIALS: '账号或密码不正确',
        ACCOUNT_REQUIRED: '请输入账号',
        PASSWORD_REQUIRED: '请输入密码',
        TOO_MANY_LOGIN_ATTEMPTS: '登录尝试过多，请稍后再试'
      }
      loginError.value = messages[code] || error.response.data?.message || '登录失败，请稍后重试'
    } else if (error.request) {
      loginError.value = '网络异常，请检查后端服务是否启动'
    } else {
      loginError.value = '请求配置错误，请检查 API 地址'
    }
  } finally {
    loginLoading.value = false
  }
}

function clearError() {
  loginError.value = ''
}

function loadRemembered() {
  const remember = localStorage.getItem('carepilot_remember') === 'true'
  if (remember) {
    const account = localStorage.getItem('carepilot_account') || ''
    loginForm.account = account
    loginForm.remember = true
  }
}

function goToForgot() {
  window.location.href = `/forgot-password.html?path=${encodeURIComponent(config.forgotPasswordPath)}`
}

onMounted(async () => {
  loadRemembered()
  try {
    await loadLoginConfig()
  } catch {
    loginError.value = '登录配置加载失败，请确认后端服务已启动'
  }
})
</script>

<style scoped>
/* ===== 全局重置 ===== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

body {
  background: #f0f4ff;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
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

/* ===== 左侧品牌区 ===== */
.brand-panel {
  flex: 0 0 55%;
  background: linear-gradient(145deg, #eef3ff 0%, #d9e6ff 100%);
  padding: 48px 50px 40px 50px;
  display: flex;
  flex-direction: column;
  color: #0a2a4a;
  position: relative;
}

.brand-panel .brand-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}

.brand-logo {
  width: 48px;
  height: 48px;
  background: transparent;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow: 0 8px 16px rgba(42, 109, 244, 0.25);
  flex-shrink: 0;
  overflow: hidden;
}

.brand-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 14px;
}

.brand-panel .brand-name {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.3px;
  color: #0a2a4a;
}

.brand-panel .brand-name span {
  font-weight: 300;
  opacity: 0.6;
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

/* ===== 右侧登录区 ===== */
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

.input-wrapper .toggle-pwd {
  padding-right: 16px;
  color: #8f9bb3;
  cursor: pointer;
  font-size: 16px;
  transition: color 0.2s;
  user-select: none;
}

.input-wrapper .toggle-pwd:hover {
  color: #2a6df4;
}

/* ===== 密码提示：一行小字（纯文字，圆形符号） ===== */
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

/* 底部版权 */
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
  .app-container { flex-direction: column; border-radius: 24px; min-height: auto; }
  .brand-panel { flex: 0 0 auto; padding: 32px; border-radius: 24px 24px 0 0; }
  .login-panel { padding: 32px; min-width: unset; }
}

@media (max-width: 640px) {
  .brand-panel { padding: 24px 20px; }
  .login-panel { padding: 24px 20px; }
  .footer-links { flex-direction: column; align-items: flex-start; gap: 6px; }
  .row-actions { flex-wrap: wrap; gap: 10px; }
}
</style>

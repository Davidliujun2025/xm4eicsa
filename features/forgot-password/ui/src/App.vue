<template>
  <div class="app-container">
    <!-- ======== 左侧品牌区（与LoginPage完全一致） ======== -->
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
    <!-- ======== 右侧表单区（找回密码功能） ======== -->
    <div class="form-panel">
      <div class="form-box">
        <div class="form-heading">
          <h2>找回密码</h2>
          <p>请使用客服账号和绑定手机号完成身份验证。</p>
        </div>
        <!-- 错误提示 -->
        <div v-if="message" class="error-msg">
          <i class="fas fa-exclamation-circle"></i> {{ message }}
        </div>
        <!-- 用户名 -->
        <div class="form-group">
          <label for="username">账号</label>
          <div class="input-wrapper">
            <span class="prefix-icon"><i class="fas fa-user"></i></span>
            <input
              id="username"
              v-model.trim="username"
              placeholder="请输入客服账号用户名"
              autocomplete="username"
              @input="invalidateResetSession"
              @keydown.enter="submit"
            />
          </div>
        </div>
        <!-- 绑定手机号 -->
        <div class="form-group">
          <label for="phone">绑定手机号</label>
          <div class="input-wrapper">
            <span class="prefix-icon"><i class="fas fa-phone"></i></span>
            <input
              id="phone"
              v-model.trim="phone"
              inputmode="tel"
              maxlength="11"
              placeholder="请输入绑定手机号"
              autocomplete="tel"
              @input="invalidateResetSession"
              @keydown.enter="submit"
            />
            <button
              type="button"
              class="code-btn"
              :disabled="isSendingCode || countdown > 0"
              @click="getCode"
            >
              {{ codeButtonText }}
            </button>
          </div>
          <p v-if="maskedBoundPhone" class="field-hint">
            <i class="fas fa-check-circle"></i> 验证通过，绑定手机号：{{ maskedBoundPhone }}
          </p>
        </div>
        <!-- 短信验证码 -->
        <div class="form-group">
          <label for="smsCode">短信验证码</label>
          <div class="input-wrapper">
            <span class="prefix-icon"><i class="fas fa-envelope"></i></span>
            <input
              id="smsCode"
              v-model.trim="smsCode"
              inputmode="numeric"
              maxlength="6"
              placeholder="请输入6位验证码"
              @keydown.enter="submit"
            />
          </div>
        </div>
        <!-- 新密码 -->
        <div class="form-group">
          <label for="password">新密码</label>
          <div class="input-wrapper">
            <span class="prefix-icon"><i class="fas fa-lock"></i></span>
            <input
              id="password"
              v-model.trim="password"
              :type="pwdVisible ? 'text' : 'password'"
              placeholder="请输入新密码"
              autocomplete="new-password"
              @keydown.enter="submit"
            />
            <span class="toggle-pwd" @click="pwdVisible = !pwdVisible">
              <i :class="pwdVisible ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
            </span>
          </div>
          <div class="pwd-hint" :class="pwdHintClass">
            <span class="hint-text">{{ pwdHintText }}</span>
          </div>
        </div>
        <!-- 确认新密码 -->
        <div class="form-group">
          <label for="confirmPassword">确认新密码</label>
          <div class="input-wrapper">
            <span class="prefix-icon"><i class="fas fa-lock"></i></span>
            <input
              id="confirmPassword"
              v-model.trim="confirmPassword"
              :type="pwdVisible ? 'text' : 'password'"
              placeholder="请再次输入新密码"
              autocomplete="new-password"
              @keydown.enter="submit"
            />
          </div>
        </div>
        <!-- 提交按钮 -->
        <button class="btn-submit" type="submit" :disabled="isSubmitting" @click="submit">
          {{ isSubmitting ? '提交中...' : '提交重置' }}
        </button>
        <!-- 底部链接 -->
        <div class="form-footer">
          <span>已有账号？</span>
          <a href="/login">返回登录</a>
        </div>
      </div>
    </div>
    <!-- Toast 提示 -->
    <div v-if="message && !loginError" class="toast">{{ message }}</div>
    <!-- 成功弹窗 -->
    <div v-if="showSuccessDialog" class="dialog-mask">
      <div class="dialog">
        <div class="dialog-icon">✓</div>
        <h3>重置完成</h3>
        <p>您的密码已重置成功，请使用新密码重新登录。</p>
        <button @click="backToLogin">返回登录页</button>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import {
  ApiRequestError,
  resetPassword,
  sendSmsCode,
  verifyAccount,
} from './services/passwordResetApi'
// =============================================================
// 品牌配置（与 LoginPage 一致）
// =============================================================
const bgImage = ref('/login-bg.png')
const config = {
  sloganTitle: '让每一位客服都拥有AI超能力',
  sloganSub: '专为电商打造的智能客服助手，实现秒级响应，提升转化效率，让沟通更智能、更高效。'
}
// =============================================================
// 表单状态（保持不变）
// =============================================================
const username = ref('')
const phone = ref('')
const smsCode = ref('')
const password = ref('')
const confirmPassword = ref('')
const message = ref('')
const resetToken = ref('')
const maskedBoundPhone = ref('')
const pwdVisible = ref(false)
const countdown = ref(0)
const isSendingCode = ref(false)
const isSubmitting = ref(false)
const showSuccessDialog = ref(false)
let timer: number | null = null
let messageTimer: number | null = null
// =============================================================
// 密码校验（与 LoginPage 一致）
// =============================================================
const pwdChecks = computed(() => {
  const p = password.value
  const len = p.length
  const hasUpper = /[A-Z]/.test(p)
  const hasLower = /[a-z]/.test(p)
  const hasNumber = /[0-9]/.test(p)
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(p)
  const lengthOk = len >= 12 && len <= 20
  const classCount = [hasUpper, hasLower, hasNumber, hasSpecial].filter(Boolean).length
  const classCountOk = classCount >= 3
  return { lengthOk, hasUpper, hasLower, hasNumber, hasSpecial, classCountOk, isValid: lengthOk && classCountOk }
})
const pwdHintText = computed(() => {
  const pwd = password.value
  if (pwd.length === 0) {
    return '密码需12-20位，包含大写、小写、数字、特殊符号中至少三类'
  }
  if (pwdChecks.value.isValid) {
    return '● 密码符合安全要求'
  } else {
    let missing: string[] = []
    if (!pwdChecks.value.lengthOk) missing.push('长度12-20位')
    if (!pwdChecks.value.classCountOk) missing.push('至少包含三类字符（大写/小写/数字/特殊符号）')
    if (missing.length) {
      return '○ 需满足：' + missing.join('、')
    }
    return '○ 密码不符合安全要求'
  }
})
const pwdHintClass = computed(() => {
  if (password.value.length === 0) return ''
  return pwdChecks.value.isValid ? 'valid' : 'invalid'
})
// =============================================================
// 计算属性
// =============================================================
const codeButtonText = computed(() => {
  if (isSendingCode.value) return '发送中...'
  return countdown.value > 0 ? `${countdown.value}s后重试` : '获取验证码'
})
// =============================================================
// 工具函数（保持不变）
// =============================================================
function showMessage(text: string) {
  message.value = text
  if (messageTimer) clearTimeout(messageTimer)
  messageTimer = window.setTimeout(() => {
    message.value = ''
    messageTimer = null
  }, 3000)
}
function invalidateResetSession() {
  resetToken.value = ''
  maskedBoundPhone.value = ''
}
function validatePhone(value: string) {
  return /^1[3-9]\d{9}$/.test(value)
}
function validateCode(value: string) {
  return /^\d{6}$/.test(value)
}
function getErrorMessage(error: unknown) {
  return error instanceof ApiRequestError ? error.message : '操作失败，请稍后重试'
}
function startCountdown(seconds: number) {
  countdown.value = Math.max(1, seconds)
  if (timer) clearInterval(timer)
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      countdown.value = 0
      if (timer) { clearInterval(timer); timer = null }
    }
  }, 1000)
}
// =============================================================
// API 调用（保持不变）
// =============================================================
async function getCode() {
  if (countdown.value > 0 || isSendingCode.value) return
  if (!username.value) { showMessage('请输入用户名'); return }
  if (!phone.value) { showMessage('请输入绑定手机号'); return }
  if (!validatePhone(phone.value)) { showMessage('请输入正确的手机号'); return }
  isSendingCode.value = true
  try {
    const accountResult = await verifyAccount(username.value)
    resetToken.value = accountResult.resetToken
    maskedBoundPhone.value = accountResult.maskedBoundPhone
    const smsResult = await sendSmsCode(accountResult.resetToken, phone.value)
    showMessage('验证码已发送')
    startCountdown(smsResult.cooldownSeconds || 60)
  } catch (error) {
    showMessage(getErrorMessage(error))
  } finally {
    isSendingCode.value = false
  }
}
async function submit() {
  if (!username.value) { showMessage('请输入用户名'); return }
  if (!phone.value) { showMessage('请输入绑定手机号'); return }
  if (!validatePhone(phone.value)) { showMessage('请输入正确的手机号'); return }
  if (!smsCode.value) { showMessage('请输入短信验证码'); return }
  if (!validateCode(smsCode.value)) { showMessage('验证码错误'); return }
  if (!password.value) { showMessage('请输入新密码'); return }
  if (!pwdChecks.value.isValid) {
    showMessage('密码需为12-20位，且至少包含大写、小写、数字、特殊符号中的三类')
    return
  }
  if (!confirmPassword.value) { showMessage('请再次输入新密码'); return }
  if (password.value !== confirmPassword.value) { showMessage('两次输入的新密码不一致'); return }
  if (!resetToken.value) { showMessage('请先获取短信验证码'); return }
  isSubmitting.value = true
  try {
    await resetPassword(resetToken.value, phone.value, smsCode.value, password.value, confirmPassword.value)
    showSuccessDialog.value = true
  } catch (error) {
    showMessage(getErrorMessage(error))
  } finally {
    isSubmitting.value = false
  }
}
function backToLogin() {
  showSuccessDialog.value = false
  window.location.href = '/login'
}
// =============================================================
// 生命周期
// =============================================================
onUnmounted(() => {
  if (timer) { clearInterval(timer); timer = null }
  if (messageTimer) { clearTimeout(messageTimer); messageTimer = null }
})
</script>
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
/* ===== 左侧品牌区（完全复用 LoginPage 标准背景样式） ===== */
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
/* 修复原代码错误透明度 0-0.3 → 0.3，匹配login-ui遮罩 */
.glass-overlay {
  position: absolute;
  inset: 0;
  /* 渐变深色蒙版，和登录页背景视觉统一，提升白色文字对比度 */
  background: linear-gradient(180deg, rgba(0,0,0,0.2) 0%, rgba(0,0,0,0.45) 100%);
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
  /* 白底logo适配深色背景，去除混合模式 */
  mix-blend-mode: normal;
}
.slogan-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
/* 核心修改：标题文字改为白色，匹配login-ui白色文案 */
.slogan-title {
  font-size: 38px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 16px;
  letter-spacing: -0.5px;
  color: #ffffff;
}
/* 副标题改为浅白色，修复原图深色文字看不清问题 */
.slogan-sub {
  font-size: 16px;
  line-height: 1.7;
  opacity: 0.85;
  max-width: 85%;
  margin-bottom: 36px;
  font-weight: 400;
  color: rgba(255,255,255,0.9);
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
/* ===== 右侧表单区（与 LoginPage 的 .login-panel 风格统一） ===== */
.form-panel {
  flex: 1;
  padding: 48px 50px 40px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #ffffff;
  min-width: 350px;
}
.form-box {
  width: 100%;
  max-width: 440px;
}
.form-heading {
  margin-bottom: 28px;
}
.form-heading h2 {
  font-size: 28px;
  font-weight: 700;
  color: #0a2a4a;
  letter-spacing: -0.3px;
  margin-bottom: 6px;
}
.form-heading p {
  font-size: 15px;
  color: #6b7a8f;
  font-weight: 400;
  margin: 0;
}
.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  background: #fef2f2;
  border-radius: 10px;
  border-left: 4px solid #dc2626;
  color: #dc2626;
  font-size: 14px;
  font-weight: 500;
}
.error-msg i {
  font-size: 16px;
}
.form-group {
  margin-bottom: 18px;
  position: relative;
}
.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1f2a44;
  margin-bottom: 6px;
}
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  border: 1.5px solid #dce3ef;
  border-radius: 12px;
  transition: border-color 0.25s, box-shadow 0.25s;
  background: #f8faff;
}
.input-wrapper:focus-within {
  border-color: #2a6df4;
  box-shadow: 0 0 0 4px rgba(42, 109, 244, 0.10);
  background: #ffffff;
}
.input-wrapper .prefix-icon {
  padding: 0 0 0 16px;
  color: #9aa9bc;
  font-size: 16px;
  transition: color 0.2s;
}
.input-wrapper:focus-within .prefix-icon {
  color: #2a6df4;
}
.input-wrapper input {
  flex: 1;
  padding: 14px 14px 14px 12px;
  border: none;
  background: transparent;
  font-size: 15px;
  color: #0a2a4a;
  outline: none;
  min-width: 0;
}
.input-wrapper input::placeholder {
  color: #b0bed6;
  font-weight: 400;
  font-size: 14px;
}
.input-wrapper .code-btn {
  flex-shrink: 0;
  height: 40px;
  margin-right: 8px;
  padding: 0 16px;
  border: none;
  border-radius: 8px;
  background: #eaf2ff;
  color: #2a6df4;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, opacity 0.2s;
  white-space: nowrap;
}
.input-wrapper .code-btn:hover:not(:disabled) {
  background: #d6e4ff;
}
.input-wrapper .code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.input-wrapper .toggle-pwd {
  padding-right: 16px;
  color: #9aa9bc;
  cursor: pointer;
  font-size: 16px;
  transition: color 0.2s;
  user-select: none;
}
.input-wrapper .toggle-pwd:hover {
  color: #2a6df4;
}
.pwd-hint {
  margin-top: 6px;
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 6px;
  background: #f8faff;
  border-left: 3px solid #2a6df4;
  transition: all 0.2s;
  color: #1f2a44;
  min-height: 28px;
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
.field-hint {
  margin-top: 6px;
  font-size: 13px;
  color: #0c8f4a;
  display: flex;
  align-items: center;
  gap: 6px;
}
.field-hint i {
  font-size: 14px;
}
.btn-submit {
  width: 100%;
  padding: 16px 0;
  margin-top: 6px;
  border: none;
  border-radius: 12px;
  background: #2a6df4;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.25s, box-shadow 0.25s, opacity 0.3s;
  letter-spacing: 0.3px;
}
.btn-submit:hover:not(:disabled) {
  background: #1a4fbf;
  box-shadow: 0 8px 24px rgba(42, 109, 244, 0.30);
}
.btn-submit:disabled {
  background: #b0bed6;
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}
.form-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: #6b7a8f;
}
.form-footer a {
  color: #2a6df4;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s;
}
.form-footer a:hover {
  color: #1a4fbf;
  text-decoration: underline;
}
.toast {
  position: fixed;
  top: 28px;
  left: 50%;
  z-index: 99;
  transform: translateX(-50%);
  max-width: min(520px, calc(100vw - 32px));
  padding: 12px 18px;
  border-radius: 10px;
  color: #ffffff;
  font-size: 14px;
  line-height: 1.5;
  background: #0f1f3d;
  box-shadow: 0 12px 32px rgba(15, 31, 61, 0.2);
}
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 31, 61, 0.45);
  backdrop-filter: blur(4px);
}
.dialog {
  width: min(380px, 100%);
  padding: 40px 32px 32px;
  border-radius: 20px;
  background: #ffffff;
  text-align: center;
  box-shadow: 0 24px 70px rgba(15, 31, 61, 0.20);
}
.dialog-icon {
  width: 60px;
  height: 60px;
  margin: 0 auto 18px;
  border-radius: 50%;
  background: #0c8f4a;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 700;
}
.dialog h3 {
  font-size: 24px;
  font-weight: 700;
  color: #0a2a4a;
  margin-bottom: 8px;
}
.dialog p {
  font-size: 15px;
  color: #6b7a8f;
  line-height: 1.6;
  margin-bottom: 24px;
}
.dialog button {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: #2a6df4;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.25s;
}
.dialog button:hover {
  background: #1a4fbf;
}
/* ===== 响应式（与 LoginPage 一致） ===== */
@media (max-width: 1024px) {
  .app-container {
    flex-direction: column;
    border-radius: 24px;
    min-height: auto;
    margin: 12px;
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
  .form-panel {
    padding: 32px 28px;
    min-width: unset;
  }
  .form-box {
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
  .form-panel {
    padding: 24px 18px;
  }
  .form-heading h2 {
    font-size: 24px;
  }
  .input-wrapper .code-btn {
    font-size: 12px;
    padding: 0 12px;
  }
}
</style>
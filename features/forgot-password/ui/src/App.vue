<template>
  <div class="page">
    <section class="brand-panel">
      <div class="brand-mark">CarePilot AI</div>
      <div class="brand-copy">
        <h1>客服智能助手</h1>
        <p>找回登录密码后，即可继续进入智能客服后台处理买家咨询。</p>
      </div>
      <img class="hero-image" :src="heroImage" alt="" />
    </section>

    <main class="form-panel">
      <form class="form-box" @submit.prevent="submit">
        <div class="form-heading">
          <h2>找回密码</h2>
          <p>请使用客服手机号账号完成身份验证。</p>
        </div>

        <label for="username">客服账号</label>
        <input
          id="username"
          v-model.trim="username"
          placeholder="请输入客服手机号账号"
          autocomplete="username"
          @input="invalidateResetSession"
        />

        <label for="phone">绑定手机号</label>
        <div class="row">
          <input
            id="phone"
            v-model.trim="phone"
            inputmode="tel"
            maxlength="11"
            placeholder="请输入绑定手机号"
            autocomplete="tel"
            @input="invalidateResetSession"
          />
          <button type="button" :disabled="isSendingCode || countdown > 0" @click="getCode">
            {{ codeButtonText }}
          </button>
        </div>
        <p v-if="maskedBoundPhone" class="field-hint">账号验证通过，绑定手机号：{{ maskedBoundPhone }}</p>

        <label for="smsCode">短信验证码</label>
        <input
          id="smsCode"
          v-model.trim="smsCode"
          inputmode="numeric"
          maxlength="6"
          placeholder="请输入6位验证码"
        />

        <label for="password">新密码</label>
        <input
          id="password"
          v-model.trim="password"
          type="password"
          placeholder="请输入新密码"
          autocomplete="new-password"
        />
        <p class="tip">密码需12-20位，且大写字母、小写字母、数字、特殊符号中至少包含三类。</p>

        <label for="confirmPassword">确认新密码</label>
        <input
          id="confirmPassword"
          v-model.trim="confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          autocomplete="new-password"
        />

        <button class="submit" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '提交中...' : '提交重置' }}
        </button>
      </form>
    </main>

    <div v-if="message" class="toast">{{ message }}</div>

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
import { computed, onUnmounted, ref } from 'vue';
import heroImage from './assets/hero.png';
import {
  ApiRequestError,
  resetPassword,
  sendSmsCode,
  verifyAccount,
} from './services/passwordResetApi';

const username = ref('');
const phone = ref('');
const smsCode = ref('');
const password = ref('');
const confirmPassword = ref('');
const message = ref('');
const resetToken = ref('');
const maskedBoundPhone = ref('');

const countdown = ref(0);
const isSendingCode = ref(false);
const isSubmitting = ref(false);
const showSuccessDialog = ref(false);

let timer: number | null = null;
let messageTimer: number | null = null;

const codeButtonText = computed(() => {
  if (isSendingCode.value) {
    return '发送中...';
  }
  return countdown.value > 0 ? `${countdown.value}s后重试` : '获取验证码';
});

function showMessage(text: string) {
  message.value = text;

  if (messageTimer) {
    clearTimeout(messageTimer);
  }

  messageTimer = window.setTimeout(() => {
    message.value = '';
    messageTimer = null;
  }, 2400);
}

function invalidateResetSession() {
  resetToken.value = '';
  maskedBoundPhone.value = '';
}

function validatePhone(value: string) {
  return /^1[3-9]\d{9}$/.test(value);
}

function validateCode(value: string) {
  return /^\d{6}$/.test(value);
}

function validatePassword(value: string) {
  if (!value || value.length < 12 || value.length > 20 || /\s/.test(value)) {
    return false;
  }

  let categoryCount = 0;
  if (/[A-Z]/.test(value)) categoryCount += 1;
  if (/[a-z]/.test(value)) categoryCount += 1;
  if (/\d/.test(value)) categoryCount += 1;
  if (/[^A-Za-z0-9\s]/.test(value)) categoryCount += 1;

  return categoryCount >= 3;
}

function getErrorMessage(error: unknown) {
  if (error instanceof ApiRequestError) {
    return error.message;
  }
  return '操作失败，请稍后重试';
}

function startCountdown(seconds: number) {
  countdown.value = Math.max(1, seconds);

  if (timer) {
    clearInterval(timer);
  }

  timer = window.setInterval(() => {
    countdown.value -= 1;

    if (countdown.value <= 0) {
      countdown.value = 0;

      if (timer) {
        clearInterval(timer);
        timer = null;
      }
    }
  }, 1000);
}

async function getCode() {
  if (countdown.value > 0 || isSendingCode.value) {
    return;
  }

  if (!username.value) {
    showMessage('请输入客服账号');
    return;
  }

  if (!phone.value) {
    showMessage('请输入绑定手机号');
    return;
  }

  if (!validatePhone(phone.value)) {
    showMessage('请输入正确的手机号');
    return;
  }

  isSendingCode.value = true;

  try {
    const accountResult = await verifyAccount(username.value);
    resetToken.value = accountResult.resetToken;
    maskedBoundPhone.value = accountResult.maskedBoundPhone;

    const smsResult = await sendSmsCode(accountResult.resetToken, phone.value);
    showMessage('验证码已发送');
    startCountdown(smsResult.cooldownSeconds || 60);
  } catch (error) {
    showMessage(getErrorMessage(error));
  } finally {
    isSendingCode.value = false;
  }
}

async function submit() {
  if (!username.value) {
    showMessage('请输入客服账号');
    return;
  }

  if (!phone.value) {
    showMessage('请输入绑定手机号');
    return;
  }

  if (!validatePhone(phone.value)) {
    showMessage('请输入正确的手机号');
    return;
  }

  if (!smsCode.value) {
    showMessage('请输入短信验证码');
    return;
  }

  if (!validateCode(smsCode.value)) {
    showMessage('验证码错误');
    return;
  }

  if (!password.value) {
    showMessage('请输入新密码');
    return;
  }

  if (!validatePassword(password.value)) {
    showMessage('密码需为12-20位，且大写字母、小写字母、数字、特殊符号中至少包含三类');
    return;
  }

  if (!confirmPassword.value) {
    showMessage('请再次输入新密码');
    return;
  }

  if (password.value !== confirmPassword.value) {
    showMessage('两次输入的新密码不一致');
    return;
  }

  if (!resetToken.value) {
    showMessage('请先获取短信验证码');
    return;
  }

  isSubmitting.value = true;

  try {
    await resetPassword(resetToken.value, phone.value, smsCode.value, password.value, confirmPassword.value);
    showSuccessDialog.value = true;
  } catch (error) {
    showMessage(getErrorMessage(error));
  } finally {
    isSubmitting.value = false;
  }
}

function backToLogin() {
  showSuccessDialog.value = false;
  showMessage('请返回登录页使用新密码登录');
}

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }

  if (messageTimer) {
    clearTimeout(messageTimer);
    messageTimer = null;
  }
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(360px, 44%) minmax(420px, 56%);
  background: #f7f9fc;
  color: #344054;
  font-family: Arial, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.brand-panel {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 52px;
  color: #ffffff;
  background: linear-gradient(145deg, #1769f6 0%, #0f1f3d 100%);
}

.brand-mark {
  width: fit-content;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 10px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.brand-copy {
  position: relative;
  z-index: 1;
  max-width: 460px;
}

.brand-copy h1 {
  margin: 0;
  color: #ffffff;
  font-size: 48px;
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: 0;
}

.brand-copy p {
  margin: 18px 0 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 17px;
  line-height: 1.75;
}

.hero-image {
  position: absolute;
  right: -58px;
  bottom: -36px;
  width: min(430px, 70%);
  opacity: 0.28;
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  background: #ffffff;
}

.form-box {
  width: min(480px, 100%);
}

.form-heading h2 {
  margin: 0 0 8px;
  color: #0f1f3d;
  font-size: 32px;
  font-weight: 800;
  letter-spacing: 0;
}

.form-heading p {
  margin: 0 0 30px;
  color: #667085;
  font-size: 15px;
}

label {
  display: block;
  margin: 18px 0 8px;
  color: #0f1f3d;
  font-size: 14px;
  font-weight: 700;
}

input {
  width: 100%;
  height: 48px;
  box-sizing: border-box;
  border: 1px solid #dde5f0;
  border-radius: 10px;
  padding: 0 14px;
  color: #0f1f3d;
  font-size: 15px;
  outline: none;
  background: #ffffff;
  transition: border-color 0.18s, box-shadow 0.18s;
}

input:focus {
  border-color: #1769f6;
  box-shadow: 0 0 0 3px rgba(23, 105, 246, 0.12);
}

input::placeholder {
  color: #98a2b3;
}

.row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 124px;
  gap: 10px;
}

.row button,
.submit,
.dialog button {
  border: none;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
  transition: background-color 0.18s, opacity 0.18s;
}

.row button {
  height: 48px;
  background: #eaf2ff;
  color: #1769f6;
  font-size: 14px;
}

.row button:disabled,
.submit:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.field-hint,
.tip {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.55;
}

.field-hint {
  color: #1769f6;
}

.tip {
  color: #667085;
}

.submit {
  width: 100%;
  height: 52px;
  margin-top: 28px;
  background: #1769f6;
  color: #ffffff;
  font-size: 16px;
}

.submit:hover:not(:disabled),
.dialog button:hover {
  background: #0f5add;
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
  z-index: 98;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 31, 61, 0.42);
}

.dialog {
  width: min(360px, 100%);
  padding: 34px 30px 28px;
  border-radius: 14px;
  background: #ffffff;
  text-align: center;
  box-shadow: 0 24px 70px rgba(15, 31, 61, 0.2);
}

.dialog-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 18px;
  border-radius: 50%;
  background: #22b573;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 900;
}

.dialog h3 {
  margin: 0;
  color: #0f1f3d;
  font-size: 24px;
}

.dialog p {
  margin: 12px 0 24px;
  color: #667085;
  font-size: 15px;
  line-height: 1.6;
}

.dialog button {
  width: 100%;
  height: 46px;
  background: #1769f6;
  color: #ffffff;
  font-size: 15px;
}

@media (max-width: 860px) {
  .page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: 260px;
    padding: 32px;
  }

  .brand-copy h1 {
    font-size: 36px;
  }

  .form-panel {
    padding: 34px 20px;
  }
}
</style>

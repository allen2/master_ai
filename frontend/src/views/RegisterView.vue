<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">📈 木木班</div>
      <h2 class="auth-title">注册账号</h2>

      <form @submit.prevent="handleRegister" class="auth-form">
        <!-- 邮箱 + 发送验证码 -->
        <div class="form-field">
          <label>邮箱</label>
          <div class="email-row">
            <input
              v-model="username"
              type="email"
              placeholder="请输入邮箱地址"
              autocomplete="email"
              required
              maxlength="100"
              :disabled="codeSent && countdown > 0"
            />
            <button
              type="button"
              class="send-btn"
              :disabled="sendingCode || countdown > 0 || !username"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : (sendingCode ? '发送中…' : '发送验证码') }}
            </button>
          </div>
        </div>

        <!-- 验证码 -->
        <div class="form-field">
          <label>验证码 <span class="hint">（6 位数字，有效期 5 分钟）</span></label>
          <input
            v-model="code"
            type="text"
            inputmode="numeric"
            placeholder="请输入邮件中的验证码"
            autocomplete="one-time-code"
            required
            maxlength="6"
            pattern="\d{6}"
          />
        </div>

        <!-- 昵称 -->
        <div class="form-field">
          <label>昵称 <span class="hint">（可选）</span></label>
          <input v-model="nickname" type="text" placeholder="显示名称" autocomplete="nickname" maxlength="64" />
        </div>

        <!-- 密码 -->
        <div class="form-field">
          <label>密码 <span class="hint">（6-64 个字符）</span></label>
          <input v-model="password" type="password" placeholder="请输入密码" autocomplete="new-password" required minlength="6" maxlength="64" />
        </div>

        <!-- 确认密码 -->
        <div class="form-field">
          <label>确认密码</label>
          <input v-model="confirm" type="password" placeholder="再次输入密码" autocomplete="new-password" required />
        </div>

        <div v-if="errorMsg" class="auth-error">{{ errorMsg }}</div>
        <div v-if="successMsg" class="auth-success">{{ successMsg }}</div>

        <button type="submit" class="auth-btn" :disabled="loading">
          {{ loading ? '注册中…' : '注册' }}
        </button>
      </form>

      <div class="auth-link">
        已有账号？<router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'

const router     = useRouter()
const auth       = useAuthStore()
const username   = ref('')
const code       = ref('')
const nickname   = ref('')
const password   = ref('')
const confirm    = ref('')
const loading    = ref(false)
const sendingCode = ref(false)
const codeSent   = ref(false)
const countdown  = ref(0)
const errorMsg   = ref('')
const successMsg = ref('')

let countdownTimer = null

async function handleSendCode() {
  errorMsg.value  = ''
  successMsg.value = ''
  if (!username.value) {
    errorMsg.value = '请先填写邮箱'
    return
  }
  sendingCode.value = true
  try {
    const res = await fetch('/auth/send-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: username.value })
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      throw new Error(data.detail || `发送失败 (${res.status})`)
    }
    codeSent.value   = true
    successMsg.value = '验证码已发送，请查收邮件'
    startCountdown(60)
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    sendingCode.value = false
  }
}

function startCountdown(seconds) {
  countdown.value = seconds
  clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleRegister() {
  errorMsg.value   = ''
  successMsg.value = ''
  if (!codeSent.value) {
    errorMsg.value = '请先获取验证码'
    return
  }
  if (password.value !== confirm.value) {
    errorMsg.value = '两次密码不一致'
    return
  }
  loading.value = true
  try {
    await auth.register(username.value, password.value, nickname.value || undefined, code.value)
    router.replace('/')
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}

onUnmounted(() => clearInterval(countdownTimer))
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}
.auth-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0,0,0,.08);
  padding: 40px 36px;
}
.auth-logo {
  font-size: 18px;
  font-weight: 700;
  color: #1e40af;
  text-align: center;
  margin-bottom: 8px;
}
.auth-title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 28px;
}
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-field label {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}
.hint {
  font-weight: 400;
  color: #94a3b8;
  font-size: 12px;
}
.email-row {
  display: flex;
  gap: 8px;
}
.email-row input {
  flex: 1;
  min-width: 0;
}
.send-btn {
  flex-shrink: 0;
  white-space: nowrap;
  padding: 9px 12px;
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background .15s;
}
.send-btn:hover:not(:disabled) {
  background: #dbeafe;
}
.send-btn:disabled {
  opacity: .6;
  cursor: not-allowed;
}
.form-field input {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 9px 12px;
  font-size: 14px;
  outline: none;
  transition: border-color .15s;
  width: 100%;
}
.form-field input:focus {
  border-color: #2563eb;
}
.form-field input:disabled {
  background: #f8fafc;
}
.auth-error {
  font-size: 13px;
  color: #ef4444;
  background: #fef2f2;
  border-radius: 6px;
  padding: 8px 12px;
}
.auth-success {
  font-size: 13px;
  color: #16a34a;
  background: #f0fdf4;
  border-radius: 6px;
  padding: 8px 12px;
}
.auth-btn {
  width: 100%;
  padding: 10px;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
  margin-top: 4px;
}
.auth-btn:hover:not(:disabled) {
  background: #1d4ed8;
}
.auth-btn:disabled {
  opacity: .6;
  cursor: not-allowed;
}
.auth-link {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: #64748b;
}
.auth-link a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}
.auth-link a:hover {
  text-decoration: underline;
}
</style>

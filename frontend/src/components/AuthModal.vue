<!-- frontend/src/components/AuthModal.vue -->
<template>
  <el-dialog
    :model-value="visible"
    width="420px"
    :close-on-click-modal="false"
    :show-close="true"
    class="auth-dialog"
    @update:model-value="(val) => emit('update:visible', val)"
  >
    <template #header>
      <div class="auth-logo">📈 金木智库</div>
      <h2 class="auth-title">{{ mode === 'login' ? '登录' : mode === 'register' ? '注册账号' : '找回密码' }}</h2>
    </template>

    <!-- 登录表单 -->
    <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="auth-form">
      <div class="form-field">
        <label>邮箱</label>
        <input v-model="loginUsername" type="email" placeholder="请输入邮箱地址" autocomplete="email" required />
      </div>
      <div class="form-field">
        <label>密码</label>
        <input v-model="loginPassword" type="password" placeholder="请输入密码" autocomplete="current-password" required />
      </div>

      <div class="auth-forgot-link">
        <a href="javascript:void(0)" @click="switchMode('forgot')">忘记密码？</a>
      </div>

      <div v-if="loginError" class="auth-error">{{ loginError }}</div>
      <div v-if="loginSuccess" class="auth-success">{{ loginSuccess }}</div>

      <button type="submit" class="auth-btn" :disabled="loginLoading">
        {{ loginLoading ? '登录中…' : '登录' }}
      </button>
    </form>

    <!-- 注册表单 -->
    <form v-else-if="mode === 'register'" @submit.prevent="handleRegister" class="auth-form">
      <div class="form-field">
        <label>邮箱</label>
        <div class="email-row">
          <input
            v-model="registerUsername"
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
            :disabled="sendingCode || countdown > 0 || !registerUsername"
            @click="handleSendCode"
          >
            {{ countdown > 0 ? `${countdown}s 后重发` : (sendingCode ? '发送中…' : '发送验证码') }}
          </button>
        </div>
      </div>

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

      <div class="form-field">
        <label>昵称 <span class="hint">（可选）</span></label>
        <input v-model="nickname" type="text" placeholder="显示名称" autocomplete="nickname" maxlength="64" />
      </div>

      <div class="form-field">
        <label>密码 <span class="hint">（6-64 个字符）</span></label>
        <input v-model="registerPassword" type="password" placeholder="请输入密码" autocomplete="new-password" required minlength="6" maxlength="64" />
      </div>

      <div class="form-field">
        <label>确认密码</label>
        <input v-model="confirm" type="password" placeholder="再次输入密码" autocomplete="new-password" required />
      </div>

      <div v-if="registerError" class="auth-error">{{ registerError }}</div>
      <div v-if="registerSuccess" class="auth-success">{{ registerSuccess }}</div>

      <button type="submit" class="auth-btn" :disabled="registerLoading">
        {{ registerLoading ? '注册中…' : '注册' }}
      </button>
    </form>

    <!-- 找回密码表单 -->
    <form v-else @submit.prevent="handleResetPassword" class="auth-form">
      <div class="form-field">
        <label>邮箱</label>
        <div class="email-row">
          <input
            v-model="forgotEmail"
            type="email"
            placeholder="请输入注册时使用的邮箱地址"
            autocomplete="email"
            required
            maxlength="100"
            :disabled="resetCodeSent && forgotCountdown > 0"
          />
          <button
            type="button"
            class="send-btn"
            :disabled="sendingResetCode || forgotCountdown > 0 || !forgotEmail"
            @click="handleSendResetCode"
          >
            {{ forgotCountdown > 0 ? `${forgotCountdown}s 后重发` : (sendingResetCode ? '发送中…' : '发送验证码') }}
          </button>
        </div>
      </div>

      <div class="form-field">
        <label>验证码 <span class="hint">（6 位数字，有效期 5 分钟）</span></label>
        <input
          v-model="forgotCode"
          type="text"
          inputmode="numeric"
          placeholder="请输入邮件中的验证码"
          autocomplete="one-time-code"
          required
          maxlength="6"
          pattern="\d{6}"
        />
      </div>

      <div class="form-field">
        <label>新密码 <span class="hint">（6-64 个字符）</span></label>
        <input v-model="forgotNewPassword" type="password" placeholder="请输入新密码" autocomplete="new-password" required minlength="6" maxlength="64" />
      </div>

      <div class="form-field">
        <label>确认新密码</label>
        <input v-model="forgotConfirmPassword" type="password" placeholder="再次输入新密码" autocomplete="new-password" required />
      </div>

      <div v-if="forgotError" class="auth-error">{{ forgotError }}</div>
      <div v-if="forgotSuccess" class="auth-success">{{ forgotSuccess }}</div>

      <button type="submit" class="auth-btn" :disabled="forgotLoading">
        {{ forgotLoading ? '提交中…' : '重置密码' }}
      </button>
    </form>

    <div class="auth-link">
      <template v-if="mode === 'login'">
        还没有账号？<a href="javascript:void(0)" @click="switchMode('register')">立即注册</a>
      </template>
      <template v-else-if="mode === 'register'">
        已有账号？<a href="javascript:void(0)" @click="switchMode('login')">去登录</a>
      </template>
      <template v-else>
        <a href="javascript:void(0)" @click="switchMode('login')">返回登录</a>
      </template>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/authStore.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  initialMode: { type: String, default: 'login' }
})
const emit = defineEmits(['update:visible', 'success'])

const auth = useAuthStore()
const mode = ref(props.initialMode)

watch(() => props.initialMode, (val) => { mode.value = val })
watch(() => props.visible, (val) => {
  if (val) {
    mode.value = props.initialMode
    loginError.value   = ''
    loginSuccess.value = ''
    registerError.value   = ''
    registerSuccess.value = ''
    forgotError.value     = ''
    forgotSuccess.value   = ''
  }
})

function switchMode(target) {
  mode.value = target
  loginError.value     = ''
  loginSuccess.value   = ''
  registerError.value  = ''
  registerSuccess.value = ''
  forgotError.value    = ''
  forgotSuccess.value  = ''
}

// 登录
const loginUsername = ref('')
const loginPassword = ref('')
const loginLoading  = ref(false)
const loginError    = ref('')
const loginSuccess  = ref('')

async function handleLogin() {
  loginError.value = ''
  loginLoading.value = true
  try {
    await auth.login(loginUsername.value, loginPassword.value)
    emit('update:visible', false)
    emit('success')
  } catch (e) {
    loginError.value = e.message
  } finally {
    loginLoading.value = false
  }
}

// 注册
const registerUsername = ref('')
const code             = ref('')
const nickname         = ref('')
const registerPassword = ref('')
const confirm          = ref('')
const registerLoading  = ref(false)
const sendingCode       = ref(false)
const codeSent          = ref(false)
const countdown         = ref(0)
const registerError     = ref('')
const registerSuccess   = ref('')

let countdownTimer = null

async function handleSendCode() {
  registerError.value   = ''
  registerSuccess.value = ''
  if (!registerUsername.value) {
    registerError.value = '请先填写邮箱'
    return
  }
  sendingCode.value = true
  try {
    const res = await fetch('/auth/send-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: registerUsername.value })
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      throw new Error(data.detail || `发送失败 (${res.status})`)
    }
    codeSent.value      = true
    registerSuccess.value = '验证码已发送，请查收邮件'
    startCountdown(60)
  } catch (e) {
    registerError.value = e.message
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
  registerError.value   = ''
  registerSuccess.value = ''
  if (!codeSent.value) {
    registerError.value = '请先获取验证码'
    return
  }
  if (registerPassword.value !== confirm.value) {
    registerError.value = '两次密码不一致'
    return
  }
  registerLoading.value = true
  try {
    await auth.register(registerUsername.value, registerPassword.value, nickname.value || undefined, code.value)
    emit('update:visible', false)
    emit('success')
  } catch (e) {
    registerError.value = e.message
  } finally {
    registerLoading.value = false
  }
}

// 找回密码
const forgotEmail           = ref('')
const forgotCode            = ref('')
const forgotNewPassword     = ref('')
const forgotConfirmPassword = ref('')
const forgotLoading         = ref(false)
const sendingResetCode      = ref(false)
const resetCodeSent         = ref(false)
const forgotCountdown       = ref(0)
const forgotError           = ref('')
const forgotSuccess         = ref('')

let forgotCountdownTimer = null

function startForgotCountdown(seconds) {
  forgotCountdown.value = seconds
  clearInterval(forgotCountdownTimer)
  forgotCountdownTimer = setInterval(() => {
    forgotCountdown.value--
    if (forgotCountdown.value <= 0) {
      clearInterval(forgotCountdownTimer)
      forgotCountdownTimer = null
    }
  }, 1000)
}

async function handleSendResetCode() {
  forgotError.value   = ''
  forgotSuccess.value = ''
  if (!forgotEmail.value) {
    forgotError.value = '请先填写邮箱'
    return
  }
  sendingResetCode.value = true
  try {
    await auth.forgotPassword(forgotEmail.value)
    resetCodeSent.value = true
    forgotSuccess.value = '验证码已发送，请查收邮件'
    startForgotCountdown(60)
  } catch (e) {
    forgotError.value = e.message
  } finally {
    sendingResetCode.value = false
  }
}

async function handleResetPassword() {
  forgotError.value   = ''
  forgotSuccess.value = ''
  if (!resetCodeSent.value) {
    forgotError.value = '请先获取验证码'
    return
  }
  if (forgotNewPassword.value !== forgotConfirmPassword.value) {
    forgotError.value = '两次输入的密码不一致'
    return
  }
  forgotLoading.value = true
  try {
    await auth.resetPassword(forgotEmail.value, forgotCode.value, forgotNewPassword.value, forgotConfirmPassword.value)
    switchMode('login')
    loginSuccess.value = '密码重置成功，请使用新密码登录'
  } catch (e) {
    forgotError.value = e.message
  } finally {
    forgotLoading.value = false
  }
}

onUnmounted(() => {
  clearInterval(countdownTimer)
  clearInterval(forgotCountdownTimer)
})
</script>

<style scoped>
.auth-dialog :deep(.el-dialog__header) {
  padding: 8px 0 0;
  text-align: center;
}
.auth-logo {
  font-size: 18px;
  font-weight: 700;
  color: #1e40af;
  margin-bottom: 4px;
}
.auth-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
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
.auth-forgot-link {
  text-align: right;
  font-size: 13px;
  margin-top: -8px;
}
.auth-forgot-link a {
  color: #2563eb;
  text-decoration: none;
}
.auth-forgot-link a:hover {
  text-decoration: underline;
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

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">📈 木木班</div>
      <h2 class="auth-title">登录</h2>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="form-field">
          <label>邮箱</label>
          <input v-model="username" type="email" placeholder="请输入邮箱地址" autocomplete="email" required />
        </div>
        <div class="form-field">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" autocomplete="current-password" required />
        </div>

        <div v-if="errorMsg" class="auth-error">{{ errorMsg }}</div>

        <button type="submit" class="auth-btn" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>

      <div class="auth-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'

const router   = useRouter()
const auth     = useAuthStore()
const username = ref('')
const password = ref('')
const loading  = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  loading.value  = true
  try {
    await auth.login(username.value, password.value)
    router.replace('/')
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}
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
  width: 360px;
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
.form-field input {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 9px 12px;
  font-size: 14px;
  outline: none;
  transition: border-color .15s;
}
.form-field input:focus {
  border-color: #2563eb;
}
.auth-error {
  font-size: 13px;
  color: #ef4444;
  background: #fef2f2;
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

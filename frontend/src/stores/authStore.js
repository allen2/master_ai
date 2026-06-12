// frontend/src/stores/authStore.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'mumu_token'
const USER_KEY  = 'mumu_user'

/**
 * authStore：管理登录状态、Token 及用户信息
 * Token 持久化到 localStorage，刷新后自动恢复
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user  = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  /**
   * 登录
   * @param {string} username
   * @param {string} password
   */
  async function login(username, password) {
    const res = await fetch('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.detail || `登录失败 (${res.status})`)
    }
    const data = await res.json()
    _setSession(data)
    return data
  }

  /**
   * 注册（成功后自动登录）
   * @param {string} username  邮箱
   * @param {string} password
   * @param {string} nickname
   * @param {string} code      邮箱验证码
   */
  async function register(username, password, nickname, code) {
    const res = await fetch('/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password, nickname, code })
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.detail || `注册失败 (${res.status})`)
    }
    const data = await res.json()
    _setSession(data)
    return data
  }

  /** 退出登录 */
  function logout() {
    token.value = ''
    user.value  = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function _setSession(data) {
    token.value = data.token
    user.value  = { userId: data.userId, username: data.username, nickname: data.nickname }
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  return { token, user, isLoggedIn, login, register, logout }
})

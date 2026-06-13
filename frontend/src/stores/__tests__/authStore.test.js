// frontend/src/stores/__tests__/authStore.test.js
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../authStore'
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'

// localStorage mock
const localStorageMock = (() => {
  let store = {}
  return {
    getItem:    (k)    => store[k] ?? null,
    setItem:    (k, v) => { store[k] = String(v) },
    removeItem: (k)    => { delete store[k] },
    clear:      ()     => { store = {} }
  }
})()
Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock })

describe('authStore', () => {
  beforeEach(() => {
    localStorageMock.clear()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('初始状态：未登录', () => {
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.token).toBe('')
    expect(auth.user).toBeNull()
  })

  it('login 成功后 isLoggedIn=true，token/user 写入 localStorage', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        token: 'test-jwt',
        userId: 1,
        username: 'tester',
        nickname: '测试用户'
      })
    })

    const auth = useAuthStore()
    await auth.login('tester', 'password')

    expect(auth.isLoggedIn).toBe(true)
    expect(auth.token).toBe('test-jwt')
    expect(auth.user.username).toBe('tester')
    expect(localStorageMock.getItem('mumu_token')).toBe('test-jwt')
  })

  it('login 失败时抛出包含 detail 的错误', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: () => Promise.resolve({ detail: '用户名或密码错误' })
    })

    const auth = useAuthStore()
    await expect(auth.login('bad', 'wrong')).rejects.toThrow('用户名或密码错误')
    expect(auth.isLoggedIn).toBe(false)
  })

  it('register 成功后自动登录', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        token: 'new-jwt',
        userId: 2,
        username: 'newuser',
        nickname: '新用户'
      })
    })

    const auth = useAuthStore()
    await auth.register('newuser', 'secret123', '新用户')

    expect(auth.isLoggedIn).toBe(true)
    expect(auth.token).toBe('new-jwt')
    expect(auth.user.nickname).toBe('新用户')
  })

  it('logout 清除 token/user 和 localStorage', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ token: 'tok', userId: 1, username: 'u', nickname: 'n' })
    })
    const auth = useAuthStore()
    await auth.login('u', 'p')
    expect(auth.isLoggedIn).toBe(true)

    auth.logout()

    expect(auth.isLoggedIn).toBe(false)
    expect(auth.token).toBe('')
    expect(auth.user).toBeNull()
    expect(localStorageMock.getItem('mumu_token')).toBeNull()
  })

  it('刷新后从 localStorage 恢复登录状态', () => {
    localStorageMock.setItem('mumu_token', 'existing-token')
    localStorageMock.setItem('mumu_user', JSON.stringify({ userId: 3, username: 'old', nickname: '旧用户' }))

    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.token).toBe('existing-token')
    expect(auth.user.username).toBe('old')
  })

  it('forgotPassword 成功时返回 message', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ message: '验证码已发送，请查收邮件' })
    })

    const auth = useAuthStore()
    const result = await auth.forgotPassword('user@example.com')

    expect(result.message).toBe('验证码已发送，请查收邮件')
    expect(globalThis.fetch).toHaveBeenCalledWith('/auth/forgot-password', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ email: 'user@example.com' })
    }))
  })

  it('forgotPassword 失败时抛出包含 detail 的错误', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: () => Promise.resolve({ detail: '该邮箱未注册' })
    })

    const auth = useAuthStore()
    await expect(auth.forgotPassword('not-exist@example.com')).rejects.toThrow('该邮箱未注册')
  })

  it('resetPassword 成功时返回 message，且不写入登录状态', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ message: '密码重置成功，请使用新密码登录' })
    })

    const auth = useAuthStore()
    const result = await auth.resetPassword('user@example.com', '123456', 'newpass', 'newpass')

    expect(result.message).toBe('密码重置成功，请使用新密码登录')
    expect(auth.isLoggedIn).toBe(false)
    expect(globalThis.fetch).toHaveBeenCalledWith('/auth/reset-password', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ email: 'user@example.com', code: '123456', newPassword: 'newpass', confirmPassword: 'newpass' })
    }))
  })

  it('resetPassword 失败时抛出包含 detail 的错误', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: () => Promise.resolve({ detail: '验证码无效或已过期，请重新获取' })
    })

    const auth = useAuthStore()
    await expect(auth.resetPassword('user@example.com', '000000', 'newpass', 'newpass'))
        .rejects.toThrow('验证码无效或已过期，请重新获取')
  })
})

<!-- frontend/src/App.vue -->
<template>
  <!-- 未登录：全屏显示 login/register 页面 -->
  <router-view v-if="!auth.isLoggedIn" />

  <!-- 已登录：显示完整布局 -->
  <div v-else class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-logo">📈 木木班</div>
      <nav class="sidebar-nav">
        <router-link to="/run"      class="nav-item">📈 分析运行</router-link>
        <router-link to="/industry-analysis" class="nav-item">🏭 产业分析</router-link>
        <router-link to="/contrarian-analysis" class="nav-item">🔄 逆向对立面</router-link>
        <router-link to="/history"  class="nav-item">📜 分析记录</router-link>
        <router-link to="/api-keys" class="nav-item">🔑 API Keys</router-link>
        <router-link to="/wallet"   class="nav-item">🪙 我的钱包</router-link>
        <router-link to="/contact"  class="nav-item">✉️ 联系我们</router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="footer-status">
          <span class="health-dot" :class="settingsStore.healthy ? 'ok' : 'fail'"></span>
          <span class="health-label">{{ settingsStore.healthy ? '服务正常' : '服务异常' }}</span>
        </div>
        <div class="footer-coins" @click="$router.push('/wallet')" title="我的金币">
          🪙 <span class="coins-num">{{ walletStore.balance ?? '—' }}</span>
        </div>
        <div class="footer-user">
          <span class="user-name">{{ auth.user?.nickname || auth.user?.username }}</span>
          <button class="logout-btn" @click="handleLogout" title="退出登录">退出</button>
        </div>
      </div>
    </aside>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSettingsStore } from './stores/settingsStore.js'
import { useAuthStore } from './stores/authStore.js'
import { useWalletStore } from './stores/walletStore.js'

const router        = useRouter()
const settingsStore = useSettingsStore()
const auth          = useAuthStore()
const walletStore   = useWalletStore()

onMounted(() => {
  if (auth.isLoggedIn) {
    settingsStore.startPolling()
    walletStore.fetchBalance()
  }
})
onUnmounted(() => settingsStore.stopPolling())

// 登录后立即拉取健康状态、模型列表和金币余额
watch(() => auth.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    settingsStore.startPolling()
    walletStore.fetchBalance()
  } else {
    settingsStore.stopPolling()
    walletStore.clear()
  }
})

function handleLogout() {
  auth.logout()
  settingsStore.stopPolling()
  router.replace('/login')
}
</script>

<style>
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f8fafc; }

.app-layout   { display: flex; height: 100vh; }
.sidebar      { width: 160px; background: #fff; border-right: 1px solid #e2e8f0;
                display: flex; flex-direction: column; flex-shrink: 0; }
.sidebar-logo { padding: 16px 12px; font-size: 14px; font-weight: 700;
                color: #1e40af; border-bottom: 1px solid #e2e8f0; }
.sidebar-nav  { flex: 1; padding: 8px; display: flex; flex-direction: column; gap: 2px; }
.nav-item     { display: block; padding: 8px 10px; border-radius: 6px; font-size: 13px;
                color: #475569; text-decoration: none; }
.nav-item:hover       { background: #f1f5f9; }
.nav-item.router-link-active { background: #eff6ff; color: #1d4ed8;
                               border-left: 3px solid #2563eb; }

.sidebar-footer { padding: 10px 12px; border-top: 1px solid #e2e8f0;
                  display: flex; flex-direction: column; gap: 6px; }
.footer-status  { display: flex; align-items: center; gap: 6px; }
.health-dot     { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.health-dot.ok  { background: #22c55e; }
.health-dot.fail{ background: #ef4444; }
.health-label   { font-size: 11px; color: #64748b; }

.footer-coins   { display: flex; align-items: center; gap: 4px; cursor: pointer; padding: 3px 6px;
                  border-radius: 6px; font-size: 12px; }
.footer-coins:hover { background: #f1f5f9; }
.coins-num      { font-weight: 600; color: #d97706; font-size: 13px; }
.footer-user    { display: flex; align-items: center; justify-content: space-between; gap: 4px; }
.user-name      { font-size: 11px; color: #475569; overflow: hidden; text-overflow: ellipsis;
                  white-space: nowrap; max-width: 90px; }
.logout-btn     { font-size: 11px; color: #94a3b8; background: none; border: none;
                  cursor: pointer; padding: 2px 4px; border-radius: 4px; flex-shrink: 0; }
.logout-btn:hover { color: #ef4444; background: #fef2f2; }

.main-content { flex: 1; overflow: auto; padding: 20px; }
.page-view    { max-width: 1200px; }
</style>

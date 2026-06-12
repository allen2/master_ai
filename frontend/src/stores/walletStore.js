// frontend/src/stores/walletStore.js
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { walletApi } from '../api/index.js'

/**
 * walletStore：管理金币余额
 */
export const useWalletStore = defineStore('wallet', () => {
  const balance = ref(null)
  const loading = ref(false)

  async function fetchBalance() {
    loading.value = true
    try {
      const data = await walletApi.balance()
      balance.value = data.balance
    } catch (_) {
      // 未登录等情况静默失败
    } finally {
      loading.value = false
    }
  }

  function clear() {
    balance.value = null
  }

  return { balance, loading, fetchBalance, clear }
})

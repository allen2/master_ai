// frontend/src/stores/settingsStore.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { settingsApi } from '../api/index.js'

export const useSettingsStore = defineStore('settings', () => {
  const healthy = ref(false)
  const models = ref([])
  let pollTimer = null

  const defaultModel = computed(() =>
    models.value.length ? models.value[0].model_name : ''
  )

  function setHealth(data) {
    healthy.value = data?.status === 'ok'
  }

  function setModels(data) {
    models.value = data
  }

  async function fetchHealth() {
    try {
      const data = await settingsApi.health()
      setHealth(data)
    } catch (_) {
      healthy.value = false
    }
  }

  async function fetchModels() {
    try {
      const data = await settingsApi.models()
      setModels(data)
    } catch (_) {}
  }

  function startPolling() {
    fetchHealth()
    fetchModels()
    pollTimer = setInterval(fetchHealth, 30_000)
  }

  function stopPolling() {
    clearInterval(pollTimer)
    pollTimer = null
  }

  return { healthy, models, defaultModel, setHealth, setModels, fetchHealth, fetchModels, startPolling, stopPolling }
})

// frontend/src/stores/__tests__/settingsStore.test.js
import { setActivePinia, createPinia } from 'pinia'
import { useSettingsStore } from '../settingsStore'
import { describe, it, expect, beforeEach } from 'vitest'

describe('settingsStore', () => {
  beforeEach(() => { setActivePinia(createPinia()) })

  it('初始状态 healthy=false, models=[]', () => {
    const store = useSettingsStore()
    expect(store.healthy).toBe(false)
    expect(store.models).toEqual([])
  })

  it('setHealth({ status: "ok" }) 设 healthy=true', () => {
    const store = useSettingsStore()
    store.setHealth({ status: 'ok' })
    expect(store.healthy).toBe(true)
  })

  it('setHealth(非 ok) 设 healthy=false', () => {
    const store = useSettingsStore()
    store.healthy = true
    store.setHealth({ status: 'error' })
    expect(store.healthy).toBe(false)
  })

  it('setModels 存储模型列表', () => {
    const store = useSettingsStore()
    store.setModels([{ model_name: 'gpt-4o', display_name: 'GPT-4o', provider: 'OpenAI' }])
    expect(store.models).toHaveLength(1)
    expect(store.models[0].model_name).toBe('gpt-4o')
  })

  it('defaultModel 返回第一个模型的 model_name', () => {
    const store = useSettingsStore()
    store.setModels([{ model_name: 'gpt-4o', display_name: 'GPT-4o', provider: 'OpenAI' }])
    expect(store.defaultModel).toBe('gpt-4o')
  })

  it('defaultModel models 为空时返回空字符串', () => {
    const store = useSettingsStore()
    expect(store.defaultModel).toBe('')
  })
})

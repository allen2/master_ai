// frontend/src/stores/__tests__/contrarianAnalysisStore.test.js
import { setActivePinia, createPinia } from 'pinia'
import { useContrarianAnalysisStore } from '../contrarianAnalysisStore'
import { describe, it, expect, beforeEach } from 'vitest'

describe('contrarianAnalysisStore._handleSseEvent', () => {
  beforeEach(() => { setActivePinia(createPinia()) })

  it('start 事件清空上次结果并设 running=true', () => {
    const store = useContrarianAnalysisStore()
    store.activities.push('上一次的活动')
    store.report = '上一次的报告'
    store._handleSseEvent('start', { status: 'started', query: '分析新能源行业的逆向对立面' })
    expect(store.activities).toHaveLength(0)
    expect(store.report).toBe('')
    expect(store.running).toBe(true)
    expect(store.error).toBe(null)
  })

  it('activity 事件追加活动消息', () => {
    const store = useContrarianAnalysisStore()
    store._handleSseEvent('activity', { message: '开始热门行业逆向对立面分析…' })
    store._handleSseEvent('activity', { message: '调用工具 webSearch 获取数据…' })
    expect(store.activities).toEqual([
      '开始热门行业逆向对立面分析…',
      '调用工具 webSearch 获取数据…'
    ])
  })

  it('complete 事件填充报告并停止 running', () => {
    const store = useContrarianAnalysisStore()
    store.running = true
    store._handleSseEvent('complete', { report: '# 一、量化判定：识别当期热门行业\n...', status: 'completed' })
    expect(store.running).toBe(false)
    expect(store.report).toBe('# 一、量化判定：识别当期热门行业\n...')
  })

  it('error 事件设置 error 消息并停止 running', () => {
    const store = useContrarianAnalysisStore()
    store.running = true
    store._handleSseEvent('error', { message: 'LLM service unavailable' })
    expect(store.running).toBe(false)
    expect(store.error).toBe('LLM service unavailable')
  })
})

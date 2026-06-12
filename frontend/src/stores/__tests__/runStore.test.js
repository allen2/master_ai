// frontend/src/stores/__tests__/runStore.test.js
import { setActivePinia, createPinia } from 'pinia'
import { useRunStore } from '../runStore'
import { describe, it, expect, beforeEach } from 'vitest'

describe('runStore._handleSseEvent', () => {
  beforeEach(() => { setActivePinia(createPinia()) })

  it('start 事件清空上次结果并设 running=true', () => {
    const store = useRunStore()
    store.analysts['Warren Buffett'] = { status: 'done', signals: {} }
    store.decisions['AAPL'] = { action: 'buy' }
    store._handleSseEvent('start', { status: 'started', tickers: ['AAPL'] })
    expect(Object.keys(store.analysts)).toHaveLength(0)
    expect(Object.keys(store.decisions)).toHaveLength(0)
    expect(store.running).toBe(true)
    expect(store.error).toBe(null)
  })

  it('progress 事件更新 analysts 对应条目', () => {
    const store = useRunStore()
    store._handleSseEvent('progress', { agent: 'Warren Buffett', ticker: 'AAPL', status: 'analyzing' })
    expect(store.analysts['Warren Buffett'].status).toBe('analyzing')
  })

  it('progress done 状态写入', () => {
    const store = useRunStore()
    store._handleSseEvent('progress', { agent: 'Warren Buffett', ticker: 'AAPL', status: 'done' })
    expect(store.analysts['Warren Buffett'].status).toBe('done')
  })

  it('complete 事件填充 analystSignals 和 decisions，停止 running', () => {
    const store = useRunStore()
    store.running = true
    store._handleSseEvent('complete', {
      analyst_signals: {
        warren_buffett: { AAPL: { signal: 'bullish', confidence: 75, reasoning: 'moat' } }
      },
      decisions: { AAPL: { action: 'buy', quantity: 10, bull_count: 3, bear_count: 1 } },
      status: 'complete'
    })
    expect(store.running).toBe(false)
    expect(store.analystSignals['warren_buffett']['AAPL'].signal).toBe('bullish')
    expect(store.decisions['AAPL'].action).toBe('buy')
  })

  it('complete 事件保留质量价值门槛字段 gate_passed / gate_detail', () => {
    const store = useRunStore()
    store._handleSseEvent('complete', {
      decisions: {
        AAPL: {
          action: 'buy',
          quantity: 50,
          gate_passed: true,
          gate_detail: ['护城河组: 通过(2多/0中)', '低估值组: 通过(1多/0中)', 'DCF内在价值组: 通过(1多/0中)']
        }
      },
      status: 'complete'
    })
    expect(store.decisions['AAPL'].gate_passed).toBe(true)
    expect(store.decisions['AAPL'].gate_detail).toHaveLength(3)
    expect(store.decisions['AAPL'].gate_detail[0]).toContain('护城河组')
  })

  it('activity 事件记录分析师最新活动，done 后清除', () => {
    const store = useRunStore()
    store._handleSseEvent('activity', { agent: '沃伦·巴菲特', agent_id: 'warren_buffett', message: '调用工具 getDaily 获取数据…' })
    expect(store.activities['沃伦·巴菲特']).toBe('调用工具 getDaily 获取数据…')

    // 后续活动覆盖为最新
    store._handleSseEvent('activity', { agent: '沃伦·巴菲特', agent_id: 'warren_buffett', message: '调用大模型推理中…' })
    expect(store.activities['沃伦·巴菲特']).toBe('调用大模型推理中…')

    // done 进度清除活动消息
    store._handleSseEvent('progress', { agent: '沃伦·巴菲特', ticker: 'AAPL', status: 'done' })
    expect(store.activities['沃伦·巴菲特']).toBeUndefined()
  })

  it('signal 事件增量填入分析师信号（即时展示）', () => {
    const store = useRunStore()
    store._handleSseEvent('signal', {
      agent: '沃伦·巴菲特',
      agent_id: 'warren_buffett',
      signals: { AAPL: { signal: 'bullish', confidence: 88, reasoning: '宽护城河' } }
    })
    expect(store.analystSignals['warren_buffett']['AAPL'].signal).toBe('bullish')
    expect(store.analystSignals['warren_buffett']['AAPL'].confidence).toBe(88)

    // 第二个 agent 的 signal 事件不应覆盖已有的
    store._handleSseEvent('signal', {
      agent: '查理·芒格',
      agent_id: 'charlie_munger',
      signals: { AAPL: { signal: 'bearish', confidence: 60, reasoning: '估值偏高' } }
    })
    expect(store.analystSignals['warren_buffett']['AAPL'].signal).toBe('bullish')
    expect(store.analystSignals['charlie_munger']['AAPL'].signal).toBe('bearish')
  })

  it('error 事件设置 error 消息并停止 running', () => {
    const store = useRunStore()
    store.running = true
    store._handleSseEvent('error', { message: 'LLM service unavailable' })
    expect(store.running).toBe(false)
    expect(store.error).toBe('LLM service unavailable')
  })
})

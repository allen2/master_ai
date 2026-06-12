// frontend/src/stores/runStore.js
import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'

/**
 * runStore：管理 SSE 流式运行状态
 * 负责追踪分析师进度、分析信号和交易决策
 */
export const useRunStore = defineStore('run', () => {
  /** 是否正在运行 */
  const running = ref(false)

  /** 错误信息，无错误时为 null */
  const error = ref(null)

  /**
   * 分析师进度映射
   * key: agentDisplayName（如 "Warren Buffett"）
   * value: { status: string, signals: {} }
   */
  const analysts = reactive({})

  /**
   * 分析师信号映射
   * key: agentId（如 "warren_buffett"）
   * value: { ticker -> AgentSignal }
   */
  const analystSignals = reactive({})

  /**
   * 交易决策映射
   * key: ticker（如 "AAPL"）
   * value: { action, quantity, bull_count, bear_count }
   */
  const decisions = reactive({})

  /**
   * 分析师最新活动消息映射（分析过程中的细粒度进度）
   * key: agentDisplayName（如 "沃伦·巴菲特"）
   * value: string（如 "调用工具 getDaily 获取数据…"）
   */
  const activities = reactive({})

  /** 用于中止 SSE 请求的 AbortController */
  let abortController = null

  /**
   * 处理 SSE 事件
   * @param {string} eventName - 事件名称：start / progress / complete / error
   * @param {object} data - 事件数据
   */
  function _handleSseEvent(eventName, data) {
    if (eventName === 'start') {
      // 清空上次运行结果
      Object.keys(analysts).forEach(k => delete analysts[k])
      Object.keys(analystSignals).forEach(k => delete analystSignals[k])
      Object.keys(decisions).forEach(k => delete decisions[k])
      Object.keys(activities).forEach(k => delete activities[k])
      error.value = null
      running.value = true
    } else if (eventName === 'progress') {
      // 更新指定分析师的进度状态
      const { agent, status } = data
      if (!analysts[agent]) {
        analysts[agent] = { status: 'waiting', signals: {} }
      }
      analysts[agent].status = status
      // 分析完成后清掉过程活动消息
      if (status === 'done' || status === 'error') {
        delete activities[agent]
      }
    } else if (eventName === 'activity') {
      // agent 分析过程中的细粒度活动，记录最新一条用于展示
      const { agent, message } = data
      if (agent && message) {
        activities[agent] = message
      }
    } else if (eventName === 'signal') {
      // 单个分析师完成后即时推送的信号，增量填入并立即展示
      const { agent_id, signals } = data
      if (agent_id && signals) {
        analystSignals[agent_id] = { ...(analystSignals[agent_id] || {}), ...signals }
      }
    } else if (eventName === 'complete') {
      // 填充分析信号和交易决策，停止运行
      Object.entries(data.analyst_signals || {}).forEach(([id, tickerMap]) => {
        analystSignals[id] = tickerMap
      })
      Object.entries(data.decisions || {}).forEach(([ticker, dec]) => {
        decisions[ticker] = dec
      })
      running.value = false
    } else if (eventName === 'error') {
      // 记录错误信息，停止运行
      error.value = data.message || '运行失败'
      running.value = false
    }
  }

  /**
   * 发起运行请求，通过 SSE 流接收进度
   * @param {object} payload - 请求参数（tickers, analysts, model 等）
   */
  async function startRun(payload) {
    if (running.value) {
      return
    }

    running.value = true
    error.value = null
    abortController = new AbortController()

    try {
      const token = localStorage.getItem('mumu_token') || ''
      const response = await fetch('/hedge-fund/run', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify(payload),
        signal: abortController.signal
      })

      if (response.status === 401) {
        localStorage.removeItem('mumu_token')
        localStorage.removeItem('mumu_user')
        window.location.hash = '#/login'
        running.value = false
        return
      }
      if (response.status === 402) {
        error.value = '金币不足，请联系管理员充值'
        running.value = false
        return
      }
      if (!response.ok) {
        let msg = `HTTP ${response.status}`
        try {
          const body = await response.json()
          msg = body.msg || body.message || msg
        } catch (_) { /* ignore */ }
        throw new Error(msg)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = ''

      // 逐块读取 SSE 流
      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          break
        }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        // 保留不完整的最后一行
        buffer = lines.pop()

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:') && currentEvent) {
            try {
              _handleSseEvent(currentEvent, JSON.parse(line.slice(5).trim()))
            } catch (_) {
              // 忽略 JSON 解析异常
            }
            currentEvent = ''
          }
        }
      }
    } catch (e) {
      if (e.name !== 'AbortError') {
        error.value = e.message
      }
      running.value = false
    } finally {
      abortController = null
    }
  }

  /**
   * 中止当前运行
   */
  function stopRun() {
    abortController?.abort()
    abortController = null
    running.value = false
  }

  return {
    running,
    error,
    analysts,
    analystSignals,
    decisions,
    activities,
    startRun,
    stopRun,
    _handleSseEvent
  }
})

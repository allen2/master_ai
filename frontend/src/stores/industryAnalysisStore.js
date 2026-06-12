// frontend/src/stores/industryAnalysisStore.js
import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * industryAnalysisStore：管理「产业瓶颈分析」SSE 流式运行状态
 * 负责追踪分析活动进度和最终 Markdown 报告
 */
export const useIndustryAnalysisStore = defineStore('industryAnalysis', () => {
  /** 是否正在运行 */
  const running = ref(false)

  /** 错误信息，无错误时为 null */
  const error = ref(null)

  /** 分析过程中的活动进度消息列表 */
  const activities = ref([])

  /** 最终 Markdown 分析报告 */
  const report = ref('')

  /** 用于中止 SSE 请求的 AbortController */
  let abortController = null

  /**
   * 处理 SSE 事件
   * @param {string} eventName - 事件名称：start / activity / complete / error
   * @param {object} data - 事件数据
   */
  function _handleSseEvent(eventName, data) {
    if (eventName === 'start') {
      activities.value = []
      report.value = ''
      error.value = null
      running.value = true
    } else if (eventName === 'activity') {
      if (data.message) {
        activities.value.push(data.message)
      }
    } else if (eventName === 'complete') {
      report.value = data.report || ''
      running.value = false
    } else if (eventName === 'error') {
      error.value = data.message || '分析失败'
      running.value = false
    }
  }

  /**
   * 发起产业瓶颈分析请求，通过 SSE 流接收进度
   * @param {object} payload - 请求参数（query, model_name, model_provider）
   */
  async function startAnalysis(payload) {
    if (running.value) {
      return
    }

    running.value = true
    error.value = null
    abortController = new AbortController()

    try {
      const token = localStorage.getItem('mumu_token') || ''
      const response = await fetch('/industry-analysis/run', {
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
   * 中止当前分析
   */
  function stopAnalysis() {
    abortController?.abort()
    abortController = null
    running.value = false
  }

  return {
    running,
    error,
    activities,
    report,
    startAnalysis,
    stopAnalysis,
    _handleSseEvent
  }
})

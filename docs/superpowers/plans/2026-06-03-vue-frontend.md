# Vue 前端 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ai-hedge-fund-java 开发 Vue 3 前端，覆盖所有 REST/SSE 接口，随 Java 一同构建和启动。

**Architecture:** 文字侧边栏布局 + 浅色主题，4 个页面（分析运行、API Keys、流程管理、设置）。分析运行页通过 `fetch` + `ReadableStream` 消费 SSE 流，Pinia store 管理状态。`frontend-maven-plugin` 在 `mvn package` 时将 Vite 产物输出到 `src/main/resources/static/`，Spring Boot 直接伺服。

**Tech Stack:** Vue 3, Vite 5, Vue Router 4, Pinia, Axios, Element Plus, Vitest, frontend-maven-plugin 1.15.0

---

## 文件结构

```
frontend/
  package.json
  index.html
  vite.config.js
  src/
    main.js
    App.vue
    router/index.js
    api/index.js
    stores/
      runStore.js
      settingsStore.js
      __tests__/
        runStore.test.js
        settingsStore.test.js
    views/
      RunView.vue
      ApiKeysView.vue
      FlowsView.vue
      SettingsView.vue
pom.xml                          ← 新增 frontend-maven-plugin
```

19 个分析师 ID（供 RunView 多选下拉硬编码）：
`fundamentals_analyst`, `growth_analyst`, `sentiment_analyst`, `technical_analyst`,
`aswath_damodaran`, `ben_graham`, `bill_ackman`, `cathie_wood`, `charlie_munger`,
`michael_burry`, `mohnish_pabrai`, `nassim_taleb`, `peter_lynch`, `phil_fisher`,
`rakesh_jhunjhunwala`, `stanley_druckenmiller`, `warren_buffett`,
`news_sentiment_analyst`, `valuation_analyst`

---

## Task 1: 脚手架 — package.json / vite.config.js / index.html / main.js

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/vite.config.js`
- Create: `frontend/src/main.js`
- Modify: `.gitignore`

- [ ] **Step 1: 创建 frontend/package.json**

```json
{
  "name": "ai-hedge-fund-frontend",
  "version": "1.0.0",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "test": "vitest run"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "axios": "^1.6.0",
    "element-plus": "^2.6.0",
    "@element-plus/icons-vue": "^2.3.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.0.0",
    "vitest": "^1.0.0",
    "@vue/test-utils": "^2.4.0",
    "jsdom": "^24.0.0"
  }
}
```

- [ ] **Step 2: 创建 frontend/index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>AI Hedge Fund</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

- [ ] **Step 3: 创建 frontend/vite.config.js**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true
  },
  server: {
    proxy: {
      '/hedge-fund':      'http://localhost:8000',
      '/api-keys':        'http://localhost:8000',
      '/flows':           'http://localhost:8000',
      '/flow-runs':       'http://localhost:8000',
      '/language-models': 'http://localhost:8000',
      '/health':          'http://localhost:8000'
    }
  }
})
```

- [ ] **Step 4: 创建 frontend/src/main.js**

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.mount('#app')
```

- [ ] **Step 5: 在项目根 .gitignore 追加前端忽略项**

在 `d:\workspace\ai-hedge\ai-hedge-fund-java\.gitignore` 末尾追加：
```
frontend/node_modules/
frontend/.vite/
```

- [ ] **Step 6: 安装依赖并验证启动**

```bash
cd frontend
npm install
npm run dev
```

预期：浏览器访问 http://localhost:5173 显示空白页（无报错）。

- [ ] **Step 7: Commit**

```bash
git add frontend/ .gitignore
git commit -m "feat: scaffold Vue 3 frontend project"
```

---

## Task 2: API 层 — frontend/src/api/index.js

**Files:**
- Create: `frontend/src/api/index.js`

- [ ] **Step 1: 创建 api/index.js**

```js
import axios from 'axios'

const api = axios.create({ baseURL: '/' })

api.interceptors.response.use(
  res => res.data,
  err => {
    const msg = err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export const apiKeysApi = {
  list:   ()              => api.get('/api-keys'),
  save:   (data)          => api.post('/api-keys', data),
  remove: (provider)      => api.delete(`/api-keys/${encodeURIComponent(provider)}`)
}

export const flowsApi = {
  list:   ()          => api.get('/flows'),
  get:    (id)        => api.get(`/flows/${id}`),
  create: (data)      => api.post('/flows', data),
  update: (id, data)  => api.put(`/flows/${id}`, data),
  remove: (id)        => api.delete(`/flows/${id}`),
  getRun: (id)        => api.get(`/flow-runs/${id}`)
}

export const settingsApi = {
  models: () => api.get('/language-models'),
  health: () => api.get('/health')
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/index.js
git commit -m "feat: add API layer (axios wrappers)"
```

---

## Task 3: runStore — TDD SSE 状态管理

**Files:**
- Create: `frontend/src/stores/__tests__/runStore.test.js`
- Create: `frontend/src/stores/runStore.js`

- [ ] **Step 1: 创建 runStore.test.js（失败态）**

```js
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

  it('error 事件设置 error 消息并停止 running', () => {
    const store = useRunStore()
    store.running = true
    store._handleSseEvent('error', { message: 'LLM service unavailable' })
    expect(store.running).toBe(false)
    expect(store.error).toBe('LLM service unavailable')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd frontend && npm test
```

预期：FAIL — `Cannot find module '../runStore'`

- [ ] **Step 3: 创建 runStore.js 使测试通过**

```js
// frontend/src/stores/runStore.js
import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'

export const useRunStore = defineStore('run', () => {
  const running = ref(false)
  const error = ref(null)
  // agentDisplayName -> { status, signals }
  const analysts = reactive({})
  // agentId -> { ticker -> AgentSignal }
  const analystSignals = reactive({})
  // ticker -> decision
  const decisions = reactive({})

  let abortController = null

  function _handleSseEvent(eventName, data) {
    if (eventName === 'start') {
      Object.keys(analysts).forEach(k => delete analysts[k])
      Object.keys(analystSignals).forEach(k => delete analystSignals[k])
      Object.keys(decisions).forEach(k => delete decisions[k])
      error.value = null
      running.value = true
    } else if (eventName === 'progress') {
      const { agent, status } = data
      if (!analysts[agent]) analysts[agent] = { status: 'waiting', signals: {} }
      analysts[agent].status = status
    } else if (eventName === 'complete') {
      Object.entries(data.analyst_signals || {}).forEach(([id, tickerMap]) => {
        analystSignals[id] = tickerMap
      })
      Object.entries(data.decisions || {}).forEach(([ticker, dec]) => {
        decisions[ticker] = dec
      })
      running.value = false
    } else if (eventName === 'error') {
      error.value = data.message || '运行失败'
      running.value = false
    }
  }

  async function startRun(payload) {
    if (running.value) return
    running.value = true
    error.value = null
    abortController = new AbortController()
    try {
      const response = await fetch('/hedge-fund/run', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        signal: abortController.signal
      })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop()
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:') && currentEvent) {
            try { _handleSseEvent(currentEvent, JSON.parse(line.slice(5).trim())) } catch (_) {}
            currentEvent = ''
          }
        }
      }
    } catch (e) {
      if (e.name !== 'AbortError') error.value = e.message
      running.value = false
    } finally {
      abortController = null
    }
  }

  function stopRun() {
    abortController?.abort()
    abortController = null
    running.value = false
  }

  return { running, error, analysts, analystSignals, decisions, startRun, stopRun, _handleSseEvent }
})
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd frontend && npm test
```

预期：5 tests passed

- [ ] **Step 5: Commit**

```bash
git add frontend/src/stores/runStore.js frontend/src/stores/__tests__/runStore.test.js
git commit -m "feat: add runStore with SSE event handling (TDD)"
```

---

## Task 4: settingsStore — 模型列表和健康状态

**Files:**
- Create: `frontend/src/stores/__tests__/settingsStore.test.js`
- Create: `frontend/src/stores/settingsStore.js`

- [ ] **Step 1: 创建 settingsStore.test.js（失败态）**

```js
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
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd frontend && npm test
```

预期：FAIL — `Cannot find module '../settingsStore'`

- [ ] **Step 3: 创建 settingsStore.js**

```js
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
```

- [ ] **Step 4: 运行全部测试**

```bash
cd frontend && npm test
```

预期：所有测试通过

- [ ] **Step 5: Commit**

```bash
git add frontend/src/stores/settingsStore.js frontend/src/stores/__tests__/settingsStore.test.js
git commit -m "feat: add settingsStore (models + health polling, TDD)"
```

---

## Task 5: App 外壳 — App.vue + router

**Files:**
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/App.vue`

- [ ] **Step 1: 创建 router/index.js**

```js
// frontend/src/router/index.js
import { createRouter, createWebHashHistory } from 'vue-router'
import RunView      from '../views/RunView.vue'
import ApiKeysView  from '../views/ApiKeysView.vue'
import FlowsView    from '../views/FlowsView.vue'
import SettingsView from '../views/SettingsView.vue'

export default createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/',          redirect: '/run' },
    { path: '/run',       component: RunView },
    { path: '/api-keys',  component: ApiKeysView },
    { path: '/flows',     component: FlowsView },
    { path: '/settings',  component: SettingsView }
  ]
})
```

- [ ] **Step 2: 创建空页面占位（四个 view 文件，各自只含一行标题）**

`frontend/src/views/RunView.vue`:
```vue
<template><div class="page-view"><h2>分析运行</h2></div></template>
```

`frontend/src/views/ApiKeysView.vue`:
```vue
<template><div class="page-view"><h2>API Key 管理</h2></div></template>
```

`frontend/src/views/FlowsView.vue`:
```vue
<template><div class="page-view"><h2>流程管理</h2></div></template>
```

`frontend/src/views/SettingsView.vue`:
```vue
<template><div class="page-view"><h2>设置</h2></div></template>
```

- [ ] **Step 3: 创建 App.vue**

```vue
<!-- frontend/src/App.vue -->
<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-logo">📈 AI Hedge Fund</div>
      <nav class="sidebar-nav">
        <router-link to="/run"      class="nav-item">📈 分析运行</router-link>
        <router-link to="/api-keys" class="nav-item">🔑 API Keys</router-link>
        <router-link to="/flows"    class="nav-item">📋 流程管理</router-link>
        <router-link to="/settings" class="nav-item">⚙️ 设置</router-link>
      </nav>
      <div class="sidebar-footer">
        <span class="health-dot" :class="settingsStore.healthy ? 'ok' : 'fail'"></span>
        <span class="health-label">{{ settingsStore.healthy ? '服务正常' : '服务异常' }}</span>
      </div>
    </aside>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useSettingsStore } from './stores/settingsStore.js'

const settingsStore = useSettingsStore()
onMounted(() => settingsStore.startPolling())
onUnmounted(() => settingsStore.stopPolling())
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
.sidebar-footer { padding: 12px; display: flex; align-items: center; gap: 6px;
                  border-top: 1px solid #e2e8f0; }
.health-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.health-dot.ok   { background: #22c55e; }
.health-dot.fail { background: #ef4444; }
.health-label { font-size: 11px; color: #64748b; }
.main-content { flex: 1; overflow: auto; padding: 20px; }
.page-view    { max-width: 1200px; }
</style>
```

- [ ] **Step 4: 启动开发服务器验证侧边栏和路由**

```bash
cd frontend && npm run dev
```

访问 http://localhost:5173：
- 侧边栏显示 4 个菜单项
- 点击各菜单项路由正常切换
- 底部健康状态圆点（首次为红，后端起来后变绿）

- [ ] **Step 5: Commit**

```bash
git add frontend/src/router/ frontend/src/App.vue frontend/src/views/
git commit -m "feat: add App shell with sidebar and router"
```

---

## Task 6: RunView.vue — 分析运行页

**Files:**
- Modify: `frontend/src/views/RunView.vue`

- [ ] **Step 1: 替换 RunView.vue 为完整实现**

```vue
<!-- frontend/src/views/RunView.vue -->
<template>
  <div class="page-view">
    <!-- 配置栏 -->
    <el-card shadow="never" class="config-card">
      <el-form :model="form" :rules="rules" ref="formRef" inline class="config-form">
        <el-form-item label="股票代码" prop="tickers">
          <el-input v-model="form.tickers" placeholder="AAPL, MSFT" style="width:140px" />
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="form.dateRange" type="daterange"
            start-placeholder="开始" end-placeholder="结束"
            value-format="YYYY-MM-DD" style="width:220px" />
        </el-form-item>
        <el-form-item label="模型">
          <el-select v-model="form.modelName" style="width:180px">
            <el-option v-for="m in settingsStore.models" :key="m.model_name"
              :label="m.display_name" :value="m.model_name" />
          </el-select>
        </el-form-item>
        <el-form-item label="分析师">
          <el-select v-model="form.selectedAnalysts" multiple collapse-tags
            collapse-tags-tooltip style="width:200px" placeholder="全选（默认）">
            <el-option v-for="a in ANALYST_LIST" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="runStore.running" @click="handleRun">
            {{ runStore.running ? '分析中...' : '▶ 开始分析' }}
          </el-button>
          <el-button v-if="runStore.running" @click="runStore.stopRun()">⏹ 停止</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 错误横幅 -->
    <el-alert v-if="runStore.error" :title="runStore.error" type="error"
      show-icon :closable="false" style="margin-top:12px" />

    <!-- 结果区：两列 -->
    <div class="results-grid" v-if="hasResults">
      <!-- 左：分析师进度 -->
      <el-card shadow="never">
        <template #header>分析师进度</template>
        <div class="analyst-list">
          <div v-for="(info, agent) in runStore.analysts" :key="agent"
            class="analyst-row">
            <span class="status-dot" :class="info.status"></span>
            <span class="agent-name">{{ agent }}</span>
            <div class="signal-tags">
              <el-tag v-for="(sig, ticker) in signalsForAgent(agent)" :key="ticker"
                :type="tagType(sig.signal)" size="small" style="margin-left:4px">
                {{ ticker }}: {{ sig.signal }} {{ sig.confidence }}%
              </el-tag>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 右：交易决策 -->
      <el-card shadow="never">
        <template #header>交易决策</template>
        <div v-if="Object.keys(runStore.decisions).length">
          <div v-for="(dec, ticker) in runStore.decisions" :key="ticker" class="decision-card">
            <div class="decision-header">
              <span class="ticker-symbol">{{ ticker }}</span>
              <el-tag :type="actionTagType(dec.action)" size="default">
                {{ dec.action?.toUpperCase() }} × {{ dec.quantity ?? 0 }}
              </el-tag>
            </div>
            <div class="signal-bar">
              <div class="bar-bull"    :style="{ flex: dec.bull_count || 0 }"></div>
              <div class="bar-bear"    :style="{ flex: dec.bear_count || 0 }"></div>
              <div class="bar-neutral" :style="{ flex: 1 }"></div>
            </div>
            <div class="signal-counts">
              bull {{ dec.bull_count ?? 0 }} · bear {{ dec.bear_count ?? 0 }}
            </div>
            <div v-if="dec.reasoning" class="reasoning">{{ dec.reasoning }}</div>
          </div>
        </div>
        <el-empty v-else description="尚无决策" />
      </el-card>
    </div>

    <el-empty v-else-if="!runStore.running && !runStore.error"
      description="填写配置后点击「开始分析」" style="margin-top:40px" />
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRunStore } from '../stores/runStore.js'
import { useSettingsStore } from '../stores/settingsStore.js'

const runStore      = useRunStore()
const settingsStore = useSettingsStore()
const formRef       = ref(null)

const ANALYST_LIST = [
  { id: 'fundamentals_analyst',    name: 'Fundamentals Analyst' },
  { id: 'growth_analyst',          name: 'Growth Analyst' },
  { id: 'sentiment_analyst',       name: 'Sentiment Analyst' },
  { id: 'technical_analyst',       name: 'Technical Analyst' },
  { id: 'aswath_damodaran',        name: 'Aswath Damodaran' },
  { id: 'ben_graham',              name: 'Ben Graham' },
  { id: 'bill_ackman',             name: 'Bill Ackman' },
  { id: 'cathie_wood',             name: 'Cathie Wood' },
  { id: 'charlie_munger',          name: 'Charlie Munger' },
  { id: 'michael_burry',           name: 'Michael Burry' },
  { id: 'mohnish_pabrai',          name: 'Mohnish Pabrai' },
  { id: 'nassim_taleb',            name: 'Nassim Taleb' },
  { id: 'peter_lynch',             name: 'Peter Lynch' },
  { id: 'phil_fisher',             name: 'Phil Fisher' },
  { id: 'rakesh_jhunjhunwala',     name: 'Rakesh Jhunjhunwala' },
  { id: 'stanley_druckenmiller',   name: 'Stanley Druckenmiller' },
  { id: 'warren_buffett',          name: 'Warren Buffett' },
  { id: 'news_sentiment_analyst',  name: 'News Sentiment Analyst' },
  { id: 'valuation_analyst',       name: 'Valuation Analyst' },
]

// 默认近 3 个月
const today     = new Date()
const threeAgo  = new Date(today); threeAgo.setMonth(today.getMonth() - 3)
const fmt = d => d.toISOString().slice(0, 10)

const form = reactive({
  tickers:           '',
  dateRange:         [fmt(threeAgo), fmt(today)],
  modelName:         settingsStore.defaultModel,
  selectedAnalysts:  []
})

const rules = {
  tickers: [{ required: true, message: '请输入至少一个股票代码', trigger: 'blur' }]
}

const hasResults = computed(() =>
  Object.keys(runStore.analysts).length > 0 || Object.keys(runStore.decisions).length > 0
)

function signalsForAgent(agentDisplayName) {
  // analystSignals key 是 agentId（snake_case），需要匹配 displayName
  const agentId = ANALYST_LIST.find(a => a.name === agentDisplayName)?.id
  return agentId ? (runStore.analystSignals[agentId] || {}) : {}
}

function tagType(signal) {
  return signal === 'bullish' ? 'success' : signal === 'bearish' ? 'danger' : 'info'
}

function actionTagType(action) {
  const a = action?.toLowerCase()
  return a === 'buy' ? 'success' : a === 'sell' ? 'danger' : 'info'
}

async function handleRun() {
  await formRef.value.validate()
  const tickers = form.tickers.split(',').map(s => s.trim().toUpperCase()).filter(Boolean)
  if (!tickers.length) { ElMessage.error('请输入有效的股票代码'); return }
  const payload = {
    tickers,
    start_date:         form.dateRange?.[0] || null,
    end_date:           form.dateRange?.[1] || null,
    model_name:         form.modelName || settingsStore.defaultModel,
    model_provider:     'OpenAI',
    initial_cash:       100000,
    selected_analysts:  form.selectedAnalysts.length ? form.selectedAnalysts : null
  }
  runStore.startRun(payload)
}
</script>

<style scoped>
.config-card   { margin-bottom: 0; }
.config-form   { flex-wrap: wrap; gap: 4px; }
.results-grid  { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 12px; }

.analyst-list  { display: flex; flex-direction: column; gap: 6px; }
.analyst-row   { display: flex; align-items: center; gap: 8px; padding: 6px 8px;
                 border-radius: 6px; background: #f8fafc; }
.status-dot    { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.status-dot.waiting   { background: #e2e8f0; }
.status-dot.analyzing { background: #f97316; }
.status-dot.done      { background: #22c55e; }
.status-dot.error     { background: #ef4444; }
.agent-name   { flex: 0 0 160px; font-size: 12px; color: #334155; }
.signal-tags  { display: flex; flex-wrap: wrap; gap: 2px; }

.decision-card   { border: 1px solid #e2e8f0; border-radius: 8px; padding: 10px; margin-bottom: 8px; }
.decision-header { display: flex; align-items: center; justify-content: space-between;
                   margin-bottom: 6px; }
.ticker-symbol   { font-size: 15px; font-weight: 600; color: #0f172a; }
.signal-bar      { display: flex; height: 6px; border-radius: 3px; overflow: hidden;
                   margin-bottom: 4px; gap: 1px; }
.bar-bull    { background: #22c55e; }
.bar-bear    { background: #ef4444; }
.bar-neutral { background: #e2e8f0; min-width: 4px; }
.signal-counts { font-size: 11px; color: #64748b; margin-bottom: 4px; }
.reasoning     { font-size: 11px; color: #475569; line-height: 1.4; }
</style>
```

- [ ] **Step 2: 验证页面**

确认 Java 后端已运行（`mvn spring-boot:run`），然后：
```bash
cd frontend && npm run dev
```
打开 http://localhost:5173/#/run：
- 配置栏一行显示，含模型下拉（需后端健康才显示条目）
- 分析师多选下拉显示 19 位
- 点击"开始分析"后结果区出现，进度实时更新

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/RunView.vue
git commit -m "feat: implement RunView with SSE streaming results"
```

---

## Task 7: ApiKeysView.vue

**Files:**
- Modify: `frontend/src/views/ApiKeysView.vue`

- [ ] **Step 1: 替换 ApiKeysView.vue 为完整实现**

```vue
<!-- frontend/src/views/ApiKeysView.vue -->
<template>
  <div class="page-view">
    <div class="page-header">
      <h2>API Key 管理</h2>
      <el-button type="primary" @click="openDialog">+ 添加 Key</el-button>
    </div>

    <el-table :data="keys" v-loading="loading" style="margin-top:16px">
      <el-table-column prop="provider" label="Provider" min-width="200" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.is_active ? 'success' : 'danger'" size="small">
            {{ row.is_active ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="last_used" label="最后使用" width="160" />
      <el-table-column prop="created_at" label="创建时间" width="160" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-popconfirm title="确认删除该 Key？" @confirm="handleDelete(row.provider)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="添加 API Key" width="400px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="Provider" prop="provider">
          <el-select v-model="form.provider" style="width:100%" placeholder="选择 Provider">
            <el-option label="OPENAI_API_KEY"             value="OPENAI_API_KEY" />
            <el-option label="ANTHROPIC_API_KEY"          value="ANTHROPIC_API_KEY" />
            <el-option label="GROQ_API_KEY"               value="GROQ_API_KEY" />
            <el-option label="FINANCIAL_DATASETS_API_KEY" value="FINANCIAL_DATASETS_API_KEY" />
          </el-select>
        </el-form-item>
        <el-form-item label="Key 值" prop="keyValue">
          <el-input v-model="form.keyValue" type="password" show-password placeholder="sk-..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiKeysApi } from '../api/index.js'

const keys           = ref([])
const loading        = ref(false)
const saving         = ref(false)
const dialogVisible  = ref(false)
const formRef        = ref(null)
const form           = ref({ provider: '', keyValue: '' })
const rules          = {
  provider: [{ required: true, message: '请选择 Provider', trigger: 'change' }],
  keyValue: [{ required: true, message: '请输入 Key 值',  trigger: 'blur'   }]
}

async function fetchKeys() {
  loading.value = true
  try { keys.value = await apiKeysApi.list() }
  catch (e) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function openDialog() {
  form.value = { provider: '', keyValue: '' }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    await apiKeysApi.save({ provider: form.value.provider, keyValue: form.value.keyValue })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchKeys()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

async function handleDelete(provider) {
  try {
    await apiKeysApi.remove(provider)
    ElMessage.success('已删除')
    fetchKeys()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(fetchKeys)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #0f172a; }
</style>
```

- [ ] **Step 2: 验证页面**

打开 http://localhost:5173/#/api-keys：
- 表格加载正常（空列表或已有条目）
- 点击"+ 添加 Key"弹出对话框，选择 Provider 填写 Key 值后保存
- 表格刷新，新条目出现
- 点击删除弹出确认，确认后从列表移除

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/ApiKeysView.vue
git commit -m "feat: implement ApiKeysView CRUD"
```

---

## Task 8: FlowsView.vue

**Files:**
- Modify: `frontend/src/views/FlowsView.vue`

- [ ] **Step 1: 替换 FlowsView.vue 为完整实现**

```vue
<!-- frontend/src/views/FlowsView.vue -->
<template>
  <div class="page-view">
    <div class="page-header">
      <h2>流程管理</h2>
      <el-button type="primary" @click="openCreate">+ 新建流程</el-button>
    </div>

    <div v-loading="loading" class="flows-grid" style="margin-top:16px">
      <el-card v-for="flow in flows" :key="flow.id" shadow="hover" class="flow-card">
        <div class="flow-name">{{ flow.name }}</div>
        <div class="flow-desc">{{ flow.description || '暂无描述' }}</div>
        <div class="flow-meta">创建: {{ flow.created_at }}</div>
        <div class="flow-actions">
          <el-button size="small" type="primary" link @click="goRun(flow)">运行</el-button>
          <el-button size="small" link @click="openEdit(flow)">编辑</el-button>
          <el-popconfirm title="确认删除该流程？" @confirm="handleDelete(flow.id)">
            <template #reference>
              <el-button size="small" type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </el-card>
      <el-empty v-if="!flows.length && !loading" description="暂无流程" />
    </div>

    <!-- 新建 / 编辑 对话框 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑流程' : '新建流程'"
      width="440px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="60px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="流程名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { flowsApi } from '../api/index.js'

const router        = useRouter()
const flows         = ref([])
const loading       = ref(false)
const saving        = ref(false)
const dialogVisible = ref(false)
const editId        = ref(null)
const formRef       = ref(null)
const form          = ref({ name: '', description: '' })
const rules         = { name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }] }

async function fetchFlows() {
  loading.value = true
  try { flows.value = await flowsApi.list() }
  catch (e) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function openCreate() {
  editId.value = null
  form.value   = { name: '', description: '' }
  dialogVisible.value = true
}

function openEdit(flow) {
  editId.value = flow.id
  form.value   = { name: flow.name, description: flow.description || '' }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editId.value) {
      await flowsApi.update(editId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await flowsApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchFlows()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  try {
    await flowsApi.remove(id)
    ElMessage.success('已删除')
    fetchFlows()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function goRun() {
  router.push('/run')
}

onMounted(fetchFlows)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #0f172a; }
.flows-grid  { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
.flow-card   { cursor: default; }
.flow-name   { font-size: 14px; font-weight: 600; color: #0f172a; margin-bottom: 4px; }
.flow-desc   { font-size: 12px; color: #64748b; margin-bottom: 4px; min-height: 18px; }
.flow-meta   { font-size: 11px; color: #94a3b8; margin-bottom: 8px; }
.flow-actions { display: flex; gap: 4px; }
</style>
```

- [ ] **Step 2: 验证页面**

打开 http://localhost:5173/#/flows：
- 卡片列表显示（空或已有流程）
- 新建流程填写名称后保存，卡片出现
- 点击"编辑"弹窗预填数据，保存后更新
- 点击"运行"跳转到 /run 页
- 删除确认后卡片消失

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/FlowsView.vue
git commit -m "feat: implement FlowsView CRUD"
```

---

## Task 9: SettingsView.vue

**Files:**
- Modify: `frontend/src/views/SettingsView.vue`

- [ ] **Step 1: 替换 SettingsView.vue 为完整实现**

```vue
<!-- frontend/src/views/SettingsView.vue -->
<template>
  <div class="page-view">
    <h2 style="font-size:18px;font-weight:600;color:#0f172a;margin-bottom:16px">设置</h2>

    <!-- 健康状态 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <template #header>后端服务状态</template>
      <div style="display:flex;align-items:center;gap:10px">
        <span class="health-dot-lg" :class="settingsStore.healthy ? 'ok' : 'fail'"></span>
        <span style="font-size:14px;color:#334155">
          {{ settingsStore.healthy ? '服务正常' : '服务异常或未启动' }}
        </span>
        <el-button size="small" link @click="settingsStore.fetchHealth()">刷新</el-button>
      </div>
    </el-card>

    <!-- 可用模型 -->
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>可用 LLM 模型</span>
          <el-button size="small" link @click="settingsStore.fetchModels()">刷新</el-button>
        </div>
      </template>
      <el-table :data="settingsStore.models">
        <el-table-column prop="display_name" label="显示名称" />
        <el-table-column prop="model_name"   label="模型 ID" />
        <el-table-column prop="provider"     label="Provider" />
      </el-table>
      <el-empty v-if="!settingsStore.models.length" description="暂无模型数据" />
    </el-card>
  </div>
</template>

<script setup>
import { useSettingsStore } from '../stores/settingsStore.js'
const settingsStore = useSettingsStore()
</script>

<style scoped>
.health-dot-lg      { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.health-dot-lg.ok   { background: #22c55e; }
.health-dot-lg.fail { background: #ef4444; }
</style>
```

- [ ] **Step 2: 验证页面**

打开 http://localhost:5173/#/settings：
- 后端健康状态显示（启动后端后绿点）
- 模型表格显示后端返回的模型列表
- "刷新"按钮重新拉取数据

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/SettingsView.vue
git commit -m "feat: implement SettingsView"
```

---

## Task 10: Maven 构建集成 — pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 在 pom.xml 的 `<plugins>` 段加入 frontend-maven-plugin**

在 `</plugins>` 闭合标签前插入：

```xml
<plugin>
  <groupId>com.github.eirslett</groupId>
  <artifactId>frontend-maven-plugin</artifactId>
  <version>1.15.0</version>
  <configuration>
    <workingDirectory>frontend</workingDirectory>
    <installDirectory>target</installDirectory>
  </configuration>
  <executions>
    <execution>
      <id>install-node-and-npm</id>
      <goals><goal>install-node-and-npm</goal></goals>
      <configuration>
        <nodeVersion>v20.11.0</nodeVersion>
      </configuration>
    </execution>
    <execution>
      <id>npm-install</id>
      <goals><goal>npm</goal></goals>
    </execution>
    <execution>
      <id>npm-build</id>
      <goals><goal>npm</goal></goals>
      <phase>generate-resources</phase>
      <configuration>
        <arguments>run build</arguments>
      </configuration>
    </execution>
  </executions>
</plugin>
```

> **注意**：`vite.config.js` 中 `build.outDir: '../src/main/resources/static'` 已将产物直接输出到 Spring Boot 静态目录，无需额外 copy 步骤。

- [ ] **Step 2: 在 `src/main/resources/static/` 的 .gitignore 排除构建产物**

在 `src/main/resources/static/` 目录下创建 `.gitignore`（防止提交编译后的前端文件）：

```
*
!.gitignore
```

- [ ] **Step 3: 运行 mvn package 验证全链路构建**

```bash
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn package -DskipTests
```

预期输出中包含：
```
[INFO] Running 'npm run build' in d:\workspace\ai-hedge\ai-hedge-fund-java\frontend
...
[INFO] BUILD SUCCESS
```

产物：`target/ai-hedge-fund-java-1.0.0-SNAPSHOT.jar`

- [ ] **Step 4: 验证 jar 包含前端，访问 http://localhost:8000**

```bash
$env:JAVA_HOME = "D:\JetBrains\IntelliJ IDEA 2025.3.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -jar target/ai-hedge-fund-java-1.0.0-SNAPSHOT.jar
```

浏览器打开 http://localhost:8000 — 应显示 AI Hedge Fund 前端（同 localhost:5173）。

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/static/.gitignore
git commit -m "feat: integrate Vue frontend into Maven build via frontend-maven-plugin"
```

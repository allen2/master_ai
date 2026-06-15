<!-- frontend/src/views/RunView.vue -->
<template>
  <div class="page-view">
    <!-- 配置栏 -->
    <el-card shadow="never" class="config-card">
      <el-form :model="form" :rules="rules" ref="formRef" inline class="config-form">
        <el-form-item label="股票代码" prop="tickers">
          <el-input v-model="form.tickers" placeholder="AAPL, MSFT" style="width:140px" />
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
            <el-option v-for="a in analystList" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
          <el-button link type="primary" class="preset-btn"
            title="一键选中护城河 / 低估值 / DCF 三组共 6 位分析师"
            @click="applyQualityValuePreset">质量价值组合</el-button>
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
    <div class="results-grid" v-if="hasResults || runStore.running">
      <!-- 左：分析师进度 -->
      <el-card shadow="never">
        <template #header>分析师进度</template>
        <div class="analyst-list">
          <div v-for="(info, agent) in runStore.analysts" :key="agent" class="analyst-row">

            <!-- 第一行：状态 + 名称 + 画像入口 + 信号 tag -->
            <div class="analyst-header">
              <span class="status-dot" :class="info.status"></span>
              <!-- 名称可点击 / 悬停弹出画像 -->
              <el-popover placement="right-start" :width="420" trigger="click"
                :show-after="0" :hide-after="0" v-if="profileFor(agent)">
                <template #reference>
                  <span class="agent-name agent-name-link">{{ agent }}</span>
                </template>
                <ProfileCard :profile="profileFor(agent)" />
              </el-popover>
              <span v-else class="agent-name">{{ agent }}</span>

              <div class="signal-tags">
                <el-tag v-for="(sig, ticker) in signalsForAgent(agent)" :key="ticker"
                  :type="tagType(sig.signal)" size="small" style="margin-left:4px">
                  {{ ticker }}: {{ sig.signal }} {{ sig.confidence }}%
                </el-tag>
              </div>
              <!-- 画像小图标（如果 profile 存在） -->
              <span v-if="profileFor(agent)" class="profile-badge" title="点击名称查看画像">📋</span>
            </div>

            <!-- 进行中：实时活动消息（调用工具/获取数据/推理中） -->
            <div v-if="info.status === 'analyzing' && runStore.activities[agent]"
              class="analyst-activity">
              <span class="activity-spinner">⏳</span>{{ runStore.activities[agent] }}
            </div>

            <!-- 第二行：reasoning（仅完成后显示） -->
            <template v-for="(sig, ticker) in signalsForAgent(agent)" :key="'r-' + ticker">
              <div v-if="sig.reasoning" class="analyst-reasoning">
                <span class="reasoning-ticker">{{ ticker }}：</span>{{ sig.reasoning }}
              </div>
            </template>
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

            <!-- 质量价值三重门槛明细 -->
            <div v-if="dec.gate_detail?.length" class="gate-box">
              <div class="gate-title">
                质量价值门槛
                <el-tag :type="dec.gate_passed ? 'success' : 'info'" size="small" effect="plain">
                  {{ dec.gate_passed ? '✓ 三组全过' : '✕ 未通过' }}
                </el-tag>
              </div>
              <div class="gate-items">
                <span v-for="(item, i) in dec.gate_detail" :key="i"
                  class="gate-item" :class="gateItemClass(item)">
                  {{ item }}
                </span>
              </div>
            </div>

            <div class="signal-bar">
              <div class="bar-bull"    :style="{ flex: dec.bull_count || 0 }"></div>
              <div class="bar-bear"    :style="{ flex: dec.bear_count || 0 }"></div>
              <div class="bar-neutral" :style="{ flex: 1 }"></div>
            </div>
            <div class="signal-counts">bull {{ dec.bull_count ?? 0 }} · bear {{ dec.bear_count ?? 0 }}</div>
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
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRunStore } from '../stores/runStore.js'
import { useSettingsStore } from '../stores/settingsStore.js'
import ProfileCard from '../components/ProfileCard.vue'

const runStore      = useRunStore()
const settingsStore = useSettingsStore()
const formRef       = ref(null)

/** 从后端 API 获取的分析师完整画像列表 */
const profiles = ref([])

/** 画像映射：displayName → AgentProfile */
const profileMap = computed(() => {
  const map = {}
  for (const p of profiles.value) {
    map[p.displayName] = p
  }
  return map
})

/** 用于下拉选择器的简版列表 */
const analystList = computed(() =>
  profiles.value.map(p => ({ id: p.agentId, name: p.displayName }))
)

onMounted(async () => {
  try {
    const res = await fetch('/hedge-fund/analysts')
    if (res.ok) {
      profiles.value = await res.json()
    }
  } catch (_) {
    // 保留硬编码兜底列表
    profiles.value = FALLBACK_ANALYST_LIST.map(a => ({ agentId: a.id, displayName: a.name }))
  }
})

/** 获取某个分析师的完整画像 */
function profileFor(displayName) {
  return profileMap.value[displayName] || null
}

/** 兜底：API 不可用时的硬编码列表 */
const FALLBACK_ANALYST_LIST = [
  { id: 'warren_buffett',          name: '沃伦·巴菲特' },
  { id: 'ben_graham',              name: '本杰明·格雷厄姆' },
  { id: 'charlie_munger',          name: '查理·芒格' },
  { id: 'phil_fisher',             name: '菲利普·费雪' },
  { id: 'peter_lynch',             name: '彼得·林奇' },
  { id: 'aswath_damodaran',        name: '阿斯沃斯·达摩达兰' },
  { id: 'nassim_taleb',            name: '纳西姆·塔勒布' },
  { id: 'stanley_druckenmiller',   name: '斯坦利·德鲁肯米勒' },
  { id: 'michael_burry',           name: '迈克尔·伯里' },
  { id: 'bill_ackman',             name: '比尔·阿克曼' },
  { id: 'cathie_wood',             name: '凯西·伍德' },
  { id: 'mohnish_pabrai',          name: '莫尼什·帕布莱' },
  { id: 'rakesh_jhunjhunwala',     name: '拉克什·胡杰' },
  { id: 'fundamentals_analyst',    name: '基本面分析师' },
  { id: 'growth_analyst',          name: '成长分析师' },
  { id: 'sentiment_analyst',       name: '情绪分析师' },
  { id: 'technical_analyst',       name: '技术分析师' },
  { id: 'valuation_analyst',       name: '估值分析师' },
  { id: 'news_sentiment_analyst',  name: '新闻情绪分析师' },
]

const form = reactive({
  tickers:          'AAPL',
  modelName:        settingsStore.defaultModel,
  selectedAnalysts: []
})

const rules = {
  tickers: [{ required: true, message: '请输入至少一个股票代码', trigger: 'blur' }]
}

/**
 * 质量价值三重门槛预设：护城河组 + 低估值组 + DCF 内在价值组，共 6 位分析师，
 * 与后端 PortfolioManagerAgent 的门槛分组保持一致。
 */
const QUALITY_VALUE_PRESET = [
  'warren_buffett',     // 护城河组
  'charlie_munger',     // 护城河组
  'ben_graham',         // 低估值组
  'mohnish_pabrai',     // 低估值组
  'aswath_damodaran',   // DCF 内在价值组
  'valuation_analyst'   // 低估值组 + DCF 组
]

/** 一键填入质量价值组合 */
function applyQualityValuePreset() {
  form.selectedAnalysts = [...QUALITY_VALUE_PRESET]
  ElMessage.success('已选中质量价值组合（6 位分析师）')
}

const hasResults = computed(() =>
  Object.keys(runStore.analysts).length > 0 || Object.keys(runStore.decisions).length > 0
)

function signalsForAgent(agentDisplayName) {
  const agentId = analystList.value.find(a => a.name === agentDisplayName)?.id
  return agentId ? (runStore.analystSignals[agentId] || {}) : {}
}

function tagType(signal) {
  return signal === 'bullish' ? 'success' : signal === 'bearish' ? 'danger' : 'info'
}

function actionTagType(action) {
  const a = action?.toLowerCase()
  return a === 'buy' ? 'success' : a === 'sell' ? 'danger' : 'info'
}

/** 门槛单项配色：通过=绿，否决=红，缺组/未确认=灰 */
function gateItemClass(item) {
  if (item.includes('通过')) {
    return 'gate-pass'
  }
  if (item.includes('否决')) {
    return 'gate-veto'
  }
  return 'gate-miss'
}

async function handleRun() {
  await formRef.value.validate()
  const tickers = form.tickers.split(',').map(s => s.trim().toUpperCase()).filter(Boolean)
  if (!tickers.length) { ElMessage.error('请输入有效的股票代码'); return }
  runStore.startRun({
    tickers,
    model_name:        form.modelName || settingsStore.defaultModel,
    model_provider:    'OpenAI',
    initial_cash:      100000,
    selected_analysts: form.selectedAnalysts.length ? form.selectedAnalysts : null
  })
}
</script>

<style scoped>
.config-card  { margin-bottom: 0; }
.config-form  { flex-wrap: wrap; gap: 4px; }
.preset-btn   { margin-left: 6px; font-size: 12px; }
.results-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 12px; }

.analyst-list    { display: flex; flex-direction: column; gap: 6px; }
.analyst-row     { display: flex; flex-direction: column; gap: 4px;
                   padding: 8px; border-radius: 6px; background: #f8fafc; }
.analyst-header  { display: flex; align-items: center; gap: 8px; }
.status-dot      { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.status-dot.waiting   { background: #e2e8f0; }
.status-dot.analyzing { background: #f97316; }
.status-dot.done      { background: #22c55e; }
.status-dot.error     { background: #ef4444; }
.agent-name      { flex: 0 0 160px; font-size: 12px; font-weight: 500; color: #334155; }
.agent-name-link { cursor: pointer; color: #1d4ed8; text-decoration: underline dotted; }
.agent-name-link:hover { color: #2563eb; }
.signal-tags     { display: flex; flex-wrap: wrap; gap: 2px; }
.profile-badge   { font-size: 13px; opacity: 0.6; margin-left: auto; }
.analyst-activity   { font-size: 11px; color: #f97316; line-height: 1.5;
                      padding-left: 16px; display: flex; align-items: center; gap: 4px; }
.activity-spinner   { font-size: 11px; }
.analyst-reasoning  { font-size: 11px; color: #64748b; line-height: 1.5;
                      padding-left: 16px; border-left: 2px solid #e2e8f0; }
.reasoning-ticker   { font-weight: 600; color: #475569; margin-right: 4px; }

.decision-card   { border: 1px solid #e2e8f0; border-radius: 8px; padding: 10px; margin-bottom: 8px; }
.decision-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.ticker-symbol   { font-size: 15px; font-weight: 600; color: #0f172a; }
.gate-box        { margin: 6px 0; padding: 6px 8px; border-radius: 6px; background: #f8fafc; }
.gate-title      { display: flex; align-items: center; gap: 6px;
                   font-size: 11px; font-weight: 600; color: #475569; margin-bottom: 4px; }
.gate-items      { display: flex; flex-direction: column; gap: 2px; }
.gate-item       { font-size: 11px; line-height: 1.5; padding: 1px 6px; border-radius: 4px; }
.gate-pass       { color: #15803d; background: #f0fdf4; }
.gate-veto       { color: #b91c1c; background: #fef2f2; }
.gate-miss       { color: #64748b; background: #f1f5f9; }
.signal-bar      { display: flex; height: 6px; border-radius: 3px; overflow: hidden;
                   margin-bottom: 4px; gap: 1px; }
.bar-bull    { background: #22c55e; }
.bar-bear    { background: #ef4444; }
.bar-neutral { background: #e2e8f0; min-width: 4px; }
.signal-counts { font-size: 11px; color: #64748b; margin-bottom: 4px; }
.reasoning     { font-size: 11px; color: #475569; line-height: 1.4; }
</style>

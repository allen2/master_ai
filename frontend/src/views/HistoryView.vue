<!-- frontend/src/views/HistoryView.vue -->
<template>
  <div class="page-view">
    <h2 class="page-title">分析记录</h2>

    <div v-if="loading" class="loading-tip">加载中…</div>
    <el-empty v-else-if="list.length === 0" description="暂无分析记录" />
    <table v-else class="history-table">
      <thead>
        <tr>
          <th>时间</th>
          <th>股票 / 分析需求</th>
          <th>模型</th>
          <th>状态</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in list" :key="item.id" class="history-row" @click="openDetail(item)">
          <td class="col-time">{{ item.created_at }}</td>
          <td class="col-tickers">{{ item.tickers }}</td>
          <td class="col-model">{{ item.model_name || '—' }}</td>
          <td>
            <el-tag :type="statusTagType(item.status)" size="small">
              {{ statusLabel(item.status) }}
            </el-tag>
          </td>
          <td class="col-action">查看 ›</td>
        </tr>
      </tbody>
    </table>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination">
      <button :disabled="pageNum <= 1" @click="changePage(pageNum - 1)">上一页</button>
      <span>第 {{ pageNum }} / {{ totalPages }} 页，共 {{ total }} 条</span>
      <button :disabled="pageNum >= totalPages" @click="changePage(pageNum + 1)">下一页</button>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="分析详情" width="640px">
      <div v-if="detailLoading" class="loading-tip">加载中…</div>
      <div v-else-if="detail">
        <div class="detail-meta">
          <span>{{ isResearchReport(detail) ? '分析需求' : '股票' }}：{{ detail.tickers }}</span>
          <span>模型：{{ detail.model_name || '—' }}</span>
          <span>时间：{{ detail.created_at }}</span>
        </div>
        <el-alert v-if="detail.status === 'ERROR'" :title="detail.error_message || '分析失败'"
          type="error" show-icon :closable="false" style="margin-bottom:12px" />

        <!-- 研究类分析报告（产业分析 / 逆向对立面分析，Markdown 渲染） -->
        <div v-if="isResearchReport(detail)">
          <div class="detail-section-title">分析报告</div>
          <div class="report-content markdown-body" v-html="renderMarkdown(detail.decisions?.report)"></div>
        </div>

        <!-- 交易决策 -->
        <div v-if="!isResearchReport(detail) && detail.decisions && Object.keys(detail.decisions).length">
          <div class="detail-section-title">交易决策</div>
          <div v-for="(dec, ticker) in detail.decisions" :key="ticker" class="decision-card">
            <div class="decision-header">
              <span class="ticker-symbol">{{ ticker }}</span>
              <el-tag :type="actionTagType(dec.action)" size="default">
                {{ dec.action?.toUpperCase() }} × {{ dec.quantity ?? 0 }}
              </el-tag>
            </div>
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

        <!-- 分析师信号 -->
        <div v-if="detail.analyst_signals && Object.keys(detail.analyst_signals).length">
          <div class="detail-section-title">分析师信号</div>
          <div class="analyst-list">
            <div v-for="(tickerSignals, agentId) in detail.analyst_signals" :key="agentId" class="analyst-row">
              <div class="analyst-header">
                <span class="agent-name">{{ agentDisplayName(agentId) }}</span>
                <div class="signal-tags">
                  <el-tag v-for="(sig, ticker) in tickerSignals" :key="ticker"
                    :type="tagType(sig.signal)" size="small" style="margin-left:4px">
                    {{ ticker }}: {{ sig.signal }} {{ sig.confidence }}%
                  </el-tag>
                </div>
              </div>
              <template v-for="(sig, ticker) in tickerSignals" :key="'r-' + ticker">
                <div v-if="sig.reasoning" class="analyst-reasoning">
                  <span class="reasoning-ticker">{{ ticker }}：</span>{{ sig.reasoning }}
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { analysisRunsApi } from '../api/index.js'
import { renderMarkdown } from '../utils/markdown.js'

/** agentId → 中文名 映射（与后端 AgentProfileRegistry + RunView FALLBACK_ANALYST_LIST 保持一致） */
const AGENT_NAME_MAP = {
  warren_buffett:          '沃伦·巴菲特',
  ben_graham:              '本杰明·格雷厄姆',
  charlie_munger:          '查理·芒格',
  phil_fisher:             '菲利普·费雪',
  peter_lynch:             '彼得·林奇',
  aswath_damodaran:        '阿斯沃斯·达摩达兰',
  nassim_taleb:            '纳西姆·塔勒布',
  stanley_druckenmiller:   '斯坦利·德鲁肯米勒',
  michael_burry:           '迈克尔·伯里',
  bill_ackman:             '比尔·阿克曼',
  cathie_wood:             '凯西·伍德',
  mohnish_pabrai:          '莫尼什·帕布莱',
  rakesh_jhunjhunwala:     '拉克什·胡杰',
  fundamentals_analyst:    '基本面分析师',
  growth_analyst:          '成长分析师',
  sentiment_analyst:       '情绪分析师',
  technical_analyst:       '技术分析师',
  valuation_analyst:       '估值分析师',
  news_sentiment_analyst:  '新闻情绪分析师',
}

/** 将 agentId 转为中文显示名，找不到则返回原值 */
function agentDisplayName(agentId) {
  return AGENT_NAME_MAP[agentId] || agentId
}

/** 以 Markdown 报告为产出的研究类分析记录类型（产业分析、逆向对立面分析） */
const RESEARCH_REPORT_TYPES = ['industry_analysis', 'contrarian_analysis']

const list    = ref([])
const loading = ref(false)
const pageNum  = ref(1)
const pageSize = ref(10)
const total    = ref(0)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

onMounted(loadList)

async function loadList() {
  loading.value = true
  try {
    const data = await analysisRunsApi.list(pageNum.value, pageSize.value)
    list.value  = data.list
    total.value = data.total
  } catch (_) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function changePage(page) {
  pageNum.value = page
  await loadList()
}

async function openDetail(item) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await analysisRunsApi.detail(item.id)
  } finally {
    detailLoading.value = false
  }
}

function statusLabel(status) {
  return { RUNNING: '进行中', COMPLETE: '已完成', ERROR: '失败' }[status] || status
}

function statusTagType(status) {
  return { RUNNING: 'warning', COMPLETE: 'success', ERROR: 'danger' }[status] || 'info'
}

function tagType(signal) {
  return signal === 'bullish' ? 'success' : signal === 'bearish' ? 'danger' : 'info'
}

function actionTagType(action) {
  const a = action?.toLowerCase()
  return a === 'buy' ? 'success' : a === 'sell' ? 'danger' : 'info'
}

function isResearchReport(item) {
  return RESEARCH_REPORT_TYPES.some((type) => item?.selected_analysts?.includes(type))
}

function gateItemClass(item) {
  if (item.includes('通过')) {
    return 'gate-pass'
  }
  if (item.includes('否决')) {
    return 'gate-veto'
  }
  return 'gate-miss'
}
</script>

<style scoped>
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}
.loading-tip {
  font-size: 13px;
  color: #94a3b8;
  padding: 24px 0;
  text-align: center;
}
.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.history-table th {
  background: #f8fafc;
  padding: 8px 12px;
  text-align: left;
  color: #64748b;
  font-weight: 500;
  border-bottom: 1px solid #e2e8f0;
}
.history-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}
.history-table tr:last-child td { border-bottom: none; }
.history-row { cursor: pointer; }
.history-row:hover { background: #f8fafc; }
.col-time { color: #94a3b8; font-size: 12px; }
.col-tickers { font-weight: 600; }
.col-action { color: #2563eb; text-align: right; font-size: 12px; }
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
  font-size: 13px;
  color: #64748b;
}
.pagination button {
  padding: 4px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
}
.pagination button:disabled {
  opacity: .4;
  cursor: not-allowed;
}

.detail-meta { display: flex; gap: 16px; font-size: 12px; color: #64748b; margin-bottom: 12px; }
.detail-section-title { font-size: 14px; font-weight: 600; color: #334155; margin: 12px 0 8px; }

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

.analyst-list    { display: flex; flex-direction: column; gap: 6px; }
.analyst-row     { display: flex; flex-direction: column; gap: 4px;
                   padding: 8px; border-radius: 6px; background: #f8fafc; }
.analyst-header  { display: flex; align-items: center; gap: 8px; }
.agent-name      { flex: 0 0 160px; font-size: 12px; font-weight: 500; color: #334155; }
.signal-tags     { display: flex; flex-wrap: wrap; gap: 2px; }
.analyst-reasoning  { font-size: 11px; color: #64748b; line-height: 1.5;
                      padding-left: 16px; border-left: 2px solid #e2e8f0; }
.reasoning-ticker   { font-weight: 600; color: #475569; margin-right: 4px; }

.report-content  { font-size: 13px; line-height: 1.7; color: #1e293b; word-break: break-word; }
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) {
  margin: 16px 0 8px; font-weight: 600; color: #0f172a;
}
.markdown-body :deep(h1) { font-size: 18px; }
.markdown-body :deep(h2) { font-size: 16px; }
.markdown-body :deep(h3) { font-size: 14px; }
.markdown-body :deep(p)  { margin: 8px 0; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: 8px 0; padding-left: 24px; }
.markdown-body :deep(li) { margin: 4px 0; }
.markdown-body :deep(table) { border-collapse: collapse; margin: 8px 0; width: 100%; }
.markdown-body :deep(th), .markdown-body :deep(td) {
  border: 1px solid #e2e8f0; padding: 6px 10px; text-align: left;
}
.markdown-body :deep(th) { background: #f1f5f9; }
.markdown-body :deep(code) {
  background: #f1f5f9; padding: 1px 4px; border-radius: 4px; font-size: 12px;
}
.markdown-body :deep(pre) {
  background: #f1f5f9; padding: 10px; border-radius: 6px; overflow-x: auto;
}
.markdown-body :deep(blockquote) {
  margin: 8px 0; padding: 4px 12px; border-left: 3px solid #cbd5e1; color: #64748b;
}
</style>

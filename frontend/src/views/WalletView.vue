<template>
  <div class="page-view">
    <h2 class="page-title">我的钱包</h2>

    <!-- 余额卡片 -->
    <div class="balance-card">
      <div class="balance-icon">🪙</div>
      <div class="balance-info">
        <div class="balance-number">{{ walletStore.balance ?? '—' }}</div>
        <div class="balance-label">可用金币</div>
      </div>
      <div class="balance-tip">每次 AI 分析消耗 1 枚金币</div>
    </div>

    <!-- 流水列表 -->
    <div class="section">
      <div class="section-title">金币流水</div>
      <div v-if="loadingTx" class="loading-tip">加载中…</div>
      <div v-else-if="transactions.length === 0" class="empty-tip">暂无流水记录</div>
      <table v-else class="tx-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>类型</th>
            <th>变动</th>
            <th>余额</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in transactions" :key="tx.id">
            <td class="col-time">{{ tx.createdAt }}</td>
            <td>
              <span :class="['tx-tag', tx.type === 'GRANT' ? 'tag-grant' : 'tag-deduct']">
                {{ tx.type === 'GRANT' ? '发放' : '消耗' }}
              </span>
            </td>
            <td :class="tx.amount > 0 ? 'amount-plus' : 'amount-minus'">
              {{ tx.amount > 0 ? '+' + tx.amount : tx.amount }}
            </td>
            <td>{{ tx.balanceAfter }}</td>
            <td class="col-reason">{{ tx.reason || '—' }}</td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination">
        <button :disabled="pageNum <= 1" @click="changePage(pageNum - 1)">上一页</button>
        <span>第 {{ pageNum }} / {{ totalPages }} 页，共 {{ total }} 条</span>
        <button :disabled="pageNum >= totalPages" @click="changePage(pageNum + 1)">下一页</button>
      </div>
    </div>

    <!-- 充值说明 -->
    <div class="recharge-tip">
      <span>金币不足？请联系管理员充值 →</span>
      <router-link to="/contact">联系我们</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { walletApi } from '../api/index.js'
import { useWalletStore } from '../stores/walletStore.js'

const walletStore = useWalletStore()
const transactions = ref([])
const loadingTx    = ref(false)
const pageNum      = ref(1)
const pageSize     = ref(20)
const total        = ref(0)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

onMounted(async () => {
  await walletStore.fetchBalance()
  await loadTransactions()
})

async function loadTransactions() {
  loadingTx.value = true
  try {
    const data = await walletApi.transactions(pageNum.value, pageSize.value)
    transactions.value = data.list
    total.value        = data.total
  } catch (_) {
    transactions.value = []
  } finally {
    loadingTx.value = false
  }
}

async function changePage(page) {
  pageNum.value = page
  await loadTransactions()
}
</script>

<style scoped>
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}
.balance-card {
  background: linear-gradient(135deg, #1e40af, #3b82f6);
  border-radius: 12px;
  padding: 24px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
  box-shadow: 0 4px 16px rgba(37,99,235,.25);
}
.balance-icon {
  font-size: 40px;
  flex-shrink: 0;
}
.balance-info {
  flex: 1;
}
.balance-number {
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
}
.balance-label {
  font-size: 13px;
  opacity: .8;
  margin-top: 4px;
}
.balance-tip {
  font-size: 12px;
  opacity: .7;
  text-align: right;
}
.section { margin-bottom: 24px; }
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #e2e8f0;
}
.loading-tip, .empty-tip {
  font-size: 13px;
  color: #94a3b8;
  padding: 24px 0;
  text-align: center;
}
.tx-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.tx-table th {
  background: #f8fafc;
  padding: 8px 12px;
  text-align: left;
  color: #64748b;
  font-weight: 500;
  border-bottom: 1px solid #e2e8f0;
}
.tx-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}
.tx-table tr:last-child td { border-bottom: none; }
.col-time { color: #94a3b8; font-size: 12px; }
.col-reason { color: #64748b; }
.tx-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}
.tag-grant  { background: #dcfce7; color: #16a34a; }
.tag-deduct { background: #fef2f2; color: #dc2626; }
.amount-plus  { color: #16a34a; font-weight: 600; }
.amount-minus { color: #dc2626; font-weight: 600; }
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
.recharge-tip {
  font-size: 13px;
  color: #64748b;
  text-align: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}
.recharge-tip a {
  color: #2563eb;
  text-decoration: none;
  margin-left: 4px;
  font-weight: 500;
}
</style>

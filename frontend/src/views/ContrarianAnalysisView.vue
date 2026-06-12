<!-- frontend/src/views/ContrarianAnalysisView.vue -->
<template>
  <div class="page-view">
    <!-- 输入栏 -->
    <el-card shadow="never" class="config-card">
      <el-form :model="form" inline class="config-form" @submit.prevent>
        <el-form-item label="逆向对立面分析需求" style="flex:1">
          <el-input v-model="form.query" placeholder="例如：分析新能源行业的逆向对立面标的"
            style="width:420px" :disabled="store.running"
            @keyup.enter="handleRun" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="store.running" @click="handleRun">
            {{ store.running ? '分析中...' : '▶ 开始分析' }}
          </el-button>
          <el-button v-if="store.running" @click="store.stopAnalysis()">⏹ 停止</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 错误横幅 -->
    <el-alert v-if="store.error" :title="store.error" type="error"
      show-icon :closable="false" style="margin-top:12px" />

    <!-- 活动进度 -->
    <el-card v-if="store.activities.length" shadow="never" style="margin-top:12px">
      <template #header>分析进度</template>
      <div class="activity-list">
        <div v-for="(msg, idx) in store.activities" :key="idx" class="activity-row">
          <span v-if="idx === store.activities.length - 1 && store.running" class="activity-spinner">⏳</span>
          <span v-else class="activity-done">✓</span>
          {{ msg }}
        </div>
      </div>
    </el-card>

    <!-- 分析报告 -->
    <el-card v-if="store.report" shadow="never" style="margin-top:12px">
      <template #header>分析报告</template>
      <div class="report-content markdown-body" v-html="reportHtml"></div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useContrarianAnalysisStore } from '../stores/contrarianAnalysisStore.js'
import { renderMarkdown } from '../utils/markdown.js'

const store = useContrarianAnalysisStore()

const reportHtml = computed(() => renderMarkdown(store.report))

const form = reactive({
  query: ''
})

function handleRun() {
  if (store.running) {
    return
  }
  if (!form.query.trim()) {
    ElMessage.warning('请输入逆向对立面分析需求')
    return
  }
  store.startAnalysis({ query: form.query.trim() })
}
</script>

<style scoped>
.config-form     { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.activity-list   { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: #475569; }
.activity-row    { display: flex; align-items: center; gap: 6px; }
.activity-spinner{ display: inline-block; }
.activity-done   { color: #22c55e; }
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

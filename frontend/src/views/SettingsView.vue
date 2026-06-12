<!-- frontend/src/views/SettingsView.vue -->
<template>
  <div class="page-view">
    <h2 style="font-size:18px;font-weight:600;color:#0f172a;margin-bottom:16px">设置</h2>

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

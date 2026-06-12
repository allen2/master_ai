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

const keys          = ref([])
const loading       = ref(false)
const saving        = ref(false)
const dialogVisible = ref(false)
const formRef       = ref(null)
const form          = ref({ provider: '', keyValue: '' })
const rules         = {
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
.page-header    { display: flex; justify-content: space-between; align-items: center; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #0f172a; }
</style>

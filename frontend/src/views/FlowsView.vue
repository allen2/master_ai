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
          <el-button size="small" type="primary" link @click="goRun">运行</el-button>
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
.page-header    { display: flex; justify-content: space-between; align-items: center; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #0f172a; }
.flows-grid  { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
.flow-card   { cursor: default; }
.flow-name   { font-size: 14px; font-weight: 600; color: #0f172a; margin-bottom: 4px; }
.flow-desc   { font-size: 12px; color: #64748b; margin-bottom: 4px; min-height: 18px; }
.flow-meta   { font-size: 11px; color: #94a3b8; margin-bottom: 8px; }
.flow-actions { display: flex; gap: 4px; }
</style>

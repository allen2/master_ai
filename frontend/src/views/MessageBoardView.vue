<!-- frontend/src/views/MessageBoardView.vue -->
<template>
  <div class="page-view">
    <h2 class="page-title">留言板</h2>

    <div class="post-box">
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="说点什么吧…"
      />
      <div class="post-actions">
        <el-button type="primary" :loading="posting" :disabled="!content.trim()" @click="handlePost">
          发表
        </el-button>
      </div>
    </div>

    <div v-if="loading && list.length === 0" class="loading-tip">加载中…</div>
    <el-empty v-else-if="list.length === 0" description="暂无留言，来发表第一条吧" />
    <div v-else class="message-list">
      <div v-for="item in list" :key="item.id" class="message-card">
        <div class="message-header">
          <span class="message-author">{{ item.nickname }}</span>
          <span class="message-time">{{ item.created_at }}</span>
        </div>
        <div class="message-content">{{ item.content }}</div>
        <div class="message-actions">
          <button class="like-btn" :class="{ liked: item.liked_by_me }" @click="handleLike(item)">
            👍 {{ item.like_count }}
          </button>
          <el-popconfirm v-if="item.can_delete" title="确认删除该留言？" @confirm="handleDelete(item)">
            <template #reference>
              <button class="delete-btn">删除</button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <div v-if="list.length < total" class="load-more">
      <el-button :loading="loading" @click="loadMore">加载更多</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { messageBoardApi } from '../api/index.js'

const list    = ref([])
const total   = ref(0)
const pageNum  = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const posting = ref(false)
const content = ref('')

onMounted(loadPage)

async function loadPage() {
  loading.value = true
  try {
    const data = await messageBoardApi.list(pageNum.value, pageSize.value)
    list.value = pageNum.value === 1 ? data.list : [...list.value, ...data.list]
    total.value = data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  pageNum.value += 1
  await loadPage()
}

async function handlePost() {
  const text = content.value.trim()
  if (!text) {
    return
  }
  posting.value = true
  try {
    const created = await messageBoardApi.create(text)
    list.value.unshift(created)
    total.value += 1
    content.value = ''
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    posting.value = false
  }
}

async function handleLike(item) {
  const wasLiked = item.liked_by_me
  item.liked_by_me = !wasLiked
  item.like_count += wasLiked ? -1 : 1
  try {
    const updated = await messageBoardApi.like(item.id)
    item.liked_by_me = updated.liked_by_me
    item.like_count = updated.like_count
  } catch (e) {
    item.liked_by_me = wasLiked
    item.like_count += wasLiked ? 1 : -1
    ElMessage.error(e.message)
  }
}

async function handleDelete(item) {
  try {
    await messageBoardApi.remove(item.id)
    list.value = list.value.filter((m) => m.id !== item.id)
    total.value -= 1
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(e.message)
  }
}
</script>

<style scoped>
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}
.post-box {
  margin-bottom: 20px;
}
.post-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
.loading-tip {
  font-size: 13px;
  color: #94a3b8;
  padding: 24px 0;
  text-align: center;
}
.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.message-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px;
  background: #fff;
}
.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.message-author {
  font-weight: 600;
  color: #0f172a;
  font-size: 13px;
}
.message-time {
  font-size: 12px;
  color: #94a3b8;
}
.message-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.message-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}
.like-btn, .delete-btn {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  padding: 4px 10px;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
}
.like-btn.liked {
  color: #2563eb;
  border-color: #93c5fd;
  background: #eff6ff;
}
.delete-btn:hover {
  color: #ef4444;
  border-color: #fca5a5;
}
.load-more {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>

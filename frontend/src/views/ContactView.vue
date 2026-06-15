<!-- frontend/src/views/ContactView.vue -->
<template>
  <div class="page-view">
    <h2 style="font-size:18px;font-weight:600;color:#0f172a;margin-bottom:16px">联系我们</h2>

    <el-card shadow="never" style="margin-bottom:16px">
      <template #header>金木智库 · 联系方式</template>
      <div class="contact-list">
        <div v-for="item in contacts" :key="item.label" class="contact-row">
          <span class="contact-icon">{{ item.icon }}</span>
          <span class="contact-label">{{ item.label }}</span>
          <a v-if="item.link" :href="item.link" target="_blank" rel="noopener" class="contact-value">
            {{ item.value }}
          </a>
          <span v-else class="contact-value">{{ item.value }}</span>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>关于</template>
      <p class="about-text">
        金木智库是一个教育性的 AI 投资分析平台，汇聚多位投资大师的分析视角，仅供学习研究，不构成任何投资建议。
        如有合作、反馈或问题，欢迎通过上方任意方式联系我们。
      </p>
    </el-card>
  </div>
</template>

<script setup>
/** 联系方式列表，按需修改 */
const contacts = [
  { icon: '📧', label: '邮箱', value: '3433256865@qq.com', link: 'mailto:3433256865@qq.com' },
  { icon: '💬', label: '微信', value: 'allenu' }
]
</script>

<style scoped>
.contact-list  { display: flex; flex-direction: column; gap: 12px; }
.contact-row   { display: flex; align-items: center; gap: 10px; font-size: 14px; }
.contact-icon  { font-size: 16px; }
.contact-label { flex: 0 0 56px; color: #64748b; }
.contact-value { color: #334155; }
a.contact-value { color: #1d4ed8; text-decoration: none; }
a.contact-value:hover { text-decoration: underline; }
.about-text    { font-size: 13px; color: #475569; line-height: 1.7; margin: 0; }
</style>

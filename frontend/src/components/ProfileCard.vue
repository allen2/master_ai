<!-- frontend/src/components/ProfileCard.vue
     分析师五位一体画像卡片（用于 popover / 详情页） -->
<template>
  <div class="profile-card" v-if="profile">
    <!-- 标题行 -->
    <div class="profile-header">
      <span class="profile-name">{{ profile.displayName }}</span>
      <span class="profile-tag" :class="categoryClass">{{ categoryLabel }}</span>
    </div>

    <!-- 五个结构化模块 -->
    <div class="profile-sections">
      <!-- 基础背景 -->
      <div class="section">
        <div class="section-title">📖 {{ profile.background?.title || '基础背景' }}</div>
        <ul class="section-points">
          <li v-for="(p, i) in profile.background?.points || []" :key="'bg'+i">{{ p }}</li>
        </ul>
      </div>
      <!-- 核心投资哲学 -->
      <div class="section">
        <div class="section-title">🧠 {{ profile.philosophy?.title || '核心投资哲学' }}</div>
        <ul class="section-points">
          <li v-for="(p, i) in profile.philosophy?.points || []" :key="'ph'+i">{{ p }}</li>
        </ul>
      </div>
      <!-- 实操体系 -->
      <div class="section">
        <div class="section-title">🔧 {{ profile.methodology?.title || '实操体系' }}</div>
        <ul class="section-points">
          <li v-for="(p, i) in profile.methodology?.points || []" :key="'mt'+i">{{ p }}</li>
        </ul>
      </div>
      <!-- 业绩与特质 -->
      <div class="section">
        <div class="section-title">📊 {{ profile.trackRecord?.title || '业绩与特质' }}</div>
        <ul class="section-points">
          <li v-for="(p, i) in profile.trackRecord?.points || []" :key="'tr'+i">{{ p }}</li>
        </ul>
      </div>
      <!-- 局限性 -->
      <div class="section">
        <div class="section-title">⚠️ {{ profile.limitations?.title || '局限性' }}</div>
        <ul class="section-points">
          <li v-for="(p, i) in profile.limitations?.points || []" :key="'lm'+i">{{ p }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  profile: { type: Object, default: null }
})

const categoryLabel = computed(() => {
  const map = { investor: '投资大师', analyst: '量化分析', specialist: '专项分析' }
  return map[props.profile?.category] || ''
})

const categoryClass = computed(() => `cat-${props.profile?.category || 'other'}`)
</script>

<style scoped>
.profile-card {
  max-height: 460px;
  overflow-y: auto;
  font-size: 12px;
  line-height: 1.6;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.profile-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.profile-tag {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 10px;
  color: #fff;
}
.cat-investor   { background: #6366f1; }
.cat-analyst    { background: #10b981; }
.cat-specialist { background: #f59e0b; }

.profile-sections {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section {
  padding: 6px 8px;
  background: #f8fafc;
  border-radius: 6px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 4px;
}

.section-points {
  margin: 0;
  padding-left: 16px;
  list-style: disc;
  color: #475569;
}

.section-points li {
  margin-bottom: 2px;
  font-size: 11px;
}
</style>

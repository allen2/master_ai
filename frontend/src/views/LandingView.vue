<!-- frontend/src/views/LandingView.vue -->
<template>
  <div class="landing-page">
    <header class="landing-header">
      <div class="landing-logo">📈 金木智库</div>
      <div class="landing-actions">
        <button class="ghost-btn" @click="openModal('login')">登录</button>
        <button class="primary-btn" @click="openModal('register')">免费注册</button>
      </div>
    </header>

    <section class="hero">
      <h1 class="hero-title">AI 对冲基金分析平台</h1>
      <p class="hero-subtitle">
        汇聚 21 位投资大师 / 分析师 Agent 并行分析，配合产业链瓶颈反向拆解与逆向对立面研究框架，
        支持实时 SSE 推流与增量信号推送。
      </p>
      <div class="hero-actions">
        <button class="primary-btn large" @click="openModal('register')">立即免费注册</button>
        <button class="ghost-btn large" @click="openModal('login')">已有账号，登录</button>
      </div>
      <p class="hero-disclaimer">仅供学习和研究，不构成投资建议，不执行真实交易。</p>
    </section>

    <section class="features">
      <div class="feature-card">
        <div class="feature-icon">🧠</div>
        <h3>21 位投资大师 Agent</h3>
        <p>汇聚巴菲特、芒格等多位投资大师与分析师视角，多 Agent 并行分析同一标的，输出多维度结论。</p>
      </div>
      <div class="feature-card">
        <div class="feature-icon">🏭</div>
        <h3>产业链瓶颈反向拆解</h3>
        <p>从产业链物理瓶颈出发，反向定位上下游受益环节，挖掘被市场忽视的低市值高弹性标的。</p>
      </div>
      <div class="feature-card">
        <div class="feature-icon">🔄</div>
        <h3>逆向对立面研究</h3>
        <p>主动构建多空对立面观点，红队证伪潜在风险，帮助发现分析中的盲点与认知偏差。</p>
      </div>
      <div class="feature-card">
        <div class="feature-icon">⚡</div>
        <h3>实时信号推送</h3>
        <p>基于 SSE 实时推流，分析过程逐步可见，增量信号即时送达，无需等待全部完成。</p>
      </div>
    </section>

    <footer class="landing-footer">
      <router-link to="/contact" class="footer-link">联系我们</router-link>
    </footer>

    <AuthModal v-model:visible="modalVisible" :initial-mode="modalMode" @success="handleAuthSuccess" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthModal from '../components/AuthModal.vue'

const route  = useRoute()
const router = useRouter()

const modalVisible = ref(false)
const modalMode    = ref('login')

function openModal(mode) {
  modalMode.value = mode
  modalVisible.value = true
}

function handleAuthSuccess() {
  router.replace('/')
}

onMounted(() => {
  if (route.path === '/login') {
    openModal('login')
  } else if (route.path === '/register') {
    openModal('register')
  }
})
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #eff6ff 100%);
  display: flex;
  flex-direction: column;
}
.landing-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 40px;
}
.landing-logo {
  font-size: 18px;
  font-weight: 700;
  color: #1e40af;
}
.landing-actions {
  display: flex;
  gap: 12px;
}
.hero {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 20px;
}
.hero-title {
  font-size: 36px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 16px;
}
.hero-subtitle {
  max-width: 640px;
  font-size: 15px;
  color: #475569;
  line-height: 1.8;
  margin-bottom: 28px;
}
.hero-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.hero-disclaimer {
  font-size: 12px;
  color: #94a3b8;
}
.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  max-width: 1080px;
  width: 100%;
  margin: 0 auto 60px;
  padding: 0 40px;
}
.feature-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,.05);
  padding: 24px 20px;
  text-align: left;
}
.feature-icon {
  font-size: 28px;
  margin-bottom: 12px;
}
.feature-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}
.feature-card p {
  font-size: 13px;
  color: #64748b;
  line-height: 1.7;
}
.landing-footer {
  text-align: center;
  padding: 20px;
}
.footer-link {
  font-size: 13px;
  color: #64748b;
  text-decoration: none;
}
.footer-link:hover {
  color: #2563eb;
  text-decoration: underline;
}

.primary-btn, .ghost-btn {
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s, color .15s;
  padding: 9px 18px;
  border: 1px solid transparent;
}
.primary-btn {
  background: #2563eb;
  color: #fff;
}
.primary-btn:hover {
  background: #1d4ed8;
}
.ghost-btn {
  background: #fff;
  color: #2563eb;
  border-color: #bfdbfe;
}
.ghost-btn:hover {
  background: #eff6ff;
}
.primary-btn.large, .ghost-btn.large {
  padding: 12px 28px;
  font-size: 15px;
}
</style>

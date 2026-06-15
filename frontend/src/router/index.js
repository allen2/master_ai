// frontend/src/router/index.js
import { createRouter, createWebHashHistory } from 'vue-router'
import RunView      from '../views/RunView.vue'
import ApiKeysView  from '../views/ApiKeysView.vue'
import FlowsView    from '../views/FlowsView.vue'
import SettingsView from '../views/SettingsView.vue'
import ContactView  from '../views/ContactView.vue'
import WalletView   from '../views/WalletView.vue'
import HistoryView  from '../views/HistoryView.vue'
import LandingView  from '../views/LandingView.vue'
import IndustryAnalysisView from '../views/IndustryAnalysisView.vue'
import ContrarianAnalysisView from '../views/ContrarianAnalysisView.vue'
import MessageBoardView from '../views/MessageBoardView.vue'

const PUBLIC_PATHS = ['/', '/login', '/register']

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/',          component: LandingView },
    { path: '/run',       component: RunView },
    { path: '/industry-analysis', component: IndustryAnalysisView },
    { path: '/contrarian-analysis', component: ContrarianAnalysisView },
    { path: '/message-board', component: MessageBoardView },
    { path: '/api-keys',  component: ApiKeysView },
    { path: '/flows',     component: FlowsView },
    { path: '/settings',  component: SettingsView },
    { path: '/contact',   component: ContactView },
    { path: '/wallet',    component: WalletView },
    { path: '/history',   component: HistoryView },
    { path: '/login',     component: LandingView },
    { path: '/register',  component: LandingView }
  ]
})

// 全局路由守卫：未登录时跳转到首页（登录弹窗将在首页弹出）
router.beforeEach((to) => {
  if (PUBLIC_PATHS.includes(to.path) || to.path === '/contact') {
    return true
  }
  const token = localStorage.getItem('mumu_token')
  if (!token) {
    return '/login'
  }
  if (to.path === '/') {
    return '/run'
  }
  return true
})

export default router

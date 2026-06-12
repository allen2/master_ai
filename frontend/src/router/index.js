// frontend/src/router/index.js
import { createRouter, createWebHashHistory } from 'vue-router'
import RunView      from '../views/RunView.vue'
import ApiKeysView  from '../views/ApiKeysView.vue'
import FlowsView    from '../views/FlowsView.vue'
import SettingsView from '../views/SettingsView.vue'
import ContactView  from '../views/ContactView.vue'
import WalletView   from '../views/WalletView.vue'
import HistoryView  from '../views/HistoryView.vue'
import LoginView    from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import IndustryAnalysisView from '../views/IndustryAnalysisView.vue'
import ContrarianAnalysisView from '../views/ContrarianAnalysisView.vue'

const PUBLIC_PATHS = ['/login', '/register']

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/',          redirect: '/run' },
    { path: '/run',       component: RunView },
    { path: '/industry-analysis', component: IndustryAnalysisView },
    { path: '/contrarian-analysis', component: ContrarianAnalysisView },
    { path: '/api-keys',  component: ApiKeysView },
    { path: '/flows',     component: FlowsView },
    { path: '/settings',  component: SettingsView },
    { path: '/contact',   component: ContactView },
    { path: '/wallet',    component: WalletView },
    { path: '/history',   component: HistoryView },
    { path: '/login',     component: LoginView },
    { path: '/register',  component: RegisterView }
  ]
})

// 全局路由守卫：未登录时强制跳转到登录页
router.beforeEach((to) => {
  if (PUBLIC_PATHS.includes(to.path)) {
    return true
  }
  const token = localStorage.getItem('mumu_token')
  if (!token) {
    return '/login'
  }
  return true
})

export default router

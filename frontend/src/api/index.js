import axios from 'axios'

const TOKEN_KEY = 'mumu_token'

const api = axios.create({ baseURL: '/' })

// 请求拦截器：自动附带 JWT
api.interceptors.request.use(config => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理 401/402
api.interceptors.response.use(
  res => res.data,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('mumu_user')
      window.location.hash = '#/login'
    }
    const msg = err.response?.data?.detail || err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export const authApi = {
  login:    (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  me:       ()     => api.get('/auth/me')
}

export const apiKeysApi = {
  list:   ()              => api.get('/api-keys'),
  save:   (data)          => api.post('/api-keys', data),
  remove: (provider)      => api.delete(`/api-keys/${encodeURIComponent(provider)}`)
}

export const flowsApi = {
  list:   ()          => api.get('/flows'),
  get:    (id)        => api.get(`/flows/${id}`),
  create: (data)      => api.post('/flows', data),
  update: (id, data)  => api.put(`/flows/${id}`, data),
  remove: (id)        => api.delete(`/flows/${id}`),
  getRun: (id)        => api.get(`/flow-runs/${id}`)
}

export const settingsApi = {
  models: () => api.get('/language-models'),
  health: () => api.get('/health')
}

export const walletApi = {
  balance:      ()                         => api.get('/wallet/balance'),
  transactions: (pageNum = 1, pageSize = 20) => api.get('/wallet/transactions', { params: { pageNum, pageSize } })
}

export const analysisRunsApi = {
  list:   (pageNum = 1, pageSize = 10) => api.get('/analysis-runs', { params: { pageNum, pageSize } }),
  detail: (id)                         => api.get(`/analysis-runs/${id}`)
}

export const messageBoardApi = {
  list:   (pageNum = 1, pageSize = 20) => api.get('/message-board', { params: { pageNum, pageSize } }),
  create: (content)  => api.post('/message-board', { content }),
  remove: (id)        => api.delete(`/message-board/${id}`),
  like:   (id)        => api.post(`/message-board/${id}/like`)
}

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true
  },
  server: {
    proxy: {
      '/auth':            'http://localhost:8888',
      '/wallet':          'http://localhost:8888',
      '/analysis-runs':   'http://localhost:8888',
      '/message-board':   'http://localhost:8888',
      '/admin':           'http://localhost:8888',
      '/hedge-fund':      'http://localhost:8888',
      '/industry-analysis': 'http://localhost:8888',
      '/contrarian-analysis': 'http://localhost:8888',
      '/api-keys':        'http://localhost:8888',
      '/flows':           'http://localhost:8888',
      '/flow-runs':       'http://localhost:8888',
      '/language-models': 'http://localhost:8888',
      '/health':          'http://localhost:8888'
    }
  }
})

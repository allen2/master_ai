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
      '/auth':            'http://localhost:8000',
      '/wallet':          'http://localhost:8000',
      '/analysis-runs':   'http://localhost:8000',
      '/admin':           'http://localhost:8000',
      '/hedge-fund':      'http://localhost:8000',
      '/industry-analysis': 'http://localhost:8000',
      '/contrarian-analysis': 'http://localhost:8000',
      '/api-keys':        'http://localhost:8000',
      '/flows':           'http://localhost:8000',
      '/flow-runs':       'http://localhost:8000',
      '/language-models': 'http://localhost:8000',
      '/health':          'http://localhost:8000'
    }
  }
})

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import securityHeaders from './security-headers.json'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  // `npm run preview` serves the production build with the same security headers
  // as the live site, so the Content-Security-Policy can be checked locally. The
  // dev server leaves them out: its hot reload needs inline scripts.
  preview: {
    port: 4173,
    headers: securityHeaders,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    // Only the unit tests in src. The Playwright tests in e2e/ have their own runner.
    include: ['src/**/*.test.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      exclude: ['node_modules/', 'src/test/', 'e2e/'],
    },
  },
})

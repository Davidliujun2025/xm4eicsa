import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // 将前端 /api 请求代理到后端服务，解决跨域
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 如果后端没有统一加 /api 前缀，可以开启 rewrite；但你的后端有 /api/v1，所以无需重写
        // rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});

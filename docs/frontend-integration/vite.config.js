/**
 * Vite 配置 — 开发代理到后端
 *
 * 合并到现有 vite.config.js 的 server.proxy 部分即可
 */
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
        // 不重写路径：前端 /api/v1/auth/login → 后端 /api/v1/auth/login
      }
    }
  }
}
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * Vite 构建配置
 *
 * 关键点说明：
 * 1. server.proxy —— 开发环境把 /api 开头的请求转发到后端 8080，
 *    这样前端代码里不用写 http://localhost:8080，也就不会触发跨域。
 * 2. resolve.alias —— 用 @ 代替 src 目录，导入路径更短。
 * 3. server.host —— 设为 true 才能被局域网/容器访问。
 * 4. server.allowedHosts —— 部署到云端域名后，防止 Vite 拦截外部 Host 请求。
 */
export default defineConfig({
  plugins: [vue()],

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },

  server: {
    host: '0.0.0.0',
    port: 5173,
    // 允许任意 Host（部署到反向代理域名后面时需要）
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
        // 不需要 rewrite：后端接口本身就带 /api 前缀
      }
    }
  },

  build: {
    outDir: 'dist',
    // 生产环境不生成 sourcemap，减小体积
    sourcemap: false,
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        // 拆包：把体积大的第三方库单独打包，利用浏览器缓存
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          elementPlus: ['element-plus', '@element-plus/icons-vue']
        }
      }
    }
  }
})

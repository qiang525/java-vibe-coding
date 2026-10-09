/**
 * 前端应用入口
 *
 * 这里做的事，按顺序：
 * 1. 创建 Vue 应用实例
 * 2. 挂载 Pinia（状态管理）
 * 3. 挂载 Vue Router（路由）
 * 4. 挂载 Element Plus（UI 组件库）
 * 5. 注册 Element Plus 图标组件
 * 6. 挂载到 index.html 里的 #app 节点
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import 'element-plus/dist/index.css'
import '@/styles/main.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// ---------- 状态管理 ----------
app.use(createPinia())

// ---------- 路由 ----------
app.use(router)

// ---------- UI 组件库（中文语言包） ----------
app.use(ElementPlus, { locale: zhCn })

// ---------- 图标组件：全局注册，模板里可直接用 <el-icon><Search /></el-icon> ----------
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')

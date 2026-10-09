/**
 * 路由配置
 *
 * 设计要点：
 * 1. 所有页面组件用「路由懒加载」——() => import(...)，
 *    这样首屏只加载首页代码，其他页面访问时才下载，加快首屏速度。
 * 2. meta 里记录页面标题和是否需要登录，供全局守卫使用。
 */
import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/api/request'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页', requiresAuth: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在', requiresAuth: false }
  }
]

const router = createRouter({
  // 用 hash 模式，部署到静态服务器不用额外配置 history fallback
  history: createWebHashHistory(),
  routes,
  // 切换页面时滚动回顶部
  scrollBehavior() {
    return { top: 0 }
  }
})

/**
 * 全局前置守卫：处理页面标题 + 登录校验
 */
router.beforeEach((to, from, next) => {
  // 设置浏览器标签标题
  document.title = to.meta.title
    ? `${to.meta.title} · CourseHub`
    : 'CourseHub · 课程资料协作平台'

  // 需要登录但没有 Token → 跳登录页，并记录来源以便登录后跳回
  if (to.meta.requiresAuth && !getToken()) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router

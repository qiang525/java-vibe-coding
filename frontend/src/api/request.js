/**
 * Axios 请求封装
 *
 * 统一处理四件事：
 * 1. 自动带上 baseURL（开发环境走 Vite 代理）
 * 2. 请求拦截：自动附加 JWT Token
 * 3. 响应拦截：自动解包统一响应体 { code, message, data }
 * 4. 错误统一提示（用 Element Plus 的 Message 组件）
 *
 * 使用方式（业务代码里只需关心 data）：
 *   const data = await request.get('/api/health')
 *   // data 已经是后端返回的 data 字段内容
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 开发环境走 Vite 代理（/api → localhost:8080），生产环境同源
const baseURL = import.meta.env.VITE_API_BASE_URL || '/'

const request = axios.create({
  baseURL,
  // 默认 30 秒；上传大文件时会在请求里单独放宽
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// ============ Token 存取（统一入口，避免到处写 localStorage 字符串） ============
const TOKEN_KEY = 'coursehub_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

// ============ 请求拦截器 ============
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // 文件上传走 multipart/form-data。这里必须把默认的 application/json 删掉，
    // 否则浏览器不会自动补上 multipart/form-data; boundary=xxx，后端解析不出文件。
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

    return config
  },
  (error) => Promise.reject(error)
)

// ============ 响应拦截器 ============
request.interceptors.response.use(
  (response) => {
    const res = response.data

    // 后端返回的不是标准结构（例如直接返回文件流），原样返回
    if (res == null || typeof res.code === 'undefined') {
      return res
    }

    // code === 0 表示业务成功，直接把 data 交给调用方
    if (res.code === 0) {
      return res.data
    }

    // 401xx 认证问题：清除 Token 并跳登录
    if (res.code >= 40101 && res.code <= 40199) {
      removeToken()
      ElMessage.error(res.message || '登录已过期，请重新登录')
      // 避免在登录页重复跳转
      if (!location.hash.includes('/login')) {
        location.hash = '#/login'
      }
      return Promise.reject(new Error(res.message))
    }

    // 其他业务错误：提示后抛出，让调用方决定是否处理
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  (error) => {
    // ---------- HTTP 层面错误 ----------
    let message = '网络异常，请稍后重试'

    if (error.response) {
      const status = error.response.status
      const map = {
        400: '请求参数有误',
        401: '未登录或登录已过期',
        403: '没有权限执行该操作',
        404: '请求的资源不存在',
        405: '请求方法不被允许',
        500: '服务器内部错误',
        502: '网关错误',
        503: '服务暂时不可用'
      }
      message = map[status] || `请求失败 (${status})`
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请检查网络'
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request

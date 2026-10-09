/**
 * 认证与用户接口
 *
 * 后端对应：com.nnuist.coursehub.controller.AuthController
 *
 * 说明：request.js 的响应拦截器已经把统一响应体 { code, message, data }
 * 解包成 data 了，所以这里的每个函数返回的都是「data 字段的内容」。
 */
import request from './request'

/**
 * 用户登录
 * POST /api/auth/login
 * @param {{username:string, password:string}} data
 * @returns {Promise<{token:string, tokenType:string, expiresIn:number, user:object}>}
 */
export function login(data) {
  return request.post('/api/auth/login', data)
}

/**
 * 用户注册
 * POST /api/auth/register
 * @returns {Promise<number>} 新用户 ID
 */
export function register(data) {
  return request.post('/api/auth/register', data)
}

/**
 * 获取当前登录用户信息（需携带 Token）
 * GET /api/auth/me
 */
export function getCurrentUser() {
  return request.get('/api/auth/me')
}

/**
 * 退出登录
 * POST /api/auth/logout
 */
export function logout() {
  return request.post('/api/auth/logout')
}

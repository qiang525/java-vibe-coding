/**
 * 健康检查接口 —— 用于验证前后端链路是否打通
 *
 * 后端对应：com.nnuist.coursehub.controller.HealthController
 */
import request from './request'

/**
 * 服务健康状态
 * GET /api/health
 */
export function getHealth() {
  return request.get('/api/health')
}

/**
 * 联调测试
 * GET /api/health/ping
 */
export function ping() {
  return request.get('/api/health/ping')
}

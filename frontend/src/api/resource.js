/**
 * 资料接口
 *
 * 后端对应：com.nnuist.coursehub.controller.ResourceController
 */
import request from './request'

/**
 * 资料列表（分页 + 筛选，已按当前用户可见性过滤）
 * GET /api/resources
 */
export function getResources(params) {
  return request.get('/api/resources', { params })
}

/**
 * 全文检索（走 MySQL ngram 全文索引，按相关度排序）
 * GET /api/resources/search
 */
export function searchResources(params) {
  return request.get('/api/resources/search', { params })
}

/**
 * 搜索热词（来自 Redis ZSet）
 * GET /api/resources/search/hot
 */
export function getHotKeywords(limit = 10) {
  return request.get('/api/resources/search/hot', { params: { limit } })
}

/**
 * 资料详情
 * GET /api/resources/{id}
 */
export function getResourceDetail(id) {
  return request.get(`/api/resources/${id}`)
}

/**
 * 发布资料（把已上传的文件挂到课程下）
 * POST /api/resources
 */
export function publishResource(data) {
  return request.post('/api/resources', data)
}

/**
 * 修改资料
 * PUT /api/resources/{id}
 */
export function updateResource(id, data) {
  return request.put(`/api/resources/${id}`, data)
}

/**
 * 删除资料
 * DELETE /api/resources/{id}
 */
export function deleteResource(id) {
  return request.delete(`/api/resources/${id}`)
}

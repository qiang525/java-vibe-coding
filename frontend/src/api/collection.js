/**
 * 收藏接口
 *
 * 后端对应：com.nnuist.coursehub.controller.CollectionController
 *
 * 收藏的本质是「用户 ↔ 资料」的多对多关系，
 * 后端靠数据库唯一索引兜底防重复收藏。
 */
import request from './request'

/**
 * 收藏资料
 * POST /api/resources/{resourceId}/collect?folder=默认收藏夹
 *
 * @param {number|string} resourceId
 * @param {string} [folder] 收藏夹名称（可选）
 */
export function collectResource(resourceId, folder) {
  return request.post(`/api/resources/${resourceId}/collect`, null, {
    params: folder ? { folder } : undefined
  })
}

/**
 * 取消收藏
 * DELETE /api/resources/{resourceId}/collect
 */
export function uncollectResource(resourceId) {
  return request.delete(`/api/resources/${resourceId}/collect`)
}

/**
 * 查询当前用户是否已收藏该资料
 * GET /api/resources/{resourceId}/collected
 *
 * @returns {Promise<boolean>}
 */
export function isCollected(resourceId) {
  return request.get(`/api/resources/${resourceId}/collected`)
}

/**
 * 我的收藏列表
 * GET /api/collections
 */
export function getMyCollections(params) {
  return request.get('/api/collections', { params })
}

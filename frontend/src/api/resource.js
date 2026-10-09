/**
 * 资料接口
 *
 * 后端对应：com.nnuist.coursehub.controller.ResourceController
 *
 * 资料（resource）是平台的核心对象，它下面挂着：
 *   · 版本树（resource_version）—— 每次更新内容生成新版本，历史永久保留
 *   · 评论、收藏、举报（分别见 comment.js / collection.js / report.js）
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

// =============================================================================
//  版本树
// -----------------------------------------------------------------------------
//  设计要点：**回滚不是"改回旧版本"，而是生成一个新版本**（标记 isRollback=1，
//  并记录回滚来源）。这样历史链路永远完整，能看出"什么时候改的、什么时候回滚的"。
// =============================================================================

/**
 * 查询资料的版本列表（按版本号倒序）
 * GET /api/resources/{id}/versions
 *
 * @returns {Promise<Array>} VersionVO 数组
 */
export function getVersions(resourceId) {
  return request.get(`/api/resources/${resourceId}/versions`)
}

/**
 * 新增版本（更新资料内容）
 * POST /api/resources/{id}/versions
 *
 * 流程：先调上传接口（或分片上传）拿到 fileId → 再调本接口生成新版本
 *
 * @param {number|string} resourceId
 * @param {{fileId:number, changeLog?:string, parentVersionId?:number}} data
 * @returns {Promise<number>} 新版本 ID
 */
export function addVersion(resourceId, data) {
  return request.post(`/api/resources/${resourceId}/versions`, data)
}

/**
 * 回滚到指定版本（会生成新版本，历史不丢）
 * POST /api/resources/{id}/rollback/{versionId}
 */
export function rollbackVersion(resourceId, versionId) {
  return request.post(`/api/resources/${resourceId}/rollback/${versionId}`)
}

/**
 * 下载指定版本对应的文件
 *
 * 说明：资料的下载接口只会给出「当前版本」的文件，
 * 想下载历史版本，要用版本记录里的 fileId 走文件下载接口。
 */
export function downloadVersionFile(fileId, timeout = 300000) {
  return request.get(`/api/files/${fileId}/download`, {
    responseType: 'blob',
    timeout
  })
}

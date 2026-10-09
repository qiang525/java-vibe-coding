/**
 * 评论接口
 *
 * 后端对应：com.nnuist.coursehub.controller.CommentController
 *
 * 评论是**两级结构**：
 *   一级评论（直接挂在资料下）
 *     └── 回复（挂在某条一级评论下）
 * 列表接口会把回复嵌在一级评论的 replies 字段里一起返回，
 * 所以前端一次请求就能渲染完整的两层。
 */
import request from './request'

/**
 * 查询资料的评论（含回复）
 * GET /api/resources/{resourceId}/comments
 *
 * @returns {Promise<{list:Array, total:number, page:number, pageSize:number}>}
 */
export function getComments(resourceId, params) {
  return request.get(`/api/resources/${resourceId}/comments`, { params })
}

/**
 * 发表评论或回复
 * POST /api/resources/{resourceId}/comments
 *
 * @param {number|string} resourceId
 * @param {{content:string, parentId?:number, type?:string}} data
 *        —— parentId 为空 = 发一级评论；填了 = 回复那条评论
 * @returns {Promise<number>} 新评论 ID
 */
export function createComment(resourceId, data) {
  return request.post(`/api/resources/${resourceId}/comments`, data)
}

/**
 * 删除评论（本人或管理员）
 * DELETE /api/comments/{id}
 */
export function deleteComment(id) {
  return request.delete(`/api/comments/${id}`)
}

/**
 * 举报接口
 *
 * 后端对应：com.nnuist.coursehub.controller.ReportController
 *
 * 后端用「多态关联」设计：targetType + targetId 两个字段
 * 就能指向资料 / 评论 / 用户中的任意一种，不用建三张举报表。
 *
 * 权限：提交举报需登录；列表与处理仅管理员（后端 @RequireRole("ADMIN")）。
 */
import request from './request'

/**
 * 提交举报
 * POST /api/reports
 *
 * @param {{targetType:string, targetId:number, reason:string, description?:string}} data
 * @returns {Promise<number>} 举报记录 ID
 */
export function submitReport(data) {
  return request.post('/api/reports', data)
}

/**
 * 举报列表（仅管理员）
 * GET /api/reports?status=PENDING&page=1&pageSize=10
 *
 * @param {{status?:string, page?:number, pageSize?:number}} params
 *        status 可选：PENDING 待处理 / HANDLED 已处理 / REJECTED 已驳回；不传 = 全部
 */
export function getReports(params) {
  return request.get('/api/reports', { params })
}

/**
 * 处理举报（仅管理员）
 * PUT /api/reports/{id}/handle
 *
 * @param {number|string} id
 * @param {{status:string, handleNote?:string}} data
 *        status 只能是 HANDLED（认定属实）或 REJECTED（驳回）
 */
export function handleReport(id, data) {
  return request.put(`/api/reports/${id}/handle`, data)
}

// =============================================================================
//  字典常量（前后端约定保持一致）
// =============================================================================

/** 举报对象类型 */
export const REPORT_TARGET_LABELS = {
  RESOURCE: '资料',
  COMMENT: '评论',
  USER: '用户'
}

/** 举报原因 */
export const REPORT_REASONS = [
  { value: 'COPYRIGHT', label: '侵权盗版' },
  { value: 'ERROR', label: '内容错误' },
  { value: 'OFFICIAL', label: '冒充官方资料' },
  { value: 'AD', label: '广告推广' },
  { value: 'SPAM', label: '垃圾内容' },
  { value: 'OTHER', label: '其他' }
]

export const REPORT_REASON_LABELS = REPORT_REASONS.reduce((acc, r) => {
  acc[r.value] = r.label
  return acc
}, {})

/** 举报状态 */
export const REPORT_STATUS_LABELS = {
  PENDING: '待处理',
  HANDLED: '已处理',
  REJECTED: '已驳回'
}

/** 状态下对应的标签颜色（Element Plus tag type） */
export const REPORT_STATUS_TYPES = {
  PENDING: 'warning',
  HANDLED: 'success',
  REJECTED: 'info'
}

/**
 * 学院接口
 *
 * 后端对应：com.nnuist.coursehub.controller.CollegeController
 */
import request from './request'

/**
 * 查询全部学院（供下拉框与筛选使用）
 * GET /api/colleges
 * @returns {Promise<Array<{id:number, name:string, code:string}>>}
 */
export function getColleges() {
  return request.get('/api/colleges')
}

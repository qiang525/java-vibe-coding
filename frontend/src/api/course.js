/**
 * 课程接口
 *
 * 后端对应：com.nnuist.coursehub.controller.CourseController
 */
import request from './request'

/**
 * 课程列表（分页 + 多条件筛选）
 * GET /api/courses
 * @param {{page?:number, pageSize?:number, keyword?:string, collegeId?:number, term?:string, status?:string}} params
 * @returns {Promise<{list:Array, total:number, page:number, pageSize:number, pages:number}>}
 */
export function getCourses(params) {
  return request.get('/api/courses', { params })
}

/**
 * 我加入的课程
 * GET /api/courses/mine
 */
export function getMyCourses(params) {
  return request.get('/api/courses/mine', { params })
}

/**
 * 课程详情
 * GET /api/courses/{id}
 */
export function getCourseDetail(id) {
  return request.get(`/api/courses/${id}`)
}

/**
 * 创建课程（需登录）
 * POST /api/courses
 */
export function createCourse(data) {
  return request.post('/api/courses', data)
}

/**
 * 修改课程（仅负责人或管理员）
 * PUT /api/courses/{id}
 */
export function updateCourse(id, data) {
  return request.put(`/api/courses/${id}`, data)
}

/**
 * 删除课程（仅负责人或管理员）
 * DELETE /api/courses/{id}
 */
export function deleteCourse(id) {
  return request.delete(`/api/courses/${id}`)
}

/**
 * 加入课程（需登录）
 * POST /api/courses/{id}/join
 */
export function joinCourse(id) {
  return request.post(`/api/courses/${id}/join`)
}

/**
 * 退出课程（需登录）
 * DELETE /api/courses/{id}/join
 */
export function leaveCourse(id) {
  return request.delete(`/api/courses/${id}/join`)
}

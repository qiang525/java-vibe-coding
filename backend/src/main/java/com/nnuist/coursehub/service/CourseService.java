package com.nnuist.coursehub.service;

import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.dto.CourseQueryDTO;
import com.nnuist.coursehub.dto.CourseSaveDTO;
import com.nnuist.coursehub.vo.CourseVO;

/**
 * 课程业务接口
 *
 * @author CourseHub Team
 */
public interface CourseService {

    /**
     * 分页查询课程列表
     */
    PageResult<CourseVO> pageCourses(CourseQueryDTO query);

    /**
     * 查询课程详情
     */
    CourseVO getCourseDetail(Long id);

    /**
     * 创建课程
     *
     * @return 新课程 ID
     */
    Long createCourse(CourseSaveDTO dto);

    /**
     * 修改课程（仅负责人或管理员可操作）
     */
    void updateCourse(Long id, CourseSaveDTO dto);

    /**
     * 删除课程（逻辑删除，仅负责人或管理员可操作）
     */
    void deleteCourse(Long id);

    /**
     * 加入课程
     */
    void joinCourse(Long id);

    /**
     * 退出课程
     */
    void leaveCourse(Long id);
}

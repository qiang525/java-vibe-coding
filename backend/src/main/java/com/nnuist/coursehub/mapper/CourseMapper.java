package com.nnuist.coursehub.mapper;

import com.nnuist.coursehub.dto.CourseQueryDTO;
import com.nnuist.coursehub.entity.Course;
import com.nnuist.coursehub.vo.CourseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程数据访问接口
 *
 * <p>这里同时出现了返回 {@link Course}（实体）和返回 {@link CourseVO}（视图对象）
 * 的方法，区别是：</p>
 * <ul>
 *   <li>返回 {@code Course}：给 Service 内部做业务判断用（要拿到 ownerId 等原始字段）</li>
 *   <li>返回 {@code CourseVO}：给前端展示用（SQL 里 JOIN 出学院名，前端直接用）</li>
 * </ul>
 *
 * @author CourseHub Team
 */
@Mapper
public interface CourseMapper {

    /**
     * 分页查询课程列表（含学院名称）
     */
    List<CourseVO> selectPage(@Param("q") CourseQueryDTO query);

    /**
     * 统计符合条件的课程总数（分页必需）
     */
    long countPage(@Param("q") CourseQueryDTO query);

    /**
     * 按主键查询课程详情（含学院名称）
     */
    CourseVO selectVOById(@Param("id") Long id);

    /**
     * 按主键查询课程实体（Service 内部判断数据权限用）
     */
    Course selectById(@Param("id") Long id);

    /**
     * 批量按主键查询课程
     *
     * <p><b>为什么需要批量查询</b>：资料列表要给每条资料标注"当前用户能不能改"，
     * 这就需要知道每条资料所属课程的负责人。如果循环里逐条调 {@code selectById}，
     * 一页 10 条就是 10 次查询 —— 这就是典型的 <b>N+1 查询问题</b>。
     * 用一次 {@code WHERE id IN (...)} 查回来，在内存里做映射即可。</p>
     */
    List<Course> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 新增课程
     */
    int insert(Course course);

    /**
     * 更新课程
     */
    int update(Course course);

    /**
     * 逻辑删除课程（只改标记位，数据仍在，可恢复）
     */
    int softDelete(@Param("id") Long id);

    // ==================== 课程成员 ====================

    /**
     * 判断用户是否已是课程成员
     */
    int countMember(@Param("courseId") Long courseId,
                    @Param("userId") Long userId);

    /**
     * 加入课程
     */
    int insertMember(@Param("courseId") Long courseId,
                     @Param("userId") Long userId,
                     @Param("role") String role);

    /**
     * 退出课程
     */
    int deleteMember(@Param("courseId") Long courseId,
                     @Param("userId") Long userId);

    /**
     * 增减成员数
     *
     * <p>用 {@code SET member_count = member_count + #{delta}} 而不是先查后写，
     * 是为了避免并发下的"读-改-写"覆盖问题（两个人同时加入只加了一次）。</p>
     *
     * <p>末尾的 {@code member_count + #{delta} >= 0} 防止计数被减成负数。</p>
     */
    int increaseMemberCount(@Param("courseId") Long courseId,
                            @Param("delta") int delta);

    /**
     * 增减资料数
     *
     * <p>课程表上的 {@code resource_count} 是冗余字段，用于列表页直接展示，
     * 避免对每门课都 COUNT 一次资料表（那就是 N+1 查询）。
     * 发布/删除资料时调用本方法维护它。</p>
     */
    int increaseResourceCount(@Param("courseId") Long courseId,
                              @Param("delta") int delta);
}

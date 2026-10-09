package com.nnuist.coursehub.service.impl;

import com.nnuist.coursehub.common.exception.BizException;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.ResultCode;
import com.nnuist.coursehub.common.util.UserContext;
import com.nnuist.coursehub.dto.CourseQueryDTO;
import com.nnuist.coursehub.dto.CourseSaveDTO;
import com.nnuist.coursehub.entity.Course;
import com.nnuist.coursehub.mapper.CourseMapper;
import com.nnuist.coursehub.service.CourseService;
import com.nnuist.coursehub.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 课程业务实现
 *
 * <p>这一层最关键、最值得讲的是<b>数据权限校验</b>（{@link #checkOwnerPermission}）。</p>
 *
 * <p><b>什么是数据权限</b>：拦截器管的是"你能不能进这个接口"（功能权限），
 * 但拦不住"你能改别人的课程"。比如改课程接口谁都能调，
 * 校验必须在 Service 里按"这条数据的归属"判断——这就是数据权限。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;

    /** 默认发布状态 */
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    /** 课程内角色：负责人 */
    private static final String ROLE_OWNER = "OWNER";
    /** 课程内角色：学生 */
    private static final String ROLE_STUDENT = "STUDENT";

    // =========================================================================
    //  查询
    // =========================================================================

    @Override
    public PageResult<CourseVO> pageCourses(CourseQueryDTO query) {
        // 先查总数：为 0 就不用查列表了，省一次数据库往返
        long total = courseMapper.countPage(query);
        if (total == 0) {
            return PageResult.empty(query.getPage(), query.getPageSize());
        }
        List<CourseVO> list = courseMapper.selectPage(query);
        return new PageResult<>(list, total, query.getPage(), query.getPageSize());
    }

    @Override
    public CourseVO getCourseDetail(Long id) {
        CourseVO vo = courseMapper.selectVOById(id);
        if (vo == null) {
            throw new BizException(ResultCode.COURSE_NOT_FOUND);
        }
        return vo;
    }

    // =========================================================================
    //  写操作
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourse(CourseSaveDTO dto) {
        Long uid = UserContext.getUserId();

        Course course = new Course();
        // 只复制 DTO 里允许前端填的字段
        BeanUtils.copyProperties(dto, course);
        // 负责人只能取"当前登录用户"，绝不能让前端传
        course.setOwnerId(uid);
        // 创建者自己就是一个成员
        course.setMemberCount(1);
        course.setResourceCount(0);
        course.setStatus(isBlank(dto.getStatus()) ? STATUS_PUBLISHED : dto.getStatus());
        course.setCourseCode(isBlank(dto.getCourseCode()) ? "" : dto.getCourseCode());
        course.setTeacher(isBlank(dto.getTeacher()) ? "" : dto.getTeacher());
        course.setTerm(isBlank(dto.getTerm()) ? "" : dto.getTerm());
        course.setCover(isBlank(dto.getCover()) ? "" : dto.getCover());
        course.setCredit(dto.getCredit() == null ? BigDecimal.ZERO : dto.getCredit());

        courseMapper.insert(course);
        // 创建者自动成为课程负责人（两个写操作在同一事务里，要么都成，要么都不成）
        courseMapper.insertMember(course.getId(), uid, ROLE_OWNER);

        log.info("创建课程 | courseId={} | ownerId={} | name={}",
                course.getId(), uid, course.getName());
        return course.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourse(Long id, CourseSaveDTO dto) {
        Course exist = getExistingOrThrow(id);
        // ---- 数据权限：不是负责人也不是管理员，直接拒绝 ----
        checkOwnerPermission(exist);

        Course update = new Course();
        BeanUtils.copyProperties(dto, update);
        update.setId(id);
        // XML 里用 <if> 判断 null，所以没传的字段不会被覆盖
        courseMapper.update(update);

        log.info("更新课程 | courseId={} | operatorId={}", id, UserContext.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long id) {
        Course exist = getExistingOrThrow(id);
        checkOwnerPermission(exist);
        // 逻辑删除：数据仍保留，管理员可恢复
        courseMapper.softDelete(id);
        log.info("删除课程 | courseId={} | operatorId={}", id, UserContext.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinCourse(Long id) {
        Long uid = UserContext.getUserId();
        getExistingOrThrow(id);

        if (courseMapper.countMember(id, uid) > 0) {
            throw new BizException(ResultCode.ALREADY_JOINED);
        }
        courseMapper.insertMember(id, uid, ROLE_STUDENT);
        // 成员数 +1（SQL 里用 member_count = member_count + 1，避免并发覆盖）
        courseMapper.increaseMemberCount(id, 1);

        log.info("加入课程 | courseId={} | userId={}", id, uid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveCourse(Long id) {
        Long uid = UserContext.getUserId();
        Course exist = getExistingOrThrow(id);

        if (courseMapper.countMember(id, uid) == 0) {
            throw new BizException(ResultCode.NOT_JOINED);
        }
        // 负责人退出会导致课程无人管理，禁止
        if (exist.getOwnerId().equals(uid)) {
            throw new BizException(ResultCode.OPERATION_NOT_ALLOWED, "你是课程负责人，不能退出自己的课程");
        }
        courseMapper.deleteMember(id, uid);
        courseMapper.increaseMemberCount(id, -1);

        log.info("退出课程 | courseId={} | userId={}", id, uid);
    }

    // =========================================================================
    //  私有方法
    // =========================================================================

    /**
     * 查课程，不存在就抛 404
     */
    private Course getExistingOrThrow(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(ResultCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    /**
     * 数据权限校验：只有课程负责人或管理员能改这门课
     *
     * <p><b>为什么必须在这一层做</b>：注解式权限（{@code @RequireLogin}）
     * 只能判断"登录了没、是什么角色"，判断不了"这门课是不是你的"。
     * 如果没有这段校验，任何登录用户改一下 URL 里的课程 ID，
     * 就能改掉别人的课程——这就是典型的<b>水平越权漏洞</b>。</p>
     */
    private void checkOwnerPermission(Course course) {
        Long uid = UserContext.getUserId();
        if (!course.getOwnerId().equals(uid) && !UserContext.isAdmin()) {
            log.warn("越权操作课程被拒绝 | courseId={} | ownerId={} | operatorId={}",
                    course.getId(), course.getOwnerId(), uid);
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

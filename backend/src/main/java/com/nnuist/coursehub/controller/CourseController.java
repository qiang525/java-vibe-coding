package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.annotation.RequireLogin;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.common.util.UserContext;
import com.nnuist.coursehub.dto.CourseQueryDTO;
import com.nnuist.coursehub.dto.CourseSaveDTO;
import com.nnuist.coursehub.service.CourseService;
import com.nnuist.coursehub.vo.CourseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 课程接口
 *
 * <p><b>权限设计说明</b>：</p>
 * <ul>
 *   <li>浏览列表/详情 —— 不要求登录（未登录也能看看有哪些课）</li>
 *   <li>创建/修改/删除/加入 —— 要求登录（{@code @RequireLogin}）</li>
 *   <li>「只能改自己的课」这一层 —— 在 Service 里按数据归属判断</li>
 * </ul>
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "03-课程")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * 课程列表（分页 + 多条件筛选）
     *
     * <pre>
     * GET /api/courses?page=1&pageSize=10&keyword=数据结构&collegeId=1&term=2026-2027-1
     * </pre>
     *
     * <p>查询条件用 {@code @ModelAttribute} 方式绑定（不加注解，
     * Spring 会把 query string 自动塞进 DTO 的同名字段）。</p>
     */
    @ApiOperation("课程列表（分页+筛选）")
    @GetMapping
    public Result<PageResult<CourseVO>> list(CourseQueryDTO query) {
        return Result.ok(courseService.pageCourses(query));
    }

    @ApiOperation("课程详情")
    @GetMapping("/{id}")
    public Result<CourseVO> detail(@PathVariable Long id) {
        return Result.ok(courseService.getCourseDetail(id));
    }

    @ApiOperation("我加入的课程")
    @RequireLogin
    @GetMapping("/mine")
    public Result<PageResult<CourseVO>> mine(CourseQueryDTO query) {
        // 强制用当前登录用户 ID，防止前端传别人的 ID 偷看
        query.setMemberUserId(UserContext.getUserId());
        return Result.ok(courseService.pageCourses(query));
    }

    @ApiOperation("创建课程")
    @RequireLogin
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CourseSaveDTO dto) {
        return Result.ok("课程创建成功", courseService.createCourse(dto));
    }

    @ApiOperation("修改课程（仅负责人或管理员）")
    @RequireLogin
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CourseSaveDTO dto) {
        courseService.updateCourse(id, dto);
        return Result.ok("课程已更新", null);
    }

    @ApiOperation("删除课程（仅负责人或管理员）")
    @RequireLogin
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.ok("课程已删除", null);
    }

    @ApiOperation("加入课程")
    @RequireLogin
    @PostMapping("/{id}/join")
    public Result<Void> join(@PathVariable Long id) {
        courseService.joinCourse(id);
        return Result.ok("已加入课程", null);
    }

    @ApiOperation("退出课程")
    @RequireLogin
    @DeleteMapping("/{id}/join")
    public Result<Void> leave(@PathVariable Long id) {
        courseService.leaveCourse(id);
        return Result.ok("已退出课程", null);
    }
}

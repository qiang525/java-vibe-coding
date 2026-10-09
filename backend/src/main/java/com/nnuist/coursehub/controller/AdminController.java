package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.annotation.LogOperation;
import com.nnuist.coursehub.common.annotation.RequireLogin;
import com.nnuist.coursehub.common.annotation.RequireRole;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.entity.AuditLog;
import com.nnuist.coursehub.mapper.AuditLogMapper;
import com.nnuist.coursehub.scheduler.ResourceScoreTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台接口（仅管理员）
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "10-管理后台")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminController {

    private final ResourceScoreTask scoreTask;
    private final AuditLogMapper auditLogMapper;

    /**
     * 手动触发可信度评分重算
     *
     * <p>定时任务每天凌晨跑一次，这个接口用于"我想立刻看到效果"的场景
     * （比如刚上传完一批资料）。</p>
     */
    @ApiOperation("手动重算所有资料的可信度评分")
    @RequireLogin
    @LogOperation(value = "重算可信度评分", targetType = "SYSTEM")
    @PostMapping("/recalc-score")
    public Result<Integer> recalcScore() {
        int count = scoreTask.recalculateAll();
        log.info("手动触发评分重算完成 | 处理 {} 条", count);
        return Result.ok("重算完成", count);
    }

    /**
     * 查看操作审计日志
     *
     * <pre>
     * GET /api/admin/audit-logs?page=1&pageSize=20
     * GET /api/admin/audit-logs?userId=1   （只看某个人的操作）
     * </pre>
     */
    @ApiOperation("查询操作审计日志")
    @RequireLogin
    @GetMapping("/audit-logs")
    public Result<PageResult<AuditLog>> auditLogs(@RequestParam(required = false) Long userId,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "20") Integer pageSize) {
        int p = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);

        long total = auditLogMapper.countPage(userId);
        List<AuditLog> list = total == 0
                ? java.util.Collections.emptyList()
                : auditLogMapper.selectPage(userId, (p - 1) * size, size);

        return Result.ok(new PageResult<>(list, total, p, size));
    }
}

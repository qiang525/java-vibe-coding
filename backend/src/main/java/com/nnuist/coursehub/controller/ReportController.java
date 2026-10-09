package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.annotation.RequireLogin;
import com.nnuist.coursehub.common.annotation.RequireRole;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.dto.ReportCreateDTO;
import com.nnuist.coursehub.dto.ReportHandleDTO;
import com.nnuist.coursehub.service.ReportService;
import com.nnuist.coursehub.vo.ReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 举报接口
 *
 * <p>普通用户提交举报；管理员查看列表并处理。
 * 管理接口用 {@code @RequireRole("ADMIN")} 限制角色。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "09-举报")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 提交举报
     *
     * <pre>
     * {
     *   "targetType": "RESOURCE",
     *   "targetId": 5,
     *   "reason": "COPYRIGHT",
     *   "description": "这份资料看起来是盗版扫描件"
     * }
     * </pre>
     */
    @ApiOperation("提交举报")
    @RequireLogin
    @PostMapping
    public Result<Long> submit(@Valid @RequestBody ReportCreateDTO dto) {
        return Result.ok("举报已提交，我们会尽快处理", reportService.submit(dto));
    }

    @ApiOperation("举报列表（仅管理员）")
    @RequireLogin
    @RequireRole("ADMIN")
    @GetMapping
    public Result<PageResult<ReportVO>> list(@RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(reportService.list(status, page, pageSize));
    }

    @ApiOperation("处理举报（仅管理员）")
    @RequireLogin
    @RequireRole("ADMIN")
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id,
                               @Valid @RequestBody ReportHandleDTO dto) {
        reportService.handle(id, dto);
        return Result.ok("处理完成", null);
    }
}

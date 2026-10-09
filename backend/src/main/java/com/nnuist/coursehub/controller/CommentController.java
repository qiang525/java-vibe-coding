package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.annotation.RequireLogin;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.dto.CommentCreateDTO;
import com.nnuist.coursehub.service.CommentService;
import com.nnuist.coursehub.vo.CommentVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 评论接口
 *
 * <p>两级结构：一级评论直接挂在资料下，回复挂在评论下。
 * 列表接口一次返回一级评论 + 它下面的所有回复。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "07-评论")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @ApiOperation("查询资料的评论（含回复）")
    @GetMapping("/resources/{resourceId}/comments")
    public Result<PageResult<CommentVO>> list(@PathVariable Long resourceId,
                                              @RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(commentService.listByResource(resourceId, page, pageSize));
    }

    /**
     * 发表评论或回复
     *
     * <pre>
     * 发表一级评论：{"content": "这份讲义很好"}
     * 回复某条评论：{"content": "同意", "parentId": 3}
     * </pre>
     */
    @ApiOperation("发表评论 / 回复")
    @RequireLogin
    @PostMapping("/resources/{resourceId}/comments")
    public Result<Long> create(@PathVariable Long resourceId,
                               @Valid @RequestBody CommentCreateDTO dto) {
        return Result.ok("评论成功", commentService.create(resourceId, dto));
    }

    @ApiOperation("删除评论（本人或管理员）")
    @RequireLogin
    @DeleteMapping("/comments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok("已删除", null);
    }
}

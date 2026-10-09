package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.annotation.RequireLogin;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.service.CollectionService;
import com.nnuist.coursehub.vo.CollectionVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 收藏接口
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "08-收藏")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @ApiOperation("收藏资料")
    @RequireLogin
    @PostMapping("/resources/{resourceId}/collect")
    public Result<Void> collect(@PathVariable Long resourceId,
                                @RequestParam(required = false) String folder) {
        collectionService.collect(resourceId, folder);
        return Result.ok("收藏成功", null);
    }

    @ApiOperation("取消收藏")
    @RequireLogin
    @DeleteMapping("/resources/{resourceId}/collect")
    public Result<Void> uncollect(@PathVariable Long resourceId) {
        collectionService.uncollect(resourceId);
        return Result.ok("已取消收藏", null);
    }

    @ApiOperation("我是否已收藏该资料")
    @RequireLogin
    @GetMapping("/resources/{resourceId}/collected")
    public Result<Boolean> collected(@PathVariable Long resourceId) {
        return Result.ok(collectionService.isCollected(resourceId));
    }

    @ApiOperation("我的收藏列表")
    @RequireLogin
    @GetMapping("/collections")
    public Result<PageResult<CollectionVO>> mine(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(collectionService.myCollections(page, pageSize));
    }
}

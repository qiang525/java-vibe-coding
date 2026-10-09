package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.annotation.RequireLogin;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.dto.ResourceQueryDTO;
import com.nnuist.coursehub.dto.ResourceSaveDTO;
import com.nnuist.coursehub.dto.VersionCreateDTO;
import com.nnuist.coursehub.service.ResourceService;
import com.nnuist.coursehub.vo.FileDownloadResult;
import com.nnuist.coursehub.vo.HotKeywordVO;
import com.nnuist.coursehub.vo.VersionVO;
import com.nnuist.coursehub.vo.ResourceVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;

/**
 * 资料接口
 *
 * <p><b>权限说明</b>：</p>
 * <ul>
 *   <li>列表 / 详情 —— 不标 {@code @RequireLogin}，但 <b>结果会按可见性过滤</b>：
 *       游客只能看到 {@code PUBLIC} 的资料</li>
 *   <li>发布 / 修改 / 删除 —— 要求登录</li>
 *   <li>「能不能看这条」「能不能改这条」—— 在 Service 层按数据归属判断</li>
 * </ul>
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "05-资料")
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    /**
     * 资料列表
     *
     * <pre>
     * GET /api/resources?page=1&pageSize=10&courseId=1&keyword=线性表
     * </pre>
     *
     * <p>返回结果已经按当前用户的权限过滤过 —— 你看不到不该看的资料，
     * 而且它们连"存在"都不会暴露。</p>
     */
    @ApiOperation("资料列表（已按可见性过滤）")
    @GetMapping
    public Result<PageResult<ResourceVO>> list(ResourceQueryDTO query) {
        return Result.ok(resourceService.pageResources(query));
    }

    /**
     * 全文检索
     *
     * <pre>
     * GET /api/resources/search?keyword=线性表&page=1&pageSize=10
     * </pre>
     *
     * <p>走 MySQL 的 ngram 全文索引，结果按<b>相关度</b>排序，
     * 并且同样遵守三级可见性（搜不到你没有权限看的资料）。</p>
     */
    @ApiOperation("全文检索资料（按相关度排序）")
    @GetMapping("/search")
    public Result<PageResult<ResourceVO>> search(ResourceQueryDTO query) {
        return Result.ok(resourceService.search(query));
    }

    /**
     * 搜索热词
     *
     * <pre>
     * GET /api/resources/search/hot?limit=10
     * </pre>
     */
    @ApiOperation("搜索热词（Redis ZSet）")
    @GetMapping("/search/hot")
    public Result<java.util.List<HotKeywordVO>> hotKeywords(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(resourceService.hotKeywords(limit));
    }

    @ApiOperation("资料详情")
    @GetMapping("/{id}")
    public Result<ResourceVO> detail(@PathVariable Long id) {
        return Result.ok(resourceService.getDetail(id));
    }

    /**
     * 发布资料
     *
     * <pre>
     * 两步走：
     *   1. POST /api/files/upload   → 拿到 fileId
     *   2. POST /api/resources      → 传 fileId + courseId + title，正式发布
     * </pre>
     */
    @ApiOperation("发布资料")
    @RequireLogin
    @PostMapping
    public Result<Long> publish(@Valid @RequestBody ResourceSaveDTO dto) {
        return Result.ok("发布成功", resourceService.publish(dto));
    }

    @ApiOperation("修改资料（仅上传者/课程负责人/管理员）")
    @RequireLogin
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ResourceSaveDTO dto) {
        resourceService.update(id, dto);
        return Result.ok("修改成功", null);
    }

    @ApiOperation("删除资料（仅上传者/课程负责人/管理员）")
    @RequireLogin
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return Result.ok("删除成功", null);
    }

    // ==================== 版本树 ====================

    @ApiOperation("查询资料的版本列表")
    @GetMapping("/{id}/versions")
    public Result<java.util.List<VersionVO>> listVersions(@PathVariable Long id) {
        return Result.ok(resourceService.listVersions(id));
    }

    /**
     * 新增版本
     *
     * <pre>
     * 流程：先调上传接口拿 fileId → 再调本接口生成新版本
     * </pre>
     */
    @ApiOperation("新增版本（更新资料内容）")
    @RequireLogin
    @PostMapping("/{id}/versions")
    public Result<Long> addVersion(@PathVariable Long id,
                                   @Valid @RequestBody VersionCreateDTO dto) {
        return Result.ok("新版本已生成", resourceService.addVersion(id, dto));
    }

    /**
     * 回滚到指定版本
     *
     * <p>回滚会<b>生成一个新版本</b>（内容取自目标版本），而不是删掉中间版本。
     * 这样历史链路完整可追溯。</p>
     */
    @ApiOperation("回滚到指定版本（会生成新版本，历史不丢）")
    @RequireLogin
    @PostMapping("/{id}/rollback/{versionId}")
    public Result<Void> rollback(@PathVariable Long id, @PathVariable Long versionId) {
        resourceService.rollback(id, versionId);
        return Result.ok("已回滚（生成了新版本）", null);
    }

    /**
     * 下载资料对应的文件
     *
     * <p>同样要求登录。M2 阶段先这样处理；将来要做"未登录也能下公开资料"，
     * 可以改成签名 URL（带过期时间的临时链接），避免把下载接口完全放开。</p>
     */
    @ApiOperation("下载资料文件")
    @RequireLogin
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        FileDownloadResult result = resourceService.download(id);

        MediaType mediaType;
        try {
            mediaType = (result.getMimeType() == null || result.getMimeType().isEmpty())
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(result.getMimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(result.getFileName(), StandardCharsets.UTF_8)
                .build());
        if (result.getFileSize() != null) {
            headers.setContentLength(result.getFileSize());
        }

        log.info("资料下载 | resourceId={} | userId={}", id, com.nnuist.coursehub.common.util.UserContext.getUserId());
        return new ResponseEntity<>(
                new InputStreamResource(result.getInputStream()), headers, HttpStatus.OK);
    }
}

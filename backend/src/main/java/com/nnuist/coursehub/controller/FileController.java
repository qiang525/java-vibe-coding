package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.annotation.RequireLogin;
import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.service.FileService;
import com.nnuist.coursehub.vo.FileDownloadResult;
import com.nnuist.coursehub.vo.FileVO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 文件接口
 *
 * <p><b>上传与下载的不同之处</b>：</p>
 * <ul>
 *   <li>上传：请求体是 {@code multipart/form-data}，Spring 用
 *       {@link MultipartFile} 接收，它会自动把文件暂存下来给你用</li>
 *   <li>下载：返回的不是 JSON，而是<b>文件流</b>。所以必须用
 *       {@code ResponseEntity<InputStreamResource>} 而不是 {@code Result}</li>
 * </ul>
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "04-文件")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件
     *
     * <pre>
     * POST /api/files/upload
     * Content-Type: multipart/form-data
     * 表单字段名：file
     * </pre>
     *
     * <p>如果响应里 {@code instantUpload=true}，说明命中了秒传 ——
     * 服务端已有完全相同的文件，只做了引用，没有真的上传。</p>
     */
    @ApiOperation("上传文件（支持秒传）")
    @RequireLogin
    @PostMapping("/upload")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file) {
        FileVO vo = fileService.upload(file);
        String message = Boolean.TRUE.equals(vo.getInstantUpload()) ? "秒传成功" : "上传成功";
        return Result.ok(message, vo);
    }

    @ApiOperation("查询文件元信息")
    @RequireLogin
    @GetMapping("/{id}")
    public Result<FileVO> info(@PathVariable Long id) {
        return Result.ok(fileService.getFileVO(id));
    }

    /**
     * 下载文件
     *
     * <p><b>返回类型为什么不是 Result</b>：这个接口的响应体就是文件本身。
     * 如果再包一层 JSON，浏览器下载下来的东西就是一段 JSON 文本，而不是文件。</p>
     *
     * <p><b>中文文件名怎么处理</b>：HTTP 头里不能直接放中文。
     * 必须按 RFC 5987 编码成 {@code filename*=UTF-8''%E8%AF%BE%E4%BB%B6.pdf}
     * 的形式，浏览器才能正确还原。Spring 的
     * {@code ContentDisposition.filename(name, UTF_8)} 帮我们做了这件事。</p>
     */
    @ApiOperation("下载文件")
    @RequireLogin
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        FileDownloadResult result = fileService.download(id);

        // 没识别出 MIME 时，统一用「二进制流」类型，让浏览器直接下载而不是尝试打开
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

        log.info("文件下载 | fileId={} | name={}", id, result.getFileName());
        return new ResponseEntity<>(
                new InputStreamResource(result.getInputStream()), headers, HttpStatus.OK);
    }

    @ApiOperation("删除文件（引用计数归零才真正删除）")
    @RequireLogin
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.ok("删除成功", null);
    }
}

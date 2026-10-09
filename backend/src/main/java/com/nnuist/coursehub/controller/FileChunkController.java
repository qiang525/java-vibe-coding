package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.annotation.RequireLogin;
import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.dto.ChunkMergeDTO;
import com.nnuist.coursehub.service.ChunkUploadService;
import com.nnuist.coursehub.vo.ChunkCheckVO;
import com.nnuist.coursehub.vo.ChunkUploadVO;
import com.nnuist.coursehub.vo.FileVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/**
 * 分片上传接口
 *
 * <p><b>完整调用流程</b>（前端按这个顺序调）：</p>
 * <pre>
 *   ① GET  /api/files/chunk/check     看是否秒传 / 拿已传分片
 *   ② POST /api/files/chunk/upload    循环上传每个分片（可以并发）
 *   ③ POST /api/files/chunk/merge     全传完后合并
 * </pre>
 *
 * <p><b>前端怎么算 fileHash</b>：分片上传要求前端先算出整个文件的 MD5。
 * 大文件直接读进内存算会卡死浏览器，通常用两种办法：</p>
 * <ul>
 *   <li>用 Web Worker 在后台线程流式计算（不阻塞界面）</li>
 *   <li>读取文件的头 2MB + 尾 2MB + 文件大小，拼起来算一个"抽样哈希"
 *       （快，但理论上存在不同文件碰巧相同的极小概率）</li>
 * </ul>
 * <p>本项目后端不关心前端怎么算，只要同一个文件每次算出来一致即可。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "06-分片上传")
@RestController
@RequestMapping("/api/files/chunk")
@RequiredArgsConstructor
public class FileChunkController {

    private final ChunkUploadService chunkUploadService;

    /**
     * 上传前检查
     *
     * <pre>
     * GET /api/files/chunk/check?fileHash=xxx&fileName=课件.pptx&fileSize=50280605
     * </pre>
     *
     * <p>返回的 {@code exists=true} 表示秒传命中，前端可以跳过整个上传过程。</p>
     */
    @ApiOperation("上传前检查（秒传 + 断点续传）")
    @RequireLogin
    @GetMapping("/check")
    public Result<ChunkCheckVO> check(@RequestParam String fileHash,
                                      @RequestParam(required = false) String fileName,
                                      @RequestParam(required = false) Long fileSize) {
        return Result.ok(chunkUploadService.check(fileHash, fileName, fileSize));
    }

    /**
     * 上传单个分片
     *
     * <pre>
     * POST /api/files/chunk/upload
     * Content-Type: multipart/form-data
     *   chunk       = 分片二进制
     *   fileHash    = 全文件 MD5（作为本次上传会话的标识）
     *   chunkIndex  = 0
     *   totalChunks = 10
     *   fileName    = 课件.pptx
     * </pre>
     */
    @ApiOperation("上传单个分片")
    @RequireLogin
    @PostMapping("/upload")
    public Result<ChunkUploadVO> uploadChunk(@RequestParam("chunk") MultipartFile chunk,
                                            @RequestParam String fileHash,
                                            @RequestParam Integer chunkIndex,
                                            @RequestParam Integer totalChunks,
                                            @RequestParam(required = false) String fileName) {
        ChunkUploadVO vo = chunkUploadService.uploadChunk(
                chunk, fileHash, chunkIndex, totalChunks, fileName);
        return Result.ok(vo);
    }

    /**
     * 合并分片
     *
     * <p>合并后会做全文件 MD5 复验，与请求里的 {@code fileHash} 比对；
     * 不一致直接拒绝（说明传输过程出了问题）。</p>
     */
    @ApiOperation("合并分片（含全文件哈希复验）")
    @RequireLogin
    @PostMapping("/merge")
    public Result<FileVO> merge(@Valid @RequestBody ChunkMergeDTO dto) {
        FileVO vo = chunkUploadService.merge(dto);
        return Result.ok("合并成功", vo);
    }
}

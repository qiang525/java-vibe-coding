package com.nnuist.coursehub.service.impl;

import com.nnuist.coursehub.common.exception.BizException;
import com.nnuist.coursehub.common.result.ResultCode;
import com.nnuist.coursehub.common.util.FileUtil;
import com.nnuist.coursehub.common.util.UserContext;
import com.nnuist.coursehub.dto.ChunkMergeDTO;
import com.nnuist.coursehub.entity.FileInfo;
import com.nnuist.coursehub.mapper.FileMapper;
import com.nnuist.coursehub.service.ChunkUploadService;
import com.nnuist.coursehub.service.FileService;
import com.nnuist.coursehub.storage.FileStorage;
import com.nnuist.coursehub.vo.ChunkCheckVO;
import com.nnuist.coursehub.vo.ChunkUploadVO;
import com.nnuist.coursehub.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 分片上传业务实现
 *
 * <p><b>Redis 在这里扮演什么角色</b>：记录"这个文件已经传了哪几片"。
 * 用 Redis 而不是数据库，因为：</p>
 * <ul>
 *   <li>上传进度是<b>临时状态</b>，文件传完就没用了，不值得进数据库</li>
 *   <li>写入极其频繁（每个分片一次），数据库扛不住</li>
 *   <li>可以设过期时间，用户放弃上传后自动清理</li>
 * </ul>
 *
 * @author CourseHub Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkUploadServiceImpl implements ChunkUploadService {

    private final FileMapper fileMapper;
    private final FileStorage fileStorage;
    private final FileService fileService;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 分片大小，默认 5MB */
    @Value("${coursehub.file.chunk-size:5242880}")
    private long chunkSize;

    /** 单个文件总大小上限，默认 500MB */
    @Value("${coursehub.file.max-size:524288000}")
    private long maxSize;

    /** 临时目录（分片先存这里，合并后清理） */
    @Value("${coursehub.file.temp-path:./upload/temp}")
    private String tempPath;

    /** 上传会话在 Redis 中的保留时长：24 小时 */
    private static final long SESSION_TTL_HOURS = 24;

    // =========================================================================
    //  ① 上传前检查
    // =========================================================================

    @Override
    public ChunkCheckVO check(String fileHash, String fileName, Long fileSize) {
        validateHash(fileHash);

        ChunkCheckVO vo = new ChunkCheckVO();
        vo.setChunkSize(chunkSize);

        // ---------- 秒传：数据库里已经有相同 MD5 的文件 ----------
        FileInfo exist = fileMapper.selectByHash(fileHash);
        if (exist != null) {
            fileMapper.increaseRefCount(exist.getId());
            vo.setExists(true);
            vo.setFileId(exist.getId());
            vo.setFileName(exist.getFileName());
            vo.setUploadedChunks(Collections.emptyList());
            vo.setUploadedCount(0);
            log.info("分片上传检查：秒传命中 | hash={} | fileId={}", fileHash, exist.getId());
            return vo;
        }

        // ---------- 断点续传：从 Redis 查已上传的分片 ----------
        vo.setExists(false);
        vo.setUploadedChunks(getUploadedChunks(fileHash));
        vo.setUploadedCount(vo.getUploadedChunks().size());

        log.info("分片上传检查 | hash={} | 已传 {}/{}",
                fileHash, vo.getUploadedCount(),
                fileSize == null ? "?" : (fileSize + chunkSize - 1) / chunkSize);
        return vo;
    }

    // =========================================================================
    //  ② 上传单个分片
    // =========================================================================

    @Override
    public ChunkUploadVO uploadChunk(MultipartFile chunk, String fileHash,
                                     Integer chunkIndex, Integer totalChunks, String fileName) {
        validateHash(fileHash);

        if (chunk == null || chunk.isEmpty()) {
            throw new BizException(ResultCode.FILE_EMPTY, "分片内容为空");
        }
        if (chunkIndex == null || chunkIndex < 0
                || totalChunks == null || totalChunks < 1
                || chunkIndex >= totalChunks) {
            throw new BizException(ResultCode.PARAM_ERROR, "分片索引不合法");
        }

        try {
            // ---------- 落盘：upload/temp/chunks/{hash}/{index} ----------
            Path dir = getChunkDir(fileHash);
            Files.createDirectories(dir);
            Path target = dir.resolve(String.valueOf(chunkIndex));
            chunk.transferTo(target.toFile());

            // ---------- 记进度：Redis 的 Set 结构，天然去重 ----------
            // 重复上传同一分片不会重复计数，这正是用 Set 而不是 List 的原因
            String key = chunkKey(fileHash);
            redisTemplate.opsForSet().add(key, String.valueOf(chunkIndex));
            // 每次上传都刷新过期时间，避免用户慢慢传时中途失效
            redisTemplate.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);

            Long count = redisTemplate.opsForSet().size(key);
            int uploaded = count == null ? 1 : count.intValue();

            log.debug("分片上传 | hash={} | index={}/{}", fileHash, chunkIndex, totalChunks - 1);
            return new ChunkUploadVO(chunkIndex, uploaded, totalChunks);

        } catch (IOException e) {
            log.error("分片写入失败 | hash={} | index={}", fileHash, chunkIndex, e);
            throw new BizException(ResultCode.FILE_UPLOAD_FAILED, "分片写入失败：" + e.getMessage());
        }
    }

    // =========================================================================
    //  ③ 合并分片
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileVO merge(ChunkMergeDTO dto) {
        String fileHash = dto.getFileHash();
        validateHash(fileHash);

        // ---------- ① 先做基本校验 ----------
        if (dto.getFileSize() > maxSize) {
            throw new BizException(ResultCode.FILE_TOO_LARGE,
                    "单个文件不能超过 " + (maxSize / 1024 / 1024) + " MB");
        }
        String originalName = sanitizeFileName(dto.getFileName());
        String ext = FileUtil.getExtension(originalName);
        if (!FileUtil.isAllowedExtension(ext)) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_ALLOWED,
                    "不支持的文件类型：" + (ext.isEmpty() ? "（无扩展名）" : ext));
        }

        // ---------- ② 校验分片是否齐全 ----------
        // 不从 Redis 查，而是直接看磁盘 —— 磁盘才是"事实来源"。
        // 因为 Redis 可能过期被清空，但分片文件还在。
        Path dir = getChunkDir(fileHash);
        if (!Files.exists(dir)) {
            throw new BizException(ResultCode.CHUNK_MERGE_FAILED, "分片目录不存在，请重新上传");
        }

        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < dto.getTotalChunks(); i++) {
            if (!Files.exists(dir.resolve(String.valueOf(i)))) {
                missing.add(i);
            }
        }
        if (!missing.isEmpty()) {
            throw new BizException(ResultCode.CHUNK_MERGE_FAILED,
                    "分片不完整，缺少：" + missing.subList(0, Math.min(10, missing.size())));
        }

        Path merged = null;
        try {
            // ---------- ③ 按序合并 ----------
            Path tempDir = Paths.get(tempPath).toAbsolutePath().normalize();
            Files.createDirectories(tempDir);
            merged = Files.createTempFile(tempDir, "merged-", "." + ext);

            try (OutputStream out = Files.newOutputStream(merged)) {
                for (int i = 0; i < dto.getTotalChunks(); i++) {
                    Path part = dir.resolve(String.valueOf(i));
                    Files.copy(part, out);   // 顺序必须严格按索引，否则文件内容就乱了
                }
            }

            // ---------- ④ 关键：全文件哈希复验 ----------
            // 前端传的 MD5 是基于"原始文件"算的；这里是基于"合并后的文件"算的。
            // 两者不一致说明传输/合并过程出了问题（分片丢失、乱序、截断等），必须拒绝。
            String actualHash = FileUtil.md5(merged);
            if (!actualHash.equalsIgnoreCase(fileHash)) {
                log.error("合并后哈希校验失败 | 期望={} | 实际={}", fileHash, actualHash);
                throw new BizException(ResultCode.FILE_HASH_MISMATCH,
                        "文件校验失败，合并结果与原始文件不一致，请重新上传");
            }

            // ---------- ⑤ 文件头校验（防止伪装文件） ----------
            if (FileUtil.needsMagicCheck(ext) && !FileUtil.matchesMagicNumber(merged, ext)) {
                log.warn("文件内容与扩展名不符 | name={} | ext={}", originalName, ext);
                throw new BizException(ResultCode.FILE_TYPE_NOT_ALLOWED,
                        "文件内容与扩展名不符，已拒绝");
            }

            // ---------- ⑥ 移到正式存储位置 ----------
            String relativePath = FileUtil.buildRelativePath(ext);
            try (InputStream in = Files.newInputStream(merged)) {
                fileStorage.store(relativePath, in);
            }

            // ---------- ⑦ 入库 ----------
            FileInfo info = new FileInfo();
            info.setFileHash(actualHash);
            info.setFileName(originalName);
            info.setFileExt(ext);
            info.setStoragePath(relativePath);
            info.setFileSize(dto.getFileSize());
            info.setMimeType("");
            info.setStorageType(fileStorage.getStorageType());
            info.setRefCount(1);
            info.setUploaderId(UserContext.getUserId());

            try {
                fileMapper.insert(info);
            } catch (DuplicateKeyException e) {
                // 并发场景：两个请求同时合并同一个文件，唯一索引会拦下第二个。
                // 这时不算失败 —— 说明文件已经有了，复用已有的那条记录即可。
                FileInfo existing = fileMapper.selectByHash(actualHash);
                if (existing == null) {
                    throw e;
                }
                fileMapper.increaseRefCount(existing.getId());
                log.info("合并时发现文件已存在，复用记录 | fileId={}", existing.getId());
                info = existing;
            }

            log.info("分片合并成功 | fileId={} | name={} | size={} | 分片数={}",
                    info.getId(), originalName, dto.getFileSize(), dto.getTotalChunks());

            return fileService.getFileVO(info.getId());

        } catch (IOException e) {
            log.error("分片合并失败 | hash={}", fileHash, e);
            throw new BizException(ResultCode.CHUNK_MERGE_FAILED, "合并失败：" + e.getMessage());
        } finally {
            // ---------- ⑧ 无论成败都清理临时数据 ----------
            deleteQuietly(merged);
            deleteDirQuietly(dir);
            redisTemplate.delete(chunkKey(fileHash));
        }
    }

    // =========================================================================
    //  私有方法
    // =========================================================================

    /**
     * 校验哈希格式
     *
     * <p><b>这不只是"格式检查"，更是安全防线</b>：文件哈希会被拼进临时目录路径，
     * 如果不限制字符集，攻击者传一个 {@code ../../..} 就能把分片写到任意目录。
     * 限定为 32 位十六进制后，路径穿越的可能性就被彻底堵死了。</p>
     */
    private void validateHash(String fileHash) {
        if (fileHash == null || !fileHash.matches("^[a-fA-F0-9]{32}$")) {
            throw new BizException(ResultCode.PARAM_ERROR, "文件哈希格式不正确");
        }
    }

    /** Redis 里的上传会话 key */
    private String chunkKey(String fileHash) {
        return com.nnuist.coursehub.common.constant.Constants.REDIS_UPLOAD_CHUNK
                + fileHash.toLowerCase();
    }

    /** 分片临时目录：upload/temp/chunks/{hash} */
    private Path getChunkDir(String fileHash) {
        return Paths.get(tempPath)
                .toAbsolutePath()
                .normalize()
                .resolve("chunks")
                .resolve(fileHash.toLowerCase());
    }

    /** 读 Redis 里已上传的分片索引（升序） */
    @SuppressWarnings("unchecked")
    private List<Integer> getUploadedChunks(String fileHash) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(chunkKey(fileHash));
            if (members == null || members.isEmpty()) {
                return Collections.emptyList();
            }
            return members.stream()
                    .map(o -> Integer.valueOf(o.toString()))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Redis 不可用时不影响主流程，最多是断点续传失效（重传一遍而已）
            log.warn("读取分片进度失败，将按全新上传处理 | hash={} | error={}", fileHash, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 清洗文件名
     *
     * <p>和普通上传一样：只取最后一段，丢掉目录部分，限制长度。</p>
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "unnamed";
        }
        String normalized = name.replace("\\", "/");
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        if (normalized.length() > 200) {
            normalized = normalized.substring(normalized.length() - 200);
        }
        return normalized;
    }

    /** 删除文件，忽略异常 */
    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("临时文件清理失败 | path={}", path, e);
        }
    }

    /** 递归删除分片目录 */
    private void deleteDirQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 清理失败不影响主流程
                }
            });
        } catch (IOException e) {
            log.warn("分片目录清理失败 | dir={}", dir, e);
        }
    }
}

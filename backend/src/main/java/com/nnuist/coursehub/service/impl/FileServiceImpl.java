package com.nnuist.coursehub.service.impl;

import com.nnuist.coursehub.common.exception.BizException;
import com.nnuist.coursehub.common.result.ResultCode;
import com.nnuist.coursehub.common.util.FileUtil;
import com.nnuist.coursehub.common.util.UserContext;
import com.nnuist.coursehub.entity.FileInfo;
import com.nnuist.coursehub.mapper.FileMapper;
import com.nnuist.coursehub.service.FileService;
import com.nnuist.coursehub.storage.FileStorage;
import com.nnuist.coursehub.vo.FileDownloadResult;
import com.nnuist.coursehub.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * 文件业务实现
 *
 * <p>本类的 {@link #upload} 方法是<b>整个 M2 最重要的方法</b>，
 * 它把"文件上传"这件看似简单的事拆成了 9 个步骤，每一步都在防一类问题。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    private final FileStorage fileStorage;

    /** 单文件大小上限（字节），默认 500MB */
    @Value("${coursehub.file.max-size:524288000}")
    private long maxSize;

    /**
     * 临时目录
     *
     * <p><b>为什么不用系统的 java.io.tmpdir</b>：Windows 上它指向
     * {@code C:\Users\xxx\AppData\Local\Temp}。500MB 的文件落到 C 盘，
     * 既占系统盘、又可能触发权限问题。所以指到项目目录下的 upload/temp。</p>
     */
    @Value("${coursehub.file.temp-path:./upload/temp}")
    private String tempPath;

    // =========================================================================
    //  上传
    // =========================================================================

    /**
     * 上传文件（9 步）
     *
     * <p><b>关于 @Transactional 的一个权衡</b>：文件 IO 其实是"耗时且不应该在事务里"
     * 的操作（事务开太久会占用数据库连接）。严格做法是把文件落盘放在事务之外，
     * 只把最后一步"写数据库"包进事务。这里为了代码连贯性先整体加事务，
     * 知道这个权衡点即可。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileVO upload(MultipartFile file) {

        // ---------- ① 空文件检查 ----------
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.FILE_EMPTY);
        }

        // ---------- ② 大小检查（防超大文件占满磁盘） ----------
        if (file.getSize() > maxSize) {
            throw new BizException(ResultCode.FILE_TOO_LARGE,
                    "单个文件不能超过 " + (maxSize / 1024 / 1024) + " MB");
        }

        // ---------- ③ 扩展名白名单（第一道类型防线） ----------
        String originalName = sanitizeFileName(file.getOriginalFilename());
        String ext = FileUtil.getExtension(originalName);
        if (!FileUtil.isAllowedExtension(ext)) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_ALLOWED,
                    "不支持的文件类型：" + (ext.isEmpty() ? "（无扩展名）" : ext));
        }

        Path tempFile = null;
        try {
            // ---------- ④ 先落到临时文件 ----------
            // 为什么要先落盘：后面要算 MD5、要校验文件头，都需要"能重复读取的内容"。
            // MultipartFile 的流只能读一次，直接读会没法复用。
            Path tempDir = Paths.get(tempPath).toAbsolutePath().normalize();
            Files.createDirectories(tempDir);
            tempFile = Files.createTempFile(tempDir, "upload-", "." + ext);
            file.transferTo(tempFile.toFile());

            // ---------- ⑤ 计算 MD5（文件指纹） ----------
            String hash = FileUtil.md5(tempFile);

            // ---------- ⑥ 秒传：数据库里已有同 MD5 的文件 ----------
            FileInfo exist = fileMapper.selectByHash(hash);
            if (exist != null) {
                // 不重新存储，只把引用计数 +1
                fileMapper.increaseRefCount(exist.getId());
                log.info("秒传命中 | hash={} | fileId={} | name={}", hash, exist.getId(), originalName);
                return toVO(exist, true);
            }

            // ---------- ⑦ 文件头（魔术数字）校验：第二道类型防线 ----------
            // 扩展名可以随便改，文件头改不了 —— 这一步专门抓"伪装文件"
            if (FileUtil.needsMagicCheck(ext) && !FileUtil.matchesMagicNumber(tempFile, ext)) {
                log.warn("文件内容与扩展名不符，疑似伪装文件 | name={} | ext={}", originalName, ext);
                throw new BizException(ResultCode.FILE_TYPE_NOT_ALLOWED,
                        "文件内容与扩展名不符，已拒绝上传");
            }

            // ---------- ⑧ 存到最终位置（相对路径 + UUID 文件名） ----------
            String relativePath = FileUtil.buildRelativePath(ext);
            try (InputStream in = Files.newInputStream(tempFile)) {
                fileStorage.store(relativePath, in);
            }

            // ---------- ⑨ 写入数据库 ----------
            FileInfo info = new FileInfo();
            info.setFileHash(hash);
            info.setFileName(originalName);          // 存原始名（给用户看）
            info.setFileExt(ext);
            info.setStoragePath(relativePath);       // 存相对路径（给系统用）
            info.setFileSize(file.getSize());
            info.setMimeType(file.getContentType() == null ? "" : file.getContentType());
            info.setStorageType(fileStorage.getStorageType());
            info.setRefCount(1);
            info.setUploaderId(UserContext.getUserId());
            fileMapper.insert(info);

            log.info("文件上传成功 | fileId={} | name={} | size={} | path={}",
                    info.getId(), originalName, file.getSize(), relativePath);
            return toVO(info, false);

        } catch (IOException e) {
            log.error("文件上传失败 | name={}", originalName, e);
            throw new BizException(ResultCode.FILE_UPLOAD_FAILED, "文件上传失败：" + e.getMessage());
        } finally {
            // ---------- 无论成败，都要清掉临时文件 ----------
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("临时文件清理失败 | path={}", tempFile, e);
                }
            }
        }
    }

    // =========================================================================
    //  查询 / 下载 / 删除
    // =========================================================================

    @Override
    public FileVO getFileVO(Long id) {
        FileInfo info = getFileInfo(id);
        return toVO(info, false);
    }

    @Override
    public FileInfo getFileInfo(Long id) {
        FileInfo info = fileMapper.selectById(id);
        if (info == null) {
            throw new BizException(ResultCode.FILE_NOT_FOUND);
        }
        return info;
    }

    @Override
    public FileDownloadResult download(Long id) {
        FileInfo info = getFileInfo(id);
        try {
            FileDownloadResult result = new FileDownloadResult();
            result.setFileName(info.getFileName());
            result.setMimeType(info.getMimeType());
            result.setFileSize(info.getFileSize());
            result.setInputStream(fileStorage.load(info.getStoragePath()));
            return result;
        } catch (IOException e) {
            log.error("读取文件失败 | fileId={} | path={}", id, info.getStoragePath(), e);
            throw new BizException(ResultCode.FILE_NOT_FOUND, "文件读取失败，可能已被清理");
        }
    }

    /**
     * 删除文件
     *
     * <p><b>为什么不能直接删</b>：这份物理文件可能正被好几份资料引用
     * （比如同一个课件被多个课程收录）。直接删会导致别人的资料打不开。</p>
     *
     * <p>正确做法是"引用计数减 1"，减到 0 才说明真的没人用了，这时才删物理文件。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FileInfo info = getFileInfo(id);
        fileMapper.decreaseRefCount(id);

        // 重新查一次，看看减完之后是不是归零了
        FileInfo after = fileMapper.selectById(id);
        if (after != null && after.getRefCount() != null && after.getRefCount() <= 0) {
            // 已经没人引用了：删物理文件 + 删数据库记录，才算真正清理干净
            fileStorage.delete(info.getStoragePath());
            fileMapper.deleteById(id);
            log.info("引用归零，文件已彻底清理 | fileId={} | path={}", id, info.getStoragePath());
        } else {
            log.info("引用计数递减，文件保留 | fileId={} | 剩余引用={}",
                    id, after == null ? "?" : after.getRefCount());
        }
    }

    // =========================================================================
    //  私有方法
    // =========================================================================

    /**
     * 清洗原始文件名
     *
     * <p>浏览器的文件名可能带着完整路径（老 IE 就干这事），
     * 比如 {@code C:\Users\x\Desktop\课件.pdf}。
     * 这里只取最后一段，把目录部分全部丢掉。</p>
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "unnamed";
        }
        // 统一分隔符后取最后一段
        String normalized = name.replace("\\", "/");
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        // 截断过长的文件名（数据库字段是 varchar(255)）
        if (normalized.length() > 200) {
            normalized = normalized.substring(normalized.length() - 200);
        }
        return normalized;
    }

    /**
     * Entity → VO
     */
    private FileVO toVO(FileInfo info, boolean instantUpload) {
        FileVO vo = new FileVO();
        vo.setId(info.getId());
        vo.setFileName(info.getFileName());
        vo.setFileExt(info.getFileExt());
        vo.setFileSize(info.getFileSize());
        vo.setMimeType(info.getMimeType());
        vo.setStorageType(info.getStorageType());
        vo.setUploaderId(info.getUploaderId());
        vo.setCreatedAt(info.getCreatedAt());
        vo.setDownloadUrl("/api/files/" + info.getId() + "/download");
        vo.setInstantUpload(instantUpload);
        vo.setReadableSize(formatSize(info.getFileSize()));
        return vo;
    }

    /** 把字节数格式化成人看得懂的样子 */
    private String formatSize(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB"};
        double size = bytes;
        int unit = 0;
        while (size >= 1024 && unit < units.length - 1) {
            size /= 1024;
            unit++;
        }
        return unit == 0
                ? bytes + " B"
                : String.format(Locale.ROOT, "%.2f %s", size, units[unit]);
    }
}

package com.nnuist.coursehub.service;

import com.nnuist.coursehub.entity.FileInfo;
import com.nnuist.coursehub.vo.FileDownloadResult;
import com.nnuist.coursehub.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件业务接口
 *
 * @author CourseHub Team
 */
public interface FileService {

    /**
     * 上传文件
     *
     * <p>内部会做五重安全校验，并尝试秒传。</p>
     *
     * @return 文件信息（{@code instantUpload=true} 表示秒传命中，没有真正上传）
     */
    FileVO upload(MultipartFile file);

    /**
     * 查询文件元信息
     */
    FileVO getFileVO(Long id);

    /**
     * 查询文件实体（内部用）
     */
    FileInfo getFileInfo(Long id);

    /**
     * 下载文件，返回"流 + 元信息"
     */
    FileDownloadResult download(Long id);

    /**
     * 删除文件
     *
     * <p>引用计数减 1；只有归零时才真正删除物理文件。</p>
     */
    void delete(Long id);
}

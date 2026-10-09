package com.nnuist.coursehub.service;

import com.nnuist.coursehub.dto.ChunkMergeDTO;
import com.nnuist.coursehub.vo.ChunkCheckVO;
import com.nnuist.coursehub.vo.ChunkUploadVO;
import com.nnuist.coursehub.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传业务接口
 *
 * <p><b>为什么需要分片上传</b>：普通上传有两个硬伤：</p>
 * <ol>
 *   <li><b>断了要从头来</b> —— 500MB 传到 99% 断网，前功尽弃</li>
 *   <li><b>大文件占内存</b> —— 服务器要一次性接收整个文件</li>
 * </ol>
 * <p>分片上传把大文件切成 5MB 的小块分别传，任何一块失败只重传那一块。</p>
 *
 * <p><b>三个接口的配合顺序</b>：</p>
 * <pre>
 *   ① check()       → 判断是否秒传 / 拿已传分片列表
 *   ② uploadChunk() → 循环上传每个分片（可并发）
 *   ③ merge()       → 全部分片传完后合并，并做全文件哈希复验
 * </pre>
 *
 * @author CourseHub Team
 */
public interface ChunkUploadService {

    /**
     * 上传前检查
     *
     * @param fileHash 全文件 MD5
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @return 秒传结果 + 已上传分片列表
     */
    ChunkCheckVO check(String fileHash, String fileName, Long fileSize);

    /**
     * 上传单个分片
     *
     * @param chunk       分片内容
     * @param fileHash    全文件 MD5（作为这次上传会话的标识）
     * @param chunkIndex  分片序号，从 0 开始
     * @param totalChunks 分片总数
     * @param fileName    原始文件名
     */
    ChunkUploadVO uploadChunk(MultipartFile chunk, String fileHash,
                              Integer chunkIndex, Integer totalChunks, String fileName);

    /**
     * 合并所有分片
     *
     * <p>合并后会<b>重新计算全文件 MD5</b>与前端传的值比对——
     * 这是防止"分片传坏了但没发现"的最后一道防线。</p>
     *
     * @return 合并后的文件信息
     */
    FileVO merge(ChunkMergeDTO dto);
}

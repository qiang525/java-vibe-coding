package com.nnuist.coursehub.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件实体 —— 对应数据库表 file
 *
 * <p><b>为什么类名不叫 File</b>：Java 标准库里已经有 {@code java.io.File} 了。
 * 如果同名，在同一个类里既想用实体又想用原生 File 就得写全限定名，非常别扭。
 * 所以加个 Info 后缀区分。表名仍是 {@code file}，注释里写清楚对应关系即可。</p>
 *
 * <p><b>这张表的定位</b>：它管的是「物理文件」，和业务无关。
 * 一份"数据结构真题.pdf"可能被多份资料（resource）引用，
 * 但磁盘上只存一份。靠 {@code refCount} 引用计数决定何时能清理。</p>
 *
 * @author CourseHub Team
 */
@Data
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /**
     * 文件 MD5（32 位小写十六进制）
     *
     * <p>唯一索引 {@code uk_file_hash}。<b>秒传功能的依据</b>：
     * 上传前先算 MD5，表里已有就直接建立引用，不用真的传文件。</p>
     */
    private String fileHash;

    /** 原始文件名（用户上传时看到的名字，下载时还原回去） */
    private String fileName;

    /** 扩展名（小写，不含点） */
    private String fileExt;

    /**
     * 存储相对路径，如 {@code 2026/10/09/a1b2c3d4e5f6.pdf}
     *
     * <p>刻意存<b>相对</b>路径：将来整个 upload 目录换位置、或者迁到 MinIO，
     * 数据库里的值不用改。</p>
     */
    private String storagePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME 类型，如 application/pdf */
    private String mimeType;

    /** 存储类型：LOCAL / MINIO（同一系统里可能新旧文件存在不同地方） */
    private String storageType;

    /**
     * 引用计数
     *
     * <p>每被一份资料引用就 +1，取消引用 -1。
     * <b>只有归零时才允许真正删除物理文件</b>，否则会删掉别人还在用的文件。</p>
     */
    private Integer refCount;

    /** 上传者 ID */
    private Long uploaderId;

    private LocalDateTime createdAt;
}

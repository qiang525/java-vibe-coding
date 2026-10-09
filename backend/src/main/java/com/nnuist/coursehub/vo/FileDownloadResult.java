package com.nnuist.coursehub.vo;

import lombok.Data;

import java.io.InputStream;

/**
 * 文件下载结果（Service 内部传递用，不直接序列化给前端）
 *
 * <p>下载要做两件事：<b>把文件流交给浏览器</b>、<b>告诉浏览器文件叫什么</b>。
 * 所以需要把"流 + 元信息"打包一起返回。</p>
 *
 * <p><b>为什么用 InputStream 而不是 byte[]</b>：500MB 的文件如果读成字节数组，
 * 一个请求就占用 500MB 堆内存，几个人同时下载服务就崩了。
 * 流式传输只占用少量缓冲区。</p>
 *
 * @author CourseHub Team
 */
@Data
public class FileDownloadResult {

    /** 原始文件名（下载后保存的名字） */
    private String fileName;

    /** MIME 类型 */
    private String mimeType;

    /** 文件大小 */
    private Long fileSize;

    /** 文件内容流（调用方负责关闭） */
    private InputStream inputStream;
}

package com.nnuist.coursehub.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * 文件存储抽象接口
 *
 * <p><b>为什么要抽象出这一层，而不是直接写 File 操作？</b>
 * 因为"文件存哪儿"是会变的：</p>
 * <ul>
 *   <li>自己电脑上跑 → 存本地磁盘（现在的做法）</li>
 *   <li>上了服务器 → 可能用 MinIO / 阿里云 OSS / 腾讯云 COS</li>
 * </ul>
 * <p>如果业务代码里到处都是 {@code new FileOutputStream("D:/upload/xxx")}，
 * 换存储时要满项目找、逐个改。抽象成接口后，<b>业务代码只调 store/load/delete</b>，
 * 换存储只需要再写一个实现类，业务代码一行不动。</p>
 *
 * <p>这叫做<b>面向接口编程</b>，也是"可替换性"的典型应用。</p>
 *
 * @author CourseHub Team
 */
public interface FileStorage {

    /**
     * 保存文件
     *
     * @param relativePath 相对路径，如 {@code 2026/10/09/a1b2c3d4.pdf}
     *                     <p>注意是<b>相对</b>路径，不含根目录。这样换存储后端时
     *                     数据库里存的值不用变。</p>
     * @param inputStream  文件内容流
     * @return 实际存储的相对路径
     */
    String store(String relativePath, InputStream inputStream) throws IOException;

    /**
     * 读取文件
     *
     * <p>调用方负责关闭返回的流（用 try-with-resources）。</p>
     */
    InputStream load(String relativePath) throws IOException;

    /**
     * 删除文件
     *
     * @return 是否真的删掉了
     */
    boolean delete(String relativePath);

    /**
     * 文件是否存在
     */
    boolean exists(String relativePath);

    /**
     * 取文件的绝对路径（本地存储专用，用于调试和日志）
     */
    Path getAbsolutePath(String relativePath);

    /**
     * 存储类型标识：LOCAL / MINIO
     *
     * <p>会存进数据库的 {@code file.storage_type} 字段。
     * 为什么要存？因为将来可能"老文件在本地、新文件在 MinIO"，
     * 读取时需要知道该用哪个实现。</p>
     */
    String getStorageType();
}

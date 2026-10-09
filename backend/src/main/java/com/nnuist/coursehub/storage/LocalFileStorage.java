package com.nnuist.coursehub.storage;

import com.nnuist.coursehub.common.exception.BizException;
import com.nnuist.coursehub.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储实现
 *
 * <p><b>什么时候生效</b>：配置 {@code coursehub.file.storage-type=local} 时
 * （默认值），Spring 才会创建这个 Bean。以后写了 MinIO 实现，
 * 改成 {@code minio} 就会自动切换到它 —— 业务代码完全不用动。</p>
 *
 * <p><b>本类最关键的一段代码是 {@link #resolveSafely}</b>，它防的是
 * "路径穿越攻击"。详见该方法的注释。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "coursehub.file.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    /** 存储根目录，从配置文件读，默认项目下的 upload/ */
    @Value("${coursehub.file.local-path:./upload}")
    private String localPath;

    /** 规范化后的绝对路径，所有文件操作都必须在它之下 */
    private Path rootPath;

    @PostConstruct
    public void init() throws IOException {
        rootPath = Paths.get(localPath).toAbsolutePath().normalize();
        // 目录不存在就创建（首次启动时会有用）
        Files.createDirectories(rootPath);
        log.info("本地文件存储初始化完成 | 根目录={}", rootPath);
    }

    @Override
    public String store(String relativePath, InputStream inputStream) throws IOException {
        Path target = resolveSafely(relativePath);
        // 父目录可能还不存在（按日期分层的新目录），先建出来
        Files.createDirectories(target.getParent());
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return relativePath;
    }

    @Override
    public InputStream load(String relativePath) throws IOException {
        Path target = resolveSafely(relativePath);
        if (!Files.exists(target)) {
            throw new BizException(ResultCode.FILE_NOT_FOUND);
        }
        return Files.newInputStream(target);
    }

    @Override
    public boolean delete(String relativePath) {
        try {
            return Files.deleteIfExists(resolveSafely(relativePath));
        } catch (IOException e) {
            log.warn("删除文件失败 | path={} | error={}", relativePath, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(String relativePath) {
        try {
            return Files.exists(resolveSafely(relativePath));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Path getAbsolutePath(String relativePath) {
        return resolveSafely(relativePath);
    }

    @Override
    public String getStorageType() {
        return "LOCAL";
    }

    // =========================================================================
    //  安全关键：路径解析
    // =========================================================================

    /**
     * 安全地把"相对路径"解析成"绝对路径"，并校验它没有跑出根目录
     *
     * <p><b>防的是什么攻击 —— 路径穿越（Path Traversal）</b></p>
     *
     * <p>假设不做校验，攻击者把文件名传成：</p>
     * <pre>
     *   ../../../../../../Windows/System32/drivers/etc/hosts
     * </pre>
     * <p>程序天真地拼成 {@code D:/upload/../../../../Windows/...}，
     * 一 normalize（化简 {@code ..}）就变成了系统目录，
     * 于是他能读到、甚至覆盖系统文件。<b>这是真实存在的高危漏洞。</b></p>
     *
     * <p><b>防御方法（三步）</b>：</p>
     * <ol>
     *   <li>{@code resolve()} —— 把相对路径接到根目录后面</li>
     *   <li>{@code normalize()} —— 化简掉所有的 {@code .} 和 {@code ..}</li>
     *   <li>{@code startsWith(rootPath)} —— <b>关键一步</b>：
     *       化简后的路径必须仍在根目录之内，否则拒绝</li>
     * </ol>
     *
     * <p>注意 {@code normalize()} 必须在 {@code startsWith} 之前调用，
     * 否则 {@code a/../../b} 这种写法会绕过检查。</p>
     */
    private Path resolveSafely(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "文件路径不能为空");
        }

        Path target = rootPath.resolve(relativePath).normalize();

        if (!target.startsWith(rootPath)) {
            log.error("检测到路径穿越攻击 | 输入={} | 解析结果={}", relativePath, target);
            throw new BizException(ResultCode.FORBIDDEN, "非法的文件路径");
        }
        return target;
    }
}

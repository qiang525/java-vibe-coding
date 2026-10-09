package com.nnuist.coursehub.common.constant;

/**
 * 全局常量
 *
 * <p>把散落在各处的"魔法值"集中到这里，改一处即可全局生效。</p>
 *
 * @author CourseHub Team
 */
public final class Constants {

    private Constants() {
        // 工具类不允许实例化
    }

    // ==================== 认证相关 ====================

    /** 请求头中携带 Token 的字段名 */
    public static final String TOKEN_HEADER = "Authorization";

    /** Token 前缀（Bearer 认证方案） */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Token 有效期：7 天（毫秒） */
    public static final long TOKEN_EXPIRE_MS = 7 * 24 * 60 * 60 * 1000L;

    // ==================== 分页相关 ====================

    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 每页最大条数 */
    public static final int MAX_PAGE_SIZE = 100;

    // ==================== 文件相关 ====================

    /** 分片大小：5MB */
    public static final long CHUNK_SIZE = 5 * 1024 * 1024L;

    /** 单文件最大：500MB */
    public static final long MAX_FILE_SIZE = 500 * 1024 * 1024L;

    /** 允许上传的扩展名（白名单，比黑名单安全） */
    public static final String[] ALLOWED_EXTENSIONS = {
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "txt", "md", "zip", "rar", "7z", "jpg", "jpeg", "png", "gif"
    };

    // ==================== Redis Key 前缀 ====================

    /** 登录令牌 */
    public static final String REDIS_TOKEN = "coursehub:token:";

    /** 下载计数（内存累加，定时落库） */
    public static final String REDIS_DOWNLOAD_COUNT = "coursehub:download:";

    /** 分片上传进度 */
    public static final String REDIS_UPLOAD_CHUNK = "coursehub:upload:chunk:";

    /** 搜索词缓存 */
    public static final String REDIS_SEARCH_CACHE = "coursehub:search:";

    /** 接口限流 */
    public static final String REDIS_RATE_LIMIT = "coursehub:ratelimit:";

    // ==================== 逻辑删除 ====================

    /** 未删除 */
    public static final int NOT_DELETED = 0;

    /** 已删除 */
    public static final int DELETED = 1;
}

package com.nnuist.coursehub.common.result;

import lombok.Getter;

/**
 * 业务错误码枚举
 *
 * <p>错误码分段规则，方便一眼看出问题归属：</p>
 * <ul>
 *   <li>0        —— 成功</li>
 *   <li>400xx    —— 参数/请求错误（客户端的问题）</li>
 *   <li>401xx    —— 认证授权问题</li>
 *   <li>403xx    —— 权限不足 / 越权</li>
 *   <li>404xx    —— 资源不存在</li>
 *   <li>409xx    —— 状态冲突（重复提交、并发修改）</li>
 *   <li>500xx    —— 服务端自身错误</li>
 * </ul>
 *
 * <p><b>为什么用枚举而不是散落的数字</b>：写代码时用
 * {@code ResultCode.USER_NOT_FOUND} 比 {@code 40401} 可读性强得多，
 * 且改错误码时只需改一处。</p>
 *
 * @author CourseHub Team
 */
@Getter
public enum ResultCode {

    // ==================== 成功 ====================
    SUCCESS(0, "ok"),

    // ==================== 客户端错误 400xx ====================
    PARAM_ERROR(40001, "参数校验失败"),
    PARAM_MISSING(40002, "缺少必要参数"),
    PARAM_FORMAT_ERROR(40003, "参数格式不正确"),
    REQUEST_METHOD_NOT_SUPPORTED(40004, "请求方法不支持"),

    // ==================== 认证授权 401xx ====================
    UNAUTHORIZED(40101, "未登录或登录已过期"),
    TOKEN_INVALID(40102, "登录令牌无效"),
    TOKEN_EXPIRED(40103, "登录令牌已过期"),
    LOGIN_FAILED(40104, "账号或密码错误"),
    ACCOUNT_DISABLED(40105, "账号已被禁用"),
    ACCOUNT_EXISTS(40106, "该账号已被注册"),

    // ==================== 权限 403xx ====================
    FORBIDDEN(40301, "无权执行该操作"),
    NO_PERMISSION(40302, "权限不足"),
    RESOURCE_NOT_ACCESSIBLE(40303, "无权访问该资源"),
    NOT_COURSE_MEMBER(40304, "你不是该课程成员，无法访问该资料"),

    // ==================== 资源不存在 404xx ====================
    NOT_FOUND(40401, "请求的资源不存在"),
    USER_NOT_FOUND(40402, "用户不存在"),
    COURSE_NOT_FOUND(40403, "课程不存在"),
    RESOURCE_NOT_FOUND(40404, "资料不存在"),
    COMMENT_NOT_FOUND(40405, "评论不存在"),
    FILE_NOT_FOUND(40406, "文件不存在"),
    VERSION_NOT_FOUND(40407, "版本不存在"),

    // ==================== 状态冲突 409xx ====================
    DATA_CONFLICT(40901, "数据已被修改，请刷新后重试"),
    DUPLICATE_SUBMIT(40902, "请勿重复提交"),
    VERSION_CONFLICT(40903, "该资料已有新版本，请先查看差异"),
    OPERATION_NOT_ALLOWED(40904, "当前状态不允许该操作"),
    ALREADY_COLLECTED(40905, "已经收藏过了"),
    ALREADY_JOINED(40906, "你已经加入该课程了"),
    NOT_JOINED(40907, "你尚未加入该课程"),

    // ==================== 文件相关 41xxx ====================
    FILE_EMPTY(41001, "上传文件为空"),
    FILE_TOO_LARGE(41002, "文件大小超出限制"),
    FILE_TYPE_NOT_ALLOWED(41003, "不支持该文件类型"),
    FILE_UPLOAD_FAILED(41004, "文件上传失败"),
    CHUNK_MERGE_FAILED(41005, "分片合并失败"),
    FILE_HASH_MISMATCH(41006, "文件校验失败，可能传输过程中损坏"),

    // ==================== 服务端错误 500xx ====================
    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试"),
    DATABASE_ERROR(50001, "数据库操作异常"),
    CACHE_ERROR(50002, "缓存服务异常"),
    REMOTE_CALL_ERROR(50003, "远程调用失败"),
    UNKNOWN_ERROR(50099, "未知错误");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

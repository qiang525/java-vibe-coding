package com.nnuist.coursehub.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 *
 * <p>所有接口都必须返回这个结构，前端只需判断 {@code code} 一个字段
 * 就知道请求成功还是失败。</p>
 *
 * <pre>
 * {
 *   "code": 0,
 *   "message": "ok",
 *   "data": { ... },
 *   "timestamp": 1759250000000
 * }
 * </pre>
 *
 * <p><b>为什么必须统一</b>：如果每个接口返回结构都不一样，前端就要针对
 * 每个接口写一套解析逻辑，一旦后端改动就会大面积报错。</p>
 *
 * @param <T> 业务数据类型
 * @author CourseHub Team
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码，0 表示成功 */
    private Integer code;

    /** 提示信息，成功时为 "ok" */
    private String message;

    /** 业务数据，失败时为 null */
    private T data;

    /** 服务器时间戳，便于前端排查时差问题 */
    private Long timestamp;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 成功 ====================

    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), "ok", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    // ==================== 失败 ====================

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.SYSTEM_ERROR.getCode(), message, null);
    }

    /**
     * 是否成功，供内部判断使用
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}

package com.nnuist.coursehub.common.exception;

import com.nnuist.coursehub.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * <p>Service 层遇到"能预料到的业务问题"时抛出这个异常，
 * 由 {@link GlobalExceptionHandler} 统一捕获并转成标准响应体。</p>
 *
 * <p><b>什么时候用它</b>：用户输入不合法、资源不存在、权限不足、
 * 状态冲突等——这些都不是系统 bug，而是正常的业务分支。</p>
 *
 * <p><b>什么时候不该用它</b>：数据库连不上、空指针这类，
 * 应该让它们抛出原始异常，由全局处理器归到 5xx。</p>
 *
 * <pre>
 * // 使用示例
 * if (user == null) {
 *     throw new BizException(ResultCode.USER_NOT_FOUND);
 * }
 * throw new BizException(ResultCode.FILE_TOO_LARGE, "单个文件不能超过 500MB");
 * </pre>
 *
 * @author CourseHub Team
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private final Integer code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 自定义提示信息（覆盖枚举里的默认文案）
     */
    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
    }
}

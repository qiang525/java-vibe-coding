package com.nnuist.coursehub.common.exception;

import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>所有 Controller 抛出的异常都会在这里被"兜住"，转成统一的响应体。
 * 好处是业务代码里不用到处写 try-catch。</p>
 *
 * <p><b>安全要点</b>：生产环境绝不能把异常堆栈返回给前端——
 * 堆栈里包含包名、类名、甚至 SQL 片段，是攻击者的重要线索。
 * 这里只返回友好提示，真实堆栈只写进服务端日志。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 —— 最常见，属于"预期内的错误"，用 info 级别记录即可
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.info("业务异常 | {} {} | code={} | msg={}",
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * @RequestBody 上的参数校验失败
     * 例如：@Valid 校验 UserDTO 时长度不符合要求
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 表单绑定校验失败（非 JSON 请求体）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", msg);
        return Result.fail(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 单参数校验失败（@Validated 标注在 Controller 类上时生效）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数约束校验失败: {}", msg);
        return Result.fail(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 缺少必填的请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String msg = "缺少必要参数: " + e.getParameterName();
        log.warn(msg);
        return Result.fail(ResultCode.PARAM_MISSING, msg);
    }

    /**
     * 参数类型不匹配，例如把 "abc" 传给 Integer 参数
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String msg = String.format("参数 '%s' 类型不正确，期望类型: %s",
                e.getName(), e.getRequiredType() == null ? "未知" : e.getRequiredType().getSimpleName());
        log.warn(msg);
        return Result.fail(ResultCode.PARAM_FORMAT_ERROR, msg);
    }

    /**
     * 请求方法不支持，例如用 GET 访问只接受 POST 的接口
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        String msg = String.format("不支持 %s 请求，请使用: %s",
                e.getMethod(), e.getSupportedHttpMethods());
        log.warn(msg);
        return Result.fail(ResultCode.REQUEST_METHOD_NOT_SUPPORTED, msg);
    }

    /**
     * 上传文件超过大小限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件超出大小限制: {}", e.getMessage());
        return Result.fail(ResultCode.FILE_TOO_LARGE, "上传文件超出大小限制");
    }

    /**
     * 404 —— 请求路径不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("请求路径不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return Result.fail(ResultCode.NOT_FOUND, "请求的接口不存在");
    }

    /**
     * 兜底处理 —— 所有未预料到的异常
     *
     * <p>这里必须记录完整堆栈（error 级别），但**不能**把堆栈返回给前端。</p>
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 | {} {}", request.getMethod(), request.getRequestURI(), e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }

    /**
     * 把字段校验错误格式化成 "字段名: 错误提示"
     */
    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}

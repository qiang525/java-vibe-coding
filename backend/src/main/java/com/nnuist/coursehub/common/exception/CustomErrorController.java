package com.nnuist.coursehub.common.exception;

import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * 统一错误控制器
 *
 * <p><b>为什么需要它</b>：Spring Boot 有两条 404 路径——</p>
 * <ol>
 *   <li>请求路径匹配不到任何 Controller，也不是静态资源 → 转发到 {@code /error}</li>
 *   <li>能被 {@code @ExceptionHandler} 拦到的 → 已由 GlobalExceptionHandler 处理</li>
 * </ol>
 *
 * <p>第 1 种情况如果不接管，会返回 Spring 自带的
 * {@code {"timestamp":...,"status":404,"error":"Not Found"}} 格式，
 * 与我们的 {@code {code,message,data}} 不一致，前端得写两套解析逻辑。</p>
 *
 * <p><b>为什么不用「关闭静态资源映射 + throw-exception-if-no-handler-found」方案</b>：
 * 那样虽然能让 404 走进 GlobalExceptionHandler，但会**同时干掉 Swagger 的
 * /doc.html**（它本质是静态资源）。这里改为接管 {@code /error} 端点，
 * 既统一了格式，又不影响任何静态资源。</p>
 *
 * <p><b>注意</b>：Spring Boot 2.5 起 {@code ErrorController} 的
 * {@code getErrorPath()} 方法已被移除，不要再实现它，否则编译失败。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@RestController
public class CustomErrorController implements ErrorController {

    /**
     * 接管 Spring Boot 的错误转发端点
     *
     * <p>这个映射路径必须是 {@code /error}，Spring Boot 会把处理不了的请求转发到这里。</p>
     */
    @RequestMapping("/error")
    public Result<Void> handleError(HttpServletRequest request) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uriObj = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int status = (statusObj == null) ? 500 : Integer.parseInt(statusObj.toString());
        String uri = (uriObj == null) ? "-" : uriObj.toString();

        ResultCode resultCode = mapStatusToResultCode(status);
        log.warn("请求错误 | status={} | uri={} | 已转换为统一响应格式", status, uri);

        return Result.fail(resultCode);
    }

    /**
     * HTTP 状态码 → 业务错误码
     */
    private ResultCode mapStatusToResultCode(int status) {
        switch (status) {
            case 400:
                return ResultCode.PARAM_ERROR;
            case 401:
                return ResultCode.UNAUTHORIZED;
            case 403:
                return ResultCode.FORBIDDEN;
            case 404:
                return ResultCode.NOT_FOUND;
            case 405:
                return ResultCode.REQUEST_METHOD_NOT_SUPPORTED;
            case 500:
            default:
                return ResultCode.SYSTEM_ERROR;
        }
    }
}

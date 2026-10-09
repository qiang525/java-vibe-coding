package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口
 *
 * <p>用于验证服务是否正常启动、前后端链路是否打通。
 * 前端启动后第一个调用的就是这个接口。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "00-系统健康检查")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Value("${spring.application.name:coursehub}")
    private String appName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * 服务健康状态
     *
     * <pre>
     * GET /api/health
     * → { "code": 0, "message": "ok", "data": { "status": "UP", ... } }
     * </pre>
     */
    @ApiOperation("服务健康检查")
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("application", appName);
        data.put("profile", activeProfile);
        data.put("serverTime", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("javaVersion", System.getProperty("java.version"));
        return Result.ok("服务运行正常", data);
    }

    /**
     * 联调测试接口
     *
     * <p>用来验证统一响应体、全局异常处理是否生效。</p>
     */
    @ApiOperation("联调测试")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }
}

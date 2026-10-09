package com.nnuist.coursehub.config;

import com.nnuist.coursehub.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置
 *
 * <p>在这里做两件事：</p>
 * <ol>
 *   <li>注册拦截器，并指定它拦截哪些路径、放行哪些路径</li>
 *   <li>（后续可扩展）注册参数解析器、消息转换器等</li>
 * </ol>
 *
 * @author CourseHub Team
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                // 拦截所有 /api 开头的接口
                .addPathPatterns("/api/**")
                // 放行不需要登录的接口与文档页面
                .excludePathPatterns(
                        // 登录注册本身当然不能要求登录
                        "/api/auth/login",
                        "/api/auth/register",
                        // 健康检查
                        "/api/health/**",
                        // 接口文档相关（否则打开文档页面会被拦）
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs/**",
                        "/favicon.ico",
                        // Druid 监控页面
                        "/druid/**",
                        // 错误页
                        "/error"
                );
    }
}

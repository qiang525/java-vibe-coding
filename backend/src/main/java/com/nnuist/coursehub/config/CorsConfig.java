package com.nnuist.coursehub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置（CORS）
 *
 * <p><b>为什么需要</b>：前后端分离后，前端跑在 5173 端口、后端跑在 8080 端口。
 * 浏览器出于同源策略会拦截这种跨端口请求，必须由后端明确声明"我允许这些来源"。</p>
 *
 * <p><b>安全要点</b>：这里用的是白名单，不是 {@code "*"}。
 * 生产环境必须把真实域名加进白名单，否则任何网站都能拿用户凭证调你的接口。</p>
 *
 * @author CourseHub Team
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许携带凭证（Cookie / Authorization 头）
        config.setAllowCredentials(true);

        // 允许的前端来源白名单
        // 开发环境：Vite 默认端口 5173，后端 8080
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://127.0.0.1:5173");
        config.addAllowedOrigin("http://localhost:8080");
        config.addAllowedOrigin("http://127.0.0.1:8080");
        // 生产环境请在此追加真实域名，例如：
        // config.addAllowedOrigin("https://coursehub.example.com");

        // 允许所有请求头（包含 Authorization）
        config.addAllowedHeader("*");

        // 允许所有请求方法
        config.addAllowedMethod("*");

        // 预检请求缓存 1 小时，减少 OPTIONS 请求次数
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}

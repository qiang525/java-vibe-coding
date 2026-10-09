package com.nnuist.coursehub.interceptor;

import com.nnuist.coursehub.common.annotation.RequireLogin;
import com.nnuist.coursehub.common.annotation.RequireRole;
import com.nnuist.coursehub.common.exception.BizException;
import com.nnuist.coursehub.common.result.ResultCode;
import com.nnuist.coursehub.common.util.JwtUtil;
import com.nnuist.coursehub.common.util.UserContext;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * 认证授权拦截器
 *
 * <p><b>它是什么</b>：介于"请求到达 Controller"之前的一道关卡。</p>
 *
 * <p><b>它做什么</b>，按顺序四步：</p>
 * <ol>
 *   <li>从请求头里取出 Token</li>
 *   <li>解析 Token，拿到用户身份</li>
 *   <li>把身份存进 UserContext（供后续代码使用）</li>
 *   <li>检查方法上的注解，判断权限够不够</li>
 * </ol>
 *
 * <p><b>为什么用拦截器而不是在每个方法里判断</b>：
 * 那样每个接口都要重复写一遍鉴权代码，漏写一个就是漏洞。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        // 放行非 Controller 的请求（如静态资源、Swagger 页面）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // ---------- ① 尝试解析 Token（不管接口要不要登录，先解析） ----------
        String token = extractToken(request);
        if (token != null) {
            Claims claims = jwtUtil.parseToken(token);
            if (claims != null) {
                Long userId = Long.valueOf(claims.get(JwtUtil.CLAIM_USER_ID).toString());
                String username = (String) claims.get(JwtUtil.CLAIM_USERNAME);
                String role = (String) claims.get(JwtUtil.CLAIM_ROLE);
                // 存入上下文，后续 Controller / Service 可直接读取
                UserContext.set(new UserContext.CurrentUser(userId, username, role));
            }
        }

        // ---------- ② 检查接口是否需要登录 ----------
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        boolean needLogin = needLogin(handlerMethod);

        if (needLogin && !UserContext.isLoggedIn()) {
            log.debug("拦截未登录访问 | {} {}", request.getMethod(), request.getRequestURI());
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // ---------- ③ 检查角色要求 ----------
        RequireRole requireRole = getAnnotation(handlerMethod, RequireRole.class);
        if (requireRole != null) {
            String currentRole = UserContext.getRole();
            boolean allowed = currentRole != null
                    && Arrays.asList(requireRole.value()).contains(currentRole);
            if (!allowed) {
                log.warn("权限不足 | userId={} | role={} | required={} | {} {}",
                        UserContext.getUserId(), currentRole,
                        Arrays.toString(requireRole.value()),
                        request.getMethod(), request.getRequestURI());
                throw new BizException(ResultCode.NO_PERMISSION);
            }
        }

        return true;
    }

    /**
     * 请求结束后清理 ThreadLocal
     *
     * <p><b>这一步绝对不能省</b>：Tomcat 线程是复用的，
     * 不清理会让下一个请求读到上一个用户的身份 —— 典型越权漏洞。</p>
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    // =========================================================================
    //  私有方法
    // =========================================================================

    /**
     * 从请求头提取 Token
     *
     * <p>标准格式：{@code Authorization: Bearer eyJhbGci...}</p>
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.trim().isEmpty()) {
            return null;
        }
        // 去掉 "Bearer " 前缀
        if (header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return header.trim();
    }

    /**
     * 判断接口是否需要登录
     *
     * <p>方法上或类上有 {@code @RequireLogin} 就要求登录。</p>
     */
    private boolean needLogin(HandlerMethod handlerMethod) {
        return getAnnotation(handlerMethod, RequireLogin.class) != null;
    }

    /**
     * 查找注解：先找方法，方法上没有就找类
     */
    private <A extends java.lang.annotation.Annotation> A getAnnotation(
            HandlerMethod handlerMethod, Class<A> annotationType) {
        A annotation = handlerMethod.getMethodAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        return handlerMethod.getBeanType().getAnnotation(annotationType);
    }
}

package com.nnuist.coursehub.common.annotation;

import java.lang.annotation.*;

/**
 * 标记接口需要登录才能访问
 *
 * <p>用法：加在 Controller 方法或类上</p>
 * <pre>
 *   {@code @RequireLogin}
 *   {@code @GetMapping("/me")}
 *   public Result&lt;UserVO&gt; me() { ... }
 * </pre>
 *
 * <p>拦截器会检查这个注解：有它但没有有效 Token 就返回 401。</p>
 *
 * @author CourseHub Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireLogin {
}

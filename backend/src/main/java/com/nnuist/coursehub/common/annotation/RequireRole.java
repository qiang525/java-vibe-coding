package com.nnuist.coursehub.common.annotation;

import java.lang.annotation.*;

/**
 * 标记接口需要特定角色才能访问
 *
 * <p>用法：</p>
 * <pre>
 *   // 只有管理员能访问
 *   {@code @RequireRole("ADMIN")}
 *
 *   // 管理员或教师都能访问
 *   {@code @RequireRole({"ADMIN", "TEACHER"})}
 * </pre>
 *
 * <p><b>这属于"功能权限"</b>——管你能不能进这个接口。
 * 还有一层"数据权限"——管你能不能看这条具体数据，
 * 那一层要在 Service 里判断，光靠注解不够。</p>
 *
 * @author CourseHub Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 允许访问的角色列表，满足其中之一即可放行
     */
    String[] value();
}

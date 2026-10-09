package com.nnuist.coursehub.common.annotation;

import java.lang.annotation.*;

/**
 * 标记需要记录操作日志的方法
 *
 * <p>用法：加在 Controller 方法上，切面会自动把"谁在什么时候做了什么"写进
 * {@code audit_log} 表。</p>
 *
 * <pre>
 *   {@code @LogOperation(value = "删除课程", targetType = "COURSE")}
 *   {@code @DeleteMapping("/{id}")}
 * </pre>
 *
 * <p><b>和普通应用日志的区别</b>：{@code log.info(...)} 输出到文件，
 * 用于开发排查 bug，会定期清理；<b>审计日志要进数据库</b>，记录操作行为本身，
 * 用于追责与合规，需要长期保留。</p>
 *
 * @author CourseHub Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {

    /** 操作描述，如"删除课程" */
    String value();

    /** 操作对象类型，如 COURSE / RESOURCE / USER */
    String targetType() default "";

    /**
     * 从第几个参数取对象 ID（-1 表示不取）
     *
     * <p>对于 {@code DELETE /api/courses/{id}} 这种方法，第 0 个参数就是课程 ID。</p>
     */
    int targetIdIndex() default -1;
}

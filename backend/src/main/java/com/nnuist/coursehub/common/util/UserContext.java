package com.nnuist.coursehub.common.util;

import lombok.Data;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 *
 * <p><b>解决什么问题</b>：Controller 和 Service 都需要知道"当前是谁在操作"。
 * 如果一路靠方法参数传，那几乎每个方法都要多一个 {@code Long userId} 参数，
 * 代码会变得很啰嗦。</p>
 *
 * <p><b>怎么做到的</b>：拦截器解析出 Token 后，把用户信息塞进 ThreadLocal；
 * 同一个请求线程内的任何地方都能直接读出来。请求结束时拦截器负责清理。</p>
 *
 * <p><b>为什么用 ThreadLocal 而不是全局变量</b>：
 * 每个请求跑在独立线程上，ThreadLocal 保证用户 A 的请求读到的是 A 的信息，
 * 不会和同时进行的用户 B 的请求串味。</p>
 *
 * <p><b>清理很关键</b>：Tomcat 的线程是复用的。如果不清理，
 * 下一个请求可能读到上一个请求残留的用户信息 —— 这是真实的越权漏洞来源。</p>
 *
 * @author CourseHub Team
 */
public final class UserContext {

    private UserContext() {
    }

    /**
     * 每个线程独享一份，互不干扰
     */
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前用户 ID；未登录返回 null
     */
    public static Long getUserId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    /**
     * 当前用户角色；未登录返回 null
     */
    public static String getRole() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getRole();
    }

    /**
     * 是否已登录
     */
    public static boolean isLoggedIn() {
        return HOLDER.get() != null;
    }

    /**
     * 是否管理员
     */
    public static boolean isAdmin() {
        return "ADMIN".equals(getRole());
    }

    /**
     * 当前登录用户的精简信息
     */
    @Data
    public static class CurrentUser {
        private Long userId;
        private String username;
        private String role;

        public CurrentUser(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }
    }
}

package com.aihedgefund.auth;

/**
 * 当前请求的已认证用户上下文（基于 ThreadLocal）。
 * 由鉴权拦截器在请求开始时写入、请求结束时清除。
 */
public final class UserContext {

    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(AuthUser user) {
        CURRENT.set(user);
    }

    public static AuthUser get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}

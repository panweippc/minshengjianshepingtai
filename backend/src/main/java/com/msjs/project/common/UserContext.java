package com.msjs.project.common;

/**
 * 当前请求登录用户持有者（ThreadLocal）
 * 由鉴权拦截器写入，请求结束后清理，防止线程复用导致的数据串号
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 当前登录用户，未登录返回 null */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 当前登录用户ID，未登录返回 null */
    public static Long getUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

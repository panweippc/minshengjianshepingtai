package com.msjs.project.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作权限校验注解
 * 标注在 Controller 方法/类上，登录用户须具备指定权限标识
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePerm {

    /** 权限标识，如 system:user:list */
    String value();
}

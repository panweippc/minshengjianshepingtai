package com.msjs.project.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * 标注模块与操作描述；未标注的接口也会被全量记录，仅缺少业务描述
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 所属模块 */
    String module() default "";

    /** 操作描述 */
    String operation() default "";
}

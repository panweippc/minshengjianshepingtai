package com.msjs.project.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解（行政区划行级隔离）
 * 标注在 Mapper 查询方法上，按当前登录用户的数据权限范围自动追加 region_code 过滤条件
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /** 行政区划字段在 SQL 中的列名（可带表别名） */
    String regionColumn() default "region_code";
}

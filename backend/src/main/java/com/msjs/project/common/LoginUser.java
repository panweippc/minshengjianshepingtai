package com.msjs.project.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 登录用户上下文信息（存储于 Redis，随请求线程传递）
 * 注意：派生方法需 @JsonIgnore，避免序列化产生多余属性导致反序列化失败
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginUser implements Serializable {

    /** 用户ID */
    private Long userId;
    /** 登录账号 */
    private String username;
    /** 姓名 */
    private String realName;
    /** 所属行政区划代码 */
    private String regionCode;
    /** 角色标识集合 */
    private Set<String> roleKeys;
    /** 权限标识集合（admin 为 "*"） */
    private Set<String> perms;
    /** 有效数据权限范围：1全部 2本区及以下 3仅本区（多角色取最大范围，即最小值） */
    private Integer dataScope;

    /** 是否超级管理员（派生属性，不参与序列化） */
    @JsonIgnore
    public boolean isAdmin() {
        return roleKeys != null && roleKeys.contains("admin");
    }

    /** 是否拥有指定权限 */
    public boolean hasPerm(String perm) {
        return isAdmin() || (perms != null && perms.contains(perm));
    }
}

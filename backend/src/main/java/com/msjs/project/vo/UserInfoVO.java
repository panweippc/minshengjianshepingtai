package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.msjs.project.entity.SysMenu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 当前登录用户信息（含权限与可见菜单树）
 */
@Data
@Schema(description = "当前登录用户信息")
public class UserInfoVO {

    /** 雪花ID超出JS安全整数范围（2^53-1），序列化为字符串，避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "所属行政区划代码")
    private String regionCode;

    @Schema(description = "所属行政区划名称")
    private String regionName;

    @Schema(description = "角色标识集合")
    private Set<String> roles;

    @Schema(description = "权限标识集合")
    private Set<String> perms;

    @Schema(description = "数据权限范围：1全部 2本区及以下 3仅本区")
    private Integer dataScope;

    @Schema(description = "可见菜单树（目录+菜单）")
    private List<SysMenu> menus;
}

package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 菜单新增/编辑入参
 */
@Data
@Schema(description = "菜单保存入参")
public class MenuSaveDTO {

    @Schema(description = "菜单ID（编辑时必填）")
    private Long id;

    @NotNull(message = "父菜单不能为空")
    @Schema(description = "父菜单ID，0为根")
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Schema(description = "菜单名称")
    private String menuName;

    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "^[MCF]$", message = "菜单类型须为 M目录/C菜单/F按钮")
    @Schema(description = "类型：M目录 C菜单 F按钮")
    private String menuType;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "权限标识")
    private String perm;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}

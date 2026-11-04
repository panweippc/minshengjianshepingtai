package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色新增/编辑入参
 */
@Data
@Schema(description = "角色保存入参")
public class RoleSaveDTO {

    @Schema(description = "角色ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称")
    private String roleName;

    @NotBlank(message = "角色标识不能为空")
    @Schema(description = "角色标识")
    private String roleKey;

    @NotNull(message = "数据权限范围不能为空")
    @Schema(description = "数据权限范围：1全部 2本区及以下 3仅本区")
    private Integer dataScope;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "菜单ID集合（分配权限）")
    private List<Long> menuIds;
}

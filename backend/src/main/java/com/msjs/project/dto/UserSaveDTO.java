package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 用户新增/编辑入参
 */
@Data
@Schema(description = "用户保存入参")
public class UserSaveDTO {

    @Schema(description = "用户ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "登录账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$", message = "登录账号须为3-32位字母数字下划线")
    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "密码（新增时必填，编辑时留空表示不修改）")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "所属行政区划代码")
    private String regionCode;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "角色ID集合")
    private List<Long> roleIds;
}

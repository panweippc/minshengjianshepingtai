package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料修改入参（仅允许修改姓名与联系电话）
 */
@Data
@Schema(description = "个人资料修改入参")
public class ProfileUpdateDTO {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度不能超过32位")
    @Schema(description = "用户姓名")
    private String realName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    @Schema(description = "联系电话")
    private String phone;
}

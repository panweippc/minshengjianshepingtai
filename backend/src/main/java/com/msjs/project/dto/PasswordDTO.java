package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码入参
 */
@Data
@Schema(description = "重置密码入参")
public class PasswordDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为6-32位")
    @Schema(description = "新密码")
    private String newPassword;
}

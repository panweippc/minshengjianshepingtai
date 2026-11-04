package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参
 */
@Data
@Schema(description = "登录入参")
public class LoginDTO {

    @NotBlank(message = "登录账号不能为空")
    @Schema(description = "登录账号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String password;
}

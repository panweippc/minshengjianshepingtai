package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录返回
 */
@Data
@Schema(description = "登录返回")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "姓名")
    private String realName;
}

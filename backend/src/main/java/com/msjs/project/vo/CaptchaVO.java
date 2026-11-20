package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录验证码返回对象
 */
@Data
@Schema(description = "登录验证码")
public class CaptchaVO {

    @Schema(description = "验证码标识，登录时需原样回传")
    private String captchaId;

    @Schema(description = "验证码图片（Base64 Data URL）")
    private String captchaImage;
}

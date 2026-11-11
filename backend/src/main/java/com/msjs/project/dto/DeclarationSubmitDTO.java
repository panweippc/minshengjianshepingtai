package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 申报提交 DTO
 * 前端点"提交"按钮 → 弹用户选择 → 选审核人 → 调此接口
 */
@Data
@Schema(description = "申报-提交 DTO（指定接收审核人）")
public class DeclarationSubmitDTO {

    @NotNull(message = "申报单ID不能为空")
    @Schema(description = "申报单ID")
    private Long id;

    @NotNull(message = "请选择接收审核人")
    @Schema(description = "接收审核人（雪花ID）")
    private Long recipientUserId;
}

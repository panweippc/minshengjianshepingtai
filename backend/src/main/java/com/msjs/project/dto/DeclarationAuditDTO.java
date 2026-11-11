package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 申报审核 DTO（approve / reject 通用）
 */
@Data
@Schema(description = "申报-审核 DTO（通过 / 驳回通用）")
public class DeclarationAuditDTO {

    @NotNull(message = "申报单ID不能为空")
    @Schema(description = "申报单ID")
    private Long id;

    @NotBlank(message = "审核动作不能为空")
    @Pattern(regexp = "approve|reject", message = "action 只能是 approve 或 reject")
    @Schema(description = "审核动作：approve 通过 / reject 驳回")
    private String action;

    @Schema(description = "审核意见（驳回时必填，通过时可空）")
    private String auditRemark;
}

package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 明细级审核 DTO（农户 / 公建设施明细通过 / 驳回通用）
 */
@Data
@Schema(description = "明细审核 DTO（农户 / 公建设施通用）")
public class DeclarationDetailAuditDTO {

    @NotNull(message = "明细ID不能为空")
    @Schema(description = "明细ID（t_declaration_household.id 或 t_declaration_public_facility.id）")
    private Long detailId;

    @NotBlank(message = "明细类型不能为空")
    @Pattern(regexp = "household|public_facility", message = "detailKind 只能是 household 或 public_facility")
    @Schema(description = "明细类型：household 农户 / public_facility 公建设施")
    private String detailKind;

    @NotBlank(message = "审核动作不能为空")
    @Pattern(regexp = "approve|reject", message = "action 只能是 approve 或 reject")
    @Schema(description = "审核动作：approve 通过 / reject 驳回")
    private String action;

    @Schema(description = "审核意见（驳回时必填，通过时可空）")
    private String auditRemark;
}

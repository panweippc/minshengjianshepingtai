package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 明细退回 DTO：将某条已审核明细（approved / rejected / pending 均可）退回到
 * 未提交状态（audit_status = pending），并强制父申报单状态回滚为 draft，
 * 清掉 recipient_user_id，让申报人可以在"申报提交"页看到并重新编辑、重新提交。
 */
@Data
@Schema(description = "明细退回 DTO（农户 / 公建设施通用）")
public class DeclarationDetailRevertDTO {

    @NotNull(message = "明细ID不能为空")
    @Schema(description = "明细ID（t_declaration_household.id 或 t_declaration_public_facility.id）")
    private Long detailId;

    @NotBlank(message = "明细类型不能为空")
    @Pattern(regexp = "household|public_facility", message = "detailKind 只能是 household 或 public_facility")
    @Schema(description = "明细类型：household 农户 / public_facility 公建设施")
    private String detailKind;

    @Schema(description = "退回备注（可空，记录退回原因）")
    private String remark;
}

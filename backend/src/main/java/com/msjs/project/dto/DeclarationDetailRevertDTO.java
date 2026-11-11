package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 明细撤销 DTO：收回某条已审核明细（approved / rejected）的审核结论，
 * audit_status 置回 pending 并清空审核痕迹，父申报单同步回到 submitted（待审核）、
 * 保留 recipient_user_id 审核指派，供审核人在"待审核"页签重新做通过/驳回。
 */
@Data
@Schema(description = "明细撤销 DTO（农户 / 公建设施通用）")
public class DeclarationDetailRevertDTO {

    @NotNull(message = "明细ID不能为空")
    @Schema(description = "明细ID（t_declaration_household.id 或 t_declaration_public_facility.id）")
    private Long detailId;

    @NotBlank(message = "明细类型不能为空")
    @Pattern(regexp = "household|public_facility", message = "detailKind 只能是 household 或 public_facility")
    @Schema(description = "明细类型：household 农户 / public_facility 公建设施")
    private String detailKind;

    @Schema(description = "撤销备注（可空，记录撤销原因）")
    private String remark;
}

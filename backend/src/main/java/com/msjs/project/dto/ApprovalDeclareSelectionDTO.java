package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 立项关联申报单选择项：
 * 同一申报单允许按三级明细拆分成多次立项，因此需要携带本次立项选中的明细 ID。
 * detailIds 为空表示该申报单下全部「已通过」明细（兼容手动输入申报单 ID 的场景）。
 */
@Data
@Schema(description = "立项关联申报单及选中的三级明细")
public class ApprovalDeclareSelectionDTO {

    @NotNull(message = "申报单ID不能为空")
    @Schema(description = "申报单ID（t_declaration.id）")
    private Long declarationId;

    @Schema(description = "选中的三级明细ID集合；为空表示该申报单下全部已通过明细")
    private List<Long> detailIds;
}

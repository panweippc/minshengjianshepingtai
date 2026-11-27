package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

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

    /**
     * 仅重新提交指定农户明细（可选）
     * 场景：已驳回页签，申报人在树形表格里勾选了部分被驳回的农户明细 → 批量重新提交
     * 不传或为空时，沿用旧逻辑：该申报单下所有 unsubmitted + rejected 的农户明细都会被置为 pending
     */
    @Schema(description = "仅重新提交的农户明细ID列表（可选；为空时全量重置 unsubmitted+rejected）")
    private List<Long> householdDetailIds;

    /**
     * 仅重新提交指定公建设施明细（可选）
     * 不传或为空时，沿用旧逻辑：该申报单下所有 unsubmitted + rejected 的公建设施明细都会被置为 pending
     */
    @Schema(description = "仅重新提交的公建设施明细ID列表（可选；为空时全量重置 unsubmitted+rejected）")
    private List<Long> facilityDetailIds;
}

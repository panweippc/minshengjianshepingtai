package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 三级（农户/设施）施工记录编辑入参
 * 关联项目只读，由后端按明细反查立项，前端仅提交明细ID
 */
@Data
@Schema(description = "三级施工记录编辑入参")
public class ConstructionLogEditDTO implements Serializable {

    @NotNull(message = "立项ID不能为空")
    @Schema(description = "立项ID")
    private Long approvalId;

    @NotNull(message = "明细ID不能为空")
    @Schema(description = "明细ID（农户/设施）")
    private Long detailId;

    @NotNull(message = "记录日期不能为空")
    @Schema(description = "记录日期（施工日期）")
    private LocalDate constructionDate;

    @Size(max = 64, message = "巡检人长度不能超过64个字符")
    @Schema(description = "巡检人")
    private String inspector;

    @NotBlank(message = "巡检状态不能为空")
    @Schema(description = "巡检状态：PASS合格 FAIL整改")
    private String inspectionStatus;

    @NotBlank(message = "施工状态不能为空")
    @Schema(description = "施工状态：NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工")
    private String constructionStatus;

    @Size(max = 1000, message = "施工内容描述长度不能超过1000个字符")
    @Schema(description = "施工内容描述")
    private String content;

    @Schema(description = "施工记录附件（未传则不修改）")
    private List<ConstructionEditDTO.AttachmentItem> attachments;
}

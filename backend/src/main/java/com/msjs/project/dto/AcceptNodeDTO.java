package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目 / 申报层级验收入参
 * 一级（项目）、二级（申报）验收时，对该节点下全部申报明细批量写入验收记录
 */
@Data
@Schema(description = "项目/申报层级验收入参")
public class AcceptNodeDTO implements Serializable {

    @NotNull(message = "立项ID不能为空")
    @Schema(description = "立项ID")
    private Long approvalId;

    @NotBlank(message = "验收层级不能为空")
    @Schema(description = "验收层级：project项目 declare申报")
    private String nodeType;

    @Schema(description = "申报ID（nodeType=declare 时必填）")
    private Long declareId;

    @NotBlank(message = "验收人不能为空")
    @Schema(description = "验收人")
    private String acceptBy;

    @NotBlank(message = "验收结果不能为空")
    @Schema(description = "验收结果（字典 yan_shou_jie_guo）")
    private String acceptResult;

    @Schema(description = "验收时间（为空时取当前时间）")
    private LocalDateTime acceptTime;

    @Schema(description = "验收备注")
    private String remark;

    @Schema(description = "验收资料附件")
    private List<AcceptDetailDTO.AttachmentItem> attachments;
}

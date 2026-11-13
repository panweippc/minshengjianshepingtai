package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 编辑验收记录入参
 * 可修改验收结果、验收时间、验收人、验收备注及验收资料附件；验收状态随验收结果联动
 */
@Data
@Schema(description = "编辑验收记录入参")
public class AcceptRecordUpdateDTO implements Serializable {

    @NotNull(message = "验收记录ID不能为空")
    @Schema(description = "验收记录ID")
    private Long id;

    @NotBlank(message = "验收结果不能为空")
    @Schema(description = "验收结果（字典 yan_shou_jie_guo）")
    private String acceptResult;

    @NotNull(message = "验收时间不能为空")
    @Schema(description = "验收时间")
    private LocalDateTime acceptTime;

    @NotBlank(message = "验收人不能为空")
    @Schema(description = "验收人")
    private String acceptBy;

    @Schema(description = "验收备注")
    private String remark;

    @Schema(description = "验收资料附件（整体替换原有附件）")
    private List<AcceptDetailDTO.AttachmentItem> attachments;
}

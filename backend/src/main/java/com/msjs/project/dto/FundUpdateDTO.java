package com.msjs.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资金流水编辑入参
 * 说明：节点定位字段（approvalId/declareId/detailId/nodeLevel）以现有记录为准，前端不允许覆盖，避免破坏余额校验链路
 */
@Data
@Schema(description = "资金流水编辑入参")
public class FundUpdateDTO {

    @NotNull(message = "资金流水ID不能为空")
    @Schema(description = "资金流水ID")
    private Long id;

    @Size(max = 8, message = "资金类型长度不能超过8个字符")
    @Schema(description = "资金类型/下拨类型（一级字典 zi_jin_lei_xing，二级/三级字典 xia_bo_lei_xing）")
    private String fundType;

    @Size(max = 64, message = "资金来源长度不能超过64个字符")
    @Schema(description = "资金来源（字典 zi_jin_lai_yuan，一级资金登记必填）")
    private String fundSource;

    @DecimalMin(value = "0.01", message = "拨付金额必须大于0")
    @Schema(description = "拨付/下拨金额（元）")
    private BigDecimal amount;

    @Size(max = 512, message = "备注长度不能超过512个字符")
    @Schema(description = "拨付说明")
    private String remark;

    @Size(max = 32, message = "操作人长度不能超过32个字符")
    @Schema(description = "操作人（人工录入）")
    private String operator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "时间（人工选择）")
    private LocalDateTime operateTime;

    @Valid
    @Schema(description = "资金凭证附件（全量替换，传 null 或空列表表示不修改；若要清空附件请传空列表）")
    private List<FundAttachmentDTO> attachments;
}

package com.msjs.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资金登记/下拨入参
 * 一级节点为资金登记（上级拨付），二级/三级节点为下拨资金
 */
@Data
@Schema(description = "资金登记/下拨入参")
public class FundSaveDTO {

    @NotBlank(message = "节点类型不能为空")
    @Schema(description = "节点类型：approval一级 declare二级 detail三级")
    private String nodeType;

    @NotNull(message = "立项ID不能为空")
    @Schema(description = "立项ID")
    private Long approvalId;

    @Schema(description = "申报内容ID（二级/三级下拨时必填）")
    private Long declareId;

    @Schema(description = "申报明细ID（三级下拨时必填）")
    private Long detailId;

    @Size(max = 64, message = "资金来源长度不能超过64个字符")
    @Schema(description = "资金来源（字典 zi_jin_lai_yuan，一级资金登记必填）")
    private String fundSource;

    @Size(max = 8, message = "资金类型长度不能超过8个字符")
    @Schema(description = "资金类型/下拨类型（一级字典 zi_jin_lei_xing，二级/三级字典 xia_bo_lei_xing）")
    private String fundType;

    @NotNull(message = "拨付金额不能为空")
    @DecimalMin(value = "0.01", message = "拨付金额必须大于0")
    @Schema(description = "拨付/下拨金额（元）")
    private BigDecimal amount;

    @Size(max = 512, message = "备注长度不能超过512个字符")
    @Schema(description = "拨付说明")
    private String remark;

    @NotBlank(message = "操作人不能为空")
    @Size(max = 32, message = "操作人长度不能超过32个字符")
    @Schema(description = "操作人（人工录入）")
    private String operator;

    @NotNull(message = "时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "时间（人工选择）")
    private LocalDateTime operateTime;

    @Valid
    @Schema(description = "资金凭证附件")
    private List<FundAttachmentDTO> attachments;
}

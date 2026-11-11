package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 明细级编辑 DTO（农户明细 / 公建设施明细通用）
 * 展开行里点"编辑"按钮弹出的表单提交用
 */
@Data
@Schema(description = "申报-明细编辑 DTO（单户金额 + 单设施金额 + 明细级附件）")
public class DeclarationDetailEditDTO {

    @NotNull(message = "明细ID不能为空")
    @Schema(description = "明细ID（t_declaration_household.id 或 t_declaration_public_facility.id）")
    private Long detailId;

    @NotNull(message = "申报单ID不能为空")
    @Schema(description = "所属申报单ID")
    private Long declarationId;

    @Schema(description = "单户/单设施申报金额（元），允许为 null（表示未分配），如果填了必须 > 0")
    @DecimalMin(value = "0.00", inclusive = false, message = "金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "事项说明（该户/该设施申报原因/情况说明）")
    private String remark;

    @Schema(description = "该明细级附件列表（owner_type 自动推导：household 或 public_facility，owner_id=detailId）")
    private List<AttachmentRow> attachments;

    @Data
    @Schema(description = "附件行（动态表单一行）")
    public static class AttachmentRow {
        @Schema(description = "附件ID（编辑已有附件时传；新增时不传）")
        private Long attachmentId;
        @Schema(description = "附件类型（字典 fu_jian_lei_xing）")
        private String attachmentType;
        @Schema(description = "附件备注")
        private String remark;
        // 新增时需要文件，编辑时仅修改附件信息不需要重传文件
        @Schema(description = "附件文件（新增时必填，编辑时可忽略）")
        private String fileKey;
        @Schema(description = "原始文件名（新增时必填）")
        private String fileName;
        @Schema(description = "存储桶（新增时必填，默认 archive-formal）")
        private String bucket;
        @Schema(description = "文件大小（字节）")
        private Long fileSize;
    }
}

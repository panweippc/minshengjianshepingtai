package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 明细验收入参
 * 对指定的三级明细进行验收确认，可同时上传验收资料附件
 */
@Data
@Schema(description = "明细验收入参")
public class AcceptDetailDTO implements Serializable {

    @NotNull(message = "立项ID不能为空")
    @Schema(description = "立项ID")
    private Long approvalId;

    @NotNull(message = "明细ID不能为空")
    @Schema(description = "明细ID")
    private Long detailId;

    @NotBlank(message = "验收人不能为空")
    @Schema(description = "验收人")
    private String acceptBy;

    @Schema(description = "验收时间（yyyy-MM-dd HH:mm:ss，可由后端自动填充当前时间）")
    private LocalDateTime acceptTime;

    @NotBlank(message = "验收结果不能为空")
    @Schema(description = "验收结果（字典 yan_shou_jie_guo）")
    private String acceptResult;

    @Schema(description = "验收备注")
    private String remark;

    @Schema(description = "验收资料附件")
    private List<AttachmentItem> attachments;

    @Data
    @Schema(description = "附件项")
    public static class AttachmentItem implements Serializable {
        @Schema(description = "附件类型（字典 yan_shou_fu_jian_lei_xing）")
        private String attachmentType;
        @Schema(description = "原始文件名")
        private String fileName;
        @Schema(description = "MinIO 对象存储 key")
        private String fileKey;
        @Schema(description = "存储桶")
        private String bucket;
        @Schema(description = "文件大小（字节）")
        private Long fileSize;
        @Schema(description = "备注")
        private String remark;
    }
}

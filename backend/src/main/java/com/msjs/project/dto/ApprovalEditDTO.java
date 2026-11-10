package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 立项编辑 DTO（仅待审核状态可编辑）
 * 包含项目信息与立项资料附件
 */
@Data
@Schema(description = "立项编辑请求")
public class ApprovalEditDTO {

    @NotNull(message = "缺少主键ID")
    @Schema(description = "立项ID")
    private Long id;

    @NotBlank(message = "请输入项目名称")
    @Size(max = 255, message = "项目名称长度不超过255")
    @Schema(description = "项目名称")
    private String projectName;

    @Size(max = 8, message = "项目类别编码长度不超过8")
    @Schema(description = "项目类别（字典 xiang_mu_lei_bie）")
    private String projectCategory;

    @Size(max = 8, message = "项目来源编码长度不超过8")
    @Schema(description = "项目来源（字典 xiang_mu_lai_yuan）")
    private String projectSource;

    @Schema(description = "项目预算（元）")
    private BigDecimal budget;

    @Size(max = 32, message = "负责人长度不超过32")
    @Schema(description = "负责人")
    private String leader;

    @Size(max = 512, message = "备注长度不超过512")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "立项资料附件列表（可新增多条）")
    private List<AttachmentItem> attachments;

    /**
     * 附件子项
     */
    @Data
    @Schema(description = "立项附件子项")
    public static class AttachmentItem {

        /** 编辑时传入已有附件ID，新增附件时不传 */
        @Schema(description = "附件ID（已有附件回传原值）")
        private Long id;

        @Size(max = 8, message = "附件类型编码长度不超过8")
        @Schema(description = "附件类型（字典 li_xiang_fu_jian_lei_xing）")
        private String attachmentType;

        @Size(max = 256, message = "原始文件名长度不超过256")
        @Schema(description = "原始文件名")
        private String fileName;

        @Size(max = 512, message = "MinIO对象key长度不超过512")
        @Schema(description = "MinIO 对象存储 key")
        private String fileKey;

        @Size(max = 64, message = "存储桶名称长度不超过64")
        @Schema(description = "存储桶")
        private String bucket;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Size(max = 256, message = "附件备注长度不超过256")
        @Schema(description = "附件备注")
        private String remark;
    }
}

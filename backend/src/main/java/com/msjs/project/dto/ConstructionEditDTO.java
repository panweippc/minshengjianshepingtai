package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 施工信息编辑入参
 * 一级节点维护施工单位与工期；三级节点维护单户/单处设施的施工状态与工期
 */
@Data
@Schema(description = "施工信息编辑入参")
public class ConstructionEditDTO implements Serializable {

    @NotNull(message = "立项ID不能为空")
    @Schema(description = "立项ID")
    private Long approvalId;

    @Schema(description = "申报ID（三级节点必填，后端会按明细自动校正）")
    private Long declareId;

    @Schema(description = "明细ID（三级节点必填）")
    private Long detailId;

    @NotNull(message = "节点层级不能为空")
    @Schema(description = "节点层级：1项目 3明细")
    private Integer nodeLevel;

    @Schema(description = "施工状态：NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工（三级节点必填）")
    private String buildStatus;

    @Size(max = 128, message = "施工单位名称长度不能超过128个字符")
    @Schema(description = "施工单位名称（一级节点必填）")
    private String builder;

    @Size(max = 64, message = "承包人姓名长度不能超过64个字符")
    @Schema(description = "承包人姓名（一级节点必填）")
    private String contractorName;

    @Size(max = 32, message = "承包人联系电话长度不能超过32个字符")
    @Schema(description = "承包人联系电话（一级节点必填）")
    private String contractorPhone;

    @Size(max = 64, message = "负责人姓名长度不能超过64个字符")
    @Schema(description = "负责人姓名（一级节点必填）")
    private String leaderName;

    @Size(max = 32, message = "负责人联系电话长度不能超过32个字符")
    @Schema(description = "负责人联系电话（一级节点必填）")
    private String leaderPhone;

    @Schema(description = "施工资料附件（一级节点使用，未传则不修改）")
    private List<AttachmentItem> attachments;

    @Schema(description = "开工日期")
    private LocalDate startDate;

    @Schema(description = "完工日期")
    private LocalDate completeDate;

    @Size(max = 512, message = "备注长度不能超过512个字符")
    @Schema(description = "备注")
    private String remark;

    /**
     * 施工资料附件子项：已有附件回传ID，新增附件不带ID
     */
    @Data
    @Schema(description = "施工资料附件子项")
    public static class AttachmentItem implements Serializable {

        @Schema(description = "已有附件ID（新增时不传）")
        private Long id;

        @Schema(description = "附件类型（字典 shi_gong_fu_jian_lei_xing）")
        private String attachmentType;

        @Schema(description = "原始文件名")
        private String fileName;

        @Schema(description = "MinIO 对象存储 key")
        private String fileKey;

        @Schema(description = "存储桶")
        private String bucket;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Schema(description = "附件备注")
        private String remark;
    }
}

package com.msjs.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案查看详情（六页签数据）
 * 事项申报 / 立项 / 资金 / 施工 / 验收 / 档案
 */
@Data
@Schema(description = "档案查看详情")
public class ArchiveDetailVO implements Serializable {

    @Schema(description = "档案基础信息（档案页签）")
    private ArchiveVO archive;

    @Schema(description = "立项信息（立项页签）")
    private ApprovalVO approval;

    @Schema(description = "项目级立项资料附件（立项页签）")
    private List<ArchiveAttachmentVO> approvalAttachments;

    @Schema(description = "事项申报清单（事项申报页签）")
    private List<ArchiveDeclareVO> declares;

    @Schema(description = "资金树（资金页签：项目 → 申报 → 明细，含资金流水）")
    private FundNodeVO fund;

    @Schema(description = "施工信息（施工页签：施工单位信息 + 施工记录）")
    private ConstructionNodeVO construction;

    @Schema(description = "验收树（验收页签：项目 → 申报 → 明细，含验收记录）")
    private AcceptanceNodeVO acceptance;

    /** 事项申报节点（申报单 + 明细） */
    @Data
    @Schema(description = "档案事项申报节点")
    public static class ArchiveDeclareVO implements Serializable {

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "立项-申报关联ID")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "申报单ID")
        private Long declarationId;

        @Schema(description = "申报编号")
        private String declareCode;

        @Schema(description = "申报标题")
        private String declareName;

        @Schema(description = "申报类型（字典 shen_bao_lei_xing）")
        private String declareType;

        @Schema(description = "申报类别（字典 shen_bao_lei_bie）")
        private String declareCategory;

        @Schema(description = "申报金额（元）")
        private BigDecimal amount;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "申报日期")
        private LocalDate declarationDate;

        @Schema(description = "所属乡镇")
        private String townName;

        @Schema(description = "所属村组")
        private String villageName;

        @Schema(description = "备注说明")
        private String remark;

        @Schema(description = "申报材料附件")
        private List<ArchiveAttachmentVO> attachments;

        @Schema(description = "申报明细（农户/公共基础设施）")
        private List<ArchiveDeclareDetailVO> details;
    }

    /** 申报明细节点（农户/公共基础设施） */
    @Data
    @Schema(description = "档案申报明细节点")
    public static class ArchiveDeclareDetailVO implements Serializable {

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "明细ID（t_project_approval_detail.id）")
        private Long id;

        @Schema(description = "明细类型：HOUSEHOLD农户 FACILITY公共基础设施")
        private String detailType;

        @Schema(description = "户号/设施编码")
        private String code;

        @Schema(description = "户主/设施名称")
        private String name;

        @Schema(description = "户主身份证号（仅农户）")
        private String idCard;

        @Schema(description = "联系电话（仅农户）")
        private String phone;

        @Schema(description = "户类型（字典 hu_lei_xing）/设施类型（字典 she_shi_lei_xing）")
        private String typeValue;

        @Schema(description = "所属村组")
        private String regionName;

        @Schema(description = "事项说明")
        private String remark;

        @Schema(description = "申报金额（元）")
        private BigDecimal amount;

        @Schema(description = "明细附件")
        private List<ArchiveAttachmentVO> attachments;
    }

    /** 通用附件 */
    @Data
    @Schema(description = "档案附件")
    public static class ArchiveAttachmentVO implements Serializable {

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "附件ID")
        private Long id;

        @Schema(description = "附件类型")
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

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "上传时间")
        private LocalDateTime createTime;
    }
}

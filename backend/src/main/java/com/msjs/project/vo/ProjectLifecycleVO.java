package com.msjs.project.vo;

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
 * 农户 / 公共基础设施关联的项目全周期明细 VO
 * 一行对应一条申报明细（农户或设施），串联 申报 → 立项 → 施工 → 资金拨付 → 验收 各阶段
 */
@Data
@Schema(description = "项目全周期明细")
public class ProjectLifecycleVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项明细ID（t_project_approval_detail.id）")
    private Long detailId;

    @Schema(description = "明细类型：HOUSEHOLD农户 FACILITY公共基础设施")
    private String detailType;

    // ==================== 申报阶段 ====================

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "申报单ID")
    private Long declarationId;

    @Schema(description = "申报编号（业务编码）")
    private String declarationCode;

    @Schema(description = "申报日期")
    private LocalDate declarationDate;

    @Schema(description = "申报标题")
    private String declarationTitle;

    @Schema(description = "申报类型（字典 shen_bao_lei_xing）")
    private String declarationType;

    @Schema(description = "申报单状态：draft/submitted/approved/rejected")
    private String declarationStatus;

    @Schema(description = "明细审核状态：unsubmitted/pending/approved/established/rejected")
    private String detailAuditStatus;

    @Schema(description = "明细申报金额（元）")
    private BigDecimal amount;

    @Schema(description = "明细事项说明")
    private String remark;

    // ==================== 立项阶段 ====================

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项ID")
    private Long approvalId;

    @Schema(description = "立项编号")
    private String approvalCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目类别（字典 xiang_mu_lei_bie）")
    private String projectCategory;

    @Schema(description = "项目类型（字典 xiang_mu_lei_xing）")
    private String projectType;

    @Schema(description = "立项状态：PENDING待审核 APPROVED已通过 REJECTED已驳回")
    private String approvalStatus;

    @Schema(description = "立项提交时间")
    private LocalDateTime approvalSubmitTime;

    @Schema(description = "所属区划名称")
    private String regionName;

    // ==================== 施工阶段 ====================

    @Schema(description = "施工状态：NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工")
    private String buildStatus;

    @Schema(description = "开工日期")
    private LocalDate startDate;

    @Schema(description = "完工日期")
    private LocalDate completeDate;

    // ==================== 资金阶段 ====================

    @Schema(description = "累计拨付到该明细的金额（元）")
    private BigDecimal fundAmount;

    // ==================== 验收阶段 ====================

    @Schema(description = "验收状态：PENDING待验收 ACCEPTED已验收")
    private String acceptStatus;

    @Schema(description = "验收结果（字典 yan_shou_jie_guo）")
    private String acceptResult;

    @Schema(description = "验收时间")
    private LocalDateTime acceptTime;

    @Schema(description = "验收人")
    private String acceptBy;

    @Schema(description = "验收备注")
    private String acceptRemark;

    // ==================== 各阶段附件与记录 ====================

    @Schema(description = "申报阶段附件（申报单材料 + 该农户/设施明细材料）")
    private List<LifecycleAttachmentVO> declarationAttachments;

    @Schema(description = "申报阶段明细审核/修改记录")
    private List<DeclarationAuditLogVO> declarationAuditLogs;

    @Schema(description = "立项阶段附件（项目立项资料 + 所属申报资料）")
    private List<LifecycleAttachmentVO> approvalAttachments;

    @Schema(description = "施工记录（按明细的施工日志/巡检记录，含记录附件）")
    private List<ConstructionRecordVO> constructionRecords;

    @Schema(description = "未关联施工记录的施工资料附件（明细级）")
    private List<LifecycleAttachmentVO> constructionAttachments;

    @Schema(description = "资金拨付记录（明细级，含凭证附件）")
    private List<FundRecordVO> fundRecords;

    @Schema(description = "验收记录（明细级，含验收资料附件）")
    private List<AcceptanceRecordVO> acceptanceRecords;

    /** 阶段附件 */
    @Data
    @Schema(description = "阶段附件")
    public static class LifecycleAttachmentVO implements Serializable {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @Schema(description = "附件归属说明：申报材料/明细材料/项目资料/申报资料")
        private String scopeLabel;

        @Schema(description = "附件类型")
        private String attachmentType;

        @Schema(description = "原始文件名")
        private String fileName;

        @Schema(description = "MinIO 对象 key")
        private String fileKey;

        @Schema(description = "MinIO 桶名")
        private String bucket;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "上传时间")
        private LocalDateTime createTime;
    }

    /** 申报明细审核/修改记录 */
    @Data
    @Schema(description = "申报明细审核/修改记录")
    public static class DeclarationAuditLogVO implements Serializable {

        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @Schema(description = "动作：AUDIT_PASS/AUDIT_REJECT/AUDIT_REVOKE/SUBMIT_FIRST/SUBMIT_RESUBMITTED/USER_EDIT")
        private String action;

        @Schema(description = "操作前明细审核状态")
        private String beforeAuditStatus;

        @Schema(description = "操作后明细审核状态")
        private String afterAuditStatus;

        @Schema(description = "审核意见 / 修改说明")
        private String remark;

        @Schema(description = "操作人")
        private String operatorName;

        @Schema(description = "附件是否有变更")
        private Boolean attachmentChanged;

        @Schema(description = "操作时间")
        private LocalDateTime createTime;
    }

    /** 施工记录 */
    @Data
    @Schema(description = "施工记录")
    public static class ConstructionRecordVO implements Serializable {

        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @Schema(description = "日志编号")
        private String logCode;

        @Schema(description = "施工日期")
        private LocalDate constructionDate;

        @Schema(description = "施工状态：NOT_STARTED/IN_PROGRESS/COMPLETED")
        private String constructionStatus;

        @Schema(description = "巡检人")
        private String inspector;

        @Schema(description = "巡检结果：PASS合格 FAIL不合格")
        private String inspectionStatus;

        @Schema(description = "施工内容")
        private String content;

        @Schema(description = "创建人")
        private String createBy;

        @Schema(description = "创建时间")
        private LocalDateTime createTime;

        @Schema(description = "记录附件")
        private List<LifecycleAttachmentVO> attachments;
    }

    /** 资金拨付记录 */
    @Data
    @Schema(description = "资金拨付记录")
    public static class FundRecordVO implements Serializable {

        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @Schema(description = "下拨类型（字典 xia_bo_lei_xing）")
        private String fundType;

        @Schema(description = "拨付金额（元）")
        private BigDecimal amount;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "经办人")
        private String createBy;

        @Schema(description = "登记时间")
        private LocalDateTime createTime;

        @Schema(description = "资金凭证附件")
        private List<LifecycleAttachmentVO> attachments;
    }

    /** 验收记录 */
    @Data
    @Schema(description = "验收记录")
    public static class AcceptanceRecordVO implements Serializable {

        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @Schema(description = "验收状态：PENDING/ACCEPTED/REJECTED")
        private String acceptStatus;

        @Schema(description = "验收结果（字典 yan_shou_jie_guo）")
        private String acceptResult;

        @Schema(description = "验收时间")
        private LocalDateTime acceptTime;

        @Schema(description = "验收人")
        private String acceptBy;

        @Schema(description = "验收备注")
        private String remark;

        @Schema(description = "验收资料附件")
        private List<LifecycleAttachmentVO> attachments;
    }
}

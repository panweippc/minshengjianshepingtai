package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.msjs.project.entity.Declaration;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 申报详情 VO（含农户/公建明细 + 申报单级附件）
 * 树形：申报单 → 农户/公建 → 附件（每级可挂）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "申报详情")
public class DeclarationDetailVO extends Declaration {

    /** 申报单级附件列表（owner_type=declaration, owner_id=申报单ID） */
    @Schema(description = "申报单级附件列表")
    private List<AttachmentVO> attachments;

    /** 农户明细（declaration_category=1 时有数据） */
    @Schema(description = "农户明细列表")
    private List<HouseholdVO> households;

    /** 公建设施明细（declaration_category=2 时有数据） */
    @Schema(description = "公建设施明细列表")
    private List<FacilityVO> facilities;

    @Schema(description = "接收审核人姓名（冗余，方便前端直接展示，不查用户表）")
    private String recipientRealName;

    @Schema(description = "实际审核人姓名（冗余）")
    private String auditByRealName;

    // ---------- 内部子 VO ----------

    @Data
    @Schema(description = "申报单级附件")
    public static class AttachmentVO {
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "附件ID")
        private Long id;
        @Schema(description = "附件类型（字典 fu_jian_lei_xing）")
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
        @Schema(description = "上传时间")
        private String createTime;
    }

    @Data
    @Schema(description = "农户明细（含该农户级附件）")
    public static class HouseholdVO {
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "明细ID（t_declaration_household.id）")
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "农户档案ID（t_base_household.id）")
        private Long householdId;
        @Schema(description = "快照-户号（业务编码）")
        private String snapshotHouseholdNo;
        @Schema(description = "快照-户主姓名")
        private String snapshotHeadName;
        @Schema(description = "快照-户主身份证号")
        private String snapshotHeadIdCard;
        @Schema(description = "快照-联系电话")
        private String snapshotContactPhone;
        @Schema(description = "快照-户类型")
        private String snapshotHouseholdType;
        @Schema(description = "快照-村组名称")
        private String snapshotRegionName;
        @Schema(description = "单户申报金额（元，可编辑，主表总金额的分配）")
        private BigDecimal amount;
        @Schema(description = "事项说明（该户申报原因/情况说明，可编辑）")
        private String remark;
        @Schema(description = "明细审核状态：pending 待审核 / approved 已通过 / rejected 已驳回")
        private String auditStatus;
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "明细实际审核人（雪花ID）")
        private Long auditBy;
        @Schema(description = "明细实际审核人姓名（冗余，前端直接展示，不查用户表）")
        private String auditByName;
        @Schema(description = "明细审核时间")
        private String auditTime;
        @Schema(description = "明细审核意见")
        private String auditRemark;
        @Schema(description = "明细级附件数量（owner_type=household）")
        private Integer attachmentCount;
        @Schema(description = "明细级附件完整列表（owner_type=household, owner_id=明细ID）")
        private List<AttachmentVO> attachments;
    }

    @Data
    @Schema(description = "公建设施明细（含该设施级附件）")
    public static class FacilityVO {
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "明细ID（t_declaration_public_facility.id）")
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "设施档案ID（t_base_facility.id）")
        private Long facilityId;
        @Schema(description = "快照-设施编号")
        private String snapshotFacilityCode;
        @Schema(description = "快照-设施名称")
        private String snapshotFacilityName;
        @Schema(description = "快照-设施类型")
        private String snapshotFacilityType;
        @Schema(description = "快照-具体位置")
        private String snapshotLocation;
        @Schema(description = "快照-村组名称")
        private String snapshotRegionName;
        @Schema(description = "单设施申报金额（元，可编辑，主表总金额的分配）")
        private BigDecimal amount;
        @Schema(description = "事项说明（该设施申报原因/情况说明，可编辑）")
        private String remark;
        @Schema(description = "明细审核状态：pending 待审核 / approved 已通过 / rejected 已驳回")
        private String auditStatus;
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "明细实际审核人（雪花ID）")
        private Long auditBy;
        @Schema(description = "明细实际审核人姓名（冗余，前端直接展示，不查用户表）")
        private String auditByName;
        @Schema(description = "明细审核时间")
        private String auditTime;
        @Schema(description = "明细审核意见")
        private String auditRemark;
        @Schema(description = "设施级附件数量（owner_type=public_facility）")
        private Integer attachmentCount;
        @Schema(description = "设施级附件完整列表（owner_type=public_facility, owner_id=明细ID）")
        private List<AttachmentVO> attachments;
    }
}

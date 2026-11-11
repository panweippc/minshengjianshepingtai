package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申报-农户明细表（仅 declaration_category=1 时有数据）
 * 存快照，防止后续农户档案变更影响申报
 */
@Data
@TableName("t_declaration_household")
@Schema(description = "申报-农户明细")
public class DeclarationHousehold implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "申报单ID（关联 t_declaration.id）")
    private Long declarationId;

    @Schema(description = "农户档案ID（关联 t_base_household.id）")
    private Long householdId;

    @Schema(description = "快照-户号（业务编码，来源于 t_base_household.household_no）")
    private String snapshotHouseholdNo;

    @Schema(description = "快照-户主姓名")
    private String snapshotHeadName;

    @Schema(description = "快照-户主身份证号")
    private String snapshotHeadIdCard;

    @Schema(description = "快照-联系电话")
    private String snapshotContactPhone;

    @Schema(description = "快照-户类型（字典 hu_lei_xing）")
    private String snapshotHouseholdType;

    @Schema(description = "快照-所属村组区划编码")
    private String snapshotRegionCode;

    @Schema(description = "快照-所属村组名称")
    private String snapshotRegionName;

    @Schema(description = "单户申报金额（元），可在明细展开后编辑；主表 amount 为总金额，一般等于所有明细 amount 之和")
    private BigDecimal amount;

    @Schema(description = "事项说明（该户申报原因/情况说明，可在明细展开后编辑）")
    private String remark;

    @Schema(description = "明细审核状态：pending 待审核 / approved 已通过 / rejected 已驳回")
    private String auditStatus;

    @Schema(description = "明细实际审核人（雪花ID）")
    private Long auditBy;

    @Schema(description = "明细审核时间")
    private java.time.LocalDateTime auditTime;

    @Schema(description = "明细审核意见（驳回通常必填）")
    private String auditRemark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableLogic
    @Schema(description = "逻辑删除：0否 1是")
    private Integer deleted;
}

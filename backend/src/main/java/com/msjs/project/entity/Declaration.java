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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 申报主表（到户/公建共用，declaration_category 区分）
 * 业务编码 business_code 保存时自动生成，全局唯一，作废编码不回收复用
 */
@Data
@TableName("t_declaration")
@Schema(description = "申报主表")
public class Declaration implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "申报编号（业务编码，保存时自动生成，全局唯一）")
    private String businessCode;

    @Schema(description = "申报日期（业务字段，实际申报时点，精确到天）")
    private LocalDate declarationDate;

    @Schema(description = "申报标题")
    private String title;

    @Schema(description = "所属乡镇区划编码（登录人自动回填，只读）")
    private String townCode;

    @Schema(description = "所属乡镇名称（冗余）")
    private String townName;

    @Schema(description = "所属村组区划编码（下拉选择，乡镇下的村组）")
    private String villageGroupCode;

    @Schema(description = "所属村组名称（冗余）")
    private String villageGroupName;

    @Schema(description = "申报类型（字典 shen_bao_lei_xing）")
    private String declarationType;

    @Schema(description = "申报类别（字典 shen_bao_lei_bie：household到户项目 / public公建项目）")
    private String declarationCategory;

    @Schema(description = "申报金额（元，申报单总金额）")
    private BigDecimal amount;

    @Schema(description = "备注说明（申报原因、情况说明）")
    private String remark;

    @Schema(description = "申报状态（draft草稿 / submitted已提交 / approved已归档 / rejected已驳回）")
    private String status;

    @Schema(description = "接收审核人（雪花ID，提交时指定，submitted 状态必填）")
    private Long recipientUserId;

    @Schema(description = "实际审核人（雪花ID）")
    private Long auditBy;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核意见（驳回时通常必填）")
    private String auditRemark;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除：0否 1是")
    private Integer deleted;
}

package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 施工管理-施工进度实体
 * 一级节点存储施工单位/工期/备注；三级节点存储每户/每处设施的施工状态；
 * 二级申报节点不落库，其施工进度由三级明细实时聚合
 */
@Data
@TableName("t_project_construction")
@Schema(description = "施工管理-施工进度")
public class ProjectConstruction implements Serializable {

    /** 雪花ID超出JS安全整数范围（2^53-1），序列化为字符串，避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项ID（关联 t_project_approval.id）")
    private Long approvalId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "申报ID（关联 t_project_approval_declare.id，三级节点必填）")
    private Long declareId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "明细ID（关联 t_project_approval_detail.id，三级节点必填）")
    private Long detailId;

    @Schema(description = "节点层级：1项目 3明细")
    private Integer nodeLevel;

    @Schema(description = "施工状态：NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工（三级节点使用）")
    private String buildStatus;

    /** 施工队信息允许清空，更新时即使为 null 也写入数据库 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "施工单位（一级节点使用，即施工队名称）")
    private String builder;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "承包人姓名")
    private String contractorName;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "承包人联系电话")
    private String contractorPhone;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "负责人姓名")
    private String leaderName;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "负责人联系电话")
    private String leaderPhone;

    @Schema(description = "开工日期")
    private LocalDate startDate;

    @Schema(description = "完工日期")
    private LocalDate completeDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除：0正常 1删除")
    private Integer deleted;
}

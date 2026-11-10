package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 立项管理-项目信息实体（树形一级节点）
 * 数据来源：事项申报管理新增立项时生成，立项管理负责审核流转
 */
@Data
@TableName("t_project_approval")
@Schema(description = "立项管理-项目信息")
public class ProjectApproval implements Serializable {

    /** 雪花ID超出JS安全整数范围（2^53-1），序列化为字符串，避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "项目编号（业务编码，全局唯一）")
    private String approvalCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目类别（字典 xiang_mu_lei_bie）")
    private String projectCategory;

    @Schema(description = "项目来源（字典 xiang_mu_lai_yuan）")
    private String projectSource;

    @Schema(description = "项目预算（元）")
    private BigDecimal budget;

    @Schema(description = "负责人")
    private String leader;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "审核状态：PENDING待审核 APPROVED已通过 REJECTED已驳回")
    private String status;

    @Schema(description = "所属区划编码")
    private String regionCode;

    @Schema(description = "所属区划名称")
    private String regionName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除：0否 1是")
    private Integer deleted;

    /** 树形二级节点：申报内容（非数据库字段，仅用于树形表格组装） */
    @TableField(exist = false)
    @Schema(description = "申报内容列表（树形二级）")
    private List<ProjectApprovalDeclare> children;
}

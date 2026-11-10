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
 * 立项管理-申报内容实体（树形二级节点）
 */
@Data
@TableName("t_project_approval_declare")
@Schema(description = "立项管理-申报内容")
public class ProjectApprovalDeclare implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项ID（关联 t_project_approval.id）")
    private Long approvalId;

    @Schema(description = "申报编号（业务编码）")
    private String declareCode;

    @Schema(description = "申报事项名称")
    private String declareName;

    @Schema(description = "申报类别（字典 shen_bao_lei_bie）")
    private String declareCategory;

    @Schema(description = "申报类型（字典 shen_bao_lei_xing）")
    private String declareType;

    @Schema(description = "所属乡镇名称")
    private String townName;

    @Schema(description = "所属村组名称")
    private String villageName;

    @Schema(description = "申报内容")
    private String declareContent;

    @Schema(description = "申报金额（元）")
    private BigDecimal budget;

    @Schema(description = "排序")
    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableLogic
    @Schema(description = "逻辑删除：0否 1是")
    private Integer deleted;

    /** 树形三级节点：农户/公共基础设施明细（非数据库字段） */
    @TableField(exist = false)
    @Schema(description = "申报明细列表（树形三级）")
    private List<ProjectApprovalDetail> children;

    /** 申报材料附件（非数据库字段，declare_id 指向本节点的附件） */
    @TableField(exist = false)
    @Schema(description = "申报材料附件列表")
    private List<ProjectApprovalAttachment> attachments;
}

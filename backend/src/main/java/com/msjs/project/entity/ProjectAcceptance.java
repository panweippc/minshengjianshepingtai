package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 验收管理-明细验收记录实体
 * 每条立项明细（t_project_approval_detail）对应一条验收记录，
 * 验收进度由明细级 accept_status 聚合计算得出
 */
@Data
@TableName("t_project_acceptance")
@Schema(description = "验收管理-明细验收记录")
public class ProjectAcceptance implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项ID（关联 t_project_approval.id）")
    private Long approvalId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "申报ID（关联 t_project_approval_declare.id）")
    private Long declareId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "明细ID（关联 t_project_approval_detail.id）")
    private Long detailId;

    @Schema(description = "验收状态：PENDING待验收 ACCEPTED已验收")
    private String acceptStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "验收时间")
    private LocalDateTime acceptTime;

    @Schema(description = "验收人")
    private String acceptBy;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除：0正常 1删除")
    private Integer deleted;

    /** 验收资料附件（非数据库字段，查询时回填） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    @Schema(description = "验收资料附件")
    private List<ProjectAcceptanceAttachment> attachments;
}

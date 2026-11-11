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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 施工管理-施工日志/巡检记录实体
 */
@Data
@TableName("t_project_construction_log")
@Schema(description = "施工管理-施工日志/巡检记录")
public class ProjectConstructionLog implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项ID（关联 t_project_approval.id）")
    private Long approvalId;

    @Schema(description = "日志编号")
    private String logCode;

    @Schema(description = "施工日期")
    private LocalDate constructionDate;

    @Schema(description = "巡检人")
    private String inspector;

    @Schema(description = "巡检状态：PASS合格 FAIL不合格")
    private String inspectionStatus;

    @Schema(description = "施工状态：NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工")
    private String constructionStatus;

    @Schema(description = "施工内容")
    private String content;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableLogic
    @Schema(description = "逻辑删除：0正常 1删除")
    private Integer deleted;
}

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
 * 物资统计报表主表
 * 支持三种报表类型：盘点表(INVENTORY)、交接表(HANDOVER)、损毁表(DAMAGE)
 * 一张报表包含多条物品明细，主表与明细表同事务写入
 */
@Data
@TableName("t_material_report")
@Schema(description = "物资统计报表主表")
public class MaterialReport implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "报表名称")
    private String reportName;

    @Schema(description = "报表类型：INVENTORY-盘点表，HANDOVER-交接表，DAMAGE-损毁表")
    private String reportType;

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "经办人")
    private String handler;

    @Schema(description = "交接人（仅交接表使用）")
    private String handoverPerson;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "部门")
    private String department;

    @Schema(description = "报表备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer deleted;
}

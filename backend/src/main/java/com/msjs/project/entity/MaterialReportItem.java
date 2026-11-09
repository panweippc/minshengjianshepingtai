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
import java.time.LocalDateTime;

/**
 * 物资统计报表明细表
 * 统一存储盘点表、交接表、损毁表三种类型的明细数据，通过主表 report_type 区分
 * 盘点表/交接表使用 bookQty、actualQty、profitQty；损毁表使用 bookQty、handler
 */
@Data
@TableName("t_material_report_item")
@Schema(description = "物资统计报表明细表")
public class MaterialReportItem implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属报表ID")
    private Long reportId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "物品名称")
    private String itemName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "账面数量")
    private Integer bookQty;

    @Schema(description = "盘点数量（仅盘点表/交接表使用）")
    private Integer actualQty;

    @Schema(description = "盘盈数量（仅盘点表/交接表使用，= actualQty - bookQty）")
    private Integer profitQty;

    @Schema(description = "明细经办人（损毁表专用，覆盖主表经办人）")
    private String handler;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer deleted;
}

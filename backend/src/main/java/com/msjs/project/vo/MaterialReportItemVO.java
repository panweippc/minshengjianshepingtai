package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统计报表明细记录（盘点表/交接表/损毁表通用）
 */
@Data
@Schema(description = "统计报表明细记录")
public class MaterialReportItemVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "明细ID")
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

    @Schema(description = "盘点数量（盘点表/交接表使用）")
    private Integer actualQty;

    @Schema(description = "盘盈数量（盘点表/交接表使用）")
    private Integer profitQty;

    @Schema(description = "明细经办人（损毁表使用）")
    private String handler;

    @Schema(description = "备注")
    private String remark;
}

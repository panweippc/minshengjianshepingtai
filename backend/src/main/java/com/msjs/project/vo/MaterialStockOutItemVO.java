package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 出库单明细记录（明细页签列表 / 出库单查看与编辑）
 * 已聚合所属出库单名称、经办人，便于明细页签直接展示与导出
 */
@Data
@Schema(description = "出库单明细记录")
public class MaterialStockOutItemVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "明细ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属出库单ID")
    private Long orderId;

    @Schema(description = "出库单名称")
    private String orderName;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "存储库房")
    private String warehouseName;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "物品ID")
    private Long itemId;

    @Schema(description = "物品名称")
    private String itemName;

    @Schema(description = "物品分类（字典 wu_pin_fen_lei）")
    private String itemCategory;

    @Schema(description = "出库数量")
    private Integer quantity;

    @Schema(description = "出库类型（字典 chu_ku_lei_xing）")
    private String outType;

    @Schema(description = "出库日期")
    private LocalDate outTime;

    @Schema(description = "经办人")
    private String handler;

    @Schema(description = "备注")
    private String remark;
}

package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 入库单明细记录（明细页签列表 / 入库单查看与编辑）
 * 已聚合所属入库单名称、经办人，便于明细页签直接展示与导出
 */
@Data
@Schema(description = "入库单明细记录")
public class MaterialStockInItemVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "明细ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属入库单ID")
    private Long orderId;

    @Schema(description = "入库单名称")
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

    @Schema(description = "入库数量")
    private Integer quantity;

    @Schema(description = "入库类型（字典 ru_ku_lei_xing）")
    private String inType;

    @Schema(description = "入库日期")
    private LocalDate inTime;

    @Schema(description = "经办人")
    private String handler;

    @Schema(description = "备注")
    private String remark;
}

package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 物品库房库存 VO（按物品+库房汇总入库减出库）
 */
@Data
@Schema(description = "物品库房库存")
public class MaterialStockVO {

    @Schema(description = "库房名称")
    private String warehouseName;

    @Schema(description = "物品名称")
    private String itemName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "账面数量（入库合计-出库合计）")
    private Integer bookQty;
}

package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据大屏物资状态
 */
@Data
@Schema(description = "物资状态项")
public class ScreenMaterialVO {

    @Schema(description = "物品名称")
    private String itemName;

    @Schema(description = "库存数量")
    private Integer stockQty;

    @Schema(description = "预警数量")
    private Integer warningQty;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "是否预警：true库存低于预警值")
    private Boolean warning;
}

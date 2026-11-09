package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 出库单明细保存入参
 */
@Data
@Schema(description = "出库单明细保存入参")
public class MaterialStockOutItemSaveDTO {

    @NotNull(message = "请选择仓库")
    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "物品ID（从物品库选择时传递）")
    private Long itemId;

    @NotBlank(message = "请输入物品名称")
    @Schema(description = "物品名称")
    private String itemName;

    @Schema(description = "物品分类（字典 wu_pin_fen_lei）")
    private String itemCategory;

    @NotNull(message = "请输入出库数量")
    @Min(value = 1, message = "出库数量必须大于 0")
    @Schema(description = "出库数量")
    private Integer quantity;

    @NotBlank(message = "请选择出库类型")
    @Schema(description = "出库类型（字典 chu_ku_lei_xing）")
    private String outType;

    @NotNull(message = "请选择出库时间")
    @Schema(description = "出库时间")
    private LocalDate outTime;

    @Schema(description = "经办人")
    private String handler;

    @Schema(description = "备注")
    private String remark;
}

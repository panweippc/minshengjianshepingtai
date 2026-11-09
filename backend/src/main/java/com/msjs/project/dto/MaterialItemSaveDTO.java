package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 物品信息新增/编辑入参
 */
@Data
@Schema(description = "物品信息保存入参")
public class MaterialItemSaveDTO {

    @Schema(description = "物品ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "物品名称不能为空")
    @Schema(description = "物品名称")
    private String itemName;

    @Schema(description = "物品分类（字典 wu_pin_fen_lei）")
    private String itemCategory;

    @NotNull(message = "预警数量不能为空")
    @Min(value = 0, message = "预警数量不能为负数")
    @Schema(description = "预警数量")
    private Integer warningQty;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "备注")
    private String remark;
}

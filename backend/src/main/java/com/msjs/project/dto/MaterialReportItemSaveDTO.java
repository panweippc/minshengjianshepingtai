package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 统计报表明细统一保存入参
 * 盘点表/交接表使用 bookQty、actualQty；损毁表使用 bookQty、handler
 */
@Data
@Schema(description = "统计报表明细保存入参")
public class MaterialReportItemSaveDTO {

    @Schema(description = "明细ID（编辑时传，新增不传）")
    private Long id;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @NotBlank(message = "请输入物品名称")
    @Schema(description = "物品名称")
    private String itemName;

    @Schema(description = "单位")
    private String unit;

    @NotNull(message = "请输入账面数量")
    @Min(value = 0, message = "账面数量不能小于 0")
    @Schema(description = "账面数量")
    private Integer bookQty;

    @Schema(description = "盘点数量（盘点表/交接表使用）")
    private Integer actualQty;

    @Schema(description = "明细经办人（损毁表使用，覆盖主表经办人）")
    private String handler;

    @Schema(description = "备注")
    private String remark;
}

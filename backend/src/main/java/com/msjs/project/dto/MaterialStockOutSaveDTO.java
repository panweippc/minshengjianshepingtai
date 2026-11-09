package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 出库单新增/编辑入参
 * 出库单与明细同事务保存，保证元数据一致性
 */
@Data
@Schema(description = "出库单保存入参")
public class MaterialStockOutSaveDTO {

    @Schema(description = "出库单ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "请输入出库单名称")
    @Schema(description = "出库单名称")
    private String orderName;

    @Schema(description = "备注")
    private String remark;

    @NotEmpty(message = "请至少添加一条物品信息")
    @Valid
    @Schema(description = "物品明细集合")
    private List<MaterialStockOutItemSaveDTO> items;
}

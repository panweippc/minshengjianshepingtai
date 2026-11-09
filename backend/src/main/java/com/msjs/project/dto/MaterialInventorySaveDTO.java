package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 盘点表新增/编辑入参
 */
@Data
@Schema(description = "盘点表保存入参")
public class MaterialInventorySaveDTO {

    @Schema(description = "盘点表ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "请输入报表名称")
    @Schema(description = "报表名称")
    private String reportName;

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "经办人")
    private String handler;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "部门")
    private String department;

    @NotEmpty(message = "请至少添加一条明细")
    @Valid
    @Schema(description = "盘点明细集合")
    private List<MaterialReportItemSaveDTO> items;
}

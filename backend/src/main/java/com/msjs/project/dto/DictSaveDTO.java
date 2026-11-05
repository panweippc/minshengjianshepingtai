package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典新增/编辑入参
 */
@Data
@Schema(description = "字典保存入参")
public class DictSaveDTO {

    @Schema(description = "字典ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "字典类型不能为空")
    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "字典描述")
    private String description;

    @NotBlank(message = "字典标签不能为空")
    @Schema(description = "字典标签")
    private String dictLabel;

    @NotBlank(message = "字典键值不能为空")
    @Schema(description = "字典键值")
    private String dictValue;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}

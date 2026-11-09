package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 库房信息新增/编辑入参
 */
@Data
@Schema(description = "库房信息保存入参")
public class MaterialWarehouseSaveDTO {

    @Schema(description = "库房ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "库房名称不能为空")
    @Size(max = 128, message = "库房名称不能超过128个字符")
    @Schema(description = "库房名称")
    private String warehouseName;

    @Size(max = 255, message = "库房位置不能超过255个字符")
    @Schema(description = "库房位置")
    private String location;

    @Size(max = 32, message = "负责人不能超过32个字符")
    @Schema(description = "负责人")
    private String manager;

    @Size(max = 20, message = "联系电话不能超过20个字符")
    @Schema(description = "联系电话")
    private String phone;
}

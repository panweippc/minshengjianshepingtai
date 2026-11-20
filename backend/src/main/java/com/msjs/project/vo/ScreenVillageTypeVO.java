package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据大屏村庄项目类型计数（村 + 项目类型 + 项目数）
 */
@Data
@Schema(description = "村庄项目类型计数")
public class ScreenVillageTypeVO {

    @Schema(description = "行政村名称")
    private String villageName;

    @Schema(description = "项目类型编码（字典 xiang_mu_lei_xing）")
    private String projectType;

    @Schema(description = "项目数")
    private Long projectCount;
}

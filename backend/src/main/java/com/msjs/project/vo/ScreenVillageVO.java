package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据大屏村域态势：行政村项目数与农户数
 */
@Data
@Schema(description = "村域态势数据")
public class ScreenVillageVO {

    @Schema(description = "行政村名称")
    private String name;

    @Schema(description = "关联项目数")
    private Long projectCount;

    @Schema(description = "农户数")
    private Long householdCount;
}

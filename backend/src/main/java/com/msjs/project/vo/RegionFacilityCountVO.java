package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 区划设施数量统计（左侧区划树徽标展示）
 */
@Data
@Schema(description = "区划设施数量统计")
public class RegionFacilityCountVO {

    @Schema(description = "区划代码")
    private String regionCode;

    @Schema(description = "设施数量")
    private Long count;
}

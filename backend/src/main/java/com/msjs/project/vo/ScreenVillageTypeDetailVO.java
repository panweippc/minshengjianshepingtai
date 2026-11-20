package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据大屏各村项目类型明细数原始统计行
 * 统计口径：仅立项状态为 APPROVED 的申报单，分别统计农户明细数与公建设施明细数
 */
@Data
@Schema(description = "各村项目类型明细数原始统计行")
public class ScreenVillageTypeDetailVO {

    @Schema(description = "行政村名称")
    private String villageName;

    @Schema(description = "项目类型字典值（1常规项目/2应急抢险/3灾后重建）")
    private String projectType;

    @Schema(description = "该村该项目类型下的农户明细数（单位：户）")
    private Long householdCount;

    @Schema(description = "该村该项目类型下的公建设施明细数（单位：处）")
    private Long facilityCount;
}

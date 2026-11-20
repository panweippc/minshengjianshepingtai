package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 数据大屏各村项目类型统计（横轴村庄 + 各类型系列）
 */
@Data
@Schema(description = "各村项目类型统计")
public class ScreenTypeStatsVO {

    @Schema(description = "横轴村庄（按项目数倒序取前6个）")
    private List<String> villages;

    @Schema(description = "项目类型系列（常规项目/应急抢险/灾后重建）")
    private List<ScreenTypeSeriesVO> series;
}

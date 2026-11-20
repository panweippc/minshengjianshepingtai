package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 数据大屏聚合数据
 */
@Data
@Schema(description = "数据大屏聚合数据")
public class ScreenOverviewVO {

    @Schema(description = "核心指标")
    private ScreenKpiVO kpi;

    @Schema(description = "资金拨付趋势（近7个自然月）")
    private List<ScreenFundTrendVO> fundTrend;

    @Schema(description = "村域态势（行政村项目数与农户数）")
    private List<ScreenVillageVO> villageStats;

    @Schema(description = "各村项目类型分布（横轴村庄，系列为三类项目类型）")
    private ScreenTypeStatsVO typeStats;

    @Schema(description = "各村项目类型明细统计（横轴村庄，系列为 项目类型×农户/公建；数值分别为户数/处数；仅立项已通过）")
    private ScreenTypeDetailStatsVO villageTypeDetailStats;

    @Schema(description = "项目状态分布")
    private List<ScreenChartItemVO> statusStats;

    @Schema(description = "预算投入排名")
    private List<ScreenBudgetRankVO> budgetRank;

    @Schema(description = "物资状态")
    private List<ScreenMaterialVO> materialStats;

    @Schema(description = "实时事件")
    private List<ScreenEventVO> events;

    @Schema(description = "数据刷新时间（yyyy-MM-dd HH:mm:ss）")
    private String updateTime;
}

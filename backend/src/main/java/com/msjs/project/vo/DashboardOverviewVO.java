package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 首页概览统计（指标卡片 + 图表）
 */
@Data
@Schema(description = "首页概览统计")
public class DashboardOverviewVO {

    @Schema(description = "行政村总数")
    private Long villageCount;

    @Schema(description = "农户总数")
    private Long householdCount;

    @Schema(description = "公共基础设施总数")
    private Long facilityCount;

    @Schema(description = "在库物资总数")
    private Long materialStockTotal;

    @Schema(description = "项目进度概览（一个项目一条记录）")
    private List<ProjectProgressVO> projectProgress;
}

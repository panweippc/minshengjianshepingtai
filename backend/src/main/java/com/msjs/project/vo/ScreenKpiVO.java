package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据大屏核心指标
 */
@Data
@Schema(description = "数据大屏核心指标")
public class ScreenKpiVO {

    @Schema(description = "项目总预算（元）")
    private BigDecimal totalBudget;

    @Schema(description = "已拨付资金（元，一级拨付登记合计）")
    private BigDecimal paidAmount;

    @Schema(description = "拨付完成率（百分比，保留1位小数）")
    private BigDecimal payRate;

    @Schema(description = "立项项目总数")
    private Long projectCount;

    @Schema(description = "在管项目数（已通过且未归档）")
    private Long buildingCount;

    @Schema(description = "在建项目数（已进入施工管理且未归档）")
    private Long constructionCount;

    @Schema(description = "行政村总数")
    private Long villageCount;

    @Schema(description = "农户总数")
    private Long householdCount;

    @Schema(description = "公共基础设施总数")
    private Long facilityCount;

    @Schema(description = "在库物资总数")
    private Long materialTotal;

    @Schema(description = "物资预警数量（库存低于预警值）")
    private Long materialWarnCount;

    @Schema(description = "申报受理总数")
    private Long declarationCount;
}

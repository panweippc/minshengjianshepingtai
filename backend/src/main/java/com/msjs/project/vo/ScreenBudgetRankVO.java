package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据大屏预算投入排名
 */
@Data
@Schema(description = "预算投入排名项")
public class ScreenBudgetRankVO {

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目预算（元）")
    private BigDecimal budget;

    @Schema(description = "占首位预算的比例（百分比，保留1位小数）")
    private BigDecimal ratio;
}

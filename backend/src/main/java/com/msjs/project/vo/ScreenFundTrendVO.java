package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据大屏资金拨付趋势（按自然月）
 */
@Data
@Schema(description = "资金拨付趋势月度数据")
public class ScreenFundTrendVO {

    @Schema(description = "月份（yyyy-MM）")
    private String month;

    @Schema(description = "当月立项预算（元）")
    private BigDecimal budget;

    @Schema(description = "当月拨付金额（元）")
    private BigDecimal paid;
}

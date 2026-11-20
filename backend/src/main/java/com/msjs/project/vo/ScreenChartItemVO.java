package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据大屏通用图表项（名称 + 数值）
 * 用于项目状态分布、各村项目数等计数类图表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据大屏通用图表项")
public class ScreenChartItemVO {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "数值")
    private Long value;
}

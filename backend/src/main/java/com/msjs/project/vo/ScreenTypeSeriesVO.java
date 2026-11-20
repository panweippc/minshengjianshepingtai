package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据大屏项目类型柱状图系列（类型名称 + 各村项目数）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目类型柱状图系列")
public class ScreenTypeSeriesVO {

    @Schema(description = "项目类型名称")
    private String name;

    @Schema(description = "各村项目数（与 villages 顺序一致）")
    private List<Long> data;
}

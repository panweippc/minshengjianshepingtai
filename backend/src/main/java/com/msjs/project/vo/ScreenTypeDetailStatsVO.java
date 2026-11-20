package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 数据大屏各村项目类型明细统计：横轴村庄 + 系列（项目类型 × 农户/公建）
 */
@Data
@Schema(description = "各村项目类型明细统计")
public class ScreenTypeDetailStatsVO {

    @Schema(description = "横轴村庄（按明细总数倒序取前8个）")
    private List<String> villages;

    @Schema(description = "系列：每个项目类型拆分为农户/公建两条，顺序为 类型1农户,类型1公建,类型2农户,类型2公建...")
    private List<ScreenTypeDetailSeriesVO> series;
}

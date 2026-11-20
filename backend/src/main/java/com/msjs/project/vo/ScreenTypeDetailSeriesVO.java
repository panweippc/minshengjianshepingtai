package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 数据大屏各村项目类型明细统计系列
 * 每个项目类型拆分为「农户明细」与「公建设施明细」两条系列：
 * 农户明细数值单位为「户」，公建设施明细数值单位为「处」。
 */
@Data
@Schema(description = "各村项目类型明细统计系列")
public class ScreenTypeDetailSeriesVO {

    @Schema(description = "系列名称，如 常规项目·农户")
    private String name;

    @Schema(description = "各村数值（与 villages 顺序严格对齐）")
    private List<Long> data;

    @Schema(description = "明细类别：household=农户明细(单位户)，facility=公建设施明细(单位处)")
    private String category;
}

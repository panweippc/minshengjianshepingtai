package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 综合查询-项目全周期 查询参数
 */
@Data
@Schema(description = "综合查询-项目全周期 查询参数")
public class ProjectCycleQueryDTO implements Serializable {

    @Schema(description = "年度（空=全部）")
    private String year;

    @Schema(description = "区划 code（末级，递归包含后代）")
    private String regionCode;

    /**
     * 查询方案类型：
     * category   - 按项目类别统计（declareCategory: household到户 / public公建）
     * source     - 按项目来源统计（projectSource）
     * establish  - 按立项状态统计（仅统计涉及户数、公建设施数）
     * status     - 按项目状态统计（综合状态：未开工/施工中/已完工/待验收/已验收/未通过）
     * village    - 按所属村组统计（项目ID去重）
     */
    @Schema(description = "查询方案：category/source/establish/status/village")
    private String queryType;
}

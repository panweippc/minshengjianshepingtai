package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前登录人区划信息（弹窗初始化用）
 * 后端已递归找到乡镇并返回，前端直接回填
 */
@Data
@Schema(description = "当前登录人区划上下文")
public class UserRegionVO {

    @Schema(description = "登录人所在区划编码（可能是乡镇/村/组）")
    private String currentRegionCode;

    @Schema(description = "登录人所在区划名称")
    private String currentRegionName;

    @Schema(description = "登录人所在区划类型（字典 qu_hua_lei_xing：1乡镇 2村 3组）")
    private String currentRegionType;

    @Schema(description = "登录人所在区划层级（4乡镇 5村 6组）")
    private Integer currentRegionLevel;

    @Schema(description = "所属乡镇区划编码（向上递归的第一个乡镇）")
    private String townCode;

    @Schema(description = "所属乡镇名称")
    private String townName;
}

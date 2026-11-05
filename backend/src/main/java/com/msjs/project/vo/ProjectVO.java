package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目VO（简化版）
 */
@Data
@Schema(description = "项目")
public class ProjectVO implements Serializable {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "项目编码")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "进度（百分比）")
    private BigDecimal progress;

    @Schema(description = "预算（元）")
    private BigDecimal budget;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "立项年度")
    private Integer startYear;

    @Schema(description = "所属区划代码")
    private String regionCode;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

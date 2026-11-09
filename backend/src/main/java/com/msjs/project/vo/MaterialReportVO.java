package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 统计报表主记录（列表页展示）
 */
@Data
@Schema(description = "统计报表主记录")
public class MaterialReportVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "报表ID")
    private Long id;

    @Schema(description = "报表名称")
    private String reportName;

    @Schema(description = "报表类型：INVENTORY-盘点表，HANDOVER-交接表，DAMAGE-损毁表")
    private String reportType;

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "经办人")
    private String handler;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

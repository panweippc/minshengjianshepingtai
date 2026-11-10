package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计报表详情（查看 / 编辑回显）
 */
@Data
@Schema(description = "统计报表详情")
public class MaterialReportDetailVO implements Serializable {

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

    @Schema(description = "交接人（仅交接表）")
    private String handoverPerson;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "部门")
    private String department;

    @Schema(description = "报表备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "明细集合")
    private List<MaterialReportItemVO> items;
}

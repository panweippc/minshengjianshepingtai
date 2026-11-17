package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 综合查询-项目全周期 明细核对行
 *
 * <p>以 declare（申报单）为粒度，用于统计表右键"查看明细"核对数据。</p>
 */
@Data
@Schema(description = "项目全周期统计明细")
public class ProjectCycleDetailVO implements Serializable {

    @Schema(description = "立项单ID")
    private Long approvalId;

    @Schema(description = "立项编号")
    private String approvalCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "申报单ID")
    private Long declareId;

    @Schema(description = "项目类别（翻译后）")
    private String categoryLabel;

    @Schema(description = "项目类型（翻译后）")
    private String typeLabel;

    @Schema(description = "项目来源")
    private String projectSource;

    @Schema(description = "村组")
    private String villageName;

    @Schema(description = "项目预算（元）")
    private BigDecimal budget;

    @Schema(description = "涉及户数")
    private Integer householdCount;

    @Schema(description = "公建设施数")
    private Integer facilityCount;

    @Schema(description = "累计拨付（元）")
    private BigDecimal totalFund;

    @Schema(description = "立项状态（待立项/已立项/已归档）")
    private String establishStatusLabel;

    @Schema(description = "项目状态（未开工/施工中/已完工/待验收/已验收/未通过）")
    private String projectStatusLabel;

    @Schema(description = "立项日期")
    private LocalDateTime submitTime;

    @Schema(description = "归档时间")
    private LocalDateTime archiveCreateTime;
}

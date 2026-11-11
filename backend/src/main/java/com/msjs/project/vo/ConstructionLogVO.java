package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 施工日志/巡检记录VO
 */
@Data
@Schema(description = "施工日志/巡检记录")
public class ConstructionLogVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "日志ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "明细ID（三级施工记录）")
    private Long detailId;

    @Schema(description = "关联项目名称（三级施工记录展示）")
    private String projectName;

    @Schema(description = "农户/设施名称（三级施工记录展示）")
    private String detailName;

    @Schema(description = "日志编号")
    private String logCode;

    @Schema(description = "施工日期")
    private LocalDate constructionDate;

    @Schema(description = "巡检人")
    private String inspector;

    @Schema(description = "巡检状态：PASS合格 FAIL不合格")
    private String inspectionStatus;

    @Schema(description = "施工状态：NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工")
    private String constructionStatus;

    @Schema(description = "施工内容")
    private String content;

    @Schema(description = "施工记录附件（三级查看使用）")
    private java.util.List<ConstructionAttachmentVO> attachments;
}

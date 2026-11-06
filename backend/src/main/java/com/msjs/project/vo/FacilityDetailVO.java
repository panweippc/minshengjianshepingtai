package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公共基础设施详情VO（含附件、关联项目）
 */
@Data
@Schema(description = "公共基础设施详情")
public class FacilityDetailVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "设施编号")
    private String facilityCode;

    @Schema(description = "设施名称")
    private String facilityName;

    @Schema(description = "设施类型")
    private String facilityType;

    @Schema(description = "所属行政村区划代码")
    private String regionCode;

    @Schema(description = "所属行政村名称")
    private String regionName;

    @Schema(description = "具体位置")
    private String location;

    @Schema(description = "建成年份")
    private Integer buildYear;

    @Schema(description = "数量")
    private BigDecimal quantity;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "附件列表")
    private List<FacilityAttachmentVO> attachments;

    @Schema(description = "关联项目列表")
    private List<ProjectVO> projects;
}

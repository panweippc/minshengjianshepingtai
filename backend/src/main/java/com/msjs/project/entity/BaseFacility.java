package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 公共基础设施
 */
@Data
@TableName("t_base_facility")
@Schema(description = "公共基础设施")
public class BaseFacility implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "设施编号（业务编码，全局唯一）")
    private String facilityCode;

    @Schema(description = "设施名称")
    private String facilityName;

    @Schema(description = "设施类型（字典 she_shi_lei_xing）")
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

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除：0正常 1删除")
    private Integer deleted;
}

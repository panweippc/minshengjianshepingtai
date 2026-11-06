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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 户基础信息实体
 * 业务编码 household_no 保存时自动生成，全局唯一，作废编码不回收复用
 */
@Data
@TableName("t_base_household")
@Schema(description = "户基础信息")
public class Household implements Serializable {

    /** 雪花ID超出JS安全整数范围（2^53-1），序列化为字符串，避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "户号（业务编码，保存时自动生成，全局唯一）")
    private String householdNo;

    @Schema(description = "所属行政村区划编码（关联 t_sys_region.code）")
    private String regionCode;

    @Schema(description = "所属行政村名称（冗余，便于列表展示）")
    private String regionName;

    @Schema(description = "户主姓名")
    private String headName;

    @Schema(description = "户主身份证号")
    private String headIdCard;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "家庭人口")
    private Integer familyPopulation;

    @Schema(description = "房屋面积（㎡）")
    private BigDecimal houseArea;

    @Schema(description = "房屋类型（字典 fang_wu_jie_gou_lei_xing）")
    private String houseType;

    @Schema(description = "户类型（字典 hu_lei_xing）")
    private String householdType;

    @Schema(description = "产权编号")
    private String propertyNo;

    @Schema(description = "建档时间")
    private LocalDate archiveTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0=正常；删除时写入本行ID（墓碑值）。
     * 与 household_no 组成复合唯一索引，保证正常数据户号唯一的同时，允许复用已删除记录的户号
     */
    @TableLogic
    @Schema(description = "逻辑删除：0=正常，其他值=被删除行ID")
    private Long deleted;
}

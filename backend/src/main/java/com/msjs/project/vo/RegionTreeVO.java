package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 区划树节点（乡镇/村/组）
 */
@Data
@Schema(description = "区划树节点")
public class RegionTreeVO implements Serializable {

    /** 雪花ID超出JS安全整数范围（2^53-1），序列化为字符串，避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "区划编码")
    private String code;

    @Schema(description = "区划名称")
    private String name;

    @Schema(description = "上级区划编码")
    private String parentCode;

    @Schema(description = "层级：4乡镇 5村 6组")
    private Integer level;

    @Schema(description = "区划类型（字典 qu_hua_lei_xing）")
    private String regionType;

    @Schema(description = "党支部书记")
    private String secretary;

    @Schema(description = "党支部书记电话")
    private String secretaryPhone;

    @Schema(description = "村委会主任")
    private String director;

    @Schema(description = "村委会主任电话")
    private String directorPhone;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "下级区划")
    private List<RegionTreeVO> children = new ArrayList<>();
}

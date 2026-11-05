package com.msjs.project.vo;

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

    @Schema(description = "下级区划")
    private List<RegionTreeVO> children = new ArrayList<>();
}

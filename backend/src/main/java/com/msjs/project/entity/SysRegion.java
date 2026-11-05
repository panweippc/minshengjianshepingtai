package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 行政区划
 */
@Data
@TableName("t_sys_region")
@Schema(description = "行政区划")
public class SysRegion implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "行政区划代码")
    private String code;

    @Schema(description = "区划名称")
    private String name;

    @Schema(description = "上级区划代码")
    private String parentCode;

    @Schema(description = "层级：1省 2市 3县区 4乡镇 5村 6组")
    private Integer level;

    @Schema(description = "区划类型（字典 qu_hua_lei_xing：1乡镇 2村 3组）")
    private String regionType;

    @Schema(description = "党支部书记")
    private String secretary;

    @Schema(description = "党支部书记电话")
    private String secretaryPhone;

    @Schema(description = "村委会主任")
    private String director;

    @Schema(description = "村委会主任电话")
    private String directorPhone;

    @Schema(description = "排序号")
    private Integer sort;

    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer deleted;
}

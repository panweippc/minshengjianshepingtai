package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 设施-项目关联
 */
@Data
@TableName("t_base_facility_project")
@Schema(description = "设施-项目关联")
public class BaseFacilityProject implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "设施ID")
    private Long facilityId;

    @Schema(description = "项目ID")
    private Long projectId;
}

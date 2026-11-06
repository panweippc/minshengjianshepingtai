package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目（简化版，供设施关联展示）
 */
@Data
@TableName("t_project")
@Schema(description = "项目")
public class Project implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "项目编码（业务编码，全局唯一）")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "进度（百分比）")
    private BigDecimal progress;

    @Schema(description = "预算（元）")
    private BigDecimal budget;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "立项年度")
    private Integer startYear;

    @Schema(description = "所属区划代码")
    private String regionCode;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除：0正常 1删除")
    private Integer deleted;
}

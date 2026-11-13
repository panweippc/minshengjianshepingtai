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
import java.time.LocalDateTime;

/**
 * 验收管理-项目归档记录实体
 * 验收进度达到 100% 的项目可归档，归档后项目进入档案管理
 */
@Data
@TableName("t_project_archive")
@Schema(description = "验收管理-项目归档记录")
public class ProjectArchive implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项ID（关联 t_project_approval.id）")
    private Long approvalId;

    @Schema(description = "档案编号（自动生成，全局唯一）")
    private String archiveNo;

    @Schema(description = "档案名称")
    private String archiveName;

    @Schema(description = "档案类型")
    private String archiveType;

    @Schema(description = "存储位置（档案室/档案柜等物理位置）")
    private String storageLocation;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "归档人")
    private String createBy;

    @Schema(description = "归档时间")
    private LocalDateTime createTime;

    @TableLogic
    @Schema(description = "逻辑删除：0正常 1删除")
    private Integer deleted;
}

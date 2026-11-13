package com.msjs.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 档案管理列表项
 * 档案信息来源于 t_project_archive，项目信息来源于 t_project_approval
 */
@Data
@Schema(description = "档案管理列表项")
public class ArchiveVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "档案ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项ID")
    private Long approvalId;

    @Schema(description = "档案编号")
    private String archiveNo;

    @Schema(description = "存储位置")
    private String storageLocation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "归档时间")
    private LocalDateTime archiveTime;

    @Schema(description = "归档人")
    private String archiveBy;

    @Schema(description = "项目编号")
    private String approvalCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目类别（字典 xiang_mu_lei_bie）")
    private String projectCategory;

    @Schema(description = "项目类型（字典 xiang_mu_lei_xing）")
    private String projectType;

    @Schema(description = "项目负责人")
    private String leader;
}

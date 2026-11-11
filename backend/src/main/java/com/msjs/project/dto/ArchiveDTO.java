package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 项目归档入参
 * 验收进度达到 100% 的项目可执行归档操作
 */
@Data
@Schema(description = "项目归档入参")
public class ArchiveDTO implements Serializable {

    @NotNull(message = "立项ID不能为空")
    @Schema(description = "立项ID")
    private Long approvalId;

    @NotBlank(message = "归档人不能为空")
    @Schema(description = "归档人")
    private String archiveBy;

    @Schema(description = "档案名称（默认取项目名称）")
    private String archiveName;

    @Schema(description = "档案类型")
    private String archiveType;

    @Schema(description = "备注")
    private String remark;
}

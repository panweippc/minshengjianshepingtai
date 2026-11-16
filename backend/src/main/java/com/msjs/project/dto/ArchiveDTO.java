package com.msjs.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

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

    @Schema(description = "档案编号（不传则后端自动生成，允许人工覆盖）")
    private String archiveNo;

    @Schema(description = "存储位置（档案室/档案柜等物理位置）")
    private String storageLocation;

    @Schema(description = "归档时间（不传则取当前时间）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime archiveTime;

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

package com.msjs.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 档案信息编辑入参
 * 业务约束：档案编号为自动生成的业务编码，只读不可修改；
 * 仅存储位置、归档时间允许维护
 */
@Data
@Schema(description = "档案信息编辑入参")
public class ArchiveEditDTO implements Serializable {

    @NotNull(message = "档案ID不能为空")
    @Schema(description = "档案ID")
    private Long id;

    @Schema(description = "存储位置")
    private String storageLocation;

    @NotNull(message = "归档时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "归档时间")
    private LocalDateTime archiveTime;
}

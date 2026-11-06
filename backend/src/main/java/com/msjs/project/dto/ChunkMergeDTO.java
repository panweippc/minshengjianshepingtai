package com.msjs.project.dto;

import com.msjs.project.common.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分片合并入参
 */
@Data
@Schema(description = "分片合并入参")
public class ChunkMergeDTO {

    @NotBlank(message = "上传标识不能为空")
    @Pattern(regexp = "^[A-Za-z0-9-]{1,64}$", message = "上传标识仅允许字母、数字、中划线，长度不超过64")
    @Schema(description = "上传标识（仅允许字母、数字、中划线，长度不超过64）")
    private String uploadId;

    @NotNull(message = "分片总数不能为空")
    @Min(value = 1, message = "分片总数不能小于1")
    @Max(value = Constants.CHUNK_MAX_COUNT, message = "分片总数不能超过2000")
    @Schema(description = "分片总数（1~2000）")
    private Integer totalChunks;

    @NotBlank(message = "文件名不能为空")
    @Size(max = 256, message = "文件名长度不能超过256")
    @Schema(description = "原始文件名（用于后缀与大小校验）")
    private String fileName;

    @Size(max = 32, message = "业务类型长度不能超过32")
    @Schema(description = "业务类型（默认 common）")
    private String bizType;
}

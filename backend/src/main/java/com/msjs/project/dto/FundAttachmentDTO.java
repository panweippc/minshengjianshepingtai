package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 资金凭证附件入参
 */
@Data
@Schema(description = "资金凭证附件入参")
public class FundAttachmentDTO {

    @Schema(description = "附件类型（字典 zi_jin_fu_jian_lei_xing）")
    private String attachmentType;

    @NotBlank(message = "附件文件名不能为空")
    @Size(max = 256, message = "附件文件名过长")
    @Schema(description = "文件原名")
    private String fileName;

    @NotBlank(message = "附件文件标识不能为空")
    @Size(max = 512, message = "附件文件标识过长")
    @Schema(description = "MinIO对象key")
    private String fileKey;

    @Schema(description = "MinIO桶名")
    private String bucket;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Size(max = 256, message = "附件备注过长")
    @Schema(description = "附件备注")
    private String remark;
}

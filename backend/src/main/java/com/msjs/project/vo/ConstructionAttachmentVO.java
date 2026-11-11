package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 施工资料附件VO
 */
@Data
@Schema(description = "施工资料附件")
public class ConstructionAttachmentVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "附件ID")
    private Long id;

    @Schema(description = "附件类型（字典 shi_gong_fu_jian_lei_xing）")
    private String attachmentType;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "MinIO 对象存储 key")
    private String fileKey;

    @Schema(description = "存储桶")
    private String bucket;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "附件备注")
    private String remark;
}

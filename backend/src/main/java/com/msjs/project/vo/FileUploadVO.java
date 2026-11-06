package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件上传结果
 */
@Data
@Schema(description = "文件上传结果")
public class FileUploadVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "文件ID（雪花ID，字符串返回避免前端精度丢失）")
    private Long fileId;

    @Schema(description = "是否命中秒传（true=已有相同内容文件，未重复存储）")
    private Boolean exist;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "MinIO 对象存储 key")
    private String fileKey;

    @Schema(description = "存储桶")
    private String bucket;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "SHA256 哈希（文件校验与去重）")
    private String fileHash;
}

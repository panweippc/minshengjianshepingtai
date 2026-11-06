package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传结果
 */
@Data
@Schema(description = "文件上传结果")
public class FileUploadVO implements Serializable {

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "MinIO 对象路径（不含桶名）")
    private String filePath;

    @Schema(description = "SHA-256 哈希")
    private String fileHash;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;
}

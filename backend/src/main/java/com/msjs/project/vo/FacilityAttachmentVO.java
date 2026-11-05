package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设施附件VO
 */
@Data
@Schema(description = "设施附件")
public class FacilityAttachmentVO implements Serializable {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "设施ID")
    private Long facilityId;

    @Schema(description = "附件类型（字典 fu_jian_lei_xing）")
    private String attachmentType;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "存储路径")
    private String filePath;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

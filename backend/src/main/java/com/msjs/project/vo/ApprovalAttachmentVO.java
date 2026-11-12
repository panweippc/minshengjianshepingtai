package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 立项资料附件 VO
 * declareId 为空表示项目级立项资料附件，非空表示某条申报的申报材料附件
 */
@Data
@Schema(description = "立项资料附件")
public class ApprovalAttachmentVO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @Schema(description = "附件ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "所属立项ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long approvalId;

    @Schema(description = "所属申报关联ID（为空表示项目级立项资料附件）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long declareId;

    @Schema(description = "附件类型（字典 li_xiang_fu_jian_lei_xing）")
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

    @Schema(description = "上传时间")
    private LocalDateTime createTime;
}

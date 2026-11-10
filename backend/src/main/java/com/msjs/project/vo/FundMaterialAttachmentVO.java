package com.msjs.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资金管理-申报明细材料附件VO
 * 来源：农户附件（t_base_household_attachment）或公共基础设施附件（t_base_facility_attachment）
 */
@Data
@Schema(description = "资金管理-申报明细材料附件")
public class FundMaterialAttachmentVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "附件类型（字典 fu_jian_lei_xing）")
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "上传时间")
    private LocalDateTime createTime;
}

package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用附件表（四种归属共用）
 * owner_type：declaration申报单 / household农户 / public_facility公建 / project项目
 */
@Data
@TableName("t_declaration_attachment")
@Schema(description = "通用附件（申报/农户/公建/项目共用）")
public class DeclarationAttachment implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "归属类型：declaration申报单 / household农户 / public_facility公建 / project项目")
    private String ownerType;

    @Schema(description = "归属ID（根据 owner_type 关联不同主表）")
    private Long ownerId;

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

    @Schema(description = "上传人")
    private String createBy;

    @Schema(description = "上传时间")
    private LocalDateTime createTime;

    @TableLogic
    @Schema(description = "逻辑删除：0否 1是")
    private Integer deleted;
}

package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 立项管理-立项资料附件实体
 */
@Data
@TableName("t_project_approval_attachment")
@Schema(description = "立项管理-立项附件")
public class ProjectApprovalAttachment implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项ID（关联 t_project_approval.id）")
    private Long approvalId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "申报ID（关联 t_project_approval_declare.id，为空表示立项资料附件）")
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "上传时间")
    private LocalDateTime createTime;

    @TableLogic
    @Schema(description = "逻辑删除：0否 1是")
    private Integer deleted;
}

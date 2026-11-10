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
 * 资金管理-资金凭证附件实体
 * 文件存储于 MinIO 正式归档桶 archive-formal，访问使用时效签名URL
 */
@Data
@TableName("t_project_fund_attachment")
@Schema(description = "资金凭证附件")
public class ProjectFundAttachment implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "资金拨付记录ID")
    private Long fundId;

    @Schema(description = "附件类型（字典 zi_jin_fu_jian_lei_xing）")
    private String attachmentType;

    @Schema(description = "文件原名")
    private String fileName;

    @Schema(description = "MinIO对象key")
    private String fileKey;

    @Schema(description = "MinIO桶名")
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

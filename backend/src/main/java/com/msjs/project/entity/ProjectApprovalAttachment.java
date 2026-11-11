package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 立项资料附件（单独一张表，不复用申报的 attachment）。
 * attachment_type 字典：li_xiang_fu_jian_lei_xing
 */
@Data
@TableName("t_project_approval_attachment")
public class ProjectApprovalAttachment implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 立项主表 ID */
    private Long approvalId;

    /** 可选：关联到某条申报单 */
    private Long declareId;

    /** 附件类型（字典：li_xiang_fu_jian_lei_xing） */
    private String attachmentType;

    private String fileName;
    /** MinIO 文件 key（upload-temp / archive-formal 桶统一由 file 模块管理） */
    private String fileKey;
    private String bucket;
    private Long fileSize;
    private String remark;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}

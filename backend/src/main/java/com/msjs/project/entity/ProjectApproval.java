package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 立项申请主表：一条立项单可关联多条申报单（declare），
 * 每条申报单下可关联多条明细（农户 / 公建）。
 */
@Data
@TableName("t_project_approval")
public class ProjectApproval implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 立项编号（业务主键，只读，前端手动填） */
    private String approvalCode;

    /** 项目名称（必填） */
    private String projectName;

    /** 项目类别（字典：xiang_mu_lei_bie） */
    private String projectCategory;

    /** 项目类型（字典：xiang_mu_lei_xing） */
    private String projectType;

    /** 项目来源 */
    private String projectSource;

    /** 项目预算（选填） */
    private BigDecimal budget;

    /** 项目负责人 */
    private String leader;

    /** 申报人（建设依据 MD 里的"申报人"字段） */
    private String applyBy;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 立项状态：pending / approved / rejected / returned */
    private String status;

    /** 验收状态：NONE未发起 APPLYING验收中 ARCHIVED已归档 */
    private String acceptStatus;

    /** 发起验收时间 */
    private LocalDateTime acceptApplyTime;

    /** 发起验收人 */
    private String acceptApplyBy;

    /** 所属乡镇 code */
    private String regionCode;

    /** 所属乡镇 name */
    private String regionName;

    /** 建设依据（必填） */
    private String remark;

    /** 申请/立项操作人 */
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 立项-申报单关联：一条立项单 → N 条申报单（declare）
 * 原始设计里 declare 是抽象概念，现在我们把它对齐到 t_declaration 主表，
 * 这里既保留原始快照字段（declare_name / declare_type / budget），也保留
 * declaration_id 做反向追踪。
 */
@Data
@TableName("t_project_approval_declare")
public class ProjectApprovalDeclare implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 立项主表 ID */
    private Long approvalId;

    /** 关联的 t_declaration.id（主申报单 ID，立项后可反向追踪） */
    private Long declarationId;

    /** 原始申报单快照 */
    private String declareCode;
    private String declareName;
    private String declareCategory;
    private String declareType;
    private String townName;
    private String villageName;
    private String declareContent;
    private BigDecimal budget;
    private Integer sort;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}

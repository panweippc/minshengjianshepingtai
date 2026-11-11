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
 * 立项-明细关联：一条申报单（declare）→ N 条明细（农户 / 公建）
 * declaration_id / household_id / facility_id 让我们可以反向把这条明细
 * 在申报单侧标记为 established（已立项）。
 */
@Data
@TableName("t_project_approval_detail")
public class ProjectApprovalDetail implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 立项-申报单关联 ID */
    private Long declareId;

    /** 明细类型：HOUSEHOLD / PUBLIC_FACILITY */
    private String detailType;

    /** 申报单主表 ID */
    private Long declarationId;

    /** 对应农户明细 ID */
    private Long householdId;

    /** 对应公建明细 ID */
    private Long facilityId;

    /** 目标快照（立项当时的明细信息） */
    private Long targetId;
    private String targetName;
    private String targetCode;
    private String regionName;
    private BigDecimal amount;
    private BigDecimal quantity;
    private String remark;
    private Integer sort;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}

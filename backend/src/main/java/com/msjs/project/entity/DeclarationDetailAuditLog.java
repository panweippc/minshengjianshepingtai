package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 明细级审核 / 用户修改历史日志
 * 每张明细（农户 / 公建设施）的每次动作（通过 / 驳回 / 撤销 / 用户修改 /
 * 重新提交时 reset rejected→pending）都追加一条。明细表本身只保留最新
 * 一次审核痕迹（audit_by / audit_time / audit_remark），历史全在这张表。
 */
@Data
@TableName(value = "t_declaration_detail_audit_log")
public class DeclarationDetailAuditLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属申报单 ID（t_declaration.id） */
    private Long declarationId;

    /** 明细类型：household / public_facility */
    private String detailKind;

    /** 明细 ID（t_declaration_household.id 或 t_declaration_public_facility.id） */
    private Long detailId;

    /** 明细快照展示名（冗余，避免明细被删后查不到；农户=户主姓名，设施=设施名称） */
    private String detailSnapshotName;

    /**
     * 动作类型常量：
     *   AUDIT_PASS          — 审核通过
     *   AUDIT_REJECT        — 审核驳回
     *   AUDIT_REVOKE        — 审核撤销（已通过/已驳回的结论撤回）
     *   SUBMIT_UNSUBMITTED  — 用户重新提交时把 rejected 明细 reset 成 pending
     *   USER_EDIT           — 用户修改明细（金额 / 说明 / 附件）
     */
    private String action;

    /** 操作前明细 audit_status（操作痕迹） */
    private String beforeAuditStatus;
    /** 操作后明细 audit_status（操作痕迹） */
    private String afterAuditStatus;

    /** 审核意见 / 用户修改说明 */
    private String remark;

    /**
     * USER_EDIT 时记录哪些字段被改了（JSON 字符串）：
     *   [{"field":"amount","old":"200.00","new":"300.00"},
     *    {"field":"remark","old":null,"new":"补充材料齐全"}]
     * 审核类动作为 NULL。用 String 是为了避免引入 TypeHandler 组件。
     */
    private String fieldDiff;

    /** USER_EDIT 时附件是否有增删改（syncDetailAttachments 返回 changes=true 时置 1） */
    private Boolean attachmentChanged;

    /** 操作人（审核动作用审核人 ID，USER_EDIT 用申报人 ID） */
    private Long operatorId;
    /** 操作人姓名（冗余，Service 层一次批量查好） */
    private String operatorName;

    /** 时间线排序用 */
    private LocalDateTime createTime;
}

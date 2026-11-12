package com.msjs.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 明细级审核 / 用户修改历史日志 VO（前端时间线用）
 * 按 create_time 升序返回，前端直接按 action 映射 el-timeline type。
 */
@Data
@Schema(description = "明细级审核/修改历史日志（时间线）")
public class DeclarationDetailAuditLogVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "日志 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属申报单 ID")
    private Long declarationId;

    @Schema(description = "明细类型：household / public_facility")
    private String detailKind;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "明细 ID")
    private Long detailId;

    @Schema(description = "明细快照展示名（冗余）")
    private String detailSnapshotName;

    @Schema(description = "动作类型：AUDIT_PASS/AUDIT_REJECT/AUDIT_REVOKE/SUBMIT_UNSUBMITTED/USER_EDIT")
    private String action;

    @Schema(description = "操作前 audit_status")
    private String beforeAuditStatus;
    @Schema(description = "操作后 audit_status")
    private String afterAuditStatus;

    @Schema(description = "审核意见 / 用户修改说明")
    private String remark;

    /**
     * USER_EDIT 时哪些字段被改了（JSON 字符串，前端 parse 后渲染）：
     * [{"field":"amount","old":"200.00","new":"300.00"},
     *  {"field":"remark","old":null,"new":"补充材料齐全"}]
     */
    @Schema(description = "USER_EDIT 字段 diff（JSON 字符串；审核类动作为 null）")
    private String fieldDiff;

    @Schema(description = "USER_EDIT 时附件是否有增删改")
    private Boolean attachmentChanged;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "操作人 ID")
    private Long operatorId;
    @Schema(description = "操作人姓名")
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "时间线时间")
    private String createTime;
}

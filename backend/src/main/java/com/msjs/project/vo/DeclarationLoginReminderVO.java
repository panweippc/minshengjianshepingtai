package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录后首页弹窗提醒 VO
 * 镇级账号（区划 level=4）：返回"待我审核的申报单"列表
 * 村级账号（区划 level=5）：返回"我提交的、审核已出结果"的申报单列表
 * 其它层级 / 无区划 / 超管：roleType=UNKNOWN，reminders 为空列表
 */
@Data
@Schema(description = "登录后弹窗提醒")
public class DeclarationLoginReminderVO {

    /** 账号层级类型：TOWN 镇级 / VILLAGE 村级 / UNKNOWN 其它 */
    @Schema(description = "账号层级类型：TOWN 镇级 / VILLAGE 村级 / UNKNOWN 其它")
    private String roleType;

    /** 待办/提醒列表 */
    @Schema(description = "提醒条目列表")
    private List<ReminderItem> reminders;

    /** 单条提醒 */
    @Data
    @Schema(description = "单条提醒")
    public static class ReminderItem {

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "申报单主键ID")
        private Long declarationId;

        @Schema(description = "申报编号")
        private String businessCode;

        @Schema(description = "申报标题")
        private String title;

        @Schema(description = "所属乡镇名称")
        private String townName;

        @Schema(description = "所属村组名称")
        private String villageGroupName;

        @Schema(description = "申报类别名称（到户 / 公建）")
        private String declarationCategoryName;

        @Schema(description = "申报金额（元）")
        private BigDecimal amount;

        @Schema(description = "申报日期")
        private LocalDate declarationDate;

        @Schema(description = "申报状态：draft/submitted/approved/rejected")
        private String status;

        @Schema(description = "状态中文名")
        private String statusName;

        @Schema(description = "审核人姓名（镇级待审核时：为空或指派的镇级审核人）")
        private String auditByName;

        @Schema(description = "审核时间（村级查看审核结果时有值）")
        private LocalDateTime auditTime;

        @Schema(description = "审核意见（驳回时通常有值）")
        private String auditRemark;

        @Schema(description = "创建时间（镇级待审核 = 提交时间 / 村级审核结果 = 审核时间）")
        private LocalDateTime createTime;
    }
}

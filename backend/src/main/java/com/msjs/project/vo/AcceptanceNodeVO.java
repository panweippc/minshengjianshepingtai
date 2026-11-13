package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.msjs.project.entity.ProjectAcceptanceAttachment;
import com.msjs.project.entity.ProjectApprovalAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 验收管理树形节点 VO
 * 一级：项目；二级：申报内容；三级：农户/公共基础设施明细
 * 验收进度 = 已验收明细数 / 全部明细数 × 100%
 */
@Data
@Schema(description = "验收管理树形节点")
public class AcceptanceNodeVO implements Serializable {

    @Schema(description = "节点ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "节点类型：project项目 declare申报 detail明细")
    private String nodeType;

    @Schema(description = "明细类型：HOUSEHOLD农户 FACILITY公共基础设施（仅三级）")
    private String detailType;

    @Schema(description = "节点ID（项目/申报/明细）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long approvalId;

    @Schema(description = "申报ID（二三级）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long declareId;

    @Schema(description = "明细ID（三级）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long detailId;

    @Schema(description = "编号：项目编号/申报编号/户号或设施编码")
    private String code;

    @Schema(description = "名称：项目名称/申报标题/农户或设施名称")
    private String name;

    @Schema(description = "项目类别（字典 xiang_mu_lei_bie）")
    private String category;

    @Schema(description = "项目类型（字典 xiang_mu_lei_xing）或所属区划")
    private String source;

    @Schema(description = "提交时间（仅一级）")
    private LocalDateTime submitTime;

    @Schema(description = "项目预算 / 申报金额 / 补助金额")
    private BigDecimal budget;

    @Schema(description = "全部明细数")
    private Integer totalCount;

    @Schema(description = "已验收明细数")
    private Integer acceptedCount;

    @Schema(description = "验收进度百分比（0-100）")
    private Integer percent;

    @Schema(description = "验收状态（仅三级：PENDING待验收 ACCEPTED已验收）")
    private String acceptStatus;

    @Schema(description = "施工状态（与施工管理同步）：NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工")
    private String buildStatus;

    @Schema(description = "施工进度分子：施工中为已开工数，已完工为已完工数")
    private Integer buildNumerator;

    @Schema(description = "施工进度百分比（0-100）：施工进度分子 / 全部明细数")
    private Integer buildPercent;

    @Schema(description = "验收时间（仅三级已验收时）")
    private LocalDateTime acceptTime;

    @Schema(description = "验收人（仅三级已验收时）")
    private String acceptBy;

    @Schema(description = "验收备注（仅三级）")
    private String acceptRemark;

    @Schema(description = "验收结果（字典 yan_shou_jie_guo，仅三级）")
    private String acceptResult;

    @Schema(description = "事项说明（仅三级，申报明细原始值）")
    private String detailRemark;

    @Schema(description = "申报内容（仅二级）")
    private String declareContent;

    @Schema(description = "验收资料附件（仅三级，取最新一次验收记录）")
    private List<ProjectAcceptanceAttachment> acceptanceAttachments;

    @Schema(description = "验收记录（仅三级，按时间倒序，第一条为最新一次）")
    private List<AcceptanceRecordVO> acceptanceRecords;

    @Schema(description = "立项资料附件（仅一级查看弹窗使用）")
    private List<ProjectApprovalAttachment> approvalAttachments;

    @Schema(description = "所属区划名称（仅二三级）")
    private String villageName;

    @Schema(description = "所属乡镇（仅二级）")
    private String townName;

    @Schema(description = "是否已归档（仅一级）")
    private Boolean archived;

    @Schema(description = "子节点")
    private List<AcceptanceNodeVO> children;
}

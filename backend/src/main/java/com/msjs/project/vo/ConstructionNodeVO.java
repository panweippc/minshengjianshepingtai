package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 施工管理树形节点VO
 * 一级：项目；二级：申报内容；三级：农户/公共基础设施明细
 * 施工状态由其三级明细聚合：有已完工即展示已完工（分子为已完工数），否则有施工中即展示施工中（分子为已开工数）
 */
@Data
@Schema(description = "施工管理树形节点")
public class ConstructionNodeVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "施工记录ID（可能为空）")
    private Long id;

    @Schema(description = "前端行标识，如 p_立项ID / d_申报ID / t_明细ID")
    private String nodeKey;

    @Schema(description = "节点类型：project项目 declare申报 detail明细")
    private String nodeType;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项ID")
    private Long approvalId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "申报ID")
    private Long declareId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "明细ID")
    private Long detailId;

    @Schema(description = "项目编号/申报编号/户号或设施编码")
    private String code;

    @Schema(description = "项目名称/申报标题/农户或设施名称")
    private String name;

    @Schema(description = "项目类别（字典值）或明细类型")
    private String category;

    @Schema(description = "项目来源（字典值）或所属区划")
    private String source;

    @Schema(description = "明细类型：HOUSEHOLD农户 FACILITY公共基础设施（仅三级节点）")
    private String detailType;

    @Schema(description = "施工状态：NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工")
    private String status;

    @Schema(description = "全部户数/设施数")
    private Integer totalCount;

    @Schema(description = "已开工数")
    private Integer startedCount;

    @Schema(description = "已完工数")
    private Integer completedCount;

    @Schema(description = "状态分子：施工中为已开工数，已完工为已完工数")
    private Integer numerator;

    @Schema(description = "进度百分比（0-100）")
    private Integer percent;

    @Schema(description = "补助/投入金额（元）：三级为明细金额，一级/二级为明细合计")
    private BigDecimal amount;

    @Schema(description = "是否可发起验收：全部明细已完工")
    private Boolean canAccept;

    @Schema(description = "三级节点施工状态原始值")
    private String buildStatus;

    @Schema(description = "施工单位（一级节点）")
    private String builder;

    @Schema(description = "承包人姓名（一级节点）")
    private String contractorName;

    @Schema(description = "承包人联系电话（一级节点）")
    private String contractorPhone;

    @Schema(description = "负责人姓名（一级节点）")
    private String leaderName;

    @Schema(description = "负责人联系电话（一级节点）")
    private String leaderPhone;

    @Schema(description = "施工记录数（一级节点）")
    private Integer logCount;

    @Schema(description = "巡检总数（一级节点）")
    private Integer inspectionCount;

    @Schema(description = "巡检合格数（一级节点）")
    private Integer inspectionPassCount;

    @Schema(description = "巡检合格率百分比（一级节点）")
    private Integer inspectionPassRate;

    @Schema(description = "施工日志/巡检记录（一级节点查看使用）")
    private List<ConstructionLogVO> logs;

    @Schema(description = "最新一条施工记录的施工内容（二级查看明细使用）")
    private String latestContent;

    @Schema(description = "最新一条施工记录的巡检状态 PASS合格 FAIL整改（二级查看明细使用）")
    private String latestInspectionStatus;

    @Schema(description = "最新一条施工记录的施工状态 NOT_STARTED未开工 IN_PROGRESS施工中 COMPLETED已完工（二级查看明细使用）")
    private String latestConstructionStatus;

    @Schema(description = "最新一条施工记录绑定的附件列表（二级查看明细使用，按 log_id 留痕快照）")
    private List<ConstructionAttachmentVO> latestAttachments;

    @Schema(description = "最新一条施工记录的巡检日期（二级查看明细使用）")
    private LocalDate latestInspectionDate;

    @Schema(description = "施工资料附件（一级节点查看/编辑使用）")
    private List<ConstructionAttachmentVO> attachments;

    @Schema(description = "三级明细最新施工记录快照（与三级查看详情页签完全同源，巡检状态/日期/内容/附件均取自此处）")
    private ConstructionLogVO detailLogSnapshot;

    @Schema(description = "开工日期")
    private LocalDate startDate;

    @Schema(description = "完工日期")
    private LocalDate completeDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "子节点：二级为申报，三级为明细")
    private List<ConstructionNodeVO> children;
}

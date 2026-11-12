package com.msjs.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.msjs.project.entity.ProjectApprovalAttachment;
import com.msjs.project.entity.ProjectFund;
import com.msjs.project.entity.ProjectFundAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资金管理树形节点（一级项目 → 二级申报 → 三级明细）
 * category/type 为字典原始值，由前端按层级选择对应字典翻译
 */
@Data
@Schema(description = "资金管理树形节点")
public class FundNodeVO implements Serializable {

    @Schema(description = "节点ID（项目/申报/明细）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "节点类型：approval项目 declare申报 detail明细")
    private String nodeType;

    @Schema(description = "明细类型：HOUSEHOLD农户 FACILITY公共基础设施")
    private String detailType;

    @Schema(description = "编号：项目编号/申报编号/户号或设施编码")
    private String code;

    @Schema(description = "名称：项目名称/申报名称/农户或设施名称")
    private String name;

    @Schema(description = "项目类别（一级：字典 xiang_mu_lei_bie；二级：字典 shen_bao_lei_bie；三级：明细类型）")
    private String category;

    @Schema(description = "项目类型（一级：字典 xiang_mu_lei_xing；二级：字典 shen_bao_lei_xing；三级：所属区划）")
    private String type;

    @Schema(description = "项目预算（一级：项目预算；二级：申报金额；三级：补助金额）")
    private BigDecimal budget;

    @Schema(description = "资金来源")
    private String fundSource;

    @Schema(description = "拨付金额（本级累计拨付/下拨金额）")
    private BigDecimal allocatedAmount;

    @Schema(description = "结余（仅一级节点：上级拨付金额 - 二级下拨金额）")
    private BigDecimal balance;

    @Schema(description = "负责人（仅一级）")
    private String leader;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "提交时间（仅一级）")
    private LocalDateTime submitTime;

    @Schema(description = "审核状态（仅一级）")
    private String status;

    @Schema(description = "备注（仅一级）")
    private String remark;

    @Schema(description = "所属乡镇（仅二级）")
    private String townName;

    @Schema(description = "所属村组（仅二级）")
    private String villageName;

    @Schema(description = "申报内容（仅二级）")
    private String declareContent;

    @Schema(description = "立项/申报材料附件（一级/二级）")
    private List<ProjectApprovalAttachment> attachments;

    @Schema(description = "事项说明（仅三级）")
    private String detailRemark;

    @Schema(description = "拨付说明（仅三级：最近一次下拨说明）")
    private String fundRemark;

    @Schema(description = "资金凭证附件（二级下拨、三级下拨）")
    private List<ProjectFundAttachment> fundAttachments;

    @Schema(description = "申报材料附件（仅三级：关联农户/公共基础设施的附件）")
    private List<FundMaterialAttachmentVO> materialAttachments;

    @Schema(description = "资金流水（一级/二级/三级查看）")
    private List<ProjectFund> records;

    @Schema(description = "子节点")
    private List<FundNodeVO> children;
}

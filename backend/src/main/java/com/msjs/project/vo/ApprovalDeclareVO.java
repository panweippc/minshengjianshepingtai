package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 立项管理-申报内容节点 VO（树形二级）
 * 关联的申报材料附件（declare_id 非空）随节点返回，供查看弹窗展示
 */
@Data
@Schema(description = "立项管理申报内容节点")
public class ApprovalDeclareVO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @Schema(description = "立项-申报关联ID")
    private Long id;

    @Schema(description = "所属立项ID")
    private Long approvalId;

    @Schema(description = "申报编号（业务编码）")
    private String declareCode;

    @Schema(description = "申报标题")
    private String declareName;

    @Schema(description = "申报类别（字典 shen_bao_lei_bie）")
    private String declareCategory;

    @Schema(description = "申报类型（字典 shen_bao_lei_xing）")
    private String declareType;

    @Schema(description = "所属乡镇名称")
    private String townName;

    @Schema(description = "所属村组名称")
    private String villageName;

    @Schema(description = "申报内容")
    private String declareContent;

    @Schema(description = "申报预算（元）")
    private BigDecimal budget;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "子节点：农户/公建明细（树形三级）")
    private List<ApprovalDetailVO> children;

    @Schema(description = "申报材料附件（declare_id 指向本申报）")
    private List<ApprovalAttachmentVO> attachments;
}

package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 立项管理-项目节点 VO（树形一级）
 * 下级为申报内容（ApprovalDeclareVO），再下级为农户/公建明细（ApprovalDetailVO）
 */
@Data
@Schema(description = "立项管理项目节点")
public class ApprovalVO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @Schema(description = "立项ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "立项编号（业务编码，只读）")
    private String approvalCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目类别（字典 xiang_mu_lei_bie）")
    private String projectCategory;

    @Schema(description = "项目类型（字典 xiang_mu_lei_xing）")
    private String projectType;

    @Schema(description = "项目来源（字典 xiang_mu_lai_yuan）")
    private String projectSource;

    @Schema(description = "项目预算（元）")
    private BigDecimal budget;

    @Schema(description = "项目负责人")
    private String leader;

    @Schema(description = "申报人")
    private String applyBy;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "审核状态：PENDING待审核 APPROVED已通过 REJECTED已驳回")
    private String status;

    @Schema(description = "所属乡镇编码")
    private String regionCode;

    @Schema(description = "所属乡镇名称")
    private String regionName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子节点：申报内容（树形二级）")
    private List<ApprovalDeclareVO> children;
}

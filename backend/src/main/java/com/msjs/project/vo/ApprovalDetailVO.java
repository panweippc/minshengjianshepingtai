package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 立项管理-明细节点 VO（树形三级：农户 / 公共基础设施）
 */
@Data
@Schema(description = "立项管理明细节点")
public class ApprovalDetailVO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "所属立项-申报关联ID")
    private Long declareId;

    @Schema(description = "明细类型：HOUSEHOLD农户 FACILITY公共基础设施")
    private String detailType;

    @Schema(description = "目标ID（农户ID/设施ID）")
    private Long targetId;

    @Schema(description = "农户/设施名称")
    private String targetName;

    @Schema(description = "户号/设施编码")
    private String targetCode;

    @Schema(description = "所属区划名称")
    private String regionName;

    @Schema(description = "金额（元）")
    private BigDecimal amount;

    @Schema(description = "数量")
    private BigDecimal quantity;

    @Schema(description = "事项说明")
    private String remark;

    @Schema(description = "排序号")
    private Integer sort;
}

package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 立项申请提交成功返回：只有立项 ID + 编号，明细落库情况后端静默。
 */
@Data
@Schema(description = "立项申请提交返回")
public class ProjectApprovalSubmitVO {
    @Schema(description = "立项主表 ID")
    private Long id;

    @Schema(description = "立项编号")
    private String approvalCode;

    @Schema(description = "立项状态（新建一律 pending）")
    private String status;

    @Schema(description = "落库的申报单数量")
    private Integer declareCount;

    @Schema(description = "落库的明细（农户 / 公建）数量")
    private Integer detailCount;

    @Schema(description = "落库的附件数量")
    private Integer attachmentCount;
}

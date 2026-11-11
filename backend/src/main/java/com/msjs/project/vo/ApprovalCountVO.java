package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 立项各页签数量统计 VO（按当前筛选条件）
 */
@Data
@Schema(description = "立项页签数量统计")
public class ApprovalCountVO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @Schema(description = "全部数量")
    private Long all;

    @Schema(description = "待审核数量")
    private Long pending;

    @Schema(description = "已驳回数量")
    private Long rejected;
}

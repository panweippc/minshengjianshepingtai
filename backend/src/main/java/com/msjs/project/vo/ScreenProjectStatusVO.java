package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据大屏项目状态判定原始数据
 */
@Data
@Schema(description = "项目状态判定原始数据")
public class ScreenProjectStatusVO {

    @Schema(description = "立项状态：PENDING待审核 / APPROVED已通过 / REJECTED已驳回")
    private String approvalStatus;

    @Schema(description = "验收状态：NONE未发起 / APPLYING验收中 / ARCHIVED已归档")
    private String acceptStatus;

    @Schema(description = "施工中明细数量（大于0视为施工中）")
    private Long progressFlag;
}

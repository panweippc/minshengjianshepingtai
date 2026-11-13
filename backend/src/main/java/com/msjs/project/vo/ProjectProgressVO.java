package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 首页项目进度概览项：一个项目一条记录，展示当前所处阶段
 */
@Data
@Schema(description = "项目进度概览项")
public class ProjectProgressVO {

    @Schema(description = "立项主键")
    private Long id;

    @Schema(description = "立项编号")
    private String approvalCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "当前阶段名称")
    private String stage;

    @Schema(description = "当前阶段序号（1待审核 2已通过 3施工中 4待验收 5已归档）")
    private Integer stageIndex;
}

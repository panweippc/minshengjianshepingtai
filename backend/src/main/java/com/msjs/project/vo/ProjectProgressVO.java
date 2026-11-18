package com.msjs.project.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 首页项目进度概览项：一个项目一条记录，展示当前所处阶段
 * 同一项目可能同时处于多个阶段（如已发起验收但施工仍在进行），因此阶段以集合返回
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

    /** 立项审核状态（仅用于计算当前阶段，不返回前端） */
    @JsonIgnore
    @Schema(hidden = true)
    private String approvalStatus;

    /** 项目验收状态（仅用于计算当前阶段，不返回前端） */
    @JsonIgnore
    @Schema(hidden = true)
    private String acceptStatus;

    @Schema(description = "当前阶段序号集合（1待审核 2资金 3施工中 4待验收 5已归档），可同时包含多个")
    private List<Integer> currentStages;
}

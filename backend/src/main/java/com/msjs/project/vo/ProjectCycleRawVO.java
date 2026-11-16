package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目全周期查询 - 原始明细行
 *
 * <p>查询粒度：以 declare（申报单）为粒度，每个 declare 关联一个村组</p>
 * <p>Service 层基于此做：累计拨付聚合、项目状态计算、5套方案的分组聚合</p>
 */
@Data
public class ProjectCycleRawVO implements Serializable {

    // ==== approval 级字段 ====
    @JsonSerialize(using = ToStringSerializer.class)
    private Long approvalId;
    private String approvalCode;
    private String projectName;
    private String projectSource;      // 项目来源
    private String projectSourceLabel; // 项目来源翻译标签
    private String projectCategory;    // 项目类别（xiang_mu_lei_bie）
    private String projectType;        // 项目类型（xiang_mu_lei_xing：1常规项目/2应急抢险/3灾后重建）
    private String projectTypeLabel;   // 项目类型翻译标签
    private String approvalStatus;     // 立项状态（t_project_approval.status）
    private String acceptStatus;       // 验收状态（t_project_approval.accept_status）
    private BigDecimal budget;         // 项目预算

    // ==== declare 级字段 ====
    @JsonSerialize(using = ToStringSerializer.class)
    private Long declareId;
    private String declareCategory;        // 申报类别（household到户 / public公建）
    private String declareCategoryLabel;   // 翻译后的显示标签
    private String villageName;            // 村组名称

    // ==== 明细计数 ====
    private Integer householdCount;    // 该 declare 下农户明细数量
    private Integer facilityCount;     // 该 declare 下公建设施明细数量

    // ==== 累计拨付（approval 级聚合） ====
    private BigDecimal totalFund;      // 该 approval 累计拨付金额

    // ==== 区划筛选 ====
    private String regionCode;         // approval.region_code

    // ==== 日期（方案3 按立项状态的年度筛选用） ====
    private LocalDateTime submitTime;       // a.submit_time 立项日期
    private LocalDateTime archiveCreateTime; // archive.create_time 归档时间（LEFT JOIN 来）
}

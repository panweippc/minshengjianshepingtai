package com.msjs.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资金记录查询 VO（综合查询模块，只读）
 * 一条资金记录 = 一个扁平流水行；同时携带汇总卡和左侧面板的统计结果，
 * 由 Service 在一次调用中组装，避免前端多次请求。
 */
public class FundQueryVO {

    @Data
    @Schema(description = "资金流水行（扁平，一行=一条 t_project_fund）")
    public static class Record {

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "资金记录 ID")
        private Long id;

        @Schema(description = "节点层级：1资金登记 / 2二级下拨 / 3三级下拨")
        private Integer nodeLevel;

        @Schema(description = "节点层级中文：资金登记 / 二级下拨 / 三级下拨")
        private String nodeLevelLabel;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "立项 ID")
        private Long approvalId;

        @Schema(description = "项目编号")
        private String approvalCode;

        @Schema(description = "项目名称")
        private String projectName;

        @Schema(description = "项目类别字典值")
        private String projectCategory;

        @Schema(description = "项目类别中文")
        private String projectCategoryLabel;

        @Schema(description = "项目类型字典值")
        private String projectType;

        @Schema(description = "项目类型中文")
        private String projectTypeLabel;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "申报单 ID（nodeLevel=1 时为空）")
        private Long declareId;

        @Schema(description = "申报编号")
        private String declareCode;

        @Schema(description = "申报标题")
        private String declareName;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "明细 ID（nodeLevel=3 时有值）")
        private Long detailId;

        @Schema(description = "明细名称（农户姓名 / 设施名称）")
        private String detailName;

        @Schema(description = "明细类型字典值（HOUSEHOLD / FACILITY），nodeLevel=3 才有")
        private String detailType;

        @Schema(description = "资金类型字典值")
        private String fundType;

        @Schema(description = "资金类型中文（nodeLevel=1 取 zi_jin_lei_xing，nodeLevel=2/3 取 xia_bo_lei_xing）")
        private String fundTypeLabel;

        @Schema(description = "资金来源字典值")
        private String fundSource;

        @Schema(description = "资金来源中文（字典 zi_jin_lai_yuan）")
        private String fundSourceLabel;

        @Schema(description = "金额（元）")
        private BigDecimal amount;

        @Schema(description = "所属乡镇")
        private String townName;

        @Schema(description = "所属村组")
        private String villageName;

        @Schema(description = "经办时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;

        @Schema(description = "经办人")
        private String createBy;

        @Schema(description = "拨付说明")
        private String remark;

        @Schema(description = "资金凭证附件数量")
        private Integer attachCount;
    }

    @Data
    @Schema(description = "顶部汇总卡统计")
    public static class Summary {
        @Schema(description = "资金记录数（当前筛选命中条数）")
        private long recordCount;
        @Schema(description = "登记合计（nodeLevel=1 金额合计）")
        private BigDecimal registerTotal;
        @Schema(description = "下拨合计（nodeLevel=2/3 金额合计）")
        private BigDecimal allocateTotal;
        @Schema(description = "涉及项目数（按 approval_id 去重）")
        private long projectCount;
        @Schema(description = "涉及申报数（nodeLevel>1 时按 declare_id 去重）")
        private long declareCount;
        @Schema(description = "涉及明细数（nodeLevel=3 时按 detail_id 去重）")
        private long detailCount;
    }

    @Data
    @Schema(description = "左侧单个统计项")
    public static class StatItem {
        @Schema(description = "字典值 / 分组 value")
        private String value;
        @Schema(description = "显示标签")
        private String label;
        @Schema(description = "记录数")
        private long count;
        @Schema(description = "金额合计（元）")
        private BigDecimal amount;
    }

    @Data
    @Schema(description = "左侧筛选面板统计")
    public static class Sidebar {
        @Schema(description = "按资金类型")
        private List<StatItem> byFundType;
        @Schema(description = "按资金来源")
        private List<StatItem> byFundSource;
        @Schema(description = "按节点层级")
        private List<StatItem> byNodeLevel;
        @Schema(description = "按所属村组")
        private List<StatItem> byVillage;
    }
}

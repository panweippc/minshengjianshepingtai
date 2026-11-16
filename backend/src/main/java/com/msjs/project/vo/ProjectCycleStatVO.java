package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 综合查询-项目全周期 统计返回
 *
 * <p>接口路径：GET /api/project/cycle/query/stat</p>
 */
@Data
public class ProjectCycleStatVO implements Serializable {

    /** 主统计表格行（展平的嵌套行：大分组 → 村组明细 → grandTotal 总计） */
    private List<Row> rows;

    /** 总行数 */
    private Long total;

    /** 全局汇总（顶部概览） */
    private GlobalSummary globalSummary;

    /** 主统计表行（嵌套展平结构） */
    @Data
    public static class Row implements Serializable {

        /** 大分组 value（字典值或计算值） */
        private String groupValue;
        /** 大分组 label（已翻译） */
        private String groupLabel;

        /** 内层村组名："全部村组"（groupSummary=true） 或 具体村组名 */
        private String villageName;

        // ==== 通用指标（category/source/status/village 方案使用） ====
        /** 项目数量（个） */
        private Integer projectCount;
        /** 覆盖村组（个），不去重 */
        private Integer villageCoverage;
        /** 项目预算（元） */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal budget;
        /** 累计拨付（元） */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal totalFund;

        // ==== 立项状态方案专用指标（establish 方案使用） ====
        /** 涉及户数（个），到户项目下农户明细数量 */
        private Integer householdCount;
        /** 公建设施数（个），公建项目下公建设施明细数量 */
        private Integer facilityCount;

        /** 是否大分组汇总行（villageName="全部村组"） */
        private boolean groupSummary;
        /** 是否最底总计行（跨全表汇总） */
        private boolean grandTotal;
    }

    @Data
    public static class GlobalSummary implements Serializable {
        /** 项目总数（去重 approvalId） */
        private Integer projectCount;
        /** 申报单总数（去重 declareId） */
        private Integer declareCount;
        /** 覆盖村组总数（不去重） */
        private Integer villageCoverage;
        /** 项目总预算（元） */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal totalBudget;
        /** 累计总拨付（元） */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal totalFund;
    }
}

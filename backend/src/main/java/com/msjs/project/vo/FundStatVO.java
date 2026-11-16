package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 资金记录查询 - 统计汇总接口返回
 *
 * <p>接口路径：GET /api/project/fund/query/stat</p>
 */
@Data
public class FundStatVO implements Serializable {

    /** 主统计表格行（展平的嵌套行：大分组 → 村组明细 → grandTotal 总计） */
    private List<Row> rows;

    /** 统计表格总行数 */
    private Long total;

    /** 全局汇总（顶部已删除卡片，但 recordCount 用于右上角 "共 N 条源记录"） */
    private GlobalSummary globalSummary;

    @Data
    public static class GlobalSummary implements Serializable {
        private Integer recordCount;
        private Integer projectCount;
        private Integer declareCount;
    }

    /**
     * 主统计表行（仿照 DeclarationStatisticsVO.Row：嵌套展平结构）
     *
     * <p>三种行类型：</p>
     * <ul>
     *   <li>grandTotal=true：最底总计行（金额加粗 / 绿色）</li>
     *   <li>groupSummary=true && grandTotal=false：大分组汇总行（villageName="全部村组"）</li>
     *   <li>普通明细行：villageName=具体村组名</li>
     * </ul>
     */
    @Data
    public static class Row implements Serializable {

        /** 大分组 value（fundType 字典值 / fundSource 字典值 / nodeLevel 数字 / village 村组 code） */
        private String groupValue;
        /** 大分组 label（已翻译：预算 / 拨付 / 项目款 / 补贴 / 资金登记 …） */
        private String groupLabel;

        /** 内层村组名："全部村组"（groupSummary=true） 或 具体村组名（groupSummary=false） */
        private String villageName;

        /** 资金笔数（条） */
        private Integer recordCount;
        /** 涉及项目数（去重项目 approvalId） */
        private Integer projectCount;
        /** 覆盖村组数（去重 villageName） */
        private Integer villageCoverage;

        /** 资金金额（登记+下拨合计） */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal amount;

        /** 金额备注（grandTotal=true 时传 "全部区划" / groupSummary=true 时传空） */
        private String remark;

        /** 是否大分组汇总行（第一行 "全部村组"） */
        private boolean groupSummary;
        /** 是否最底总计行（跨全表汇总） */
        private boolean grandTotal;
    }
}

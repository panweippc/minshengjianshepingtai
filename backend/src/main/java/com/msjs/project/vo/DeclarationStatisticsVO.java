package com.msjs.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 申报综合查询统计 VO
 */
@Data
@Schema(description = "申报综合查询统计结果")
public class DeclarationStatisticsVO {

    @Schema(description = "分组维度：category 按申报类别 / status 按审批状态 / village 按所属村组")
    private String groupBy;

    @Schema(description = "统计行列表（含分组汇总行 + 村组明细行 + 总计行）")
    private List<Row> rows;

    @Schema(description = "左侧筛选面板的统计结果（用于构建左侧统计卡片）")
    private SidebarStats sidebar;

    @Data
    @Schema(description = "统计行")
    public static class Row {
        @Schema(description = "是否为分组汇总行（是 = true）")
        private boolean groupSummary;

        @Schema(description = "分组一级值（如 公建申报 / 到户申报 / 已通过 / 光明村）")
        private String groupLabel;

        @Schema(description = "分组二级值（通常是村组名；汇总行固定为「全部村组」）")
        private String villageName;

        @Schema(description = "村组区划编码（汇总行为 null）")
        private String villageGroupCode;

        @Schema(description = "申报次数（次）")
        private int declarationCount;

        @Schema(description = "涉及户数（户），仅到户申报统计户主去重")
        private int householdCount;

        @Schema(description = "公建设施数（个），仅公建申报统计设施去重")
        private int facilityCount;

        @Schema(description = "申报金额（元）")
        private BigDecimal amount;

        @Schema(description = "是否为总计行（汇总全部分组）")
        private boolean grandTotal;

        @Schema(description = "该行对应的明细数据（用于右键核对）")
        private List<DetailItem> details;
    }

    @Data
    @Schema(description = "统计行对应的明细数据")
    public static class DetailItem {
        @Schema(description = "申报单ID")
        private Long declarationId;

        @Schema(description = "申报编号")
        private String businessCode;

        @Schema(description = "申报标题")
        private String title;

        @Schema(description = "申报类别名称")
        private String categoryLabel;

        @Schema(description = "所属村组")
        private String villageName;

        @Schema(description = "户主姓名 / 公共基础设施名称")
        private String targetName;

        @Schema(description = "审批状态名称")
        private String auditStatusLabel;

        @Schema(description = "申报金额（元）")
        private BigDecimal amount;
    }

    @Data
    @Schema(description = "左侧筛选面板的统计")
    public static class SidebarStats {
        /** 按申报类别 */
        private List<StatItem> byCategory;
        /** 按审批状态 */
        private List<StatItem> byStatus;
        /** 按所属村组 */
        private List<StatItem> byVillage;
    }

    @Data
    @Schema(description = "单个统计项")
    public static class StatItem {
        private String value;
        private String label;
        private int count;
    }
}

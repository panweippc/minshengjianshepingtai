package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金记录查询 - 分组明细流水接口返回的一行扁平流水。
 *
 * <p>接口路径：GET /api/project/fund/query/detailList</p>
 */
@Data
public class FundDetailRowVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 1=一级登记 2=二级下拨 3=三级下拨 */
    private Integer nodeLevel;
    private String nodeLevelLabel;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long approvalId;
    private String approvalCode;
    private String projectName;
    private String projectCategory;
    private String projectCategoryLabel;
    private String projectType;
    private String projectTypeLabel;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long declareId;
    private String declareCode;
    private String declareName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long detailId;
    private String detailName;
    private String detailType;

    /** 资金类型字典值（nodeLevel=1 用 zi_jin_lei_xing，2/3 用 xia_bo_lei_xing） */
    private String fundType;
    private String fundTypeLabel;

    private String fundSource;
    private String fundSourceLabel;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amount;

    private String townName;
    private String villageName;
    private String regionCode;

    private LocalDateTime createTime;
    private String createBy;
    private String remark;

    /** 附件数量（Service 批量回填） */
    private Integer attachCount;
}

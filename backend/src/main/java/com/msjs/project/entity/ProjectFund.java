package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资金管理-拨付记录实体
 * 一级为上级资金登记，二级/三级为向下级节点下拨资金
 */
@Data
@TableName("t_project_fund")
@Schema(description = "资金管理-拨付记录")
public class ProjectFund implements Serializable {

    /** 雪花ID超出JS安全整数范围（2^53-1），序列化为字符串，避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "立项ID（关联 t_project_approval.id）")
    private Long approvalId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "申报内容ID（二级/三级下拨时非空）")
    private Long declareId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "申报明细ID（三级下拨时非空）")
    private Long detailId;

    @Schema(description = "资金层级：1一级资金登记 2二级下拨 3三级下拨")
    private Integer nodeLevel;

    @Schema(description = "资金类型（字典 zi_jin_lei_xing）")
    private String fundType;

    @Schema(description = "资金来源（字典 zi_jin_lai_yuan）")
    private String fundSource;

    @Schema(description = "拨付/下拨金额（元）")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "经办人")
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "登记时间")
    private LocalDateTime createTime;

    @TableLogic
    @Schema(description = "逻辑删除：0否 1是")
    private Integer deleted;

    /** 资金凭证附件（非数据库字段，查询时回填） */
    @TableField(exist = false)
    @Schema(description = "资金凭证附件")
    private List<ProjectFundAttachment> attachments;
}

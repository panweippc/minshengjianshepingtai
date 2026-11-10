package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

/**
 * 立项管理-申报明细实体（树形三级节点：农户 / 公共基础设施）
 */
@Data
@TableName("t_project_approval_detail")
@Schema(description = "立项管理-申报明细")
public class ProjectApprovalDetail implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "申报内容ID（关联 t_project_approval_declare.id）")
    private Long declareId;

    @Schema(description = "明细类型：HOUSEHOLD农户 FACILITY公共基础设施")
    private String detailType;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "关联农户/设施ID")
    private Long targetId;

    @Schema(description = "农户户主姓名/设施名称")
    private String targetName;

    @Schema(description = "户号/设施编码")
    private String targetCode;

    @Schema(description = "所属区划名称")
    private String regionName;

    @Schema(description = "补助/投入金额（元）")
    private BigDecimal amount;

    @Schema(description = "数量")
    private BigDecimal quantity;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序")
    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableLogic
    @Schema(description = "逻辑删除：0否 1是")
    private Integer deleted;
}

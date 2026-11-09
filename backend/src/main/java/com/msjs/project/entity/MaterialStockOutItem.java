package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出库单明细（物资管理模块）
 * 一条明细对应一件出库物品，归属唯一出库单
 */
@Data
@TableName("t_material_stock_out_item")
@Schema(description = "出库单明细")
public class MaterialStockOutItem implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属出库单ID")
    private Long orderId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "物品ID")
    private Long itemId;

    @Schema(description = "物品名称")
    private String itemName;

    @Schema(description = "物品分类（字典 wu_pin_fen_lei）")
    private String itemCategory;

    @Schema(description = "出库数量")
    private Integer quantity;

    @Schema(description = "出库类型（字典 chu_ku_lei_xing）")
    private String outType;

    @Schema(description = "出库时间")
    private LocalDate outTime;

    @Schema(description = "经办人")
    private String handler;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer deleted;
}

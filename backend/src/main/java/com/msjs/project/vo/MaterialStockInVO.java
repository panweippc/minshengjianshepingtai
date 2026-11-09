package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入库单记录（入库单页签列表）
 */
@Data
@Schema(description = "入库单记录")
public class MaterialStockInVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "入库单ID")
    private Long id;

    @Schema(description = "入库单名称")
    private String orderName;

    @Schema(description = "入库日期")
    private LocalDate inDate;

    @Schema(description = "经办人")
    private String handler;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

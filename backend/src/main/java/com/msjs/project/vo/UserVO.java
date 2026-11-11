package com.msjs.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户分页记录 VO（带所属区划名称，便于前端列表直接展示）
 */
@Data
@Schema(description = "用户分页记录")
public class UserVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "所属行政区划代码")
    private String regionCode;

    /**
     * 所属区划名称（LEFT JOIN t_sys_region 取出，便于列表展示）
     */
    @Schema(description = "所属区划名称")
    private String regionName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

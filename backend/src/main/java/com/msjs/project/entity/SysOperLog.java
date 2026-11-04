package com.msjs.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("t_sys_oper_log")
@Schema(description = "操作日志")
public class SysOperLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "操作人ID")
    private Long userId;

    @Schema(description = "操作人账号")
    private String username;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "操作描述")
    private String operation;

    @Schema(description = "HTTP方法")
    private String method;

    @Schema(description = "请求地址")
    private String url;

    @Schema(description = "操作IP")
    private String ip;

    @Schema(description = "请求参数")
    private String params;

    @Schema(description = "返回状态码")
    private Integer resultCode;

    @Schema(description = "异常信息")
    private String errorMsg;

    @Schema(description = "操作时间")
    private LocalDateTime operTime;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;
}

package com.msjs.project.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据大屏实时事件（来源操作日志）
 * 仅展示业务写操作：method 为 POST/PUT/DELETE 且返回成功，排除查询、登录鉴权、大屏轮询、验证码等噪声
 */
@Data
@Schema(description = "实时事件")
public class ScreenEventVO {

    /** 雪花ID超出JS安全整数范围，序列化为字符串，避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "日志主键（雪花ID，用于前端去重）")
    private Long id;

    @Schema(description = "操作人账号")
    private String username;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "操作描述")
    private String operation;

    @Schema(description = "事件摘要：村/对象/动作/数量，富文本粒度")
    private String summary;

    @Schema(description = "事件类别（用于前端彩色圆点映射）：declaration/approval/construction/acceptance/fund/material/archive/base/system/file")
    private String category;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "操作时间")
    private LocalDateTime operTime;

    /** 以下字段仅服务端用于摘要提取，不返回前端 */
    @JsonIgnore
    private String method;

    @JsonIgnore
    private String url;

    @JsonIgnore
    private String params;
}

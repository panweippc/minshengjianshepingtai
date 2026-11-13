package com.msjs.project.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.msjs.project.entity.ProjectAcceptanceAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 验收记录 VO
 * 同一条申报明细可多次验收（如未通过后整改再次验收），按时间倒序展示，第一条为最新一次
 */
@Data
@Schema(description = "验收记录")
public class AcceptanceRecordVO implements Serializable {

    @Schema(description = "验收记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "验收状态：ACCEPTED已验收 REJECTED未通过")
    private String acceptStatus;

    @Schema(description = "验收结果（字典 yan_shou_jie_guo）")
    private String acceptResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "验收时间")
    private LocalDateTime acceptTime;

    @Schema(description = "验收人")
    private String acceptBy;

    @Schema(description = "验收备注")
    private String remark;

    @Schema(description = "验收资料附件")
    private List<ProjectAcceptanceAttachment> attachments;
}

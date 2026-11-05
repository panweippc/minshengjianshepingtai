package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 户基础信息保存 DTO（新增/编辑共用）
 */
@Data
@Schema(description = "户基础信息保存请求")
public class HouseholdSaveDTO {

    /** 编辑时必填，新增时不传（后端生成雪花ID） */
    @Schema(description = "主键ID（编辑时传入）")
    private Long id;

    /** 户号：新增时由用户手工录入，全局唯一；编辑时不可修改，回传原值 */
    @NotBlank(message = "请输入户号")
    @Schema(description = "户号（业务编码，新增时人工录入，编辑时回传原值且不可修改）")
    private String householdNo;

    @NotBlank(message = "请选择行政村")
    @Size(max = 20, message = "行政村区划编码长度不超过20")
    @Schema(description = "所属行政村区划编码")
    private String regionCode;

    @NotBlank(message = "请输入户主姓名")
    @Size(max = 32, message = "户主姓名长度不超过32")
    @Schema(description = "户主姓名")
    private String headName;

    @Size(max = 20, message = "身份证号长度不超过20")
    @Pattern(regexp = "^$|^[0-9Xx]{15,20}$", message = "身份证号格式不正确")
    @Schema(description = "户主身份证号")
    private String headIdCard;

    @Size(max = 20, message = "联系电话长度不超过20")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "家庭人口")
    private Integer familyPopulation;

    @Schema(description = "房屋面积（㎡）")
    private BigDecimal houseArea;

    @Size(max = 8, message = "房屋类型编码长度不超过8")
    @Schema(description = "房屋类型（字典 fang_wu_jie_gou_lei_xing）")
    private String houseType;

    @Size(max = 8, message = "户类型编码长度不超过8")
    @Schema(description = "户类型（字典 hu_lei_xing）")
    private String householdType;

    @Size(max = 64, message = "产权编号长度不超过64")
    @Schema(description = "产权编号")
    private String propertyNo;

    @Schema(description = "建档时间")
    private LocalDate archiveTime;

    @Size(max = 512, message = "备注长度不超过512")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "附件列表（可新增多条）")
    private List<AttachmentItem> attachments;

    /**
     * 附件子项
     */
    @Data
    @Schema(description = "附件子项")
    public static class AttachmentItem {

        /** 编辑时传入已有附件ID，新增附件时不传 */
        @Schema(description = "附件ID（已有附件回传原值）")
        private Long id;

        @Size(max = 8, message = "附件类型编码长度不超过8")
        @Schema(description = "附件类型（字典 fu_jian_lei_xing）")
        private String attachmentType;

        @Size(max = 256, message = "原始文件名长度不超过256")
        @Schema(description = "原始文件名")
        private String fileName;

        @Size(max = 512, message = "MinIO对象key长度不超过512")
        @Schema(description = "MinIO 对象存储 key")
        private String fileKey;

        @Size(max = 64, message = "存储桶名称长度不超过64")
        @Schema(description = "存储桶")
        private String bucket;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Size(max = 256, message = "附件备注长度不超过256")
        @Schema(description = "附件备注")
        private String remark;
    }
}

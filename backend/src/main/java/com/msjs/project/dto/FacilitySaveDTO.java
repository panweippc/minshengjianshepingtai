package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 公共基础设施保存入参
 */
@Data
@Schema(description = "公共基础设施保存入参")
public class FacilitySaveDTO implements Serializable {

    @Schema(description = "主键（编辑时传入）")
    private Long id;

    @NotBlank(message = "请输入设施编号")
    @Size(max = 32, message = "设施编号长度不能超过32个字符")
    @Schema(description = "设施编号（业务编码，全局唯一）")
    private String facilityCode;

    @NotBlank(message = "请输入设施名称")
    @Size(max = 128, message = "设施名称长度不能超过128个字符")
    @Schema(description = "设施名称")
    private String facilityName;

    @Size(max = 16, message = "设施类型长度不能超过16个字符")
    @Schema(description = "设施类型（字典 she_shi_lei_xing）")
    private String facilityType;

    @NotBlank(message = "请选择所属行政村")
    @Size(max = 20, message = "区划代码长度不能超过20个字符")
    @Schema(description = "所属行政村区划代码")
    private String regionCode;

    @Size(max = 64, message = "行政村名称长度不能超过64个字符")
    @Schema(description = "所属行政村名称")
    private String regionName;

    @Size(max = 255, message = "具体位置长度不能超过255个字符")
    @Schema(description = "具体位置")
    private String location;

    @Schema(description = "建成年份")
    private Integer buildYear;

    @Schema(description = "数量")
    private BigDecimal quantity;

    @Size(max = 16, message = "单位长度不能超过16个字符")
    @Schema(description = "单位")
    private String unit;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    @Valid
    @Schema(description = "附件列表")
    private List<FacilityAttachmentSaveDTO> attachments;

    /**
     * 附件保存入参
     */
    @Data
    @Schema(description = "设施附件保存入参")
    public static class FacilityAttachmentSaveDTO implements Serializable {

        @Schema(description = "附件ID（编辑时传入）")
        private Long id;

        @Size(max = 16, message = "附件类型长度不能超过16个字符")
        @Schema(description = "附件类型（字典 fu_jian_lei_xing）")
        private String attachmentType;

        @Size(max = 255, message = "文件名长度不能超过255个字符")
        @Schema(description = "原始文件名")
        private String fileName;

        @Size(max = 512, message = "文件路径长度不能超过512个字符")
        @Schema(description = "存储路径")
        private String filePath;

        @Size(max = 64, message = "文件哈希长度不能超过64个字符")
        @Schema(description = "SHA256哈希")
        private String fileHash;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Size(max = 500, message = "附件备注长度不能超过500个字符")
        @Schema(description = "备注")
        private String remark;
    }
}

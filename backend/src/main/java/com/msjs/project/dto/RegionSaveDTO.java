package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 村委会区划保存入参
 */
@Data
@Schema(description = "村委会区划保存入参")
public class RegionSaveDTO implements Serializable {

    @NotBlank(message = "请输入区划名称")
    @Size(max = 64, message = "区划名称长度不能超过64个字符")
    @Schema(description = "区划名称")
    private String name;

    @NotBlank(message = "请输入区划编码")
    @Size(max = 20, message = "区划编码长度不能超过20个字符")
    @Pattern(regexp = "^[0-9A-Za-z-]+$", message = "区划编码仅支持数字、字母和短横线")
    @Schema(description = "区划编码")
    private String code;

    @Size(max = 20, message = "上级区划编码长度不能超过20个字符")
    @Schema(description = "上级区划编码（添加下级时传入）")
    private String parentCode;

    @NotBlank(message = "请选择区划类型")
    @Pattern(regexp = "^[1-3]$", message = "区划类型取值须为1乡镇、2村、3组")
    @Schema(description = "区划类型（字典 qu_hua_lei_xing：1乡镇 2村 3组）")
    private String regionType;

    @Size(max = 32, message = "党支部书记姓名长度不能超过32个字符")
    @Schema(description = "党支部书记")
    private String secretary;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的党支部书记手机号")
    @Schema(description = "党支部书记电话")
    private String secretaryPhone;

    @Size(max = 32, message = "村委会主任姓名长度不能超过32个字符")
    @Schema(description = "村委会主任")
    private String director;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的村委会主任手机号")
    @Schema(description = "村委会主任电话")
    private String directorPhone;

    @Schema(description = "排序号")
    private Integer sort;
}

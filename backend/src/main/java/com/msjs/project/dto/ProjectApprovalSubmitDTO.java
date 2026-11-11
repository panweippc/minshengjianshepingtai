package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 立项申请 DTO：申报审核模块"已通过"页签 → 新建立项弹窗 → POST /api/project/approval/submit
 */
@Data
@Schema(description = "立项申请提交 DTO")
public class ProjectApprovalSubmitDTO {

    @NotBlank(message = "立项编号不能为空")
    @Size(max = 32, message = "立项编号最多 32 字符")
    @Schema(description = "项目/立项编号（前端手动填，唯一校验）", example = "LXR-20261111-001")
    private String approvalCode;

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 255)
    @Schema(description = "项目名称（必填）")
    private String projectName;

    @Schema(description = "项目类别（字典：xiang_mu_lei_bie）")
    private String projectCategory;

    @Schema(description = "项目类型（字典：xiang_mu_lei_xing）")
    private String projectType;

    @Schema(description = "项目来源")
    private String projectSource;

    @Schema(description = "项目预算（选填）")
    private BigDecimal budget;

    @Schema(description = "项目负责人")
    private String leader;

    @Schema(description = "申报人（MD 里的申报人字段，选填）")
    private String applyBy;

    @Schema(description = "建设依据（必填）")
    private String remark;

    // ---- 所属乡镇 / 村组（选填，弹窗里多选村组） ----
    @Schema(description = "所属乡镇 region_code")
    private String townCode;

    @Schema(description = "所属乡镇 region_name")
    private String townName;

    @Schema(description = "所属村组 code 集合（多选）")
    private List<String> villageCodes;

    // ---- 被选中的申报单（立项弹窗里勾选的主申报单 ID 集合） ----
    @NotNull(message = "至少勾选一条申报单")
    @Schema(description = "要新建立项的 t_declaration.id 集合")
    private List<Long> declarationIds;

    // ---- 立项资料附件（多行动态表单） ----
    @Valid
    @Schema(description = "立项资料附件（多行动态表单）")
    private List<AttachmentItem> attachments;

    @Data
    @Schema(description = "立项资料附件行")
    public static class AttachmentItem {
        @Schema(description = "附件类型（字典：li_xiang_fu_jian_lei_xing）", requiredMode = Schema.RequiredMode.REQUIRED)
        private String attachmentType;

        @Schema(description = "MinIO file_key（由 /api/file/upload 上传后返回）", requiredMode = Schema.RequiredMode.REQUIRED)
        private String fileKey;

        @Schema(description = "桶名，可选；后端若为空则默认用 upload-temp")
        private String bucket;

        @Schema(description = "原文件名（展示用）")
        private String fileName;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Schema(description = "备注")
        private String remark;
    }
}

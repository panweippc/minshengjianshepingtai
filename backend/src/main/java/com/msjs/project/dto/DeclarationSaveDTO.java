package com.msjs.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 申报保存 DTO（新增/编辑共用）
 * 主表 + 明细 + 附件 在同一事务中保存
 */
@Data
@Schema(description = "申报保存请求")
public class DeclarationSaveDTO {

    /** 编辑时必填，新增时不传 */
    @Schema(description = "主键ID（编辑时传入）")
    private Long id;

    /**
     * 业务编号（申报编号）：可选
     * - 新增时：前端弹窗预生成并回传，后端校验唯一性；不传则后端自动生成
     * - 编辑时：业务编号只读，不可修改（前端不回传或回传原值，后端忽略传入值，用原值）
     */
    @Size(max = 32, message = "申报编号长度不超过32")
    @Schema(description = "申报编号（业务编码，可选，重复时保存会提示）")
    private String businessCode;

    @NotBlank(message = "请输入申报标题")
    @Size(max = 128, message = "申报标题长度不超过128")
    @Schema(description = "申报标题")
    private String title;

    /** 所属乡镇由后端根据登录人区划自动回填，前端只读回传，不能修改 */
    @NotBlank(message = "所属乡镇不能为空（由系统自动回填）")
    @Schema(description = "所属乡镇区划编码（登录人自动回填，只读）")
    private String townCode;

    @NotBlank(message = "请选择所属村组")
    @Schema(description = "所属村组区划编码（下拉选择）")
    private String villageGroupCode;

    @NotBlank(message = "请选择申报类型")
    @Schema(description = "申报类型（字典 shen_bao_lei_xing）")
    private String declarationType;

    @NotBlank(message = "请选择申报类别")
    @Pattern(regexp = "^(1|2)$", message = "申报类别只能是 1(到户项目) 或 2(公建项目)")
    @Schema(description = "申报类别（1到户项目 / 2公建项目），与字典 shen_bao_lei_bie 对齐")
    private String declarationCategory;

    @NotNull(message = "请填写申报金额")
    @DecimalMin(value = "0.00", inclusive = false, message = "申报金额必须大于0")
    @Schema(description = "申报金额（元，申报单总金额）")
    private BigDecimal amount;

    @Size(max = 1024, message = "备注说明长度不超过1024")
    @Schema(description = "备注说明（申报原因、情况说明）")
    private String remark;

    /**
     * 指定接收审核人（可选，仅新增时生效）
     * - 新增时传了审核人 → 状态直接 submitted（跳过 draft）
     * - 新增时不传 → 还是 draft（向后兼容，可后续在列表行点"提交"选审核人）
     * - 编辑时忽略此字段（编辑已存在的单子，不改变审核人）
     */
    @Schema(description = "新增时可选：指定接收审核人用户ID，传了则直接进入待审核(submitted)状态")
    private Long recipientUserId;

    // ============ 明细多选（按 declarationCategory 区分） ============

    /**
     * 农户 ID 列表（仅 declarationCategory=household 时生效）
     * 后端保存时会去农户档案表查最新数据作为快照写入明细
     */
    @Schema(description = "农户ID列表（到户项目必填，至少1户）")
    private List<Long> householdIds;

    /**
     * 公建设施 ID 列表（仅 declarationCategory=public 时生效）
     */
    @Schema(description = "公建设施ID列表（公建项目必填，至少1个）")
    private List<Long> facilityIds;

    // ============ 附件多行 ============

    /**
     * 申报单级附件（最多30组，前端控制上限）
     */
    @Schema(description = "申报材料附件（最多30组）")
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

        @Schema(description = "附件类型（字典 fu_jian_lei_xing）")
        private String attachmentType;

        @Schema(description = "原始文件名")
        private String fileName;

        @Schema(description = "MinIO 对象存储 key")
        private String fileKey;

        @Schema(description = "存储桶")
        private String bucket;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Schema(description = "附件备注")
        private String remark;
    }
}

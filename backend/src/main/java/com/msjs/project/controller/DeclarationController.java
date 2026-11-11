package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.DeclarationAuditDTO;
import com.msjs.project.dto.DeclarationDetailAuditDTO;
import com.msjs.project.dto.DeclarationDetailEditDTO;
import com.msjs.project.dto.DeclarationDetailRevertDTO;
import com.msjs.project.dto.DeclarationSaveDTO;
import com.msjs.project.dto.DeclarationSubmitDTO;
import com.msjs.project.service.DeclarationService;
import com.msjs.project.vo.DeclarationDetailVO;
import com.msjs.project.vo.UserRegionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 事项申报管理 Controller
 * 所属乡镇自动回填规则：根据登录人绑定区划向上递归找第一个乡镇区划
 */
@Tag(name = "事项申报管理")
@RestController
@RequestMapping("/api/declaration")
@RequiredArgsConstructor
public class DeclarationController {

    private final DeclarationService declarationService;

    // ============ 业务 CRUD ============

    @Operation(summary = "申报单分页查询（创建人视角）")
    @RequirePerm("declaration:list")
    @GetMapping("/page")
    public Result<PageVO<DeclarationDetailVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "申报标题") @RequestParam(required = false) String title,
            @Parameter(description = "申报类型（字典 shen_bao_lei_xing）") @RequestParam(required = false) String declarationType,
            @Parameter(description = "申报类别（字典 shen_bao_lei_bie：household到户 / public公建）") @RequestParam(required = false) String declarationCategory,
            @Parameter(description = "申报状态（draft/submitted/approved/rejected）") @RequestParam(required = false) String status,
            @Parameter(description = "乡镇区划编码") @RequestParam(required = false) String townCode,
            @Parameter(description = "村组区划编码") @RequestParam(required = false) String villageGroupCode) {
        return Result.ok(declarationService.page(pageNum, pageSize, title, declarationType,
                declarationCategory, status, townCode, villageGroupCode));
    }

    @Operation(summary = "接收申报分页查询（审核人视角：recipient_user_id=当前登录人）")
    @RequirePerm("declaration:audit")
    @GetMapping("/received-page")
    public Result<PageVO<DeclarationDetailVO>> receivedPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "申报标题") @RequestParam(required = false) String title,
            @Parameter(description = "申报类型") @RequestParam(required = false) String declarationType,
            @Parameter(description = "申报类别") @RequestParam(required = false) String declarationCategory,
            @Parameter(description = "申报状态（查待审核传 submitted）") @RequestParam(required = false) String status) {
        return Result.ok(declarationService.receivedPage(pageNum, pageSize, title, declarationType,
                declarationCategory, status));
    }

    @Operation(summary = "申报单详情（含农户/公建明细 + 附件树）")
    @RequirePerm("declaration:view")
    @GetMapping("/detail/{id}")
    public Result<DeclarationDetailVO> detail(
            @Parameter(description = "申报单ID") @PathVariable Long id) {
        return Result.ok(declarationService.detail(id));
    }

    @Operation(summary = "新增申报单")
    @RequirePerm("declaration:add")
    @OperLog(module = "事项申报", operation = "新增申报单")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody DeclarationSaveDTO dto) {
        return Result.ok(declarationService.add(dto));
    }

    @Operation(summary = "编辑申报单（仅草稿状态可编辑）")
    @RequirePerm("declaration:edit")
    @OperLog(module = "事项申报", operation = "编辑申报单")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody DeclarationSaveDTO dto) {
        declarationService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除申报单（仅草稿可删）")
    @RequirePerm("declaration:delete")
    @OperLog(module = "事项申报", operation = "删除申报单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "申报单ID") @PathVariable Long id) {
        declarationService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "提交申报单（指定接收审核人，draft→submitted）")
    @RequirePerm("declaration:edit")
    @OperLog(module = "事项申报", operation = "提交申报")
    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody DeclarationSubmitDTO dto) {
        declarationService.submit(dto);
        return Result.ok();
    }

    @Operation(summary = "审核申报单（approve 通过 / reject 驳回，仅接收人可操作）")
    @RequirePerm("declaration:audit")
    @OperLog(module = "事项申报", operation = "审核申报")
    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody DeclarationAuditDTO dto) {
        declarationService.audit(dto);
        return Result.ok();
    }

    @Operation(summary = "明细级审核（approve 通过 / reject 驳回，农户或公建设施明细）")
    @RequirePerm("declaration:audit")
    @OperLog(module = "事项申报", operation = "明细审核")
    @PostMapping("/audit-detail")
    public Result<Void> auditDetail(@Valid @RequestBody DeclarationDetailAuditDTO dto) {
        declarationService.auditDetail(dto);
        return Result.ok();
    }

    @Operation(summary = "明细撤销（收回审核结论，明细回到待审核 pending，父申报单回到 submitted 并保留审核指派）")
    @RequirePerm("declaration:audit")
    @OperLog(module = "事项申报", operation = "明细撤销")
    @PostMapping("/revert-detail")
    public Result<Void> revertDetail(@Valid @RequestBody DeclarationDetailRevertDTO dto) {
        declarationService.revertDetail(dto);
        return Result.ok();
    }

    @Operation(summary = "预生成申报编号（弹窗打开时调用，只读展示）")
    @RequirePerm("declaration:add")
    @GetMapping("/next-business-code")
    public Result<String> nextBusinessCode() {
        return Result.ok(declarationService.generateBusinessCode());
    }

    @Operation(summary = "校验申报编号是否唯一")
    @GetMapping("/check-business-code")
    public Result<Boolean> checkBusinessCode(
            @Parameter(description = "待校验的申报编号") @RequestParam String businessCode) {
        return Result.ok(declarationService.isBusinessCodeUnique(businessCode));
    }

    // ============ 弹窗初始化 & 联动辅助接口 ============

    @Operation(summary = "当前登录人区划上下文（向上递归找所属乡镇）")
    @GetMapping("/user-region")
    public Result<UserRegionVO> userRegion() {
        return Result.ok(declarationService.resolveUserTown());
    }

    @Operation(summary = "乡镇下村组列表（弹窗所属村组下拉数据源）")
    @GetMapping("/village-groups")
    public Result<List<Map<String, Object>>> villageGroups(
            @Parameter(description = "乡镇区划编码") @RequestParam String townCode) {
        return Result.ok(declarationService.listVillageGroupsByTown(townCode));
    }

    @Operation(summary = "村组下农户列表（到户项目弹窗多选用）")
    @GetMapping("/households-by-vg")
    public Result<List<Map<String, Object>>> householdsByVg(
            @Parameter(description = "村组区划编码") @RequestParam String villageGroupCode) {
        return Result.ok(declarationService.listHouseholdsByVillageGroup(villageGroupCode));
    }

    @Operation(summary = "村组下公建设施列表（公建项目弹窗多选用）")
    @GetMapping("/facilities-by-vg")
    public Result<List<Map<String, Object>>> facilitiesByVg(
            @Parameter(description = "村组区划编码") @RequestParam String villageGroupCode) {
        return Result.ok(declarationService.listFacilitiesByVillageGroup(villageGroupCode));
    }

    @Operation(summary = "审核人下拉（keyword 支持按姓名/账号模糊搜索，最多返回 100 条）")
    @RequirePerm("declaration:edit")
    @GetMapping("/recipients")
    public Result<List<Map<String, Object>>> recipients(
            @Parameter(description = "搜索关键字（可空）") @RequestParam(required = false) String keyword) {
        return Result.ok(declarationService.listRecipients(keyword));
    }

    // ============ 明细级编辑接口（展开行里用） ============

    @Operation(summary = "编辑农户明细（单户金额 + 事项说明 + 附件）")
    @RequirePerm("declaration:edit")
    @OperLog(module = "事项申报", operation = "编辑农户明细")
    @PutMapping("/household-detail")
    public Result<Void> editHouseholdDetail(@Valid @RequestBody DeclarationDetailEditDTO dto) {
        declarationService.editHouseholdDetail(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑公建设施明细（单设施金额 + 事项说明 + 附件）")
    @RequirePerm("declaration:edit")
    @OperLog(module = "事项申报", operation = "编辑公建设施明细")
    @PutMapping("/public-facility-detail")
    public Result<Void> editPublicFacilityDetail(@Valid @RequestBody DeclarationDetailEditDTO dto) {
        declarationService.editPublicFacilityDetail(dto);
        return Result.ok();
    }
}

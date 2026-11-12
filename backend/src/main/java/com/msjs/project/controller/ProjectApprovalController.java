package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.ApprovalEditDTO;
import com.msjs.project.dto.ProjectApprovalSubmitDTO;
import com.msjs.project.service.ProjectApprovalService;
import com.msjs.project.vo.ApprovalAttachmentVO;
import com.msjs.project.vo.ApprovalCountVO;
import com.msjs.project.vo.ApprovalVO;
import com.msjs.project.vo.ProjectApprovalSubmitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 立项管理模块后端 Controller。
 * 覆盖立项列表（待审核/已驳回）、数量统计、查看详情、附件查询、编辑、审核通过、驳回、退回。
 */
@Tag(name = "立项管理")
@RestController
@RequestMapping("/api/project/approval")
@RequiredArgsConstructor
public class ProjectApprovalController {

    private final ProjectApprovalService projectApprovalService;

    /**
     * 提交立项申请。
     * 业务动作：保存立项主表 + declare + detail + attachment，
     * 同时把所选申报单下的 approved 明细 audit_status 改成 established，
     * 父申报单状态回算并维持 approved。
     */
    @Operation(summary = "提交立项申请")
    @PostMapping("/submit")
    public Result<ProjectApprovalSubmitVO> submit(@Valid @RequestBody ProjectApprovalSubmitDTO dto) {
        return Result.ok(projectApprovalService.submit(dto));
    }

    @Operation(summary = "立项分页查询（树形：项目 → 申报内容 → 农户/设施明细）")
    @RequirePerm("project:approval:list")
    @GetMapping("/page")
    public Result<PageVO<ApprovalVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "页签：pending待审核 rejected已驳回") @RequestParam(defaultValue = "pending") String tab,
            @Parameter(description = "项目名称") @RequestParam(required = false) String projectName,
            @Parameter(description = "项目类别（字典 xiang_mu_lei_bie）") @RequestParam(required = false) String projectCategory,
            @Parameter(description = "项目类型（字典 xiang_mu_lei_xing）") @RequestParam(required = false) String projectType,
            @Parameter(description = "提交开始日期 yyyy-MM-dd") @RequestParam(required = false) String startTime,
            @Parameter(description = "提交结束日期 yyyy-MM-dd") @RequestParam(required = false) String endTime) {
        return Result.ok(projectApprovalService.page(pageNum, pageSize, tab, projectName,
                projectCategory, projectType, startTime, endTime));
    }

    @Operation(summary = "立项页签数量统计（按当前筛选条件）")
    @RequirePerm("project:approval:list")
    @GetMapping("/counts")
    public Result<ApprovalCountVO> counts(
            @Parameter(description = "项目名称") @RequestParam(required = false) String projectName,
            @Parameter(description = "项目类别（字典 xiang_mu_lei_bie）") @RequestParam(required = false) String projectCategory,
            @Parameter(description = "项目类型（字典 xiang_mu_lei_xing）") @RequestParam(required = false) String projectType,
            @Parameter(description = "提交开始日期 yyyy-MM-dd") @RequestParam(required = false) String startTime,
            @Parameter(description = "提交结束日期 yyyy-MM-dd") @RequestParam(required = false) String endTime) {
        return Result.ok(projectApprovalService.counts(projectName, projectCategory, projectType, startTime, endTime));
    }

    @Operation(summary = "立项详情（含申报内容与明细树）")
    @RequirePerm("project:approval:view")
    @GetMapping("/detail/{id}")
    public Result<ApprovalVO> detail(@Parameter(description = "立项ID") @PathVariable Long id) {
        return Result.ok(projectApprovalService.detail(id));
    }

    @Operation(summary = "查询立项附件列表（项目级 + 申报材料级）")
    @RequirePerm("project:approval:view")
    @GetMapping("/attachments/{approvalId}")
    public Result<List<ApprovalAttachmentVO>> attachments(
            @Parameter(description = "立项ID") @PathVariable Long approvalId) {
        return Result.ok(projectApprovalService.attachments(approvalId));
    }

    @Operation(summary = "编辑立项（仅待审核状态，同步维护立项资料附件）")
    @RequirePerm("project:approval:edit")
    @OperLog(module = "立项管理", operation = "编辑立项")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody ApprovalEditDTO dto) {
        projectApprovalService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "审核通过（待审核 → 已通过）")
    @RequirePerm("project:approval:approve")
    @OperLog(module = "立项管理", operation = "立项审核通过")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@Parameter(description = "立项ID") @PathVariable Long id) {
        projectApprovalService.approve(id);
        return Result.ok();
    }

    @Operation(summary = "审核驳回（待审核 → 已驳回）")
    @RequirePerm("project:approval:reject")
    @OperLog(module = "立项管理", operation = "立项审核驳回")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@Parameter(description = "立项ID") @PathVariable Long id) {
        projectApprovalService.reject(id);
        return Result.ok();
    }

    @Operation(summary = "退回（已驳回 → 待审核）")
    @RequirePerm("project:approval:return")
    @OperLog(module = "立项管理", operation = "立项退回待审核")
    @PostMapping("/{id}/return")
    public Result<Void> returnToPending(@Parameter(description = "立项ID") @PathVariable Long id) {
        projectApprovalService.returnToPending(id);
        return Result.ok();
    }
}

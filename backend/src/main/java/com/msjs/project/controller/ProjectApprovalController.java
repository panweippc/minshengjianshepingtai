package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.ApprovalEditDTO;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.ProjectApprovalAttachment;
import com.msjs.project.service.ProjectApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 立项管理接口（项目全周期管控）
 * 涉及立项查看、编辑、审核通过、驳回、退回，均写入操作日志
 */
@Tag(name = "立项管理")
@RestController
@RequestMapping("/api/project/approval")
@RequiredArgsConstructor
public class ProjectApprovalController {

    private final ProjectApprovalService approvalService;

    @Operation(summary = "立项分页查询（树形：项目 → 申报内容 → 农户/设施明细）")
    @RequirePerm("project:approval:list")
    @GetMapping("/page")
    public Result<PageVO<ProjectApproval>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "页签：all全部 pending待审核 rejected已驳回") @RequestParam(defaultValue = "all") String tab,
            @Parameter(description = "项目名称") @RequestParam(required = false) String projectName,
            @Parameter(description = "项目类别（字典 xiang_mu_lei_bie）") @RequestParam(required = false) String projectCategory,
            @Parameter(description = "项目来源（字典 xiang_mu_lai_yuan）") @RequestParam(required = false) String projectSource,
            @Parameter(description = "提交开始日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startTime,
            @Parameter(description = "提交结束日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endTime) {
        return Result.ok(approvalService.page(pageNum, pageSize, tab, projectName,
                projectCategory, projectSource, startTime, endTime));
    }

    @Operation(summary = "立项各页签数量统计（按当前筛选条件）")
    @RequirePerm("project:approval:list")
    @GetMapping("/counts")
    public Result<Map<String, Long>> counts(
            @Parameter(description = "项目名称") @RequestParam(required = false) String projectName,
            @Parameter(description = "项目类别（字典 xiang_mu_lei_bie）") @RequestParam(required = false) String projectCategory,
            @Parameter(description = "项目来源（字典 xiang_mu_lai_yuan）") @RequestParam(required = false) String projectSource,
            @Parameter(description = "提交开始日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startTime,
            @Parameter(description = "提交结束日期") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endTime) {
        return Result.ok(approvalService.counts(projectName, projectCategory, projectSource, startTime, endTime));
    }

    @Operation(summary = "立项详情（含申报内容与明细树）")
    @RequirePerm("project:approval:view")
    @OperLog(module = "立项管理", operation = "查看立项详情")
    @GetMapping("/detail/{id}")
    public Result<ProjectApproval> detail(@Parameter(description = "立项ID") @PathVariable Long id) {
        return Result.ok(approvalService.detail(id));
    }

    @Operation(summary = "查询立项附件列表")
    @RequirePerm("project:approval:view")
    @GetMapping("/attachments/{approvalId}")
    public Result<List<ProjectApprovalAttachment>> attachments(
            @Parameter(description = "立项ID") @PathVariable Long approvalId) {
        return Result.ok(approvalService.listAttachments(approvalId));
    }

    @Operation(summary = "编辑立项（仅待审核状态，含立项资料附件）")
    @RequirePerm("project:approval:edit")
    @OperLog(module = "立项管理", operation = "编辑立项")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody ApprovalEditDTO dto) {
        approvalService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "审核通过")
    @RequirePerm("project:approval:approve")
    @OperLog(module = "立项管理", operation = "审核通过立项")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@Parameter(description = "立项ID") @PathVariable Long id) {
        approvalService.approve(id);
        return Result.ok();
    }

    @Operation(summary = "审核驳回")
    @RequirePerm("project:approval:reject")
    @OperLog(module = "立项管理", operation = "驳回立项")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@Parameter(description = "立项ID") @PathVariable Long id) {
        approvalService.reject(id);
        return Result.ok();
    }

    @Operation(summary = "退回（已驳回 → 待审核）")
    @RequirePerm("project:approval:return")
    @OperLog(module = "立项管理", operation = "退回立项")
    @PostMapping("/{id}/return")
    public Result<Void> returnBack(@Parameter(description = "立项ID") @PathVariable Long id) {
        approvalService.returnBack(id);
        return Result.ok();
    }
}

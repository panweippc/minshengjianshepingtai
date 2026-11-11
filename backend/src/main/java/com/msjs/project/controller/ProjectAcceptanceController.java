package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.AcceptDetailDTO;
import com.msjs.project.dto.ArchiveDTO;
import com.msjs.project.service.ProjectAcceptanceService;
import com.msjs.project.vo.AcceptanceNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验收管理接口
 * 数据来源：施工管理"验收"按钮发起的项目（accept_status = APPLYING）
 * 三级树形：项目 → 申报内容 → 农户/公共基础设施明细
 * 验收进度 = 已验收明细数 / 全部明细数 × 100%
 */
@Tag(name = "验收管理")
@RestController
@RequestMapping("/api/project/acceptance")
@RequiredArgsConstructor
public class ProjectAcceptanceController {

    private final ProjectAcceptanceService acceptanceService;

    @Operation(summary = "验收分页查询（仅已发起验收的项目）")
    @RequirePerm("project:acceptance:list")
    @GetMapping("/page")
    public Result<PageVO<AcceptanceNodeVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "项目名称") @RequestParam(required = false) String projectName,
            @Parameter(description = "项目类别（字典 xiang_mu_lei_bie）") @RequestParam(required = false) String projectCategory,
            @Parameter(description = "项目类型（字典 xiang_mu_lei_xing）") @RequestParam(required = false) String projectSource) {
        return Result.ok(acceptanceService.page(pageNum, pageSize, projectName, projectCategory, projectSource));
    }

    @Operation(summary = "施工管理发起验收（标记项目进入验收管理）")
    @RequirePerm("project:construction:edit")
    @OperLog(module = "施工管理", operation = "发起验收")
    @PostMapping("/apply")
    public Result<Void> apply(
            @Parameter(description = "立项ID") @RequestParam Long approvalId,
            @Parameter(description = "操作人") @RequestParam(required = false) String applyBy) {
        acceptanceService.apply(approvalId, applyBy);
        return Result.ok();
    }

    @Operation(summary = "三级明细验收确认")
    @RequirePerm("project:acceptance:accept")
    @OperLog(module = "验收管理", operation = "明细验收")
    @PostMapping("/accept")
    public Result<Void> accept(@Valid @RequestBody AcceptDetailDTO dto) {
        acceptanceService.acceptDetail(dto);
        return Result.ok();
    }

    @Operation(summary = "项目归档（验收进度必须 100%）")
    @RequirePerm("project:acceptance:archive")
    @OperLog(module = "验收管理", operation = "项目归档")
    @PostMapping("/archive")
    public Result<Void> archive(@Valid @RequestBody ArchiveDTO dto) {
        acceptanceService.archive(dto);
        return Result.ok();
    }
}

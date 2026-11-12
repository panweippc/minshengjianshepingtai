package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.ConstructionEditDTO;
import com.msjs.project.dto.ConstructionLogEditDTO;
import com.msjs.project.service.ProjectConstructionService;
import com.msjs.project.service.ProjectFundService;
import com.msjs.project.vo.ConstructionLogVO;
import com.msjs.project.vo.FundNodeVO;
import com.msjs.project.vo.ConstructionNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 施工管理接口（项目全周期管控）
 * 查询审核通过立项的三级施工树，编辑项目/明细施工信息
 */
@Tag(name = "施工管理")
@RestController
@RequestMapping("/api/project/construction")
@RequiredArgsConstructor
public class ProjectConstructionController {

    private final ProjectConstructionService constructionService;
    private final ProjectFundService fundService;

    @Operation(summary = "施工分页查询（树形：项目 → 申报内容 → 农户/设施明细，含施工进度聚合）")
    @RequirePerm("project:construction:list")
    @GetMapping("/page")
    public Result<PageVO<ConstructionNodeVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "项目编码") @RequestParam(required = false) String projectCode,
            @Parameter(description = "项目名称") @RequestParam(required = false) String projectName,
            @Parameter(description = "项目类别（字典 xiang_mu_lei_bie）") @RequestParam(required = false) String projectCategory,
            @Parameter(description = "项目类型（字典 xiang_mu_lei_xing）") @RequestParam(required = false) String projectType) {
        return Result.ok(constructionService.page(pageNum, pageSize, projectCode, projectName,
                projectCategory, projectType));
    }

    @Operation(summary = "项目施工详情（施工队信息、施工日志、巡检合格率、附件及申报明细进度）")
    @RequirePerm("project:construction:list")
    @GetMapping("/{approvalId}")
    public Result<ConstructionNodeVO> detail(
            @Parameter(description = "立项ID") @PathVariable Long approvalId) {
        return Result.ok(constructionService.detail(approvalId));
    }

    @Operation(summary = "三级施工记录详情（关联项目、记录日期、巡检人、巡检状态、施工状态、施工内容及附件）")
    @RequirePerm("project:construction:list")
    @GetMapping("/detail/{detailId}/log")
    public Result<ConstructionLogVO> detailLog(
            @Parameter(description = "明细ID") @PathVariable Long detailId) {
        return Result.ok(constructionService.detailLog(detailId));
    }

    @Operation(summary = "三级施工记录历史（每次编辑留痕，倒序）")
    @RequirePerm("project:construction:list")
    @GetMapping("/detail/{detailId}/log/history")
    public Result<java.util.List<ConstructionLogVO>> detailLogHistory(
            @Parameter(description = "明细ID") @PathVariable Long detailId) {
        return Result.ok(constructionService.detailLogHistory(detailId));
    }

    @Operation(summary = "一级项目资金查看（与资金管理项目资金页签一致）")
    @RequirePerm("project:construction:list")
    @GetMapping("/approval/{approvalId}/fund")
    public Result<FundNodeVO> approvalFund(
            @Parameter(description = "立项ID") @PathVariable Long approvalId) {
        return Result.ok(fundService.approvalDetail(approvalId));
    }

    @Operation(summary = "二级事项申报资金查看（与资金管理事项申报资金页签一致）")
    @RequirePerm("project:construction:list")
    @GetMapping("/declare/{declareId}/fund")
    public Result<FundNodeVO> declareFund(
            @Parameter(description = "申报ID") @PathVariable Long declareId) {
        return Result.ok(fundService.declareDetail(declareId));
    }

    @Operation(summary = "三级明细资金查看（与资金管理三级查看一致，含申报材料附件及资金流水）")
    @RequirePerm("project:construction:list")
    @GetMapping("/detail/{detailId}/fund")
    public Result<FundNodeVO> detailFund(
            @Parameter(description = "明细ID") @PathVariable Long detailId) {
        return Result.ok(fundService.detail(detailId));
    }

    @Operation(summary = "保存三级施工记录（施工记录与附件，并同步明细施工状态）")
    @RequirePerm("project:construction:edit")
    @OperLog(module = "施工管理", operation = "保存三级施工记录")
    @PutMapping("/detail/log")
    public Result<Void> saveDetailLog(@Valid @RequestBody ConstructionLogEditDTO dto) {
        constructionService.saveDetailLog(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑施工信息（一级：施工单位/工期；三级：施工状态/工期）")
    @RequirePerm("project:construction:edit")
    @OperLog(module = "施工管理", operation = "编辑施工信息")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody ConstructionEditDTO dto) {
        constructionService.edit(dto);
        return Result.ok();
    }
}

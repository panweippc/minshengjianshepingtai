package com.msjs.project.controller;

import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.FundSaveDTO;
import com.msjs.project.entity.ProjectFund;
import com.msjs.project.service.ProjectFundService;
import com.msjs.project.vo.FundNodeVO;
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

import java.util.List;

/**
 * 资金管理接口（项目全周期管控）
 * 数据来源：立项管理中审核通过的项目，按三级树形展示；资金登记与下拨均写入操作日志
 */
@Tag(name = "资金管理")
@RestController
@RequestMapping("/api/project/fund")
@RequiredArgsConstructor
public class ProjectFundController {

    private final ProjectFundService fundService;

    @Operation(summary = "资金分页查询（树形：项目 → 申报内容 → 农户/设施明细）")
    @RequirePerm("project:fund:list")
    @GetMapping("/page")
    public Result<PageVO<FundNodeVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "项目名称") @RequestParam(required = false) String projectName,
            @Parameter(description = "项目类别（字典 xiang_mu_lei_bie）") @RequestParam(required = false) String projectCategory,
            @Parameter(description = "项目类型（字典 xiang_mu_lei_xing）") @RequestParam(required = false) String projectSource) {
        return Result.ok(fundService.page(pageNum, pageSize, projectName, projectCategory, projectSource));
    }

    @Operation(summary = "查询节点资金拨付记录")
    @RequirePerm("project:fund:view")
    @GetMapping("/records")
    public Result<List<ProjectFund>> records(
            @Parameter(description = "节点类型：approval项目 declare申报 detail明细") @RequestParam String nodeType,
            @Parameter(description = "节点ID") @RequestParam Long nodeId) {
        return Result.ok(fundService.listRecords(nodeType, nodeId));
    }

    @Operation(summary = "资金登记（一级项目节点，登记上级拨付金额）")
    @RequirePerm("project:fund:register")
    @OperLog(module = "资金管理", operation = "资金登记")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody FundSaveDTO dto) {
        if (!Constants.FUND_NODE_APPROVAL.equals(dto.getNodeType())) {
            throw new BizException("资金登记仅支持一级项目节点");
        }
        fundService.save(dto);
        return Result.ok();
    }

    @Operation(summary = "下拨资金（二级申报节点、三级明细节点）")
    @RequirePerm("project:fund:allocate")
    @OperLog(module = "资金管理", operation = "下拨资金")
    @PostMapping("/allocate")
    public Result<Void> allocate(@Valid @RequestBody FundSaveDTO dto) {
        if (Constants.FUND_NODE_APPROVAL.equals(dto.getNodeType())) {
            throw new BizException("项目节点请使用资金登记");
        }
        fundService.save(dto);
        return Result.ok();
    }
}

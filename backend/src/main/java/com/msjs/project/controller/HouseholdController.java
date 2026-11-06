package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.HouseholdSaveDTO;
import com.msjs.project.entity.Household;
import com.msjs.project.entity.HouseholdAttachment;
import com.msjs.project.service.HouseholdService;
import com.msjs.project.vo.RegionHouseholdCountVO;
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

/**
 * 户基础信息接口（基础信息管理）
 * 涉及档案类操作：新增、编辑、删除需写入操作日志
 */
@Tag(name = "户基础信息")
@RestController
@RequestMapping("/api/base/household")
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdService householdService;

    @Operation(summary = "户基础信息分页查询")
    @RequirePerm("base:household:list")
    @GetMapping("/page")
    public Result<PageVO<Household>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "户主姓名") @RequestParam(required = false) String headName,
            @Parameter(description = "户主身份证号") @RequestParam(required = false) String headIdCard,
            @Parameter(description = "树节点区划编码") @RequestParam(required = false) String nodeCode) {
        return Result.ok(householdService.page(pageNum, pageSize, headName, headIdCard, nodeCode));
    }

    @Operation(summary = "按区划统计农户数量（区划树徽标）")
    @RequirePerm("base:household:list")
    @GetMapping("/region-counts")
    public Result<List<RegionHouseholdCountVO>> regionCounts() {
        return Result.ok(householdService.regionCounts());
    }

    @Operation(summary = "户基础信息详情（含附件）")
    @RequirePerm("base:household:list")
    @GetMapping("/detail/{id}")
    public Result<Household> detail(@Parameter(description = "主键ID") @PathVariable Long id) {
        return Result.ok(householdService.detail(id));
    }

    @Operation(summary = "查询附件列表")
    @RequirePerm("base:household:list")
    @GetMapping("/attachments/{householdId}")
    public Result<List<HouseholdAttachment>> attachments(@Parameter(description = "户基础信息ID") @PathVariable Long householdId) {
        return Result.ok(householdService.listAttachments(householdId));
    }

    @Operation(summary = "新增户基础信息（含附件批量保存）")
    @RequirePerm("base:household:add")
    @OperLog(module = "户基础信息", operation = "新增户基础信息")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody HouseholdSaveDTO dto) {
        return Result.ok(householdService.add(dto));
    }

    @Operation(summary = "编辑户基础信息（含附件更新）")
    @RequirePerm("base:household:edit")
    @OperLog(module = "户基础信息", operation = "编辑户基础信息")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody HouseholdSaveDTO dto) {
        householdService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除户基础信息（主表 + 附件一并逻辑删除）")
    @RequirePerm("base:household:delete")
    @OperLog(module = "户基础信息", operation = "删除户基础信息")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "主键ID") @PathVariable Long id) {
        householdService.delete(id);
        return Result.ok();
    }
}

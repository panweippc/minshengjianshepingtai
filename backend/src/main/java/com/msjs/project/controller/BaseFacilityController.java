package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.FacilitySaveDTO;
import com.msjs.project.entity.BaseFacility;
import com.msjs.project.service.BaseFacilityService;
import com.msjs.project.vo.FacilityDetailVO;
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
 * 公共基础设施接口（基础信息管理）
 */
@Tag(name = "公共基础设施")
@RestController
@RequestMapping("/api/base/facility")
@RequiredArgsConstructor
public class BaseFacilityController {

    private final BaseFacilityService facilityService;

    @Operation(summary = "公共基础设施分页查询")
    @RequirePerm("base:facility:list")
    @GetMapping("/page")
    public Result<PageVO<BaseFacility>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "设施编号") @RequestParam(required = false) String facilityCode,
            @Parameter(description = "设施名称") @RequestParam(required = false) String facilityName,
            @Parameter(description = "设施类型") @RequestParam(required = false) String facilityType,
            @Parameter(description = "树节点区划编码") @RequestParam(required = false) String nodeCode) {
        return Result.ok(facilityService.page(pageNum, pageSize, facilityCode, facilityName, facilityType, nodeCode));
    }

    @Operation(summary = "设施详情（含附件和关联项目）")
    @RequirePerm("base:facility:list")
    @GetMapping("/detail/{id}")
    public Result<FacilityDetailVO> detail(@Parameter(description = "设施主键ID") @PathVariable Long id) {
        return Result.ok(facilityService.detail(id));
    }

    @Operation(summary = "新增设施")
    @RequirePerm("base:facility:add")
    @OperLog(module = "公共基础设施", operation = "新增设施")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody FacilitySaveDTO dto) {
        return Result.ok(facilityService.add(dto));
    }

    @Operation(summary = "编辑设施")
    @RequirePerm("base:facility:edit")
    @OperLog(module = "公共基础设施", operation = "编辑设施")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody FacilitySaveDTO dto) {
        facilityService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除设施")
    @RequirePerm("base:facility:delete")
    @OperLog(module = "公共基础设施", operation = "删除设施")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "设施主键ID") @PathVariable Long id) {
        facilityService.delete(id);
        return Result.ok();
    }
}

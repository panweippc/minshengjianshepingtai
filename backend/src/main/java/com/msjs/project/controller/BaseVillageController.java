package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.RegionSaveDTO;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.service.BaseVillageService;
import com.msjs.project.vo.RegionTreeVO;
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
 * 村委会信息接口（基础信息管理）
 */
@Tag(name = "村委会信息")
@RestController
@RequestMapping("/api/base/village")
@RequiredArgsConstructor
public class BaseVillageController {

    private final BaseVillageService villageService;

    @Operation(summary = "乡镇/村/组区划树")
    @RequirePerm("base:village:list")
    @GetMapping("/tree")
    public Result<List<RegionTreeVO>> tree() {
        return Result.ok(villageService.tree());
    }

    @Operation(summary = "村委会分页查询")
    @RequirePerm("base:village:list")
    @GetMapping("/page")
    public Result<PageVO<SysRegion>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "村名") @RequestParam(required = false) String name,
            @Parameter(description = "党支部书记") @RequestParam(required = false) String secretary,
            @Parameter(description = "村委会主任") @RequestParam(required = false) String director,
            @Parameter(description = "树节点区划编码") @RequestParam(required = false) String nodeCode) {
        return Result.ok(villageService.page(pageNum, pageSize, name, secretary, director, nodeCode));
    }

    @Operation(summary = "村委会树形列表（用于树形表格）")
    @RequirePerm("base:village:list")
    @GetMapping("/tree-page")
    public Result<List<RegionTreeVO>> treePage(
            @Parameter(description = "村名") @RequestParam(required = false) String name,
            @Parameter(description = "党支部书记") @RequestParam(required = false) String secretary,
            @Parameter(description = "村委会主任") @RequestParam(required = false) String director,
            @Parameter(description = "树节点区划编码") @RequestParam(required = false) String nodeCode) {
        return Result.ok(villageService.treePage(name, secretary, director, nodeCode));
    }

    @Operation(summary = "区划详情")
    @RequirePerm("base:village:list")
    @GetMapping("/detail/{code}")
    public Result<SysRegion> detail(@Parameter(description = "区划编码") @PathVariable String code) {
        return Result.ok(villageService.detail(code));
    }

    @Operation(summary = "新增区划")
    @RequirePerm("base:village:add")
    @OperLog(module = "村委会信息", operation = "新增区划")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody RegionSaveDTO dto) {
        return Result.ok(villageService.add(dto));
    }

    @Operation(summary = "编辑区划")
    @RequirePerm("base:village:edit")
    @OperLog(module = "村委会信息", operation = "编辑区划")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody RegionSaveDTO dto) {
        villageService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除区划")
    @RequirePerm("base:village:delete")
    @OperLog(module = "村委会信息", operation = "删除区划")
    @DeleteMapping("/{code}")
    public Result<Void> delete(@Parameter(description = "区划编码") @PathVariable String code) {
        villageService.delete(code);
        return Result.ok();
    }
}

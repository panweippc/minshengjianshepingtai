package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.DictSaveDTO;
import com.msjs.project.entity.SysDict;
import com.msjs.project.service.SysDictService;
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
 * 数据字典管理接口
 */
@Tag(name = "字典管理")
@RestController
@RequestMapping("/api/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    @Operation(summary = "字典分页查询")
    @RequirePerm("system:dict:list")
    @GetMapping("/page")
    public Result<PageVO<SysDict>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "字典类型") @RequestParam(required = false) String dictType,
            @Parameter(description = "字典描述") @RequestParam(required = false) String description,
            @Parameter(description = "字典标签") @RequestParam(required = false) String dictLabel) {
        return Result.ok(dictService.page(pageNum, pageSize, dictType, description, dictLabel));
    }

    @Operation(summary = "全部字典类型（筛选下拉用）")
    @RequirePerm("system:dict:list")
    @GetMapping("/types")
    public Result<List<String>> types() {
        return Result.ok(dictService.types());
    }

    @Operation(summary = "新增字典")
    @RequirePerm("system:dict:add")
    @OperLog(module = "字典管理", operation = "新增字典")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody DictSaveDTO dto) {
        return Result.ok(dictService.add(dto));
    }

    @Operation(summary = "编辑字典")
    @RequirePerm("system:dict:edit")
    @OperLog(module = "字典管理", operation = "编辑字典")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody DictSaveDTO dto) {
        dictService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除字典")
    @RequirePerm("system:dict:delete")
    @OperLog(module = "字典管理", operation = "删除字典")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.delete(id);
        return Result.ok();
    }
}

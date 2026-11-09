package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.MaterialItemSaveDTO;
import com.msjs.project.entity.MaterialItem;
import com.msjs.project.service.MaterialItemService;
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
 * 物品信息接口
 */
@Tag(name = "物品信息")
@RestController
@RequestMapping("/api/material/item")
@RequiredArgsConstructor
public class MaterialItemController {

    private final MaterialItemService itemService;

    @Operation(summary = "物品分页查询")
    @RequirePerm("material:item:list")
    @GetMapping("/page")
    public Result<PageVO<MaterialItem>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "物品名称") @RequestParam(required = false) String itemName,
            @Parameter(description = "物品分类") @RequestParam(required = false) String itemCategory,
            @Parameter(description = "仅查询低于预警数量") @RequestParam(required = false) Boolean lowWarning) {
        return Result.ok(itemService.page(pageNum, pageSize, itemName, itemCategory, lowWarning));
    }

    @Operation(summary = "根据ID集合查询物品（导出用）")
    @RequirePerm("material:item:list")
    @GetMapping("/list")
    public Result<List<MaterialItem>> list(@Parameter(description = "物品ID集合") @RequestParam List<Long> ids) {
        return Result.ok(itemService.listByIds(ids));
    }

    @Operation(summary = "新增物品")
    @RequirePerm("material:item:add")
    @OperLog(module = "物品信息", operation = "新增物品")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody MaterialItemSaveDTO dto) {
        return Result.ok(itemService.add(dto));
    }

    @Operation(summary = "编辑物品")
    @RequirePerm("material:item:edit")
    @OperLog(module = "物品信息", operation = "编辑物品")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody MaterialItemSaveDTO dto) {
        itemService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除物品")
    @RequirePerm("material:item:delete")
    @OperLog(module = "物品信息", operation = "删除物品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return Result.ok();
    }
}

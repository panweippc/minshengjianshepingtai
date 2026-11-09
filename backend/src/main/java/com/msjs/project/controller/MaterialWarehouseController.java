package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.MaterialWarehouseSaveDTO;
import com.msjs.project.entity.MaterialWarehouse;
import com.msjs.project.service.MaterialWarehouseService;
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

/**
 * 库房信息接口
 */
@Tag(name = "库房信息")
@RestController
@RequestMapping("/api/material/warehouse")
@RequiredArgsConstructor
public class MaterialWarehouseController {

    private final MaterialWarehouseService warehouseService;

    @Operation(summary = "库房分页查询")
    @RequirePerm("material:warehouse:list")
    @GetMapping("/page")
    public Result<PageVO<MaterialWarehouse>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "库房名称") @RequestParam(required = false) String warehouseName,
            @Parameter(description = "库房位置") @RequestParam(required = false) String location,
            @Parameter(description = "负责人") @RequestParam(required = false) String manager,
            @Parameter(description = "联系电话") @RequestParam(required = false) String phone) {
        return Result.ok(warehouseService.page(pageNum, pageSize, warehouseName, location, manager, phone));
    }

    @Operation(summary = "新增库房")
    @RequirePerm("material:warehouse:add")
    @OperLog(module = "库房信息", operation = "新增库房")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody MaterialWarehouseSaveDTO dto) {
        return Result.ok(warehouseService.add(dto));
    }

    @Operation(summary = "编辑库房")
    @RequirePerm("material:warehouse:edit")
    @OperLog(module = "库房信息", operation = "编辑库房")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody MaterialWarehouseSaveDTO dto) {
        warehouseService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除库房")
    @RequirePerm("material:warehouse:delete")
    @OperLog(module = "库房信息", operation = "删除库房")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.ok();
    }
}

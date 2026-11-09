package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.MaterialStockInSaveDTO;
import com.msjs.project.service.MaterialStockInService;
import com.msjs.project.vo.MaterialStockInDetailVO;
import com.msjs.project.vo.MaterialStockInItemVO;
import com.msjs.project.vo.MaterialStockInVO;
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
 * 入库单接口
 */
@Tag(name = "入库单")
@RestController
@RequestMapping("/api/material/stockin")
@RequiredArgsConstructor
public class MaterialStockInController {

    private final MaterialStockInService stockInService;

    @Operation(summary = "入库单分页查询（入库单页签）")
    @RequirePerm("material:stockin:list")
    @GetMapping("/page")
    public Result<PageVO<MaterialStockInVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "入库单名称") @RequestParam(required = false) String orderName,
            @Parameter(description = "经办人") @RequestParam(required = false) String handler) {
        return Result.ok(stockInService.pageOrders(pageNum, pageSize, orderName, handler));
    }

    @Operation(summary = "入库单明细分页查询（明细页签）")
    @RequirePerm("material:stockin:list")
    @GetMapping("/item/page")
    public Result<PageVO<MaterialStockInItemVO>> itemPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "物品名称") @RequestParam(required = false) String itemName,
            @Parameter(description = "库房ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "物品分类") @RequestParam(required = false) String itemCategory,
            @Parameter(description = "入库类型") @RequestParam(required = false) String inType) {
        return Result.ok(stockInService.pageItems(pageNum, pageSize, itemName, warehouseId, itemCategory, inType));
    }

    @Operation(summary = "根据明细ID集合查询（导出用）")
    @RequirePerm("material:stockin:list")
    @GetMapping("/item/list")
    public Result<List<MaterialStockInItemVO>> itemList(
            @Parameter(description = "明细ID集合") @RequestParam List<Long> ids) {
        return Result.ok(stockInService.listItemsByIds(ids));
    }

    @Operation(summary = "入库单详情")
    @RequirePerm("material:stockin:list")
    @GetMapping("/{id}")
    public Result<MaterialStockInDetailVO> detail(@PathVariable Long id) {
        return Result.ok(stockInService.detail(id));
    }

    @Operation(summary = "新增入库单")
    @RequirePerm("material:stockin:add")
    @OperLog(module = "入库单", operation = "新增入库单")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody MaterialStockInSaveDTO dto) {
        return Result.ok(stockInService.add(dto));
    }

    @Operation(summary = "编辑入库单")
    @RequirePerm("material:stockin:edit")
    @OperLog(module = "入库单", operation = "编辑入库单")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody MaterialStockInSaveDTO dto) {
        stockInService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除入库单")
    @RequirePerm("material:stockin:delete")
    @OperLog(module = "入库单", operation = "删除入库单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        stockInService.delete(id);
        return Result.ok();
    }
}

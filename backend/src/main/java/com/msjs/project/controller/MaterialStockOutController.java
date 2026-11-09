package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.MaterialStockOutSaveDTO;
import com.msjs.project.service.MaterialStockOutService;
import com.msjs.project.vo.MaterialStockOutDetailVO;
import com.msjs.project.vo.MaterialStockOutItemVO;
import com.msjs.project.vo.MaterialStockOutVO;
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
 * 出库单接口
 */
@Tag(name = "出库单")
@RestController
@RequestMapping("/api/material/stockout")
@RequiredArgsConstructor
public class MaterialStockOutController {

    private final MaterialStockOutService stockOutService;

    @Operation(summary = "出库单分页查询（出库单页签）")
    @RequirePerm("material:stockout:list")
    @GetMapping("/page")
    public Result<PageVO<MaterialStockOutVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "出库单名称") @RequestParam(required = false) String orderName,
            @Parameter(description = "经办人") @RequestParam(required = false) String handler) {
        return Result.ok(stockOutService.pageOrders(pageNum, pageSize, orderName, handler));
    }

    @Operation(summary = "出库单明细分页查询（明细页签）")
    @RequirePerm("material:stockout:list")
    @GetMapping("/item/page")
    public Result<PageVO<MaterialStockOutItemVO>> itemPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "物品名称") @RequestParam(required = false) String itemName,
            @Parameter(description = "库房ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "物品分类") @RequestParam(required = false) String itemCategory,
            @Parameter(description = "出库类型") @RequestParam(required = false) String outType) {
        return Result.ok(stockOutService.pageItems(pageNum, pageSize, itemName, warehouseId, itemCategory, outType));
    }

    @Operation(summary = "根据明细ID集合查询（导出用）")
    @RequirePerm("material:stockout:list")
    @GetMapping("/item/list")
    public Result<List<MaterialStockOutItemVO>> itemList(
            @Parameter(description = "明细ID集合") @RequestParam List<Long> ids) {
        return Result.ok(stockOutService.listItemsByIds(ids));
    }

    @Operation(summary = "出库单详情")
    @RequirePerm("material:stockout:list")
    @GetMapping("/{id}")
    public Result<MaterialStockOutDetailVO> detail(@PathVariable Long id) {
        return Result.ok(stockOutService.detail(id));
    }

    @Operation(summary = "新增出库单")
    @RequirePerm("material:stockout:add")
    @OperLog(module = "出库单", operation = "新增出库单")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody MaterialStockOutSaveDTO dto) {
        return Result.ok(stockOutService.add(dto));
    }

    @Operation(summary = "编辑出库单")
    @RequirePerm("material:stockout:edit")
    @OperLog(module = "出库单", operation = "编辑出库单")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody MaterialStockOutSaveDTO dto) {
        stockOutService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除出库单")
    @RequirePerm("material:stockout:delete")
    @OperLog(module = "出库单", operation = "删除出库单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        stockOutService.delete(id);
        return Result.ok();
    }
}

package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.MaterialDamageSaveDTO;
import com.msjs.project.dto.MaterialHandoverSaveDTO;
import com.msjs.project.dto.MaterialInventorySaveDTO;
import com.msjs.project.service.MaterialReportService;
import com.msjs.project.vo.MaterialReportDetailVO;
import com.msjs.project.vo.MaterialReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 物资统计报表接口
 * 支持三种报表类型：盘点表(INVENTORY)、交接表(HANDOVER)、损毁表(DAMAGE)
 */
@Tag(name = "物资统计报表")
@RestController
@RequestMapping("/api/material/report")
@RequiredArgsConstructor
public class MaterialReportController {

    private final MaterialReportService reportService;

    @Operation(summary = "统计报分页查询")
    @RequirePerm("material:report:list")
    @GetMapping("/page")
    public Result<PageVO<MaterialReportVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "报表名称") @RequestParam(required = false) String reportName,
            @Parameter(description = "报表类型：INVENTORY/HANDOVER/DAMAGE") @RequestParam(required = false) String reportType,
            @Parameter(description = "开始日期") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startTime,
            @Parameter(description = "结束日期") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endTime) {
        return Result.ok(reportService.page(pageNum, pageSize, reportName, reportType, startTime, endTime));
    }

    @Operation(summary = "统计报表详情")
    @RequirePerm("material:report:list")
    @GetMapping("/{id}")
    public Result<MaterialReportDetailVO> detail(@PathVariable Long id) {
        return Result.ok(reportService.detail(id));
    }

    // ==================== 盘点表 ====================

    @Operation(summary = "新增盘点表")
    @RequirePerm("material:report:add")
    @OperLog(module = "盘点表", operation = "新增盘点表")
    @PostMapping("/inventory")
    public Result<Long> addInventory(@Valid @RequestBody MaterialInventorySaveDTO dto) {
        return Result.ok(reportService.addInventory(dto));
    }

    @Operation(summary = "编辑盘点表")
    @RequirePerm("material:report:edit")
    @OperLog(module = "盘点表", operation = "编辑盘点表")
    @PutMapping("/inventory")
    public Result<Void> editInventory(@Valid @RequestBody MaterialInventorySaveDTO dto) {
        reportService.editInventory(dto);
        return Result.ok();
    }

    // ==================== 交接表 ====================

    @Operation(summary = "新增交接表")
    @RequirePerm("material:report:add")
    @OperLog(module = "交接表", operation = "新增交接表")
    @PostMapping("/handover")
    public Result<Long> addHandover(@Valid @RequestBody MaterialHandoverSaveDTO dto) {
        return Result.ok(reportService.addHandover(dto));
    }

    @Operation(summary = "编辑交接表")
    @RequirePerm("material:report:edit")
    @OperLog(module = "交接表", operation = "编辑交接表")
    @PutMapping("/handover")
    public Result<Void> editHandover(@Valid @RequestBody MaterialHandoverSaveDTO dto) {
        reportService.editHandover(dto);
        return Result.ok();
    }

    // ==================== 损毁表 ====================

    @Operation(summary = "新增损毁表")
    @RequirePerm("material:report:add")
    @OperLog(module = "损毁表", operation = "新增损毁表")
    @PostMapping("/damage")
    public Result<Long> addDamage(@Valid @RequestBody MaterialDamageSaveDTO dto) {
        return Result.ok(reportService.addDamage(dto));
    }

    @Operation(summary = "编辑损毁表")
    @RequirePerm("material:report:edit")
    @OperLog(module = "损毁表", operation = "编辑损毁表")
    @PutMapping("/damage")
    public Result<Void> editDamage(@Valid @RequestBody MaterialDamageSaveDTO dto) {
        reportService.editDamage(dto);
        return Result.ok();
    }

    // ==================== 通用 ====================

    @Operation(summary = "删除统计报表")
    @RequirePerm("material:report:delete")
    @OperLog(module = "统计报表", operation = "删除统计报表")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return Result.ok();
    }
}

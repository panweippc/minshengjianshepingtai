package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.entity.SysOperLog;
import com.msjs.project.service.SysOperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作日志接口（查阅、追溯）
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/system/log")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysOperLogService operLogService;

    @Operation(summary = "日志分页查询")
    @RequirePerm("system:log:list")
    @GetMapping("/page")
    public Result<PageVO<SysOperLog>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "操作人账号") @RequestParam(required = false) String username,
            @Parameter(description = "所属模块") @RequestParam(required = false) String module) {
        return Result.ok(operLogService.page(pageNum, pageSize, username, module));
    }

    @Operation(summary = "批量删除日志")
    @RequirePerm("system:log:delete")
    @OperLog(module = "操作日志", operation = "批量删除日志")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        operLogService.deleteBatch(ids);
        return Result.ok();
    }

    @Operation(summary = "清空日志")
    @RequirePerm("system:log:delete")
    @OperLog(module = "操作日志", operation = "清空日志")
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        operLogService.clear();
        return Result.ok();
    }
}

package com.msjs.project.controller;

import com.msjs.project.common.Result;
import com.msjs.project.service.DashboardService;
import com.msjs.project.vo.DashboardOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页概览接口
 */
@Tag(name = "首页概览")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "首页概览统计（行政村、农户、公共基础设施、在库物资种类）")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.ok(dashboardService.overview());
    }
}

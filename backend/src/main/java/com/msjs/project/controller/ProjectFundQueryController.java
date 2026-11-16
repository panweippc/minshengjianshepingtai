package com.msjs.project.controller;

import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.service.ProjectFundQueryService;
import com.msjs.project.vo.FundDetailRowVO;
import com.msjs.project.vo.FundStatVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 综合查询 - 资金记录查询
 *
 * <p>和资金管理 (ProjectFundController) 写接口隔离，三个核心接口：</p>
 * <ol>
 *   <li>{@link #stat}        主统计表格（返回汇总 + 统计行）</li>
 *   <li>{@link #detailList}  分组明细流水（弹窗内用）</li>
 *   <li>{@link #export}      CSV 导出</li>
 * </ol>
 */
@Tag(name = "综合查询-资金记录查询")
@RestController
@RequestMapping("/api/project/fund/query")
@RequiredArgsConstructor
public class ProjectFundQueryController {

    private final ProjectFundQueryService fundQueryService;

    @Operation(summary = "主统计汇总（筛选后全量聚合，不做分页）")
    @RequirePerm("project:fund:query")
    @GetMapping("/stat")
    public Result<FundStatVO> stat(
            @Parameter(description = "年度（空=全部）")
            @RequestParam(required = false) String year,
            @Parameter(description = "区划 code（末级，递归包含后代）")
            @RequestParam(required = false) String regionCode,
            @Parameter(description = "分组维度：fundType(默认) / fundSource / nodeLevel / village")
            @RequestParam(required = false) String groupType,
            @Parameter(description = "分组值（选填：传了则 statList 只返回这一个分组，globalSummary 不受影响）")
            @RequestParam(required = false) String groupValue) {
        return Result.ok(fundQueryService.stat(year, regionCode, groupType, groupValue));
    }

    @Operation(summary = "分组明细流水（弹窗内一行一条扁平资金记录）")
    @RequirePerm("project:fund:query")
    @GetMapping("/detailList")
    public Result<List<FundDetailRowVO>> detailList(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String groupType,
            @RequestParam(required = false) String groupValue) {
        return Result.ok(fundQueryService.detailList(year, regionCode, groupType, groupValue));
    }

    @Operation(summary = "导出当前筛选 + 当前分组下的全部明细到 CSV")
    @RequirePerm("project:fund:export")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String groupType,
            @RequestParam(required = false) String groupValue,
            HttpServletResponse response) throws Exception {
        byte[] data = fundQueryService.exportCsv(year, regionCode, groupType, groupValue);
        String fn = "资金记录查询_" + System.currentTimeMillis() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(fn, StandardCharsets.UTF_8));
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }
}

package com.msjs.project.controller;

import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.ProjectCycleQueryDTO;
import com.msjs.project.service.ProjectCycleQueryService;
import com.msjs.project.vo.ProjectCycleDetailVO;
import com.msjs.project.vo.ProjectCycleStatVO;
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
 * 综合查询 - 项目全周期查询
 *
 * <p>5套统计方案（queryType 切换）：
 * <ol>
 *   <li>category   - 按项目类别统计</li>
 *   <li>source     - 按项目来源统计</li>
 *   <li>establish  - 按立项状态统计</li>
 *   <li>status     - 按项目状态统计</li>
 *   <li>village    - 按所属村组统计</li>
 * </ol>
 */
@Tag(name = "综合查询-项目全周期")
@RestController
@RequestMapping("/api/project/cycle/query")
@RequiredArgsConstructor
public class ProjectCycleQueryController {

    private final ProjectCycleQueryService cycleQueryService;

    @Operation(summary = "主统计汇总（5套方案通用）")
    @RequirePerm("project:cycle:query")
    @GetMapping("/stat")
    public Result<ProjectCycleStatVO> stat(
            @Parameter(description = "年度（空=全部）")
            @RequestParam(required = false) String year,
            @Parameter(description = "区划 code（末级，递归包含后代）")
            @RequestParam(required = false) String regionCode,
            @Parameter(description = "查询方案：category/source/establish/status/village，默认 category")
            @RequestParam(required = false) String queryType) {

        ProjectCycleQueryDTO dto = new ProjectCycleQueryDTO();
        dto.setYear(year);
        dto.setRegionCode(regionCode);
        dto.setQueryType(queryType);
        return Result.ok(cycleQueryService.stat(dto));
    }

    @Operation(summary = "查看明细（按统计行核对原始数据）")
    @RequirePerm("project:cycle:query")
    @GetMapping("/detail")
    public Result<List<ProjectCycleDetailVO>> detail(
            @Parameter(description = "年度（空=全部）")
            @RequestParam(required = false) String year,
            @Parameter(description = "区划 code（末级，递归包含后代）")
            @RequestParam(required = false) String regionCode,
            @Parameter(description = "查询方案：category/source/establish/status/village")
            @RequestParam(required = false) String queryType,
            @Parameter(description = "分组值（统计行 groupValue）")
            @RequestParam(required = false) String groupValue,
            @Parameter(description = "村组名称（统计行 villageName，全部村组/全部区划=不限制）")
            @RequestParam(required = false) String villageName) {

        ProjectCycleQueryDTO dto = new ProjectCycleQueryDTO();
        dto.setYear(year);
        dto.setRegionCode(regionCode);
        dto.setQueryType(queryType);
        dto.setGroupValue(groupValue);
        dto.setVillageName(villageName);
        return Result.ok(cycleQueryService.detail(dto));
    }

    @Operation(summary = "导出当前筛选 + 当前方案下的全部统计到 Excel")
    @RequirePerm("project:cycle:export")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String queryType,
            HttpServletResponse response) throws Exception {

        ProjectCycleQueryDTO dto = new ProjectCycleQueryDTO();
        dto.setYear(year);
        dto.setRegionCode(regionCode);
        dto.setQueryType(queryType);

        byte[] data = cycleQueryService.exportExcel(dto);
        String fn = "项目全周期统计_" + System.currentTimeMillis() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(fn, StandardCharsets.UTF_8));
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }
}

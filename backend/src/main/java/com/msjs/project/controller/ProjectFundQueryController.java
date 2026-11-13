package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.service.ProjectFundQueryService;
import com.msjs.project.vo.FundQueryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 综合查询 - 资金记录查询 Controller（只读）
 * 与项目全周期管控 → 资金管理 互补：资金管理按"项目→申报→明细"树形展示每个节点的累计拨付；
 * 本模块按"单条资金流水"维度扁平展示所有登记/下拨记录，支持多维交叉筛选、汇总统计、CSV 导出。
 */
@Tag(name = "综合查询-资金记录查询")
@RestController
@RequestMapping("/api/project/fund/query")
@RequiredArgsConstructor
public class ProjectFundQueryController {

    private final ProjectFundQueryService fundQueryService;

    /**
     * 资金记录分页查询：一次返回 records（分页）+ summary（顶部汇总卡）+ sidebar（左侧筛选面板统计）。
     * Service 在一次调用中完成全量命中筛选、字典翻译、附件数回填、summary/sidebar 内存聚合、分页切片。
     */
    @Operation(summary = "资金记录分页查询（含汇总卡 + 左侧面板统计）")
    @RequirePerm("project:fund:query")
    @GetMapping("/page")
    public Result<FundQueryPageResult> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "项目名称（模糊）") @RequestParam(required = false) String projectName,
            @Parameter(description = "项目编号（模糊）") @RequestParam(required = false) String approvalCode,
            @Parameter(description = "申报编号（模糊）") @RequestParam(required = false) String declareCode,
            @Parameter(description = "明细名称（农户姓名 / 设施名称，模糊）") @RequestParam(required = false) String detailName,
            @Parameter(description = "节点层级：1资金登记 / 2二级下拨 / 3三级下拨") @RequestParam(required = false) Integer nodeLevel,
            @Parameter(description = "资金类型字典值（nodeLevel=1 取 zi_jin_lei_xing，2/3 取 xia_bo_lei_xing）")
            @RequestParam(required = false) String fundType,
            @Parameter(description = "资金来源字典值（zi_jin_lai_yuan）") @RequestParam(required = false) String fundSource,
            @Parameter(description = "区划编码（乡镇或村组，自动包含全部子孙级）") @RequestParam(required = false) String regionCode,
            @Parameter(description = "经办时间起（yyyy-MM-dd HH:mm:ss，闭区间左）") @RequestParam(required = false) String startTime,
            @Parameter(description = "经办时间止（yyyy-MM-dd HH:mm:ss，闭区间右）") @RequestParam(required = false) String endTime,
            @Parameter(description = "经办人（精确）") @RequestParam(required = false) String createBy,
            @Parameter(description = "金额区间下限（元）") @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "金额区间上限（元）") @RequestParam(required = false) BigDecimal maxAmount
    ) {
        PageVO<FundQueryVO.Record> page = fundQueryService.page(
                pageNum, pageSize,
                projectName, approvalCode, declareCode, detailName,
                nodeLevel, fundType, fundSource,
                regionCode, startTime, endTime,
                createBy, minAmount, maxAmount
        );
        FundQueryPageResult result = new FundQueryPageResult();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getPageNum());
        result.setPageSize(page.getPageSize());
        result.setSummary(fundQueryService.lastSummary());
        result.setSidebar(fundQueryService.lastSidebar());
        return Result.ok(result);
    }

    /**
     * 全量 CSV 导出（UTF-8 BOM，Excel 直接双击识别中文）。
     * 不分页，筛选条件与 /page 完全一致。导出上限由后端控制（Service 层自然处理）。
     */
    @Operation(summary = "资金记录 CSV 导出（UTF-8 BOM）")
    @RequirePerm("project:fund:export")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "项目名称") @RequestParam(required = false) String projectName,
            @Parameter(description = "项目编号") @RequestParam(required = false) String approvalCode,
            @Parameter(description = "申报编号") @RequestParam(required = false) String declareCode,
            @Parameter(description = "明细名称") @RequestParam(required = false) String detailName,
            @Parameter(description = "节点层级") @RequestParam(required = false) Integer nodeLevel,
            @Parameter(description = "资金类型") @RequestParam(required = false) String fundType,
            @Parameter(description = "资金来源") @RequestParam(required = false) String fundSource,
            @Parameter(description = "区划编码") @RequestParam(required = false) String regionCode,
            @Parameter(description = "经办时间起") @RequestParam(required = false) String startTime,
            @Parameter(description = "经办时间止") @RequestParam(required = false) String endTime,
            @Parameter(description = "经办人") @RequestParam(required = false) String createBy,
            @Parameter(description = "金额下限") @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "金额上限") @RequestParam(required = false) BigDecimal maxAmount
    ) {
        byte[] bytes = fundQueryService.export(
                projectName, approvalCode, declareCode, detailName,
                nodeLevel, fundType, fundSource,
                regionCode, startTime, endTime,
                createBy, minAmount, maxAmount
        );
        String fileName = "资金记录查询_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=utf-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        return new ResponseEntity<>(bytes, headers, 200);
    }

    /**
     * page 接口返回体：在 PageVO<Record> 基础上携带 summary + sidebar，
     * 避免前端为同一筛选重复请求三次（records / summary / sidebar）。
     */
    @lombok.Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "资金记录分页结果（含汇总卡 + 左侧面板统计）")
    public static class FundQueryPageResult {
        @io.swagger.v3.oas.annotations.media.Schema(description = "数据列表")
        private java.util.List<FundQueryVO.Record> records;

        @io.swagger.v3.oas.annotations.media.Schema(description = "总条数")
        private Long total;

        @io.swagger.v3.oas.annotations.media.Schema(description = "当前页码")
        private Long pageNum;

        @io.swagger.v3.oas.annotations.media.Schema(description = "每页条数")
        private Long pageSize;

        @io.swagger.v3.oas.annotations.media.Schema(description = "顶部汇总卡统计")
        private FundQueryVO.Summary summary;

        @io.swagger.v3.oas.annotations.media.Schema(description = "左侧筛选面板统计")
        private FundQueryVO.Sidebar sidebar;
    }
}

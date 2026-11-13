package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.vo.FundQueryVO;

import java.math.BigDecimal;

/**
 * 资金记录查询服务（综合查询模块，只读）
 */
public interface ProjectFundQueryService {

    /**
     * 资金流水分页查询（一次返回 records + summary + sidebar）
     */
    PageVO<FundQueryVO.Record> page(
            long pageNum, long pageSize,
            String projectName, String approvalCode, String declareCode, String detailName,
            Integer nodeLevel, String fundType, String fundSource,
            String regionCode,
            String startTime, String endTime,
            String createBy, BigDecimal minAmount, BigDecimal maxAmount
    );

    /** 查询结果附带的顶部汇总卡统计 */
    FundQueryVO.Summary lastSummary();

    /** 查询结果附带的左侧筛选面板统计 */
    FundQueryVO.Sidebar lastSidebar();

    /**
     * 全量导出（CSV，UTF-8 BOM），不分页，走同一套筛选条件
     */
    byte[] export(
            String projectName, String approvalCode, String declareCode, String detailName,
            Integer nodeLevel, String fundType, String fundSource,
            String regionCode,
            String startTime, String endTime,
            String createBy, BigDecimal minAmount, BigDecimal maxAmount
    );
}

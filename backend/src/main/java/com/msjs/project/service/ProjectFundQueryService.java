package com.msjs.project.service;

import com.msjs.project.vo.FundDetailRowVO;
import com.msjs.project.vo.FundStatVO;

import java.util.List;

/**
 * 资金记录查询 Service —— 只读统计查询
 */
public interface ProjectFundQueryService {

    /** 主接口：返回全局汇总 + 主统计表格（按 groupType 分组聚合）；groupValue 传了则 statList 只留该行 */
    FundStatVO stat(String year, String regionCode, String groupType, String groupValue);

    /** 分组明细流水（弹窗内展示） */
    List<FundDetailRowVO> detailList(String year, String regionCode,
                                      String groupType, String groupValue);

    /** 导出 CSV（UTF-8 BOM），上限 Constants.MAX_EXPORT_ROWS */
    byte[] exportCsv(String year, String regionCode,
                     String groupType, String groupValue);
}

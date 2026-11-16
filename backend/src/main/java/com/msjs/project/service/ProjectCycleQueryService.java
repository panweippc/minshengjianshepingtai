package com.msjs.project.service;

import com.msjs.project.dto.ProjectCycleQueryDTO;
import com.msjs.project.vo.ProjectCycleStatVO;

/**
 * 综合查询-项目全周期 Service
 */
public interface ProjectCycleQueryService {

    /**
     * 主统计汇总（筛选后全量聚合，不做分页）
     */
    ProjectCycleStatVO stat(ProjectCycleQueryDTO query);

    /**
     * 导出当前筛选 + 当前方案下的全部统计到 Excel
     */
    byte[] exportExcel(ProjectCycleQueryDTO query);
}

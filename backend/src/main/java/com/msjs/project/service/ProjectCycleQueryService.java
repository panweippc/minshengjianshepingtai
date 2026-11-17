package com.msjs.project.service;

import com.msjs.project.dto.ProjectCycleQueryDTO;
import com.msjs.project.vo.ProjectCycleDetailVO;
import com.msjs.project.vo.ProjectCycleStatVO;

import java.util.List;

/**
 * 综合查询-项目全周期 Service
 */
public interface ProjectCycleQueryService {

    /**
     * 主统计汇总（筛选后全量聚合，不做分页）
     */
    ProjectCycleStatVO stat(ProjectCycleQueryDTO query);

    /**
     * 查看明细：按当前筛选 + 统计行（分组值/村组）返回组成该行的原始记录
     */
    List<ProjectCycleDetailVO> detail(ProjectCycleQueryDTO query);

    /**
     * 导出当前筛选 + 当前方案下的全部统计到 Excel
     */
    byte[] exportExcel(ProjectCycleQueryDTO query);
}

package com.msjs.project.service;

import com.msjs.project.vo.ProjectLifecycleVO;

import java.util.List;

/**
 * 农户 / 公共基础设施关联的项目全周期明细查询服务
 * 仅返回明细级（单个农户 / 单处设施）在各阶段的记录，不做项目级汇总
 */
public interface ProjectLifecycleService {

    /** 查询农户关联的项目全周期明细（申报 → 立项 → 施工 → 资金 → 验收） */
    List<ProjectLifecycleVO> listByHousehold(Long householdId);

    /** 查询公共基础设施关联的项目全周期明细（申报 → 立项 → 施工 → 资金 → 验收） */
    List<ProjectLifecycleVO> listByFacility(Long facilityId);
}
